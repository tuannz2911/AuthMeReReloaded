package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import static fr.xephi.authme.listener.GuiCaptchaHandler.closeReasonMap;

public class GUICaptchaEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;

    public GUICaptchaEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isCaptchaVerified(Player player){
        return closeReasonMap.containsKey(player);

    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }


}
