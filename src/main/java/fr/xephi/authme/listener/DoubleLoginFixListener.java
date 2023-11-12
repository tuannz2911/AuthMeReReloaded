package fr.xephi.authme.listener;
//Prevent Ghost Players

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collection;
import java.util.HashSet;

public class DoubleLoginFixListener implements Listener {

    public DoubleLoginFixListener() {
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
