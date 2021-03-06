package cf.jerryzrf.bedwarsx.listener;

import cf.jerryzrf.bedwarsx.Config;
import cf.jerryzrf.bedwarsx.Utils;
import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
import cf.jerryzrf.bedwarsx.game.Damage;
import cf.jerryzrf.bedwarsx.game.Game;
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
                event.setJoinMessage(apply(player, message.getString("rejoin"), Map.of(
                        "{team_color}", Game.PLAYERS.get(player.getUniqueId()).color.getColorString(),
                        "{player}", player.getDisplayName()
                )));
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                Game.PLAYERS.put(player.getUniqueId(), Game.WATCHER);
            }
        } else if (Game.status == GameStatus.Editing) {
            if (!player.hasPermission("bwx.edit")) {
                player.kickPlayer(apply(player, message.getString("edit")));
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
        Map<String, String> map = new HashMap<>(2, 1f);
        map.put("{team_color}", Game.PLAYERS.get(player.getUniqueId()).color.getColorString());
        if (Game.status == GameStatus.Running) {
            Game.REJOIN_PLAYERS.add(player.getUniqueId());
            Game.IN_GAME_PLAYERS.remove(player);
            map.put("{player}", player.getDisplayName());
            event.setQuitMessage(apply(player, message.getString("quit.game"), map));
        } else if (Game.status == GameStatus.Waiting) {
            event.setQuitMessage(apply(player, message.getString("quit.cd"), map));
        }
    }
    @EventHandler
    public void playerChat(AsyncPlayerChatEvent event) {
        if (Game.status == GameStatus.Running) {
            if (Game.PLAYERS.get(event.getPlayer().getUniqueId()) == Game.WATCHER && !config.getBoolean("watcher.chat")) {
                event.setCancelled(true);
                Utils.getPlayersByTeam(Game.WATCHER).forEach(player ->
                        player.sendMessage("[" + config.getString("watcher.name", "?????????") + "]<" + event.getPlayer() + ">" + event.getMessage()));
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
        event.getPlayer().sendActionBar(message.getString("sleep", "???????????????"));
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
            injured.sendMessage(apply(injured, message.getString("noHurt", "????????????")));
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
    /** ?????? */
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
