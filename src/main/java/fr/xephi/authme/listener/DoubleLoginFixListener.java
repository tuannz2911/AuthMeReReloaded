package fr.xephi.authme.listener;
//Prevent Ghost Players

import fr.xephi.authme.AuthMe;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashSet;

public class DoubleLoginFixListener implements Listener {
    private final Plugin plugin;



    public DoubleLoginFixListener(Plugin plugin) {
        this.plugin = plugin;

    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Collection<? extends Player> PlayerList = Bukkit.getServer().getOnlinePlayers();
        HashSet<String> PlayerSet = new HashSet<String>();
        for (Player ep : PlayerList) {
            if (PlayerSet.contains(ep.getName().toLowerCase())) {
                ep.kickPlayer("You have been disconnected due to doubled login.");
                break;
            }
            PlayerSet.add(ep.getName().toLowerCase());
        }
    }
}
//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent event) {
//        Player player = event.getPlayer();
//        String LastAddr = Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress();
//        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
//            @Override
//            public void run() {
//                if (player.isOnline()) {
//                    player.kickPlayer("You have been disconnected.");
//                }
//            }
//        }, 20L); // 20 ticks = 1 second
//    }
//}
