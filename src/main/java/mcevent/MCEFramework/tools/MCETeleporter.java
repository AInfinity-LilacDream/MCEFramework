package mcevent.MCEFramework.tools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

/*
MCETeleporter: 进行玩家传送的工具类
 */
public class MCETeleporter {
    public static void globalSwapWorld(String worldName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Duel 世界内的玩家不参与任何游戏传送
            if (player.getWorld() != null && "duel".equals(player.getWorld().getName()))
                continue;
            player.teleport(Objects.requireNonNull(Bukkit.getWorld(worldName)).getSpawnLocation());
        }
    }
}