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

    private static final Map<String, List<SavedBlockState>> worldToSavedStates = new HashMap<>();

    public static void recordReplacedState(BlockState replacedState) {
        if (replacedState == null)
            return;
        Location loc = replacedState.getLocation();
        World world = loc.getWorld();
        if (world == null)
            return;
        String worldName = world.getName();

        SavedBlockState snapshot = new SavedBlockState(
                worldName,
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ(),
                replacedState.getType().createBlockData(replacedState.getBlockData().getAsString()));
        worldToSavedStates.computeIfAbsent(worldName, k -> new ArrayList<>()).add(snapshot);
    }

    public static int restoreAllForWorld(String worldName) {
        List<SavedBlockState> list = worldToSavedStates.get(worldName);
        if (list == null || list.isEmpty())
            return 0;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            list.clear();
            return 0;
        }

        int restored = 0;
        for (SavedBlockState s : list) {
            Block block = world.getBlockAt(s.x, s.y, s.z);
            block.setBlockData(s.blockData, false);
            restored++;
        }
        list.clear();
        return restored;
    }

    private static class SavedBlockState {
        final String worldName;
        final int x;
        final int y;
        final int z;
        final BlockData blockData;

        SavedBlockState(String worldName, int x, int y, int z, BlockData blockData) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockData = blockData;
        }
    }
}
