package com.creeperface.nukkitx.colormatch.event;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import com.creeperface.nukkitx.colormatch.arena.Arena;
import lombok.Getter;

public class PlayerJoinArenaEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    @Getter
    private Arena arena;

    public static HandlerList getHandlers() {
        return handlers;
    }

    public PlayerJoinArenaEvent(Player p, Arena a) {
        this.player = p;
        this.arena = a;
    }
}
