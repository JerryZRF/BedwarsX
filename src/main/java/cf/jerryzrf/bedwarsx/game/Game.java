package cf.jerryzrf.bedwarsx.game;

import cf.jerryzrf.bedwarsx.Config;
import cf.jerryzrf.bedwarsx.Utils;
import cf.jerryzrf.bedwarsx.api.Event.GameStatusChangeEvent;
import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
import cf.jerryzrf.bedwarsx.listener.PlayerListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

import static cf.jerryzrf.bedwarsx.BedwarsX.plugin;
import static cf.jerryzrf.bedwarsx.Config.config;
import static cf.jerryzrf.bedwarsx.Config.message;
import static cf.jerryzrf.bedwarsx.Utils.apply;

/**
 * @author JerryZRF
 */
@SuppressWarnings("deprecation")
public final class Game {
    public static GameStatus status = GameStatus.Waiting;
    public static final World WORLD = Bukkit.getWorld(config.getString("world", "world"));
    public static final TeamManager.Team WATCHER = new TeamManager.Team(apply(null, Config.config.getString("watcher.name", "旁观者")),
            null,
            null,
            new Location(WORLD,
                    Config.config.getInt("watcher.loc.x"),
                    Config.config.getInt("watcher.loc.y"),
                    Config.config.getInt("watcher.loc.z")
            )
    );  //旁观者队伍
    public static final Map<UUID, TeamManager.Team> PLAYERS = new HashMap<>();
    public static final Set<Player> IN_GAME_PLAYERS = new HashSet<>();
    public static final Set<UUID> REJOIN_PLAYERS = new HashSet<>();
    public static final Map<Entity, UUID> ANIMALS = new HashMap<>();
    public static final Location CENTRE = new Location(WORLD, config.getInt("watcher.loc.x"), config.getInt("watcher.loc.y"), config.getInt("watcher.loc.z"));
    public static final Map<UUID, Long> NO_HURT_TIME = new HashMap<>();

    public static void start() {
        changeStatus(GameStatus.Running);
        cdTask.cancel();
        //分配队伍
        int n = IN_GAME_PLAYERS.size() / TeamManager.TEAMS.size() + (IN_GAME_PLAYERS.size() % TeamManager.TEAMS.size() == 0 ? 0 : 1);
        Random random = new Random();
        IN_GAME_PLAYERS.forEach(player -> {
            NO_HURT_TIME.put(player.getUniqueId(), 0L);
            TeamManager.Team team;
            do {
                team = TeamManager.TEAMS.get(random.nextInt(TeamManager.TEAMS.size()));
            } while (Utils.getPlayersByTeam(team).size() == n);
            player.setDisplayName(team.color.getChatColor() + "[" + team.name + "]" + player.getDisplayName() + "§r");
            PLAYERS.put(player.getUniqueId(), team);
        });  //智障算法
        //游戏开始
        IN_GAME_PLAYERS.forEach(player -> {
            player.showTitle(Title.title(
                    Component.text(apply(player, message.getString("startTitle0", "游戏开始"))),
                    Component.text(apply(player, message.getString("startTitle1", "摧毁敌方的床")))
            ));
            player.teleport(PLAYERS.get(player.getUniqueId()).spawn);
            player.setGameMode(GameMode.SURVIVAL);
        });
        new BukkitRunnable() {
            @Override
            public void run() {
                IN_GAME_PLAYERS.forEach(Player::resetTitle);
            }
        }.runTaskLater(plugin, 20);
        ResourceManager.start();
    }

    public static void end(TeamManager.Team winner) {
        changeStatus(GameStatus.Ending);
        reset();
        init();
    }

    public static void init() {
        PlayerListener.init();
        WORLD.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        Bukkit.getOnlinePlayers().forEach(player -> player.kick(Component.text("服务器重置")));
        changeStatus(GameStatus.Waiting);
        countdown = Config.config.getInt("countdown.default");
        countDown();
    }

    public static void reset() {
        PLAYERS.clear();
        IN_GAME_PLAYERS.clear();
        ResourceManager.stop();
        BlockManager.reset();
        TeamManager.reset();
    }

    public static void changeStatus(GameStatus s) {
        status = s;
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().callEvent(new GameStatusChangeEvent(s));
            }
        }.runTaskAsynchronously(plugin);
    }

    public static void playerDie(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(CENTRE);
        if (!PLAYERS.get(player.getUniqueId()).isBed) {
            PLAYERS.put(player.getUniqueId(), WATCHER);
            player.setInvisible(true);
            IN_GAME_PLAYERS.remove(player);
            TeamManager.Team team = checkWin();
            if (team != null) {
                end(team);
            }
        } else {
            Map<String, String> map = new HashMap<>(2, 1f);
            map.put("{color}", PLAYERS.get(player.getUniqueId()).color.getColorString());
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i = config.getInt("respawn.time"); i > 0; i--) {
                        map.put("{time}", String.valueOf(i));
                        player.showTitle(Title.title(
                                Component.text(apply(player, message.getString("respawningTitle0", "{time}"), map)),
                                Component.text(apply(player, message.getString("respawningTitle1", "等待复活中..."), map))
                        ));
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.teleport(PLAYERS.get(player.getUniqueId()).spawn);
                            player.setGameMode(GameMode.SURVIVAL);
                        }
                    }.runTask(plugin);
                    NO_HURT_TIME.put(player.getUniqueId(), Utils.getTime());
                    player.clearTitle();
                    player.showTitle(Title.title(
                            Component.text(apply(player, message.getString("respawnTitle0", "你复活了"), map)),
                            Component.text(apply(player, message.getString("respawnTitle1", "Go Go Go"), map)),
                            Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                    ));
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    public static TeamManager.Team checkWin() {
        TeamManager.Team team = null;
        for (Player player : IN_GAME_PLAYERS) {
            if (team == null) {
                team = PLAYERS.get(player.getUniqueId());
            } else if (team != PLAYERS.get(player.getUniqueId())) {
                return null;
            }
        }
        return team;
    }

    public static void debug() {
        System.out.println(status.name());
        PLAYERS.forEach(((uuid, team) ->
                System.out.println(Bukkit.getPlayer(uuid).getDisplayName() + " " + team.name)));
    }

    public static int countdown;
    private static BukkitTask cdTask;
    private static void countDown() {
         cdTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (IN_GAME_PLAYERS.size() == config.getInt("maxPlayer") && countdown > 5) {
                    countdown = 5;
                    IN_GAME_PLAYERS.forEach(player -> player.showTitle(Title.title(
                            Component.text(apply(player, message.getString("cdCutTitle0"))),
                            Component.text(apply(player, message.getString("cdCutTitle1"))),
                            Title.Times.of(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                    )));
                }
                if (config.getBoolean("fair")) {
                    if (config.getInt("maxPlayer") % IN_GAME_PLAYERS.size() == 0) {
                        cd();
                    } else {
                        cdStop();
                    }
                } else {
                    if (IN_GAME_PLAYERS.size() >= 2) {
                        cd();
                    } else {
                        cdStop();
                    }
                }
                if (countdown <= 0) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Game.start();
                        }
                    }.runTask(plugin);
                    cdTask.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20);
    }
    private static void cdStop() {
        IN_GAME_PLAYERS.forEach(player -> player.showTitle(Title.title(
                Component.text(apply(player, message.getString("cdStopTitle0"))),
                Component.text(apply(player, message.getString("cdStopTitle1"))),
                Title.Times.of(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
        )));
    }
    private static void cd() {
        countdown--;
        IN_GAME_PLAYERS.forEach(player -> player.showTitle(Title.title(
                Component.text(apply(player, message.getString("cdTitle0"))),
                Component.text(apply(player, message.getString("cdTitle1"))),
                Title.Times.of(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
        )));
    }
}