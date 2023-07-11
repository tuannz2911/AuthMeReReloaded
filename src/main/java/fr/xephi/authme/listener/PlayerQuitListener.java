package fr.xephi.authme.listener;
//Prevent Ghost Players

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class PlayerQuitListener implements Listener {
    private final Plugin plugin;
    public PlayerQuitListener(Plugin plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String LastAddr = Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.kickPlayer("You have been disconnected.");
                }
            }
        }, 20L); // 20 ticks = 1 second
    }
}
