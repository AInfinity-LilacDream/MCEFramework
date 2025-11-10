package mcevent.MCEFramework.games.underworldGame.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.underworldGame.UnderworldGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * PlayerRespawnHandler: 处理阴间游戏中玩家重生事件
 * 确保玩家重生时被传送到游戏世界的出生点，而不是主城
 */
public class PlayerRespawnHandler extends MCEResumableEventHandler implements Listener {

    private UnderworldGame underworldGame;

    public PlayerRespawnHandler(UnderworldGame game) {
        this.underworldGame = game;
        setSuspended(true); // 默认挂起
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // 在方法最开始就输出调试信息，确认方法是否被调用
        Player player = event.getPlayer();
        plugin.getLogger().info("[UnderworldGame][RespawnDebug] ===== 重生事件触发 =====");
        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家: " + player.getName() +
                ", 处理器状态: " + (isSuspended() ? "挂起" : "激活") +
                ", 事件重生位置: "
                + (event.getRespawnLocation() != null ? event.getRespawnLocation().getWorld().getName() + " " +
                        event.getRespawnLocation().getX() + "," +
                        event.getRespawnLocation().getY() + "," +
                        event.getRespawnLocation().getZ() : "null"));

        // 输出玩家的所有标签，用于调试
        java.util.Set<String> tags = player.getScoreboardTags();
        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家所有标签: " + tags.toString());

        // 注意：即使处理器被挂起，也要检查是否是阴间游戏的重生，因为游戏可能在玩家死亡后立即结束
        // 但玩家仍然需要正确重生到游戏世界

        // 关键修复：即使游戏已经结束（isRunningGame = false），只要 currentRunningGame 还是 UnderworldGame，
        // 就应该处理重生，因为玩家可能刚死亡，游戏刚结束，但玩家需要正确重生
        MCEGame current = MCEMainController.getCurrentRunningGame();
        boolean isRunningGame = MCEMainController.isRunningGame();

        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 游戏是否运行中: " + isRunningGame);
        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 当前游戏: "
                + (current != null ? current.getClass().getSimpleName() : "null"));

        // 检查是否是阴间游戏（即使游戏已结束，只要 currentRunningGame 还是 UnderworldGame 就处理）
        if (!(current instanceof UnderworldGame)) {
            plugin.getLogger().info("[UnderworldGame][RespawnDebug] 当前游戏不是阴间游戏，退出处理");
            return;
        }

        // 如果游戏已结束，但 currentRunningGame 还是 UnderworldGame，仍然处理重生
        if (!isRunningGame) {
            plugin.getLogger().info("[UnderworldGame][RespawnDebug] 游戏已结束，但当前游戏仍是阴间游戏，继续处理重生");
        }

        // 关键修复：检查玩家是否有 Participant 标签，而不是使用 isGameParticipant
        // 因为 isGameParticipant 还会检查玩家是否在游戏世界，但重生时玩家可能已经被传送到主城了
        // 关键修复：检查玩家是否有 Participant 标签，或者是否有 dead/Active 标签
        // 因为游戏结束时 onEnd() 会立即移除 Participant 标签，但玩家可能还没重生
        // 所以如果玩家有 dead 或 Active 标签，也应该处理重生位置
        boolean hasParticipantTag = player.getScoreboardTags().contains("Participant");
        boolean hasDeadTag = player.getScoreboardTags().contains("dead");
        boolean hasActiveTag = player.getScoreboardTags().contains("Active");
        boolean isParticipantByMethod = underworldGame.isGameParticipant(player);

        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家是否有 Participant 标签: " + hasParticipantTag);
        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家是否有 dead 标签: " + hasDeadTag);
        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家是否有 Active 标签: " + hasActiveTag);
        plugin.getLogger().info("[UnderworldGame][RespawnDebug] isGameParticipant 结果: " + isParticipantByMethod);
        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家当前世界: "
                + (player.getWorld() != null ? player.getWorld().getName() : "null"));
        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 游戏世界: " + underworldGame.getWorldName());

        // 只要玩家有 Participant 标签，或者有 dead/Active 标签（说明是游戏参与者，只是标签被移除了），就处理重生位置
        boolean shouldHandleRespawn = hasParticipantTag || (hasDeadTag || hasActiveTag);

        if (!shouldHandleRespawn) {
            plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家没有相关标签，退出处理");
            return;
        }

        if (!hasParticipantTag && (hasDeadTag || hasActiveTag)) {
            plugin.getLogger().info(
                    "[UnderworldGame][RespawnDebug] 玩家没有 Participant 标签但有 dead/Active 标签，继续处理重生（游戏可能已结束但玩家需要正确重生）");
        }

        // 如果处理器被挂起，仍然处理重生位置（因为游戏可能刚结束，但玩家需要正确重生）
        // 添加调试日志
        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家 " + player.getName() +
                " 重生事件触发，处理器状态: " + (isSuspended() ? "挂起" : "激活"));

        // 获取游戏世界的出生点
        String worldName = underworldGame.getWorldName();
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("[UnderworldGame][RespawnDebug] 玩家 " + player.getName() +
                    " 重生时无法找到游戏世界: " + worldName);
            return;
        }

        Location spawnLoc = world.getSpawnLocation();

        // 关键修复：在重生事件中强制设置重生位置（使用 HIGHEST 优先级确保最先执行）
        // 这确保即使玩家在主城有床/重生锚，也会在游戏世界重生
        event.setRespawnLocation(spawnLoc);

        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家 " + player.getName() +
                " 重生事件中设置重生位置: " + spawnLoc.getWorld().getName() +
                ", 位置: " + spawnLoc.getX() + "," + spawnLoc.getY() + "," + spawnLoc.getZ());

        // 额外保险：重生后立即传送玩家到游戏世界（防止其他逻辑覆盖）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // 检查玩家当前世界
                World currentWorld = player.getWorld();
                String targetWorldName = underworldGame.getWorldName();

                // 如果玩家不在游戏世界，强制传送
                if (currentWorld == null || !currentWorld.getName().equals(targetWorldName)) {
                    World targetWorld = Bukkit.getWorld(targetWorldName);
                    if (targetWorld != null) {
                        Location targetLoc = targetWorld.getSpawnLocation();
                        player.teleport(targetLoc);
                        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家 " + player.getName() +
                                " 重生后不在游戏世界（当前: " + (currentWorld != null ? currentWorld.getName() : "null") +
                                "），强制传送到游戏世界: " + targetWorldName);
                    }
                }

                // 清除床/重生锚重生点并重新设置游戏世界重生点
                player.setRespawnLocation(null);
                player.setRespawnLocation(spawnLoc);
            }
        }, 1L);

        plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家 " + player.getName() +
                " 重生，强制设置重生位置到游戏世界: " + worldName +
                ", 位置: " + spawnLoc.getX() + "," + spawnLoc.getY() + "," + spawnLoc.getZ() +
                ", 当前世界: " + (player.getWorld() != null ? player.getWorld().getName() : "null") +
                ", 重生位置世界: " + spawnLoc.getWorld().getName() +
                ", 事件重生位置: "
                + (event.getRespawnLocation() != null ? event.getRespawnLocation().getWorld().getName() : "null"));

        // 如果玩家已经死亡（有dead标签），确保设置为旁观模式
        // 延迟执行以确保重生完成
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.getScoreboardTags().contains("dead")) {
                player.setGameMode(GameMode.SPECTATOR);
                plugin.getLogger().info("[UnderworldGame][RespawnDebug] 玩家 " + player.getName() +
                        " 已死亡，设置为旁观模式");
            }
        }, 1L);
    }
}
