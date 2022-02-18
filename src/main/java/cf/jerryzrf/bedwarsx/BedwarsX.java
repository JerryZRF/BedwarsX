package cf.jerryzrf.bedwarsx;

import cf.jerryzrf.bedwarsx.game.Game;
import cf.jerryzrf.bedwarsx.game.ResourceManager;
import cf.jerryzrf.bedwarsx.game.TeamManager;
import cf.jerryzrf.bedwarsx.listener.BlockListener;
import cf.jerryzrf.bedwarsx.listener.GameModeListener;
import cf.jerryzrf.bedwarsx.listener.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * @author JerryZRF
 */
public final class BedwarsX extends JavaPlugin {
    public static final String VERSION = "0.1.0-dev";
    public static final int VERSION_NUM = 2022012301;
    public static BedwarsX plugin;

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        getLogger().info("当前版本" + VERSION + "-" + VERSION_NUM);
        int newVersion = getNewVersion();
        if (newVersion != -1) {
            if (VERSION_NUM == newVersion) {
                getLogger().info("当前版本是最新版本");
            } else {
                getLogger().warning("当前版本不是最新版本，请尽快更新");
            }
        }  //检查版本
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            (new Placeholder()).register();
        } else {
            getLogger().warning("未安装PlaceholderAPI");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(new BlockListener(), this);
        Bukkit.getPluginManager().registerEvents(new GameModeListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
        Config.loadMainConfig();
        Config.loadMessage();
        ResourceManager.load();
        TeamManager.load();
        Bukkit.getPluginCommand("bwx").setExecutor(new Commander());
        Bukkit.getPluginCommand("bwx").setTabCompleter(new Commander());
        Game.init();
    }

    @Override
    public void onDisable() {
        getLogger().info("BedwarsX已卸载");
    }

    private int getNewVersion() {
        byte[] buffer = new byte[40];
        try {
            HttpURLConnection conn = (HttpURLConnection) (new URL("https://github.com/JerryZRF/BedwarsX/version").openConnection());
            conn.setConnectTimeout(800);
            conn.getInputStream().read(buffer);
            return Integer.parseInt(Base64.getEncoder().encodeToString(buffer));
        } catch (IOException e) {
            getLogger().warning("最新版本获取失败: " + e.getMessage());
        }
        return -1;
    }
}
