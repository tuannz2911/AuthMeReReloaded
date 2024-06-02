package fr.xephi.authme.util.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;

public class MiniMessageUtils {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Parse a MiniMessage string into a legacy string.
     *
     * @param message The message to parse.
     * @return The parsed message.
     */
    public static String parseMiniMessageToLegacy(String message) {
        Component component = miniMessage.deserialize(message);
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    /**
     * Parse a MiniMessage string into a BaseComponent.
     *
     * @param message The message to parse.
     * @return The parsed message.
     */
    public static BaseComponent[] parseMiniMessageToBaseComponent(String message) {
        Component component = miniMessage.deserialize(message);
        return BungeeComponentSerializer.legacy().serialize(component);
    }

    private MiniMessageUtils() {
    }
}
