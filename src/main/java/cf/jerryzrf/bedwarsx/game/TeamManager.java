package cf.jerryzrf.bedwarsx.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;

import static cf.jerryzrf.bedwarsx.Config.config;
import static cf.jerryzrf.bedwarsx.Config.message;
import static cf.jerryzrf.bedwarsx.Utils.apply;

/**
 * @author JerryZRF
 */
public final class TeamManager {
    public static final List<Team> TEAMS = new ArrayList<>();

    public static void load() {
        List<Map<?, ?>> teamList = config.getMapList("team");
        teamList.forEach(team -> TEAMS.add(new Team(
                (String) team.get("name"),
                TeamColor.valueOf((String) team.get("color")),
                new Location(Game.WORLD,
                        (double) ((Map<?, ?>) team.get("bed")).get("x"),
                        (double) ((Map<?, ?>) team.get("bed")).get("y"),
                        (double) ((Map<?, ?>) team.get("bed")).get("z")),
                new Location(Game.WORLD,
                        (double) ((Map<?, ?>) team.get("spawn")).get("x"),
                        (double) ((Map<?, ?>) team.get("spawn")).get("y"),
                        (double) ((Map<?, ?>) team.get("spawn")).get("z"))
        )));
    }

    @Nullable
    public static Team getTeam(String name) {
        Team[] team = {null};
        TEAMS.forEach(t -> {
            if (t.name.equalsIgnoreCase(name)) {
                team[0] = t;
            }
        });
        return team[0];
    }
    @Nullable
    public static Team getTeam(Block bed) {
        for (Team t : TEAMS) {
            if (t.bed.equals(bed)) {
                return t;
            }
        }
        return null;
    }
    public static void debug() {
        TEAMS.forEach(team -> {
            System.out.println(team.name);
            System.out.println(team.color);
            System.out.println(team.bed.getX() + " " + team.bed.getY() + " " + team.bed.getZ());
        });
    }
    public static void reset() {
        TEAMS.forEach(Team::reset);
    }

    public enum TeamColor {
        //白色
        WHITE,
        //橙色
        ORANGE,
        MAGENTA,
        //浅蓝色
        LIGHT_BLUE,
        //黄色
        YELLOW,
        //黄绿色
        LIME,
        //粉色
        PINK,
        //灰色
        GREY,
        //浅灰色
        LIGHT_GRAY,
        //青色
        CYAN,
        //紫色
        PURPLE,
        //蓝色
        BLUE,
        //棕色
        BROWN,
        //绿色
        GREEN,
        //红色
        RED,
        //黑色
        BLACK;
        public ChatColor getChatColor() {
            return ChatColor.valueOf(name());
        }
        public String getColorString() {
            return "§" + getChatColor().getChar();
        }
    }

    public static final class Team {
        public final String name;
        public final TeamColor color;
        public final Block bed;
        public final Material bedType;
        public final Location spawn;
        public boolean isBed = true;
        public boolean useRestore = false;

        public Team(String name, TeamColor color, Location bed, Location spawn) {
            this.name = name;
            this.color = color;
            if (bed != null) {
                this.bed = Bukkit.getWorld(config.getString("world", "world")).getBlockAt(bed);
                bedType = this.bed.getType();
            } else {
                this.bed = null;
                bedType = null;
            }
            this.spawn = spawn;
        }

        public void reset() {
            isBed = true;
            useRestore = false;
            bed.setType(bedType);
        }

        public void bedBroken(Player player) {
            isBed = false;
            Game.IN_GAME_PLAYERS.forEach(p -> {
                Map<String, String> map = new HashMap<>(4, 1f);
                map.put("{player}", player.getDisplayName());
                map.put("{team}", name);
                map.put("{team_color}", color.getColorString());
                map.put("{player_color}", Game.PLAYERS.get(player.getUniqueId()).color.getColorString());
                if (Game.PLAYERS.get(p.getUniqueId()) == this) {
                    p.showTitle(Title.title(
                            Component.text(apply(p, message.getString("breakBed.self.title0"), map)),
                            Component.text(apply(p, message.getString("breakBed.self.title1"), map))
                    ));
                    p.sendMessage(apply(p, message.getString("breakBed.self.message"), map));
                } else {
                    p.showTitle(Title.title(
                            Component.text(apply(p, message.getString("breakBed.others.title0"), map)),
                            Component.text(apply(p, message.getString("breakBed.others.title1"), map))
                    ));
                    p.sendMessage(apply(p, message.getString("breakBed.others.message"), map));
                }
            });
        }

        public void restoreBed(Player player) {
            isBed = true;
            useRestore = true;
            Game.IN_GAME_PLAYERS.forEach(p -> {
                Map<String, String> map = new HashMap<>(3, 1f);
                map.put("{player}", player.getDisplayName());
                map.put("{team}", name);
                map.put("{color}", color.getColorString());
                if (Game.PLAYERS.get(p.getUniqueId()) == this) {
                    p.showTitle(Title.title(
                            Component.text(apply(p, message.getString("useRestore.self.title0"), map)),
                            Component.text(apply(p, message.getString("useRestore.self.title1"), map))
                    ));
                    p.sendMessage(apply(p, message.getString("useRestore.self.message"), map));
                } else {
                    p.showTitle(Title.title(
                            Component.text(apply(p, message.getString("useRestore.others.title0"), map)),
                            Component.text(apply(p, message.getString("useRestore.others.title1"), map))
                    ));
                    p.sendMessage(apply(p, message.getString("useRestore.others.message"), map));
                }
            });
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Team team)) {
                return false;
            }
            return Objects.equals(name, team.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
