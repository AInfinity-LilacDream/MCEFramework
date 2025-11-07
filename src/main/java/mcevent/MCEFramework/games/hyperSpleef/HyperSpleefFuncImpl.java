package mcevent.MCEFramework.games.hyperSpleef;

import mcevent.MCEFramework.games.hyperSpleef.gameObject.*;
import mcevent.MCEFramework.tools.MCEMessenger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;
import static mcevent.MCEFramework.miscellaneous.Constants.hyperSpleef;

/*
HyperSpleefFuncImpl: 冰雪乱斗游戏逻辑实现
*/
public class HyperSpleefFuncImpl {

    private static final Random random = new Random();

    /**
     * 处理雪块破坏事件
     */
    public static void handleSnowBreak(Player player, Material brokenBlock) {
        if (!player.getScoreboardTags().contains("Participant") ||
                player.getScoreboardTags().contains("dead")) {
            return;
        }

        // 只有雪块和雪层可以给予雪球
        if (brokenBlock == Material.SNOW_BLOCK || brokenBlock == Material.SNOW) {
            int snowballAmount = getSnowballAmount(brokenBlock);
            if (hyperSpleef != null) {
                hyperSpleef.addPlayerSnowballs(player, snowballAmount);
            }
        }
    }

    /**
     * 根据破坏的方块类型决定雪球数量
     */
    private static int getSnowballAmount(Material material) {
        return switch (material) {
            case SNOW_BLOCK -> 4; // 固定4个雪球
            case SNOW -> 2; // 固定2个雪球
            default -> 0;
        };
    }

    /**
     * 处理玩家掉落事件
     */
    public static void handlePlayerFall(Player player) {
        // PlayerFallHandler已经检查过Y坐标，直接调用处理方法
        if (hyperSpleef != null) {
            hyperSpleef.handlePlayerFall(player);
        }
    }

    /**
     * 启动随机事件系统
     */
    public static void startRandomEvents(HyperSpleef game, List<EventInfo> eventSchedule,
            Map<Integer, String> scheduledEventTypes) {
        // 1:00 - 随机事件
        game.setDelayedTask(60, () -> {
            String eventType = scheduledEventTypes.get(60);
            if (eventType != null) {
                triggerSpecificEvent(game, eventType);
            }
            game.updateCurrentEventIndex();
        });

        // 1:30 - 随机事件
        game.setDelayedTask(90, () -> {
            String eventType = scheduledEventTypes.get(90);
            if (eventType != null) {
                triggerSpecificEvent(game, eventType);
            }
            game.updateCurrentEventIndex();
        });

        // 2:00 - 随机事件
        game.setDelayedTask(120, () -> {
            String eventType = scheduledEventTypes.get(120);
            if (eventType != null) {
                triggerSpecificEvent(game, eventType);
            }
            game.updateCurrentEventIndex();
        });

        // 地图变动前10秒提示（2:20）
        game.setDelayedTask(140, () -> {
            sendMapChangeWarning(game);
        });

        // 2:30 - 地图变动
        game.setDelayedTask(150, () -> {
            triggerMapChange(game);
            game.updateCurrentEventIndex();
        });

        // 3:00 - 随机事件
        game.setDelayedTask(180, () -> {
            String eventType = scheduledEventTypes.get(180);
            if (eventType != null) {
                triggerSpecificEvent(game, eventType);
            }
            game.updateCurrentEventIndex();
        });

        // 4:00 - 游戏结束
        game.setDelayedTask(240, () -> {
            if (game.getTimeline() != null) {
                game.getTimeline().nextState();
            }
            game.updateCurrentEventIndex();
        });
    }

    /**
     * 触发特定类型的事件
     */
    private static void triggerSpecificEvent(HyperSpleef game, String eventType) {
        switch (eventType) {
            case "darkness":
                applyDarkness(game);
                break;
            case "levitation":
                applyLevitation(game);
                break;
            case "snowSupply":
                applySnowSupply(game);
                break;
            case "tntSupply":
                applyTNTSupply(game);
                break;
            case "slowFall":
                applySlowFall(game);
                break;
            case "jumpBoost":
                applyJumpBoost(game);
                break;
        }
    }

    /**
     * 触发随机事件
     */
    private static void triggerRandomEvent(HyperSpleef game, int eventNumber) {
        List<String> events = Arrays.asList(
                "darkness", // 天黑请闭眼
                "levitation", // 飘浮
                "snowSupply", // 雪块补给
                "tntSupply", // TNT补给
                "slowFall", // 失重
                "jumpBoost" // 超绝弹射力
        );

        String selectedEvent = events.get(random.nextInt(events.size()));

        switch (selectedEvent) {
            case "darkness":
                applyDarkness(game);
                break;
            case "levitation":
                applyLevitation(game);
                break;
            case "snowSupply":
                applySnowSupply(game);
                break;
            case "tntSupply":
                applyTNTSupply(game);
                break;
            case "slowFall":
                applySlowFall(game);
                break;
            case "jumpBoost":
                applyJumpBoost(game);
                break;
        }
    }

    /**
     * 天黑请闭眼：10s 失明
     */
    private static void applyDarkness(HyperSpleef game) {
        MCEMessenger.sendGlobalInfo("<dark_purple><bold>随机事件：天黑请闭眼！</bold></dark_purple>");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Active") &&
                    !player.getScoreboardTags().contains("dead")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0, false, false));
            }
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                MCEMessenger.sendGlobalInfo("<green>天黑请闭眼效果结束！</green>");
            }
        };
        task.runTaskLater(plugin, 200);
        game.addEventTask(task);
    }

    /**
     * 飘浮：5s 飘浮 IV
     */
    private static void applyLevitation(HyperSpleef game) {
        MCEMessenger.sendGlobalInfo("<aqua><bold>随机事件：飘浮！</bold></aqua>");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Active") &&
                    !player.getScoreboardTags().contains("dead")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 3, false, false));
            }
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                MCEMessenger.sendGlobalInfo("<green>飘浮效果结束！</green>");
            }
        };
        task.runTaskLater(plugin, 100);
        game.addEventTask(task);
    }

    /**
     * 雪块补给：给所有玩家9个雪块
     */
    private static void applySnowSupply(HyperSpleef game) {
        MCEMessenger.sendGlobalInfo("<white><bold>随机事件：雪块补给！</bold></white>");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Active") &&
                    !player.getScoreboardTags().contains("dead")) {
                player.getInventory().addItem(new ItemStack(Material.SNOW_BLOCK, 9));
            }
        }
    }

    /**
     * TNT补给：给所有玩家7个TNT
     */
    private static void applyTNTSupply(HyperSpleef game) {
        MCEMessenger.sendGlobalInfo("<red><bold>随机事件：TNT补给！</bold></red>");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Active") &&
                    !player.getScoreboardTags().contains("dead")) {
                player.getInventory().addItem(new ItemStack(Material.TNT, 7));
            }
        }
    }

    /**
     * 失重：5s 缓降 IV
     */
    private static void applySlowFall(HyperSpleef game) {
        MCEMessenger.sendGlobalInfo("<yellow><bold>随机事件：失重！</bold></yellow>");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Active") &&
                    !player.getScoreboardTags().contains("dead")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 3, false, false));
            }
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                MCEMessenger.sendGlobalInfo("<green>失重效果结束！</green>");
            }
        };
        task.runTaskLater(plugin, 100);
        game.addEventTask(task);
    }

    /**
     * 超绝弹射力：10s 跳跃提升 V
     */
    private static void applyJumpBoost(HyperSpleef game) {
        MCEMessenger.sendGlobalInfo("<gold><bold>随机事件：超绝弹射力！</bold></gold>");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Active") &&
                    !player.getScoreboardTags().contains("dead")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, 4, false, false));
            }
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                MCEMessenger.sendGlobalInfo("<green>超绝弹射力效果结束！</green>");
            }
        };
        task.runTaskLater(plugin, 200);
        game.addEventTask(task);
    }

    /**
     * 地图变动前10秒提示
     */
    private static void sendMapChangeWarning(HyperSpleef game) {
        MCEMessenger.sendGlobalTitle("<red><bold>地图变动即将开始！</bold></red>", "<yellow>10秒后...</yellow>");
    }

    /**
     * 触发地图变动
     */
    private static void triggerMapChange(HyperSpleef game) {
        MCEMessenger.sendGlobalInfo("<red><bold>地图变动！</bold></red>");

        World world = Bukkit.getWorld(game.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("HyperSpleef: 地图变动时世界不存在！");
            return;
        }

        // 填充第一个区域为空气：(-21, 190, -16) 到 (21, 205, 26)
        fillRegionWithAir(world, -21, 190, -16, 21, 205, 26);

        // 填充第二个区域为空气：(-14, 186, -9) 到 (14, 189, 19)
        fillRegionWithAir(world, -14, 186, -9, 14, 189, 19);

        // 发送title提示
        MCEMessenger.sendGlobalTitle("<red><bold>蛋糕没了啊！</bold></red>", null);
    }

    /**
     * 填充指定区域为空气
     */
    private static void fillRegionWithAir(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    block.setType(Material.AIR);
                    count++;
                }
            }
        }
        plugin.getLogger().info("HyperSpleef: 地图变动 - 填充区域 (" + minX + "," + minY + "," + minZ + ") 到 (" + maxX + ","
                + maxY + "," + maxZ + ") 为空气，共 " + count + " 个方块");
    }

    private static HyperSpleef hyperSpleef;

    public static void setHyperSpleef(HyperSpleef game) {
        hyperSpleef = game;
    }
}
