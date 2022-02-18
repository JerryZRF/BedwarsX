package cf.jerryzrf.bedwarsx;

import cf.jerryzrf.bedwarsx.game.Game;
import cf.jerryzrf.bedwarsx.game.TeamManager;
import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static cf.jerryzrf.bedwarsx.Config.message;

/**
 * @author JerryZRF
 */
public final class Commander implements TabExecutor {
    @Override
    @ParametersAreNonnullByDefault
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return true;
        }
        switch (args[0]) {
            case "work" -> {
                if (!sender.hasPermission("bwx.edit")) {
                    sender.sendMessage(message.getString("noPerm", "你没有权限这么做!"));
                    return true;
                }
                Game.init();
                return true;
            }
            case "start" -> {
                if (!sender.hasPermission("bwx.forcestart")) {
                    sender.sendMessage(message.getString("noPerm", "你没有权限这么做!"));
                    return true;
                }
                if (Game.status != GameStatus.Waiting) {
                    sender.sendMessage(ChatColor.RED + "只能在Waiting阶段这么做!");
                    return true;
                }
                Game.start();
                return true;
            }
            case "kill" -> {
                if (!sender.hasPermission("bwx.admin")) {
                    sender.sendMessage(message.getString("noPerm", "你没有权限这么做!"));
                    return true;
                }
                if (args.length != 2) {
                    return false;
                }
                TeamManager.Team team = TeamManager.getTeam(args[1]);
                if (team == null) {
                    sender.sendMessage(message.getString("noTeam", "不存在指定队伍"));
                    return true;
                }
                team.bedBroken((Player) sender);
                return true;
            }
            case "edit" -> {
                if (!sender.hasPermission("bwx.edit")) {
                    sender.sendMessage(message.getString("noPerm", "你没有权限这么做!"));
                    return true;
                }
                Game.reset();
                sender.sendMessage("进入编辑模式");
                Game.changeStatus(GameStatus.Editing);
                return true;
            }
            case "debug" -> {
                TeamManager.debug();
                Game.debug();
                return true;
            }
            case "stop" -> {
                if (!sender.hasPermission("bwx.admin")) {
                    sender.sendMessage(message.getString("noPerm", "你没有权限这么做!"));
                    return true;
                }
                Game.end(null);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @Nullable List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
