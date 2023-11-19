package fr.xephi.authme.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
public class TeleportUtils {
    private static Method teleportAsyncMethod;

    static {
        try {//Detect Paper class
            Class<?> paperClass = Class.forName("com.destroystokyo.paper.PaperConfig");
            teleportAsyncMethod = Player.class.getMethod("teleportAsync", Location.class);
            teleportAsyncMethod.setAccessible(true);
            // if detected,use teleportAsync()
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            teleportAsyncMethod = null;
            //if not, set method to null
        }
    }

    public static void teleport(Player player, Location location) {
        if (teleportAsyncMethod != null) {
            try {
                teleportAsyncMethod.invoke(player, location);
            } catch (IllegalAccessException | InvocationTargetException e) {
                player.teleport(location);
            }
        } else {
            player.teleport(location);
        }
    }
}
