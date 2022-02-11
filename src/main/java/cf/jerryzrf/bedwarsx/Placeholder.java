package cf.jerryzrf.bedwarsx;

import cf.jerryzrf.bedwarsx.Game.Game;
import cf.jerryzrf.bedwarsx.api.Game.GameStatus;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static cf.jerryzrf.bedwarsx.BedwarsX.plugin;

public final class Placeholder extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "bwx";
    }
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    @Override
    public String getRequiredPlugin() {
        return "BedwarsX";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        return switch (identifier) {
            case "team" -> Game.players.get(player.getUniqueId()).name;
            case "team_color" -> Game.players.get(player.getUniqueId()).color.getColorString();
            case "player" -> String.valueOf(Game.inGamePlayers.size());
            case "max_player" -> String.valueOf(Config.config.getInt("maxPlayer"));
            case "cut_down" -> String.valueOf(Game.status == GameStatus.Waiting ? Game.countdown : -1);
            default -> null;
        };
    }
}
