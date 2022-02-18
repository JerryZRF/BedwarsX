package cf.jerryzrf.bedwarsx.game;

import cf.jerryzrf.bedwarsx.BedwarsX;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static cf.jerryzrf.bedwarsx.Config.config;
import static cf.jerryzrf.bedwarsx.game.Game.WORLD;

/**
 * @author JerryZRF
 */
public final class ResourceManager {
    private static final Set<Resource> RESOURCES = new HashSet<>();
    static final List<BukkitTask> TASKS = new ArrayList<>();

    public static void load() {
        RESOURCES.clear();
        List<Map<?, ?>> res = config.getMapList("resource");
        res.forEach(r -> {
            ItemStack item = new ItemStack(Material.getMaterial((String) r.get("material")));
            ItemMeta im = item.getItemMeta();
            im.setLore((List<String>) r.get("lore"));
            item.setItemMeta(im);
            item.setDisplayName((String) r.get("name"));
            List<Map<?, ?>> loc = (List<Map<?, ?>>) r.get("loc");
            List<Location> locations = new ArrayList<>();
            loc.forEach(l -> locations.add(new Location(
                    WORLD,
                    (double) l.get("x"),
                    (double) l.get("y"),
                    (double) l.get("z"))
            ));
        RESOURCES.add(new Resource(item, (int) r.get("time"), locations));
        });
    }

    public static void start() {
        stop();
        load();
        RESOURCES.forEach(resource -> TASKS.add(resource.runTaskTimer(BedwarsX.plugin, 0, resource.time)));
    }

    public static void stop() {
        TASKS.forEach(BukkitTask::cancel);
        TASKS.clear();
    }

    public final static class Resource extends BukkitRunnable {
        ItemStack item;
        int time;
        List<Location> loc;

        public Resource(ItemStack item, int time, List<Location> loc) {
            this.item = item;
            this.time = time;
            this.loc = loc;
        }

        @Override
        public void run() {
            loc.forEach(l -> {
                Item i = WORLD.dropItem(l, item);
                i.setVelocity(i.getVelocity().multiply(0));
            });
        }
    }
}