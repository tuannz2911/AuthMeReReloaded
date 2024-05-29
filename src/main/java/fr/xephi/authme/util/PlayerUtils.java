package fr.xephi.authme.util;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Player utilities.
 */
public final class PlayerUtils {

    // Utility class
    private PlayerUtils() {
    }

    private static final boolean IS_LEAVES_SERVER = Utils.isClassLoaded("top.leavesmc.leaves.LeavesConfig");
    private static final List<String> LOCALE_LIST = Arrays.asList(
        "en", "bg", "de", "eo", "es", "et", "eu", "fi", "fr", "gl", "hu", "id", "it", "ja", "ko", "lt", "nl", "pl",
        "pt", "ro", "ru", "sk", "sr", "tr", "uk"
    );

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
                locale = player.getLocale().toLowerCase();
            } else {
                try {
                    Method spigotMethod = player.getClass().getMethod("spigot");
                    Object spigot = spigotMethod.invoke(player);

                    Method spigotGetLocaleMethod = spigot.getClass().getMethod("getLocale");
                    locale = ((String) spigotGetLocaleMethod.invoke(spigot)).toLowerCase();
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
     * @param locale   The locale that player client setting uses.
     * @param settings The AuthMe settings, for default/fallback language usage.
     */
    public static String LocaleToCode(String locale, Settings settings) {
        // Certain locale code to AuthMe language code redirect
        if (!settings.getProperty(PluginSettings.I18N_CODE_REDIRECT).isEmpty()) {
            for (String raw : settings.getProperty(PluginSettings.I18N_CODE_REDIRECT)) {
                String[] split = raw.split(":");

                if (locale.equalsIgnoreCase(split[0])) {
                    return split[1];
                }
            }
        }

        // Match certain locale code
        switch (locale) {
            case "pt_br":
                return "br";
            case "cs_cz":
                return "cz";
            case "nds_de":
            case "sxu":
            case "swg":
                return "de";
            case "rpr":
                return "ru";
            case "sl_si":
                return "si";
            case "vi_vn":
                return "vn";
            case "lzh":
            case "zh_cn":
                return "zhcn";
            case "zh_hk":
                return "zhhk";
            case "zh_tw":
                return "zhtw";
            //case "zhmc":
            //    return "zhmc";
        }

        if (locale.contains("_")) {
            locale = locale.substring(0, locale.indexOf("_"));
        }

        // Match locale code with "_"
        if (LOCALE_LIST.contains(locale)) {
            return locale;
        }

        return settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
    }
}
