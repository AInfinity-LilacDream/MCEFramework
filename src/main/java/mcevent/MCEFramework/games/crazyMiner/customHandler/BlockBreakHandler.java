package mcevent.MCEFramework.games.crazyMiner.customHandler;

import mcevent.MCEFramework.games.crazyMiner.CrazyMinerFuncImpl;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;
import static mcevent.MCEFramework.miscellaneous.Constants.crazyMiner;

public class BlockBreakHandler implements Listener {

    private boolean isActive = false;
    private final java.util.Map<java.util.UUID, Long> lastDeathHandledAt = new java.util.HashMap<>();

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

        // Only handle blocks broken by survival players with Participant tag
        if (player.getGameMode() != GameMode.SURVIVAL ||
                !player.getScoreboardTags().contains("Participant")) {
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
                    // 额外掉落：1-2块远古残片 + 1个下界合金升级模板
                    Location loc = event.getBlock().getLocation();
                    int scraps = 1 + (int) (Math.random() * 2);
                    event.getBlock().getWorld().dropItemNaturally(loc, new ItemStack(Material.NETHERITE_SCRAP, scraps));
                    event.getBlock().getWorld().dropItemNaturally(loc,
                            new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
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

        // Only handle active game players (Participant)
        if (!deadPlayer.getScoreboardTags().contains("Participant")) {
            return;
        }

        // 仅处理 CrazyMiner 游戏世界中的死亡
        if (deadPlayer.getWorld() == null
                || crazyMiner == null
                || !deadPlayer.getWorld().getName().equals(crazyMiner.getWorldName())) {
            return;
        }

        // 防抖：短时间内同一玩家重复死亡事件只处理一次
        long now = System.currentTimeMillis();
        Long last = lastDeathHandledAt.get(deadPlayer.getUniqueId());
        if (last != null && (now - last) < 1500L) {
            return;
        }
        lastDeathHandledAt.put(deadPlayer.getUniqueId(), now);
        // 简单清理过期记录
        if (lastDeathHandledAt.size() > 128) {
            java.util.Iterator<java.util.Map.Entry<java.util.UUID, Long>> it = lastDeathHandledAt.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<java.util.UUID, Long> e = it.next();
                if ((now - e.getValue()) > 60000L)
                    it.remove();
            }
        }

        // Prevent item dropping on death
        event.getDrops().clear();
        event.setDroppedExp(0);

        // 统一淘汰处理（消息+音效+旁观）与展示板更新（放到淘汰后，确保计数基于旁观状态）
        crazyMiner.setDelayedTask(0.05, () -> {
            mcevent.MCEFramework.customHandler.GlobalEliminationHandler.eliminateNow(deadPlayer);
            CrazyMinerFuncImpl.updateGameBoardOnPlayerDeath(crazyMiner, deadPlayer);
        });
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
        // 树叶：50% 概率掉落羽毛（覆盖所有 *_LEAVES）
        if (blockType.name().endsWith("_LEAVES")) {
            event.setDropItems(false);
            if (Math.random() < 0.50) {
                event.getBlock().getWorld().dropItemNaturally(blockLocation, new ItemStack(Material.FEATHER, 1));
            } else if (blockType == Material.OAK_LEAVES && Math.random() < 0.04) {
                // 橡树叶保留少量苹果掉落作为彩蛋
                event.getBlock().getWorld().dropItemNaturally(blockLocation, new ItemStack(Material.APPLE, 1));
            }
            return true;
        }

        // 泥土/粗泥土/灵魂沙：2% 概率生成奖励箱
        if (blockType == Material.DIRT || blockType == Material.COARSE_DIRT || blockType == Material.SOUL_SAND) {
            if (Math.random() < 0.02) {
                // 阻止默认掉落，1tick后在方块原位置生成箱子并填充战利品
                event.setDropItems(false);
                Location chestLoc = event.getBlock().getLocation().clone();
                Bukkit.getScheduler().runTaskLater(plugin, () -> spawnRewardChest(chestLoc), 1L);
                return true;
            }
        }

        switch (blockType) {
            case REDSTONE_ORE -> {
                event.setDropItems(false);
                if (Math.random() < 0.8) { // 80%概率
                    // 不掉落任何东西
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

    private void spawnRewardChest(Location chestLocation) {
        try {
            chestLocation.getBlock().setType(Material.CHEST);
            org.bukkit.block.BlockState state = chestLocation.getBlock().getState();
            if (!(state instanceof Chest chest)) {
                return;
            }
            Inventory inv = chest.getBlockInventory();

            // 按 SurvivalGame 的风格：随机 4-6 次抽取战利品条目（来自指定物品池）
            java.util.Random r = new java.util.Random();
            int rolls = 4 + r.nextInt(3); // 4-6 次
            for (int i = 0; i < rolls; i++) {
                // 先抽随机槽位，优先找空位
                int size = inv.getSize();
                int chosenSlot = -1;
                for (int attempt = 0; attempt < Math.min(size, 10); attempt++) {
                    int slot = r.nextInt(size);
                    ItemStack cur = inv.getItem(slot);
                    if (cur == null || cur.getType() == Material.AIR) {
                        chosenSlot = slot;
                        break;
                    }
                }
                if (chosenSlot < 0) {
                    chosenSlot = r.nextInt(size); // 没有空位则覆盖随机槽位
                }

                // 再抽具体物品（数量从0开始，可能不生成）
                int type = r.nextInt(13); // 原9类 + 4种药水/图腾
                Material mat;
                int amount;
                switch (type) {
                    case 0 -> {
                        mat = Material.ARROW;
                        amount = r.nextInt(6);
                    } // 0-5
                    case 1 -> {
                        mat = Material.DIAMOND;
                        amount = r.nextInt(3);
                    } // 0-2
                    case 2 -> {
                        mat = Material.GOLD_INGOT;
                        amount = r.nextInt(5);
                    } // 0-4
                    case 3 -> {
                        mat = Material.APPLE;
                        amount = r.nextInt(5);
                    } // 0-4
                    case 4 -> {
                        mat = Material.FLINT;
                        amount = r.nextInt(3);
                    } // 0-2
                    case 5 -> {
                        mat = Material.FEATHER;
                        amount = r.nextInt(7);
                    } // 0-6
                    case 6 -> {
                        mat = Material.COBWEB;
                        amount = r.nextInt(6);
                    } // 0-5
                    case 7 -> {
                        mat = Material.ENDER_PEARL;
                        amount = r.nextInt(2);
                    } // 0-1
                    case 8 -> {
                        mat = Material.TNT;
                        amount = 1 + r.nextInt(4);
                    } // 1-4
                    case 9 -> {
                        // 不死图腾 0-1
                        mat = Material.TOTEM_OF_UNDYING;
                        amount = r.nextInt(2);
                    }
                    case 10 -> {
                        // 喷溅型剧毒药水 0-1
                        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.SPLASH_POTION,
                                1);
                        org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item
                                .getItemMeta();
                        if (pm != null) {
                            pm.setBasePotionType(org.bukkit.potion.PotionType.POISON);
                            item.setItemMeta(pm);
                        }
                        if (r.nextBoolean())
                            inv.setItem(chosenSlot, item);
                        continue; // 已直接放入
                    }
                    case 11 -> {
                        // 滞留型治疗药水 0-1（瞬间治疗）
                        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(
                                Material.LINGERING_POTION, 1);
                        org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item
                                .getItemMeta();
                        if (pm != null) {
                            pm.setBasePotionType(org.bukkit.potion.PotionType.HEALING);
                            item.setItemMeta(pm);
                        }
                        if (r.nextBoolean())
                            inv.setItem(chosenSlot, item);
                        continue;
                    }
                    case 12 -> {
                        // 喷溅型瞬间伤害药水 0-1
                        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.SPLASH_POTION,
                                1);
                        org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item
                                .getItemMeta();
                        if (pm != null) {
                            pm.setBasePotionType(org.bukkit.potion.PotionType.HARMING);
                            item.setItemMeta(pm);
                        }
                        if (r.nextBoolean())
                            inv.setItem(chosenSlot, item);
                        continue;
                    }
                    default -> {
                        mat = Material.AIR;
                        amount = 0;
                    }
                }
                if (amount <= 0) {
                    continue; // 本次不放置物品
                }
                inv.setItem(chosenSlot, new ItemStack(mat, amount));
            }
        } catch (Throwable ignored) {
        }
    }

    // 20% 概率掉燧石（对GRAVEL）
    @org.bukkit.event.EventHandler
    public void onGravelBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (!isActive)
            return;
        if (event.getBlock().getType() != Material.GRAVEL)
            return;
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL
                || !event.getPlayer().getScoreboardTags().contains("Participant"))
            return;
        if (Math.random() < 0.20) {
            event.setDropItems(false);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                    new ItemStack(Material.FLINT, 1));
        }
    }
}