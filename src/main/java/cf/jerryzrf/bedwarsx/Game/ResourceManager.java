package cf.jerryzrf.bedwarsx.Game;

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
import static cf.jerryzrf.bedwarsx.Game.Game.world;

public final class ResourceManager {
    private static final Set<Resource> resources = new HashSet<>();
    static final List<BukkitTask> tasks = new ArrayList<>();

    public static void load() {
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
                    world,
                    (double) l.get("x"),
                    (double) l.get("y"),
                    (double) l.get("z"))
            ));
        resources.add(new Resource(item, (int) r.get("time"), locations));
        });
    }

    public static void start() {
        resources.forEach(resource -> tasks.add(resource.runTaskTimer(BedwarsX.plugin, 0, resource.time)));
    }

    public static void end() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
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
                Item i = world.dropItem(l, item);
                i.setVelocity(i.getVelocity().multiply(0));
            });
        }
    }
}