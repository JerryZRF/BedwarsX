package cf.jerryzrf.bedwarsx.listener;

import cf.jerryzrf.bedwarsx.Config;
import cf.jerryzrf.bedwarsx.game.Game;
import cf.jerryzrf.bedwarsx.Utils;
import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static cf.jerryzrf.bedwarsx.Config.config;
import static cf.jerryzrf.bedwarsx.Config.message;
import static cf.jerryzrf.bedwarsx.Utils.apply;

/**
 * @author JerryZRF
 */
@SuppressWarnings("deprecation")
public final class PlayerListener implements Listener {
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Game.status == GameStatus.Waiting) {
            player.setGameMode(GameMode.SURVIVAL);
            Game.IN_GAME_PLAYERS.add(player);
        } else if (Game.status == GameStatus.Running) {
            if (Game.REJOIN_PLAYERS.contains(player.getUniqueId())) {
                player.setDisplayName(Game.PLAYERS.get(player.getUniqueId()).color.getColorString() + "[" + Game.PLAYERS.get(player.getUniqueId()).name + "]" + player.getDisplayName());
                Game.IN_GAME_PLAYERS.add(player);
                event.setJoinMessage(player.getDisplayName() + "断线重连");
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                Game.PLAYERS.put(player.getUniqueId(), Game.WATCHER);
            }
        } else if (Game.status == GameStatus.Editing) {
            if (!player.hasPermission("bwx.edit")) {
                player.kick(Component.text("服务器正在施工，请稍后再试..."));
            }
        }
        player.clearTitle();
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvisible(false);
    }
    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        if (Game.status == GameStatus.Editing) {
            return;
        }
        Player player = event.getPlayer();
        Game.IN_GAME_PLAYERS.remove(player);
        Game.PLAYERS.remove(player.getUniqueId());
        if (Game.status == GameStatus.Running) {
            Game.REJOIN_PLAYERS.add(player.getUniqueId());
            Game.IN_GAME_PLAYERS.remove(player);
            //TODO 添加至语言文件
            event.quitMessage(Component.text(player.getDisplayName() + "退出了游戏，但他仍能断线重连"));
            return;
        }
        //TODO 添加至语言文件
        event.quitMessage(Component.text(player.getDisplayName() + "退出了游戏"));
    }
    @EventHandler
    public void playerChat(AsyncPlayerChatEvent event) {
        if (Game.status == GameStatus.Running) {
            if (Game.PLAYERS.get(event.getPlayer().getUniqueId()) == Game.WATCHER && !config.getBoolean("watcher.chat")) {
                event.setCancelled(true);
                Utils.getPlayersByTeam(Game.WATCHER).forEach(player ->
                        player.sendMessage("[" + config.getString("watcher.name", "旁观者") + "]<" + event.getPlayer() + ">" + event.getMessage()));
            }
            event.setFormat(
                    apply(event.getPlayer(), Config.config.getString("chatInGame"), Map.of("{team_color}", Game.PLAYERS.get(event.getPlayer().getUniqueId()).color.getColorString())));
        }
    }
    @EventHandler
    public void playerSleep(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick() || event.getClickedBlock() == null || event.getClickedBlock().getType().data != Bed.class) {
            return;
        }
        event.getPlayer().sendActionBar(message.getString("sleep", "你不能睡觉"));
        event.setCancelled(true);
    }
    @EventHandler
    public void playerDamage(EntityDamageEvent event) {
        if (Game.status != GameStatus.Running) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if ((new Date()).getTime() - Game.noHurtTime.get(player.getUniqueId()) <= config.getInt("respawn.noHurt") * 1000L) {
            player.sendMessage(apply(player, message.getString("noHurt", "无敌时间")));
            event.setCancelled(true);
        }
    }
    /** 一血 */
    private static boolean firstBlood = true;
    @EventHandler
    public void playerDie(PlayerDeathEvent event) {
        if (Game.status != GameStatus.Running) {
            event.setCancelled(true);
            return;
        }
        Player player = event.getEntity();
        if (player.getLastDamageCause() == null) {
            Game.playerDie(player);
        }
        EntityDamageEvent.DamageCause cause = player.getLastDamageCause().getCause();
        Entity entity = player.getLastDamageCause().getEntity();
        Player killer;
        if (entity instanceof Player) {
            killer = (Player) entity;
        } else {
            if ((Game.ANIMALS.get(entity) == null)) {
                Game.playerDie(player);
                return;
            }
            killer = Bukkit.getPlayer(Game.ANIMALS.get(entity));
        }
        Map<String, String> map = new HashMap<>();
        map.put("{player}", player.getDisplayName());
        map.put("{player_team}", Game.PLAYERS.get(player.getUniqueId()).name);
        map.put("{player_color}", Game.PLAYERS.get(player.getUniqueId()).color.getColorString());
        map.put("{killer}", killer.getDisplayName());
        map.put("{killer_team}", Game.PLAYERS.get(killer.getUniqueId()).name);
        map.put("{killer_color}", Game.PLAYERS.get(killer.getUniqueId()).color.getColorString());
        if (firstBlood) {
            event.setDeathMessage(apply(player, message.getString("playerDie.first"), map));
            firstBlood = false;
        } else if (!Game.PLAYERS.get(player.getUniqueId()).isBed) {
            event.setDeathMessage(apply(player, message.getString("playerDie.last"), map));
        }
        Game.playerDie(player);
    }
}
