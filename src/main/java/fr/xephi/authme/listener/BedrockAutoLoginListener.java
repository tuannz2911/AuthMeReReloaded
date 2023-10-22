package fr.xephi.authme.listener;
/* Inspired by DongShaoNB/BedrockPlayerSupport **/
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class BedrockAutoLoginListener implements Listener {
    private final AuthMeApi authmeApi = AuthMeApi.getInstance();
    private final Plugin plugin;

    public BedrockAutoLoginListener(Plugin plugin) {
        this.plugin = plugin;
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
            player.sendMessage("§a基岩版自动登录完成!");
        }
    }
}
