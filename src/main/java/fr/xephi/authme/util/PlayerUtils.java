package fr.xephi.authme.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Player utilities.
 */
public final class PlayerUtils {

    // Utility class
    private PlayerUtils() {
    }
    private static final boolean IS_LEAVES_SERVER = Utils.isClassLoaded("top.leavesmc.leaves.LeavesConfig");

    /**
     * Returns the IP of the given player.
     *
     * @param player The player to return the IP address for
     * @return The player's IP address
     */
    public static String getPlayerIp(Player player) {
        return player.getAddress().getAddress().getHostAddress();
    }

    /**
     * Returns if the player is an NPC or not.
     *
     * @param player The player to check
     * @return True if the player is an NPC, false otherwise
     */
    public static boolean isNpc(Player player) {
        if (IS_LEAVES_SERVER) {
            return player.hasMetadata("NPC") || player.getAddress() == null;
        } else {
            return player.hasMetadata("NPC");
        }
    }

    /**
     * Returns the locale that player uses.
     *
     * @param sender The player
     */
    public static String getLocale(CommandSender sender) {
        String locale = null;

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (Utils.majorVersion >= 12) {
                locale = player.getLocale();
            } else {
                try {
                    Method spigotMethod = player.getClass().getMethod("spigot");
                    Object spigot = spigotMethod.invoke(player);

                    Method spigotGetLocaleMethod = spigot.getClass().getMethod("getLocale");
                    locale = (String) spigotGetLocaleMethod.invoke(spigot);
                } catch (Exception ignored) {
                }
            }
        }

        return locale;
    }
}
