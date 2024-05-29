package fr.xephi.authme.util;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
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

    /**
     * Returns the AuthMe messages file language code, by given locale and settings.
     * Dreeam - Hard mapping, based on mc1.20.6, 5/29/2024
     *
     * @param locale The locale that player client setting uses.
     * @param settings The AuthMe settings, for default/fallback language usage.
     */
    public static String LocaleToCode(String locale, Settings settings) {
        locale = locale.toLowerCase();

        switch (locale) {
            case "pt_br":
                return "br";
            case "cs_cz":
                return "cz";
            case "lzh":
            case "zh_cn":
                return "zhcn";
            case "zh_hk":
                return "zhhk";
            case "zh_tw":
                return "zhtw";
        }

        if (locale.contains("_")) {
            locale = locale.substring(0, locale.indexOf("_"));
        }

        switch (locale) {
            case "en":
                return "en";
            case "bg":
                return "bg";
            case "de":
            case "nds":
            case "sxu":
            case "swg":
                return "de";
            case "eo":
                return "eo";
            case "es":
                return "es";
            case "et":
                return "et";
            case "eu":
                return "eu";
            case "fi":
                return "fi";
            case "fr":
                return "fr";
            case "gl":
                return "gl";
            case "hu":
                return "hu";
            case "id":
                return "id";
            case "it":
                return "it";
            case "ja":
                return "ja";
            case "ko":
                return "ko";
            case "lt":
                return "lt";
            case "nl":
                return "nl";
            case "pl":
                return "pl";
            case "pt":
                return "pt";
            case "ro":
                return "ro";
            case "ru":
            case "rpr":
                return "ru";
            case "sl":
                return "si";
            case "sk":
                return "sk";
            case "sr":
                return "sr";
            case "tr":
                return "tr";
            case "uk":
                return "uk";
            case "vi":
                return "vn";
            case "lzh":
                return "zhcn";
            //case "zhmc":
            //    return "zhmc";
            default:
                return settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
        }
    }
}
