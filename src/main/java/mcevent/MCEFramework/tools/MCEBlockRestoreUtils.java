package mcevent.MCEFramework.tools;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import java.util.*;

/*
MCEBlockRestoreUtils: 记录并恢复方块原状的工具类
*/
public class MCEBlockRestoreUtils {

    // 每个世界存一份“原始方块状态”，按坐标去重：对于同一坐标仅记录首次替换前的原状态
    private static final Map<String, Map<BlockKey, BlockData>> worldToOriginalStates = new HashMap<>();

    public static void recordReplacedState(BlockState replacedState) {
        if (replacedState == null)
            return;
        Location loc = replacedState.getLocation();
        World world = loc.getWorld();
        if (world == null)
            return;
        String worldName = world.getName();

        Map<BlockKey, BlockData> map = worldToOriginalStates.computeIfAbsent(worldName, k -> new HashMap<>());
        BlockKey key = new BlockKey(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        // 只在首次出现时记录原状态，后续同坐标忽略，确保最终恢复到真正的原始状态
        map.computeIfAbsent(key, k -> replacedState.getBlockData());
    }

    public static int restoreAllForWorld(String worldName) {
        Map<BlockKey, BlockData> map = worldToOriginalStates.get(worldName);
        if (map == null || map.isEmpty())
            return 0;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            map.clear();
            return 0;
        }

        int restored = 0;
        for (Map.Entry<BlockKey, BlockData> e : map.entrySet()) {
            BlockKey k = e.getKey();
            Block block = world.getBlockAt(k.x, k.y, k.z);
            block.setBlockData(e.getValue(), false);
            restored++;
        }
        map.clear();
        return restored;
    }

    private static class BlockKey {
        final int x;
        final int y;
        final int z;

        BlockKey(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof BlockKey))
                return false;
            BlockKey other = (BlockKey) o;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(x);
            result = 31 * result + Integer.hashCode(y);
            result = 31 * result + Integer.hashCode(z);
            return result;
        }
    }
}
