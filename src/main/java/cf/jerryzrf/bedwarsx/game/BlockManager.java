package cf.jerryzrf.bedwarsx.game;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.util.LinkedList;
import java.util.List;

import static cf.jerryzrf.bedwarsx.BedwarsX.plugin;

/**
 * @author JerryZRF
 */
public final class BlockManager {
    /** 玩家放置的方块 */
    public static final List<Block> BLOCKS = new LinkedList<>();

    public static void reset() {
        plugin.getLogger().info("开始重置地图");
        BLOCKS.forEach(block -> block.setType(Material.AIR));
        BLOCKS.clear();
        //清理掉落物
        Game.WORLD.getEntitiesByClass(Item.class).forEach(Entity::remove);
        plugin.getLogger().info("地图重置完成");
    }
}
