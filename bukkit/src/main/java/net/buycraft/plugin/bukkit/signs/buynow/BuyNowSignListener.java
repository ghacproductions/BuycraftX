package net.buycraft.plugin.bukkit.signs.buynow;

import lombok.Getter;
import net.buycraft.plugin.bukkit.BuycraftPlugin;
import net.buycraft.plugin.bukkit.tasks.SendCheckoutLink;
import net.buycraft.plugin.bukkit.util.SerializedBlockLocation;
import net.buycraft.plugin.data.Package;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class BuyNowSignListener implements Listener {
    @Getter
    private final Map<UUID, SerializedBlockLocation> settingUpSigns = new HashMap<>();
    private final BuycraftPlugin plugin;
    private boolean signLimited = false;

    public BuyNowSignListener(BuycraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        boolean relevant;
        try {
            String t = event.getLine(0);
            if (t.equals(ChatColor.BLUE + "[Buycraft]"))
                event.setLine(0, "[Buycraft]");
            relevant = t.equalsIgnoreCase("[buycraft_buy]");
        } catch (IndexOutOfBoundsException e) {
            return;
        }

        if (!relevant)
            return;

        for (int i = 0; i < 4; i++) {
            event.setLine(i, "");
        }

        settingUpSigns.put(event.getPlayer().getUniqueId(), SerializedBlockLocation.fromBukkitLocation(event.getBlock().getLocation()));
        event.getPlayer().sendMessage(ChatColor.GREEN + "Navigate to the item you want to set this sign for.");

        plugin.getViewCategoriesGUI().open(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block b = event.getClickedBlock();

            if (!(b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST))
                return;

            Sign sign = (Sign) b.getState();

            try {
                if (!sign.getLine(0).equals(ChatColor.BLUE + "[Buycraft]"))
                    return;
            } catch (IndexOutOfBoundsException e) {
                return;
            }

            // Signs are rate limited in order to limit API calls issued.
            if (signLimited) {
                return;
            }
            signLimited = true;

            Bukkit.getScheduler().runTaskAsynchronously(plugin, new SendCheckoutLink(plugin, Integer.parseInt(sign.getLine(2)),
                    event.getPlayer()));
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    signLimited = false;
                }
            }, 4);
        }
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (settingUpSigns.containsKey(event.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if ((event.getPlayer().getOpenInventory().getTopInventory() == null ||
                            !event.getPlayer().getOpenInventory().getTopInventory().getTitle().startsWith("Buycraft: ")) &&
                            settingUpSigns.remove(event.getPlayer().getUniqueId()) != null) {
                        event.getPlayer().sendMessage(ChatColor.RED + "Buy sign set up cancelled.");
                    }
                }
            }, 3);
        }
    }

    public void doSignSetup(Player player, Package p) {
        SerializedBlockLocation sbl = settingUpSigns.remove(player.getUniqueId());
        if (sbl == null)
            return;

        Block b = sbl.toBukkitLocation().getBlock();

        if (!(b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST))
            return;

        Sign sign = (Sign) b.getState();
        sign.setLine(0, ChatColor.BLUE + "[Buycraft]");
        sign.setLine(1, StringUtils.abbreviate(p.getName(), 16));
        sign.setLine(2, Integer.toString(p.getId()));
        sign.setLine(3, "");
        sign.update();
    }
}
