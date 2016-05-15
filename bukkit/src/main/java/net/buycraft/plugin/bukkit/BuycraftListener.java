package net.buycraft.plugin.bukkit;

import lombok.RequiredArgsConstructor;
import net.buycraft.plugin.data.QueuedPlayer;
import net.buycraft.plugin.execution.PlayerCommandExecutor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class BuycraftListener implements Listener {
    private final BuycraftPlugin plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getApiClient() == null) {
            return;
        }

        QueuedPlayer qp = plugin.getDuePlayerFetcher().fetchAndRemoveDuePlayer(event.getPlayer().getName());
        if (qp != null) {
            plugin.getLogger().info(String.format("Executing login commands for %s...", event.getPlayer().getName()));
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new PlayerCommandExecutor(qp, plugin.getPlatform()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfiguration().isDisableBuyCommand()) {

            for (String s : plugin.getConfiguration().getBuyCommandName()) {
                if (StringUtils.equalsIgnoreCase(event.getMessage().substring(1), s)/* ||
                        StringUtils.startsWithIgnoreCase(event.getMessage().substring(1), s + " ")*/) {
                    plugin.getViewCategoriesGUI().open(event.getPlayer());
                    event.setCancelled(true);
                }
            }
        }
    }
}
