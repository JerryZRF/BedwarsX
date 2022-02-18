package cf.jerryzrf.bedwarsx;

import cf.jerryzrf.bedwarsx.game.Game;
import cf.jerryzrf.bedwarsx.game.TeamManager;
import lombok.NonNull;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author JerryZRF
 */
public final class Utils {
    /**
     * 把玩家传送到指定服务器
     * https://www.spigotmc.org/threads/the-bungee-bukkit-plugin-messaging-channel.499/
     *
     * @param player 玩家
     * @param server 服务器在BungeeCord中的名字
     */
    public static void toServer(Player player, String server) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);

        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException e) {
            // Can never happen
        }
        player.sendPluginMessage(BedwarsX.plugin, "BungeeCord", b.toByteArray());
    }

    /**
     * 应用占位符
     *
     * @param player 操作的玩家
     * @param source 字符串
     * @return 结果
     */
    public static String apply(Player player, String source) {
        return PlaceholderAPI.setPlaceholders(player, source.replace("&","§"));
    }

    /**
     * 应用占位符
     *
     * @param player 操作的玩家
     * @param source 字符串
     * @param map 映射表
     * @return 结果
     */
    public static String apply(Player player, String source, @NonNull Map<String, String> map) {
        final String[] tmp = {source};
        map.forEach((s, t) -> tmp[0] = tmp[0].replace(s, t));
        return PlaceholderAPI.setPlaceholders(player, tmp[0].replace("&","§"));
    }

    /**
     * 应用占位符
     *
     * @param player 操作的玩家
     * @param source 字符串
     * @param map 映射表
     * @return 结果
     */
    public static List<String> apply(Player player, List<String> source, @NonNull Map<String, String> map) {
        map.forEach((s, t) -> source.forEach(str -> str = str.replace(s, t)));
        source.forEach(str -> str = str.replace("&","§"));
        return PlaceholderAPI.setPlaceholders(player, source);
    }
    /**
     * 应用占位符
     *
     * @param player 操作的玩家
     * @param source 字符串
     * @return 结果
     */
    public static List<String> apply(Player player, List<String> source) {
        source.forEach(s -> s = s.replace("&","§"));
        return PlaceholderAPI.setPlaceholders(player, source);
    }

    /**
     * 获取该队伍所有的玩家
     *
     * @param team 队伍
     * @return 玩家
     */
    public static List<Player> getPlayersByTeam(TeamManager.Team team) {
        List<Player> players = new ArrayList<>();
        Game.PLAYERS.forEach(((p, t) -> {
            if (t == team) {
                players.add(Bukkit.getPlayer(p));
            }
        }));
        return players;
    }
}
