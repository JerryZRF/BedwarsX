package cf.jerryzrf.bedwarsx.Listener;

import cf.jerryzrf.bedwarsx.Config;
import cf.jerryzrf.bedwarsx.Game.Game;
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

@SuppressWarnings("deprecation")
public final class PlayerListener implements Listener {
    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Game.status == GameStatus.Waiting) {
            player.setGameMode(GameMode.SURVIVAL);
            Game.inGamePlayers.add(player);
        } else if (Game.status == GameStatus.Running) {
            if (Game.rejoinPlayers.contains(player.getUniqueId())) {
                player.setDisplayName(Game.players.get(player.getUniqueId()).color.getColorString() + "[" + Game.players.get(player.getUniqueId()).name + "]" + player.getDisplayName());
                Game.inGamePlayers.add(player);
                event.setJoinMessage(player.getDisplayName() + "断线重连");
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                Game.players.put(player.getUniqueId(), Game.watcher);
            }
        } else if (Game.status == GameStatus.Editing) {
            if (!player.hasPermission("bwx.edit")) {
                player.kick(Component.text("服务器正在施工，请稍后再试..."));
            }
        }
    }
    @EventHandler
    public void PlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Game.inGamePlayers.remove(player);
        Game.players.remove(player.getUniqueId());
        if (Game.status == GameStatus.Running) {
            Game.rejoinPlayers.add(player.getUniqueId());
            Game.inGamePlayers.remove(player);
            //稍后添加至配置文件
            event.quitMessage(Component.text(player.getDisplayName() + "退出了游戏，但他仍能断线重连"));
            return;
        }
        event.quitMessage(Component.text(player.getDisplayName() + "退出了游戏"));
    }
    @EventHandler
    public void PlayerChat(AsyncPlayerChatEvent event) {
        if (Game.status == GameStatus.Running) {
            if (Game.players.get(event.getPlayer().getUniqueId()) == Game.watcher && !config.getBoolean("watcher.chat")) {
                event.setCancelled(true);
                Utils.getPlayersByTeam(Game.watcher).forEach(player ->
                        player.sendMessage("[" + config.getString("watcher.name", "旁观者") + "]<" + event.getPlayer() + ">" + event.getMessage()));
            }
            event.setFormat(
                    apply(event.getPlayer(), Config.config.getString("chatInGame"), Map.of("{team_color}", Game.players.get(event.getPlayer().getUniqueId()).color.getColorString())));
        }
    }
    @EventHandler
    public void PlayerSleep(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick() || event.getClickedBlock() == null || event.getClickedBlock().getType().data != Bed.class) {
            return;
        }
        event.getPlayer().sendActionBar(message.getString("sleep", "你不能睡觉"));
        event.setCancelled(true);
    }
    @EventHandler
    public void PlayerDamage(EntityDamageEvent event) {
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
    private static boolean firstBlood = true;
    @EventHandler
    public void PlayerDie(PlayerDeathEvent event) {
        if (Game.status != GameStatus.Running) {
            event.setCancelled(true);
            return;
        }
        Player player = event.getEntity();
        if (player.getLastDamageCause() == null) {
            return;
        }
        Entity entity = player.getLastDamageCause().getEntity();
        Player killer;
        if (entity instanceof Player) {
            killer = (Player) entity;
        } else {
            if ((Game.animals.get(entity) == null)) {
                return;
            }
            killer = Bukkit.getPlayer(Game.animals.get(entity));
        }
        Map<String, String> map = new HashMap<>();
        map.put("{player}", player.getDisplayName());
        map.put("{player_team}", Game.players.get(player.getUniqueId()).name);
        map.put("{player_color}", Game.players.get(player.getUniqueId()).color.getColorString());
        map.put("{killer}", killer.getDisplayName());
        map.put("{killer_team}", Game.players.get(killer.getUniqueId()).name);
        map.put("{killer_color}", Game.players.get(killer.getUniqueId()).color.getColorString());
        if (firstBlood) {
            event.setDeathMessage(apply(player, message.getString("playerDie.first"), map));
            firstBlood = false;
        } else if (!Game.players.get(player.getUniqueId()).isBed) {
            event.setDeathMessage(apply(player, message.getString("playerDie.last"), map));
        }
        Game.playerDie(player);
    }
}
