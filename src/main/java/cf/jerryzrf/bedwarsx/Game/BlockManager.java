package cf.jerryzrf.bedwarsx.Game;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.util.LinkedList;
import java.util.List;

import static cf.jerryzrf.bedwarsx.BedwarsX.plugin;

public final class BlockManager {
    public static final List<Block> blocks = new LinkedList<>();  //玩家放置的方块

    public static void reset() {
        plugin.getLogger().info("开始重置地图");
        blocks.forEach(block -> block.setType(Material.AIR));
        Game.world.getEntitiesByClass(Item.class).forEach(Entity::remove);  //清理掉落物
        plugin.getLogger().info("地图重置完成");
    }
}
