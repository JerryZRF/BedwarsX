package cf.jerryzrf.bedwarsx.Game;

import cf.jerryzrf.bedwarsx.Config;
import cf.jerryzrf.bedwarsx.Utils;
import cf.jerryzrf.bedwarsx.api.Event.GameStatusChangeEvent;
import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
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


public final class Game {
    public static GameStatus status = GameStatus.Waiting;
    public static final World world = Bukkit.getWorld(config.getString("world", "world"));
    public static final TeamManager.Team watcher = new TeamManager.Team(apply(null, Config.config.getString("watcher.name", "旁观者")),
            null,
            null,
            new Location(world,
                    Config.config.getInt("watcher.loc.x"),
                    Config.config.getInt("watcher.loc.y"),
                    Config.config.getInt("watcher.loc.z")
            )
    );  //旁观者队伍
    public static final Map<UUID, TeamManager.Team> players = new HashMap<>();
    public static final Set<Player> inGamePlayers = new HashSet<>();
    public static final Set<UUID> rejoinPlayers = new HashSet<>();
    public static final Map<Entity, UUID> animals = new HashMap<>();
    public static final Location center = new Location(world, config.getInt("watcher.loc.x"), config.getInt("watcher.loc.y"), config.getInt("watcher.loc.z"));
    public static final Map<UUID, Long> noHurtTime = new HashMap<>();

    public static void start() {
        changeStatus(GameStatus.Running);
        cdTask.cancel();
        //分配队伍
        int n = inGamePlayers.size() / TeamManager.teams.size() + (inGamePlayers.size() % TeamManager.teams.size() == 0 ? 0 : 1);
        Random random = new Random();
        inGamePlayers.forEach(player -> {
            noHurtTime.put(player.getUniqueId(), 0L);
            TeamManager.Team team;
            do {
                team = TeamManager.teams.get(random.nextInt(TeamManager.teams.size()));
            } while (Utils.getPlayersByTeam(team).size() == n);
            player.setDisplayName(team.color.getChatColor() + "[" + team.name + "]" + player.getDisplayName() + "§r");
            players.put(player.getUniqueId(), team);
        });  //智障算法
        //游戏开始
        inGamePlayers.forEach(player -> player.showTitle(Title.title(
                        Component.text(apply(player, message.getString("startTitle0", "游戏开始"))),
                        Component.text(apply(player, message.getString("startTitle1", "摧毁敌方的床")))
                ))
        );
        new BukkitRunnable() {
            @Override
            public void run() {
                inGamePlayers.forEach(Player::resetTitle);
            }
        }.runTaskLater(plugin, 20);
        ResourceManager.start();
    }

    public static void end(TeamManager.Team winner) {
        changeStatus(GameStatus.Ending);
        ResourceManager.end();
        BlockManager.reset();
        TeamManager.reset();
        init();
    }

    public static void init() {
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        Bukkit.getOnlinePlayers().forEach(player -> player.kick(Component.text("服务器重置")));
        players.clear();
        inGamePlayers.clear();
        changeStatus(GameStatus.Waiting);
        countdown = Config.config.getInt("countdown.default");
        countDown();
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
        player.setGameMode(GameMode.SPECTATOR);  //旁观模式
        player.teleport(center);
        if (players.get(player.getUniqueId()).isBed) {
            players.put(player.getUniqueId(), watcher);
            player.setInvisible(true);
            inGamePlayers.remove(player);
            TeamManager.Team team = checkWin();
            if (team != null) {
                end(team);
            }
        } else {
            Map<String, String> map = new HashMap<>();
            map.put("{color}", players.get(player.getUniqueId()).color.getColorString());
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
                    noHurtTime.put(player.getUniqueId(), (new Date()).getTime());
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
        for (Player player : inGamePlayers) {
            if (team == null) {
                team = players.get(player.getUniqueId());
            } else if (team != players.get(player.getUniqueId())) {
                return null;
            }
        }
        return team;
    }

    public static void debug() {
        System.out.println(status.name());
        players.forEach(((uuid, team) ->
                System.out.println(Bukkit.getPlayer(uuid).getDisplayName() + " " + team.name)));
    }

    public static int countdown;
    private static BukkitTask cdTask;
    private static void countDown() {
         cdTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (inGamePlayers.size() == config.getInt("maxPlayer") && countdown > 5) {
                    countdown = 5;
                    inGamePlayers.forEach(player -> player.showTitle(Title.title(
                            Component.text(apply(player, message.getString("cdCutTitle0"))),
                            Component.text(apply(player, message.getString("cdCutTitle1"))),
                            Title.Times.of(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                    )));
                }
                if (config.getBoolean("fair")) {
                    if (config.getInt("maxPlayer") % inGamePlayers.size() == 0) {
                        cd();
                    } else {
                        cdStop();
                    }
                } else {
                    if (inGamePlayers.size() >= 2) {
                        cd();
                    } else {
                        cdStop();
                    }
                }
                if (countdown <= 0) {
                    Game.start();
                    cdTask.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20);
    }
    private static void cdStop() {
        inGamePlayers.forEach(player -> player.showTitle(Title.title(
                Component.text(apply(player, message.getString("cdStopTitle0"))),
                Component.text(apply(player, message.getString("cdStopTitle1"))),
                Title.Times.of(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
        )));
    }
    private static void cd() {
        countdown--;
        inGamePlayers.forEach(player -> player.showTitle(Title.title(
                Component.text(apply(player, message.getString("cdTitle0"))),
                Component.text(apply(player, message.getString("cdTitle1"))),
                Title.Times.of(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
        )));
    }
}