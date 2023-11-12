package fr.xephi.authme.listener;
/* Inspired by DongShaoNB/BedrockPlayerSupport **/

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.inject.Inject;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class BedrockAutoLoginListener implements Listener {
    private final AuthMeApi authmeApi = AuthMeApi.getInstance();
    @Inject
    private BukkitService bukkitService;
    @Inject
    private AuthMe plugin;
    @Inject
    private Messages messages;


    public BedrockAutoLoginListener() {
    }

    private boolean isBedrockPlayer(UUID uuid) {
        return AuthMe.settings.getProperty(HooksSettings.HOOK_FLOODGATE_PLAYER) && AuthMe.settings.getProperty(SecuritySettings.FORCE_LOGIN_BEDROCK) && org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgateId(uuid) && getServer().getPluginManager().getPlugin("floodgate") != null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = event.getPlayer().getName();
        UUID uuid = event.getPlayer().getUniqueId();
        if (isBedrockPlayer(uuid) && !authmeApi.isAuthenticated(player) && authmeApi.isRegistered(name)) {
            authmeApi.forceLogin(player);
            messages.send(player, MessageKey.BEDROCK_AUTO_LOGGED_IN);
        }
    }

    /* prevent sending duplicate messages */
//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void onPlayerRestoreSession(RestoreSessionEvent event) {
//        Player player = event.getPlayer();
//        String name = event.getPlayer().getName();
//        UUID uuid = event.getPlayer().getUniqueId();
//        if (isBedrockPlayer(uuid) && !authmeApi.isAuthenticated(player) && authmeApi.isRegistered(name)) {
//            event.setCancelled(true);
//        }
//    }
}
