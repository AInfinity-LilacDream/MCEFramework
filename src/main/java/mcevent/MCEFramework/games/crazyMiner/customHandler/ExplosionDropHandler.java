package mcevent.MCEFramework.games.crazyMiner.customHandler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;
import static mcevent.MCEFramework.miscellaneous.Constants.crazyMiner;

/**
 * 让TNT爆炸破坏的方块遵循与玩家破坏相同的掉落与特殊处理逻辑。
 */
public class ExplosionDropHandler implements Listener {

    private boolean isActive = false;

    public ExplosionDropHandler() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void start() {
        isActive = true;
    }

    public void stop() {
        isActive = false;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isActive)
            return;
        if (crazyMiner == null)
            return;

        // 仅处理CrazyMiner游戏世界内的爆炸
        if (event.getLocation().getWorld() == null ||
                !event.getLocation().getWorld().getName().equals(crazyMiner.getWorldName())) {
            return;
        }

        // 关闭原版爆炸掉落，避免与自定义掉落冲突（尤其是铁/金矿爆炸掉原矿）
        try {
            event.setYield(0.0f);
        } catch (Throwable ignored) {
        }

        // 遍历所有被炸毁的方块，分别应用与BlockBreakHandler相同的逻辑
        // 取消默认掉落，自行按规则掉落
        event.blockList().removeIf(block -> {
            // 在此不移除方块，让爆炸完成后自然移除；我们只控制掉落
            return false;
        });

        java.util.Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            Material type = block.getType();
            Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);

            // 0) 矿物：TNT 炸矿也出“熔炼物”
            if (isOreBlock(type)) {
                // 由我们接管该方块的破坏与掉落，避免原版掉落原矿
                it.remove();
                block.setType(Material.AIR, false);
                ItemStack smelted = getSmeltedOre(type);
                if (smelted != null && smelted.getType() != Material.AIR) {
                    block.getWorld().dropItemNaturally(dropLoc, smelted);
                }
                if (type == Material.ANCIENT_DEBRIS) {
                    // 额外：1-2块远古残片 + 1个升级模板（与手动挖掘保持一致）
                    int scraps = 1 + (int) (Math.random() * 2);
                    block.getWorld().dropItemNaturally(dropLoc, new ItemStack(Material.NETHERITE_SCRAP, scraps));
                    block.getWorld().dropItemNaturally(dropLoc,
                            new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
                }
                continue;
            }

            // 1) 叶子：50%掉羽毛（保留苹果彩蛋）
            if (type.name().endsWith("_LEAVES")) {
                // 不生成原版掉落
                it.remove();
                block.setType(Material.AIR, false);
                if (Math.random() < 0.50) {
                    block.getWorld().dropItemNaturally(dropLoc, new ItemStack(Material.FEATHER, 1));
                } else if (type == Material.OAK_LEAVES && Math.random() < 0.04) {
                    block.getWorld().dropItemNaturally(dropLoc, new ItemStack(Material.APPLE, 1));
                }
                continue;
            }

            // 2) 泥土/粗泥土/灵魂沙：2% 概率生成奖励箱（与挖掘一致）
            if (type == Material.DIRT || type == Material.COARSE_DIRT || type == Material.SOUL_SAND) {
                if (Math.random() < 0.02) {
                    Location chestLoc = block.getLocation().clone();
                    // 延后一tick放置箱子，避免与爆炸方块更新冲突
                    Bukkit.getScheduler().runTaskLater(plugin, () -> spawnRewardChest(chestLoc), 1L);
                }
                // 不掉落原始方块
                it.remove();
                block.setType(Material.AIR, false);
                continue;
            }

            // 3) 苔藓块：12% 掉箭，88% 掉苔藓块
            switch (type) {
                case MOSS_BLOCK -> {
                    it.remove();
                    block.setType(Material.AIR, false);
                    if (Math.random() < 0.12) {
                        block.getWorld().dropItemNaturally(dropLoc, new ItemStack(Material.ARROW, 1));
                    } else {
                        block.getWorld().dropItemNaturally(dropLoc, new ItemStack(Material.MOSS_BLOCK, 1));
                    }
                    continue;
                }
                default -> {
                }
            }

            // 4) 其他方块：保持原逻辑（让爆炸摧毁，不额外改动）
        }
    }

    private boolean isOreBlock(Material blockType) {
        return switch (blockType) {
            case IRON_ORE, GOLD_ORE, DIAMOND_ORE, REDSTONE_ORE, LAPIS_ORE, COAL_ORE,
                    DEEPSLATE_IRON_ORE, DEEPSLATE_GOLD_ORE, DEEPSLATE_DIAMOND_ORE,
                    DEEPSLATE_REDSTONE_ORE, DEEPSLATE_LAPIS_ORE, DEEPSLATE_COAL_ORE,
                    EMERALD_ORE, DEEPSLATE_EMERALD_ORE, ANCIENT_DEBRIS ->
                true;
            default -> false;
        };
    }

    private ItemStack getSmeltedOre(Material oreType) {
        return switch (oreType) {
            case IRON_ORE, DEEPSLATE_IRON_ORE -> new ItemStack(Material.IRON_INGOT);
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> new ItemStack(Material.GOLD_INGOT);
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> new ItemStack(Material.DIAMOND);
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE ->
                new ItemStack(Material.REDSTONE, 4 + (int) (Math.random() * 2));
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> new ItemStack(Material.LAPIS_LAZULI, 4 + (int) (Math.random() * 5));
            case COAL_ORE, DEEPSLATE_COAL_ORE -> new ItemStack(Material.COAL);
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> new ItemStack(Material.EMERALD);
            case ANCIENT_DEBRIS -> new ItemStack(Material.NETHERITE_SCRAP);
            default -> null;
        };
    }

    // 复用BlockBreakHandler的奖励箱生成逻辑
    private void spawnRewardChest(Location chestLocation) {
        try {
            mcevent.MCEFramework.games.crazyMiner.customHandler.BlockBreakHandler handler = mcevent.MCEFramework.games.crazyMiner.customHandler.BlockBreakHandler.class
                    .getDeclaredConstructor().newInstance();
            java.lang.reflect.Method m = mcevent.MCEFramework.games.crazyMiner.customHandler.BlockBreakHandler.class
                    .getDeclaredMethod("spawnRewardChest", Location.class);
            m.setAccessible(true);
            m.invoke(handler, chestLocation);
        } catch (Throwable ignored) {
            // 发生异常则降级：直接放一个空箱子
            chestLocation.getBlock().setType(Material.CHEST);
        }
    }
}
