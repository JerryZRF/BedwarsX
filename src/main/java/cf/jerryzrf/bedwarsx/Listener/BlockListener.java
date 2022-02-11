package cf.jerryzrf.bedwarsx.Listener;

import cf.jerryzrf.bedwarsx.Config;
import cf.jerryzrf.bedwarsx.Game.Game;
import cf.jerryzrf.bedwarsx.Game.TeamManager;
import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

import static cf.jerryzrf.bedwarsx.Config.message;
import static cf.jerryzrf.bedwarsx.Game.BlockManager.blocks;
import static cf.jerryzrf.bedwarsx.Utils.apply;

public final class BlockListener implements Listener {
    //人为
    @EventHandler
    public void PlayerPutBlock(BlockPlaceEvent event) {
        if (Game.status == GameStatus.Running) {
            if (event.getBlock().getType().data != Bed.class) {
                blocks.add(event.getBlock());
                return;
            }
            TeamManager.Team team = Game.players.get(event.getPlayer().getUniqueId());
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
    public void PlayerBreakBlock(BlockBreakEvent event) {
        if (event.getBlock().getType().data != Bed.class) {
            if (!blocks.remove(event.getBlock())) {  //不是玩家放置的(地图)
                event.setCancelled(artificialIsCancelled());
            }
            return;
        }
        if (Game.status != GameStatus.Running && Game.status != GameStatus.Editing) {
            return;
        }
        TeamManager.Team team = TeamManager.getTeam(event.getBlock());
        if (team != null) {
            if (Game.players.get(event.getPlayer().getUniqueId()).equals(team)) {
                event.getPlayer().sendMessage(apply(event.getPlayer(), message.getString("brokeSelf", "你不可以破坏自己的床")));
                event.setCancelled(true);
                return;
            }
            team.bedBroken(event.getPlayer());
        } else {
            event.setCancelled(artificialIsCancelled());
        }
    }
    //自然
    @EventHandler
    public void BlockChange(BlockFadeEvent event) {
        event.setCancelled(naturalChanceIsCancelled());
    }
    @EventHandler
    public void BlockChange(BlockBurnEvent event) {
        event.setCancelled(naturalChanceIsCancelled());
    }
    @EventHandler
    public void BlockChange(BlockFormEvent event) {
        event.setCancelled(naturalChanceIsCancelled());
    }
    @EventHandler
    public void BlockChange(BlockIgniteEvent event) {
        event.setCancelled(naturalChanceIsCancelled());
    }

    //自然改变
    private boolean naturalChanceIsCancelled() {
        return Game.status != GameStatus.Editing ||
                !Config.config.getBoolean("allowBlockChangeInEditing");
    }
    //人为破坏
    private boolean artificialIsCancelled() {
        return Game.status != GameStatus.Editing;
    }
}