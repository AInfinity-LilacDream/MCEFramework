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
            player.teleport(Objects.requireNonNull(Bukkit.getWorld(worldName)).getSpawnLocation());
        }
    }
}