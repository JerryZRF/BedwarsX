package cf.jerryzrf.bedwarsx.Listener;

import cf.jerryzrf.bedwarsx.BedwarsX;
import cf.jerryzrf.bedwarsx.Config;
import cf.jerryzrf.bedwarsx.Game.BlockManager;
import cf.jerryzrf.bedwarsx.Game.Game;
import cf.jerryzrf.bedwarsx.Utils;
import cf.jerryzrf.bedwarsx.api.Event.GameStatusChangeEvent;
import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import static cf.jerryzrf.bedwarsx.Config.message;
import static cf.jerryzrf.bedwarsx.Utils.apply;

public final class GameModeListener implements Listener {
    @EventHandler
    public void GameEnd(GameStatusChangeEvent event) {
        if (event.getStatus() != GameStatus.Ending) {
            return;
        }
        BedwarsX.plugin.getLogger().info("5s后重置地图");
        Bukkit.broadcast(Component.text(apply(null, message.getString("kick", "5秒后退出"))));
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(p -> Utils.toServer(p, apply(null, Config.config.getString("lobby"))));
                BlockManager.reset();
                Game.init();
            }
        }.runTaskLater(BedwarsX.plugin, 100);
    }
}
