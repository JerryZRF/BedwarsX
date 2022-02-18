package cf.jerryzrf.bedwarsx.listener;

import cf.jerryzrf.bedwarsx.Config;
import cf.jerryzrf.bedwarsx.Utils;
import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
import cf.jerryzrf.bedwarsx.game.Damage;
import cf.jerryzrf.bedwarsx.game.Game;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
                //TODO 添加到语言文件
                event.setJoinMessage(player.getDisplayName() + "断线重连");
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                Game.PLAYERS.put(player.getUniqueId(), Game.WATCHER);
            }
        } else if (Game.status == GameStatus.Editing) {
            if (!player.hasPermission("bwx.edit")) {
                //TODO 添加到语言文件
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

    private static final Map<UUID, Damage> PLAYER_DAMAGE = new HashMap<>(config.getInt("maxPlayer", 16), 1f);
    @EventHandler
    public void playerDamageByEntity(EntityDamageEvent ede) {
        if (Game.status != GameStatus.Running) {
            ede.setCancelled(true);
            return;
        }
        if (!(ede.getEntity() instanceof Player injured)) {
            return;
        }
        if ((Utils.getTime() - Game.NO_HURT_TIME.get(injured.getUniqueId()) <= config.getInt("respawn.noHurt") * 1000L)) {
            injured.sendMessage(apply(injured, message.getString("noHurt", "无敌时间")));
            ede.setCancelled(true);
            return;
        }
        EntityDamageByEntityEvent edbee;
        if (ede instanceof EntityDamageByEntityEvent) {
            edbee = (EntityDamageByEntityEvent) ede;
        } else {
            return;
        }
        if (!(edbee.getDamager() instanceof Player damager)) {
            UUID attacker = Game.ANIMALS.get(edbee.getEntity());
            if (attacker != null) {
                PLAYER_DAMAGE.put(injured.getUniqueId(), new Damage(attacker, Utils.getTime()));
            } else {
                edbee.setCancelled(true);
            }
        } else {
            PLAYER_DAMAGE.put(injured.getUniqueId(), new Damage(damager.getUniqueId(), Utils.getTime()));
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
        Player killer = player.getKiller();
        if (killer == null) {

        }
        Map<String, String> map = new HashMap<>(8, 1f);
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
    @EventHandler
    public void playerCraftItem(CraftItemEvent event) {
        event.setCancelled(true);
    }

    public static void init() {
        firstBlood = true;
        PLAYER_DAMAGE.clear();
    }
}
