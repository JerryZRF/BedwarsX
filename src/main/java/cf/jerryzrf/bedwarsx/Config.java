package cf.jerryzrf.bedwarsx;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

import static cf.jerryzrf.bedwarsx.BedwarsX.plugin;

public final class Config {
    public static YamlConfiguration config;
    public static YamlConfiguration message;
    public static void loadMainConfig() {
        config = loadConfig(new File(plugin.getDataFolder(), "config.yml"), 0);
    }
    public static void loadMessage() {
        message = loadConfig(new File(plugin.getDataFolder(), "message.yml"), 0);
    }


    @Nullable
    private static YamlConfiguration loadConfig(File file, int version) {
        if (!file.exists()) {
            plugin.saveResource(file.getName(), true);
        }
        final YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (config.getInt("version", -1) != version) {
            plugin.getLogger().warning("配置文件" + file.getName() + "版本错误");
            if (!file.renameTo(new File(plugin.getDataFolder(), file.getName() + ".old"))) {
                plugin.getLogger().warning("备份配置文件" + file.getName() + "失败");
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
            plugin.saveResource(file.getName(), true);
            plugin.getLogger().warning("已备份并覆盖");
            try {
                config.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
                return null;
            }
            return config;
        } //版本错误
        return config;
    }
}
