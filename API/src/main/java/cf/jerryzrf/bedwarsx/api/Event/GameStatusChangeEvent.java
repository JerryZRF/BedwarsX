package cf.jerryzrf.bedwarsx.api.Event;

import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class GameStatusChangeEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled = false;
    private final GameStatus status;

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    public GameStatusChangeEvent(GameStatus status) {
        super(true);  //异步
        this.status = status;
    }

    public GameStatus getStatus() {
        return status;
    }
}
