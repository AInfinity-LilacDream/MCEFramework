package mcevent.MCEFramework.games.crazyMiner.customHandler;

import mcevent.MCEFramework.games.crazyMiner.CrazyMinerFuncImpl;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;
import static mcevent.MCEFramework.miscellaneous.Constants.crazyMiner;

public class BlockBreakHandler implements Listener {

    private boolean isActive = false;

    public BlockBreakHandler() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void start() {
        isActive = true;
    }

    public void stop() {
        isActive = false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isActive)
            return;

        Player player = event.getPlayer();

        // Only handle blocks broken by survival players with Active tag
        if (player.getGameMode() != GameMode.SURVIVAL ||
                !player.getScoreboardTags().contains("Active")) {
            return;
        }

        Material brokenBlock = event.getBlock().getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Location blockLocation = event.getBlock().getLocation().add(0.5, 0.5, 0.5);

        // 1. 镐子挖矿物方块的特殊处理
        if (tool != null && tool.getType().name().contains("PICKAXE") && isOreBlock(brokenBlock)) {
            if (canPickaxeBreakOre(tool.getType(), brokenBlock)) {
                event.setDropItems(false); // 取消默认掉落
                ItemStack smeltedDrop = getSmeltedOre(brokenBlock);
                if (smeltedDrop != null) {
                    event.getBlock().getWorld().dropItemNaturally(blockLocation, smeltedDrop);
                }

                // 掉落挖矿经验
                int expToDrop = getOreExperience(brokenBlock);
                if (expToDrop > 0) {
                    player.giveExp(expToDrop);
                }

                // 远古残骸特殊处理
                if (brokenBlock == Material.ANCIENT_DEBRIS) {
                    CrazyMinerFuncImpl.handleAncientDebrisBreak(player, event.getBlock().getLocation());
                }
            } else {
                // 镐子等级不够，取消掉落
                event.setDropItems(false);
            }
            return;
        }

        // 2. 特殊方块处理
        if (handleSpecialBlocks(event, brokenBlock, blockLocation)) {
            return; // 特殊方块已处理，直接返回
        }

        // 3. 其余情况按原版默认逻辑（不做任何处理，让原版逻辑生效）
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isActive)
            return;

        Player deadPlayer = event.getEntity();

        // Only handle active game players
        if (!deadPlayer.getScoreboardTags().contains("Active")) {
            return;
        }

        // Prevent item dropping on death
        event.getDrops().clear();
        event.setDroppedExp(0);

        // 统一淘汰处理（消息+音效+旁观）
        crazyMiner.setDelayedTask(0.05, () -> {
            mcevent.MCEFramework.customHandler.GlobalEliminationHandler.eliminateNow(deadPlayer);
        });

        // CrazyMiner 特有：更新计分板
        CrazyMinerFuncImpl.updateGameBoardOnPlayerDeath(crazyMiner, deadPlayer);
    }

    /**
     * 检查是否是矿物方块
     */
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

    /**
     * 检查镐子是否能挖掘指定矿物
     */
    private boolean canPickaxeBreakOre(Material pickaxeType, Material oreType) {
        return switch (pickaxeType) {
            case WOODEN_PICKAXE -> switch (oreType) {
                // 木镐只能挖煤矿
                case COAL_ORE, DEEPSLATE_COAL_ORE -> true;
                default -> false;
            };

            case STONE_PICKAXE -> switch (oreType) {
                // 石镐能挖煤矿、铁矿、青金石矿
                case COAL_ORE, DEEPSLATE_COAL_ORE,
                        IRON_ORE, DEEPSLATE_IRON_ORE,
                        LAPIS_ORE, DEEPSLATE_LAPIS_ORE ->
                    true;
                default -> false;
            };

            case IRON_PICKAXE -> switch (oreType) {
                // 铁镐能挖大部分矿物，但不能挖远古残骸
                case COAL_ORE, DEEPSLATE_COAL_ORE,
                        IRON_ORE, DEEPSLATE_IRON_ORE,
                        GOLD_ORE, DEEPSLATE_GOLD_ORE,
                        DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                        REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                        EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                        LAPIS_ORE, DEEPSLATE_LAPIS_ORE ->
                    true;
                case ANCIENT_DEBRIS -> false;
                default -> false;
            };

            case DIAMOND_PICKAXE, NETHERITE_PICKAXE -> true; // 钻石镐和下界合金镐能挖所有矿物

            case GOLDEN_PICKAXE -> switch (oreType) {
                // 金镐虽然快但很脆弱，只能挖基础矿物
                case COAL_ORE, DEEPSLATE_COAL_ORE,
                        IRON_ORE, DEEPSLATE_IRON_ORE,
                        LAPIS_ORE, DEEPSLATE_LAPIS_ORE ->
                    true;
                default -> false;
            };

            default -> false;
        };
    }

    /**
     * 获取矿物的熔炼产物
     */
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

    /**
     * 获取矿物破坏时的经验掉落
     */
    private int getOreExperience(Material oreType) {
        return switch (oreType) {
            // 煤矿: 0-2 经验
            case COAL_ORE, DEEPSLATE_COAL_ORE -> (int) (Math.random() * 3);

            // 铁矿和金矿: 0-0 经验 (原版熔炼后才有经验)
            case IRON_ORE, DEEPSLATE_IRON_ORE,
                    GOLD_ORE, DEEPSLATE_GOLD_ORE ->
                0;

            // 钻石矿: 3-7 经验
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> 3 + (int) (Math.random() * 5);

            // 红石矿: 1-5 经验
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> 1 + (int) (Math.random() * 5);

            // 青金石矿: 2-5 经验
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> 2 + (int) (Math.random() * 4);

            // 绿宝石矿: 3-7 经验
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> 3 + (int) (Math.random() * 5);

            // 远古残骸: 0-0 经验 (原版不给经验)
            case ANCIENT_DEBRIS -> 0;

            default -> 0;
        };
    }

    /**
     * 处理特殊方块
     */
    private boolean handleSpecialBlocks(BlockBreakEvent event, Material blockType, Location blockLocation) {
        switch (blockType) {
            case REDSTONE_ORE -> {
                event.setDropItems(false);
                if (Math.random() < 0.8) { // 80%概率
                    // 不掉落任何东西
                }
                return true;
            }
            case OAK_LEAVES -> {
                event.setDropItems(false);
                if (Math.random() < 0.04) { // 4%概率
                    event.getBlock().getWorld().dropItemNaturally(blockLocation, new ItemStack(Material.APPLE, 1));
                }
                return true;
            }
            case MOSS_BLOCK -> {
                event.setDropItems(false);
                if (Math.random() < 0.12) { // 12%概率
                    event.getBlock().getWorld().dropItemNaturally(blockLocation, new ItemStack(Material.ARROW, 1));
                } else { // 88%概率
                    event.getBlock().getWorld().dropItemNaturally(blockLocation, new ItemStack(Material.MOSS_BLOCK, 1));
                }
                return true;
            }
            default -> {
                return false; // 不是特殊方块
            }
        }
    }
}