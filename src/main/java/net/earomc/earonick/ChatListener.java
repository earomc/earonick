package net.earomc.earonick;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @author tiiita_
 * Created on Dezember 11, 2022 | 13:10:09
 * (●'◡'●)
 */
public class ChatListener implements Listener {
    private final EaroNick plugin;

    public ChatListener(EaroNick plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(PlayerChatEvent event) {

        Player player = event.getPlayer();
        NickManager nickManager = plugin.getNickManager();
        FileConfiguration config = plugin.getConfig();
        if (!nickManager.isNicked(player)) {
            return;
        }

        event.setFormat(config.getString("nick-chat-format")
                .replaceAll("&", "§")
                .replaceAll("%nickname%", nickManager.getNickName(player))
                .replaceAll("%message%", event.getMessage()));
    }
}