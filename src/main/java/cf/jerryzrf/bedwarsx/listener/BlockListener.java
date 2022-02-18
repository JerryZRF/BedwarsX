package cf.jerryzrf.bedwarsx.listener;

import cf.jerryzrf.bedwarsx.Config;
import cf.jerryzrf.bedwarsx.game.Game;
import cf.jerryzrf.bedwarsx.game.TeamManager;
import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

import static cf.jerryzrf.bedwarsx.Config.message;
import static cf.jerryzrf.bedwarsx.game.BlockManager.BLOCKS;
import static cf.jerryzrf.bedwarsx.Utils.apply;

/**
 * @author JerryZRF
 */
public final class BlockListener implements Listener {
    @EventHandler
    public void playerPutBlock(BlockPlaceEvent event) {
        if (Game.status == GameStatus.Running) {
            if (event.getBlock().getType().data != Bed.class) {
                BLOCKS.add(event.getBlock());
                return;
            }
            TeamManager.Team team = Game.PLAYERS.get(event.getPlayer().getUniqueId());
            Player player = event.getPlayer();
            if (team.isBed) {
                player.sendMessage(apply(player, message.getString("haveBed", "床未被破坏!")));
                event.setCancelled(true);
                return;
            }
            if (team.useRestore) {
                player.sendMessage(apply(player, message.getString("usedRestore", "你已经恢复过一次床了")));
                event.setCancelled(true);
                return;
            }
            team.restoreBed(player);
        } else if (Game.status != GameStatus.Editing) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void playerBreakBlock(BlockBreakEvent event) {
        if (Game.status == GameStatus.Editing) {
            return;
        }
        if (event.getBlock().getType().data != Bed.class) {
            //不是玩家放置的(地图)
            if (!BLOCKS.remove(event.getBlock())) {
                event.setCancelled(true);
            }
            return;
        }
        TeamManager.Team team = TeamManager.getTeam(event.getBlock());
        if (team != null) {
            if (Game.PLAYERS.get(event.getPlayer().getUniqueId()).equals(team)) {
                event.getPlayer().sendMessage(apply(event.getPlayer(), message.getString("brokeSelf", "你不可以破坏自己的床")));
                event.setCancelled(true);
                return;
            }
            team.bedBroken(event.getPlayer());
        } else {
            //地图
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void blockChange(BlockFadeEvent event) {
        event.setCancelled(naturalChanceIsCancelled());
    }
    @EventHandler
    public void blockChange(BlockBurnEvent event) {
        event.setCancelled(naturalChanceIsCancelled());
    }
    @EventHandler
    public void blockChange(BlockFormEvent event) {
        event.setCancelled(naturalChanceIsCancelled());
    }
    @EventHandler
    public void blockChange(BlockIgniteEvent event) {
        event.setCancelled(naturalChanceIsCancelled());
    }

    private boolean naturalChanceIsCancelled() {
        return Game.status != GameStatus.Editing ||
                !Config.config.getBoolean("allowBlockChangeInEditing");
    }
}