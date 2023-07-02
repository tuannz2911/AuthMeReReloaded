package fr.xephi.authme.service;

import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The AntiBot Service Management class.
 */
public class WhiteListService implements SettingsDependent {

    private final PermissionsManager permissionsManager;
    private final Messages messages;
    private final BukkitService bukkitService;
    private WhiteListStatus whiteListStatus;

    @Inject
    WhiteListService(Settings settings, Messages messages, PermissionsManager permissionsManager,
                     BukkitService bukkitService) {
        // Instances
        this.messages = messages;
        this.permissionsManager = permissionsManager;
        this.bukkitService = bukkitService;
        // Initial status
        whiteListStatus = WhiteListStatus.DISABLED;
        // Load settings and start if required
        reload(settings);
    }

    @Override
    public void reload(Settings settings) {
    }

    private void startProtection() {
        if (whiteListStatus == WhiteListStatus.ACTIVE) {
            return;
        }
        whiteListStatus = WhiteListStatus.ACTIVE;
        bukkitService.getOnlinePlayers().stream()
            .filter(player -> permissionsManager.hasPermission(player, AdminPermission.WHITELIST_MESSAGE))
            .forEach(player -> messages.send(player, MessageKey.WHITELIST_ENABLED_MESSAGE));

    }

    private void stopProtection() {
        if (whiteListStatus == WhiteListStatus.DISABLED) {
            return;
        }
        whiteListStatus = WhiteListStatus.DISABLED;
        bukkitService.getOnlinePlayers().stream()
            .filter(player -> permissionsManager.hasPermission(player, AdminPermission.WHITELIST_MESSAGE))
            .forEach(player -> messages.send(player, MessageKey.WHITELIST_DISABLED_MESSAGE));
    }
    public WhiteListStatus getWhiteListStatus() {
        return whiteListStatus;
    }

    public void overrideWhiteListStatus(boolean started) {
        if (started) {
            startProtection();
        } else {
            stopProtection();
        }
    }
    public boolean shouldKick() {
        return whiteListStatus != WhiteListStatus.DISABLED;
    }

    public enum WhiteListStatus {
        DISABLED,
        ACTIVE
    }
}
