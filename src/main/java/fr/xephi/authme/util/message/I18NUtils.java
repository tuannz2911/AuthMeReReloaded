package fr.xephi.authme.util.message;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class I18NUtils {

    private static Method spigotMethod;
    private static final List<String> LOCALE_LIST = Arrays.asList(
        "en", "bg", "de", "eo", "es", "et", "eu", "fi", "fr", "gl", "hu", "id", "it", "ja", "ko", "lt", "nl", "pl",
        "pt", "ro", "ru", "sk", "sr", "tr", "uk"
    );

    static {
        try {
            spigotMethod = Player.class.getMethod("spigot");
        } catch (NoSuchMethodException e) {
            spigotMethod = null;
        }
    }

    /**
     * Returns the locale that player uses.
     *
     * @param player The player
     */
    public static String getLocale(Player player) {
        if (Utils.majorVersion >= 12) {
            return player.getLocale().toLowerCase();
        } else {
            try {
                Object spigot = spigotMethod.invoke(player);
                Method spigotGetLocaleMethod = spigot.getClass().getMethod("getLocale");
                spigotGetLocaleMethod.setAccessible(true);

                return ((String) spigotGetLocaleMethod.invoke(spigot)).toLowerCase();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Returns the AuthMe messages file language code, by given locale and settings.
     * Dreeam - Hard mapping, based on mc1.20.6, 5/29/2024
     *
     * @param locale   The locale that player client setting uses.
     * @param settings The AuthMe settings, for default/fallback language usage.
     */
    public static String localeToCode(String locale, Settings settings) {
        // Certain locale code to AuthMe language code redirect
        if (!settings.getProperty(PluginSettings.I18N_CODE_REDIRECT).isEmpty()) {
            for (String raw : settings.getProperty(PluginSettings.I18N_CODE_REDIRECT)) {
                String[] split = raw.split(":");

                if (split.length == 2 && locale.equalsIgnoreCase(split[0])) {
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
