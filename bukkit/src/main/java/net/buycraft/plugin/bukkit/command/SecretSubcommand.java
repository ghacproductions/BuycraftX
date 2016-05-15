package net.buycraft.plugin.bukkit.command;

import lombok.RequiredArgsConstructor;
import net.buycraft.plugin.bukkit.BuycraftPlugin;
import net.buycraft.plugin.client.ApiClient;
import net.buycraft.plugin.client.ApiException;
import net.buycraft.plugin.client.ProductionApiClient;
import net.buycraft.plugin.data.responses.ServerInformation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;

@RequiredArgsConstructor
public class SecretSubcommand implements Subcommand {
    private final BuycraftPlugin plugin;

    @Override
    public void execute(final CommandSender sender, final String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + plugin.getI18n().get("secret_console_only"));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + plugin.getI18n().get("secret_need_key"));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                ApiClient client = new ProductionApiClient(args[0], plugin.getHttpClient());
                try {
                    plugin.updateInformation(client);
                } catch (IOException | ApiException e) {
                    sender.sendMessage(ChatColor.RED + plugin.getI18n().get("secret_does_not_work"));
                    return;
                }

                ServerInformation information = plugin.getServerInformation();
                plugin.setApiClient(client);
                plugin.getListingUpdateTask().run();
                plugin.getConfiguration().setServerKey(args[0]);
                try {
                    plugin.saveConfiguration();
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + plugin.getI18n().get("secret_cant_be_saved"));
                }

                sender.sendMessage(ChatColor.GREEN + plugin.getI18n().get("secret_success",
                        information.getServer().getName(), information.getAccount().getName()));

                Bukkit.getScheduler().runTaskAsynchronously(plugin, plugin.getDuePlayerFetcher());
            }
        });
    }

    @Override
    public String getDescription() {
        return plugin.getI18n().get("usage_secret");
    }
}
