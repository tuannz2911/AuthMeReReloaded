package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.service.WhiteListService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Display or change the status of the antibot mod.
 */
public class WhitelistCommand implements ExecutableCommand {

    @Inject
    private WhiteListService whiteListService;


    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        if (arguments.isEmpty()) {
            sender.sendMessage("[AuthMe] WhiteList: " + whiteListService.getWhiteListStatus().name());
            return;
        }

        String newState = arguments.get(0);

        // Enable or disable the mod
        if ("ON".equalsIgnoreCase(newState)) {
            whiteListService.overrideWhiteListStatus(true);
            sender.sendMessage("[AuthMe] WhiteList enabled!");
        } else if ("OFF".equalsIgnoreCase(newState)) {
            sender.sendMessage("[AuthMe] WhiteList disabled!");
            whiteListService.overrideWhiteListStatus(false);
        } else {
            sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE + "/authme help whitelist");
        }
    }
}
