package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.GeoIpService;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.util.lazytags.Tag;
import fr.xephi.authme.util.lazytags.TagReplacer;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.util.lazytags.TagBuilder.createTag;

/**
 * Configuration for the welcome message (welcome.txt).
 */
public class WelcomeMessageConfiguration implements Reloadable {
    
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(WelcomeMessageConfiguration.class);

    @DataFolder
    @Inject
    private File pluginFolder;

    @Inject
    private Server server;

    @Inject
    private GeoIpService geoIpService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private CommonService service;

    /** List of all supported tags for the welcome message. */
    private final List<Tag<Player>> availableTags = Arrays.asList(
        createTag("&",             () -> String.valueOf(ChatColor.COLOR_CHAR)),
        createTag("{PLAYER}",      HumanEntity::getName),
        createTag("{DISPLAYNAME}", Player::getDisplayName),
        createTag("{DISPLAYNAMENOCOLOR}", Player::getDisplayName),
        createTag("{ONLINE}",      () -> Integer.toString(bukkitService.getOnlinePlayers().size())),
        createTag("{MAXPLAYERS}",  () -> Integer.toString(server.getMaxPlayers())),
        createTag("{IP}",          PlayerUtils::getPlayerIp),
        createTag("{LOGINS}",      () -> Integer.toString(playerCache.getLogged())),
        createTag("{WORLD}",       pl -> pl.getWorld().getName()),
        createTag("{SERVER}",      () -> service.getProperty(PluginSettings.SERVER_NAME)),
        createTag("{VERSION}",     () -> server.getBukkitVersion()),
        createTag("{COUNTRY}",     pl -> geoIpService.getCountryName(PlayerUtils.getPlayerIp(pl))));

    private TagReplacer<Player> messageSupplier;

    @PostConstruct
    @Override
    public void reload() {
        if (!(service.getProperty(RegistrationSettings.USE_WELCOME_MESSAGE))) {
            return;
        }

        List<String> welcomeMessage = new ArrayList<>();
        messageSupplier = TagReplacer.newReplacer(availableTags, welcomeMessage);
    }

    /**
     * Returns the welcome message for the given player.
     *
     * @param player the player for whom the welcome message should be prepared
     * @return the welcome message
     */
    public List<String> getWelcomeMessage(Player player) {
        return messageSupplier.getAdaptedMessages(player);
    }

    /**
     * Sends the welcome message accordingly to the configuration
     *
     * @param player the player for whom the welcome message should be prepared
     */
    public void sendWelcomeMessage(Player player) {
        if (service.getProperty(RegistrationSettings.USE_WELCOME_MESSAGE)) {
            List<String> welcomeMessage = getWelcomeMessage(player);
            if (service.getProperty(RegistrationSettings.BROADCAST_WELCOME_MESSAGE)) {
                welcomeMessage.forEach(bukkitService::broadcastMessage);
            } else {
                welcomeMessage.forEach(player::sendMessage);
            }
        }
    }
}
