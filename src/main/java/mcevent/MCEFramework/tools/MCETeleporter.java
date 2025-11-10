package mcevent.MCEFramework.tools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
MCETeleporter: 进行玩家传送的工具类
 */
public class MCETeleporter {
    public static void globalSwapWorld(String worldName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != null && "duel".equals(player.getWorld().getName())){
                // Duel 世界内的玩家清空物品栏之后再传送
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.getInventory().clear();
                    player.updateInventory();
                });
            }
            player.teleport(Objects.requireNonNull(Bukkit.getWorld(worldName)).getSpawnLocation());
        }
    }
}