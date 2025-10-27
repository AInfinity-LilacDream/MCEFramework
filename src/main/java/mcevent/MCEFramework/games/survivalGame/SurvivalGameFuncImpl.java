package mcevent.MCEFramework.games.survivalGame;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.survivalGame.gameObject.SurvivalGameGameBoard;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
SurvivalGameFuncImpl: 封装SurvivalGame游戏逻辑函数
 */
public class SurvivalGameFuncImpl {

    private static final SurvivalGameConfigParser survivalGameConfigParser = survivalGame.getSurvivalGameConfigParser();

    // 战利品表缓存（首次使用时加载）
    private static Map<String, TypeLoot> lootTableByType;
    private static int lootTypeWeightTotal;
    // 记录本局中玩家死亡生成的箱子位置
    private static final java.util.Set<Location> deathChestLocations = new java.util.HashSet<>();
    // 记录本回合玩家击杀数
    private static final java.util.Map<java.util.UUID, Integer> playerKillCounts = new java.util.HashMap<>();
    // 记录本回合队伍的淘汰顺序（先被淘汰的先入列）
    private static final java.util.List<Team> teamEliminationOrder = new java.util.ArrayList<>();
    // 记录玩家在本世界放置的方块坐标（块坐标）
    private static final java.util.Set<org.bukkit.Location> playerPlacedBlocks = new java.util.HashSet<>();

    // 从配置文件加载数据
    protected static void loadConfig() {
        survivalGame.setIntroTextList(
                survivalGame.getSurvivalGameConfigParser().openAndParse(survivalGame.getConfigFileName()));
        survivalGame.setSpawnPoints(survivalGameConfigParser.getSpawnPoints());
        survivalGame.setChestLocations(survivalGameConfigParser.getChestLocations());
        survivalGame.setCenterLocation(survivalGameConfigParser.getCenterLocation());
    }

    // 分配出生点
    protected static void assignSpawnPoints() {
        List<Location> spawnPoints = new ArrayList<>(survivalGame.getSpawnPoints());
        Collections.shuffle(spawnPoints);

        List<Team> activeTeams = survivalGame.getActiveTeams();
        int spawnIndex = 0;

        for (Team team : activeTeams) {
            if (spawnIndex >= spawnPoints.size()) {
                plugin.getLogger().warning("出生点不足！");
                break;
            }

            Location spawnPoint = spawnPoints.get(spawnIndex);
            spawnIndex++;

            // 传送该队伍的所有玩家到该出生点
            for (Player player : Bukkit.getOnlinePlayers()) {
                Team playerTeam = MCETeamUtils.getTeam(player);
                if (playerTeam != null && playerTeam.equals(team)) {
                    player.teleport(spawnPoint);
                }
            }
        }
    }

    // 刷新战利品箱
    protected static void spawnLootChests() {
        List<Location> chestLocations = survivalGame.getChestLocations();
        World world = Bukkit.getWorld(survivalGame.getWorldName());

        if (world == null) {
            plugin.getLogger()
                    .warning("[SurvivalGame] spawnLootChests: world is null for name=" + survivalGame.getWorldName());
            return;
        }

        if (chestLocations == null || chestLocations.isEmpty()) {
            plugin.getLogger().warning("[SurvivalGame] spawnLootChests: no chest locations configured.");
        }

        // 懒加载战利品表
        if (lootTableByType == null || lootTableByType.isEmpty()) {
            loadLootTables();
            plugin.getLogger().info("[SurvivalGame] loot tables loaded. types="
                    + (lootTableByType == null ? 0 : lootTableByType.size()));
        }

        // (调试已移除)

        java.util.List<Location> locs = (chestLocations != null ? chestLocations
                : java.util.Collections.<Location>emptyList());
        for (Location loc : locs) {
            Block block = world.getBlockAt(loc);
            // 仅当当前位置已存在箱子时才清空并重新填充
            if (block.getState() instanceof Chest chest) {
                Inventory chestInv = chest.getBlockInventory();
                chestInv.clear();
                fillChestInventory(chestInv);
            }
        }
    }

    // 清理战利品箱（不删除方块，仅清空已有箱子的物品）
    protected static void clearLootChests() {
        List<Location> chestLocations = survivalGame.getChestLocations();
        World world = Bukkit.getWorld(survivalGame.getWorldName());

        if (world == null)
            return;

        for (Location loc : chestLocations) {
            Block block = world.getBlockAt(loc);
            if (block.getState() instanceof Chest chest) {
                chest.getBlockInventory().clear();
            }
        }
    }

    // 使用原版 Lock 机制在准备阶段上锁所有配置的箱子（需手持同名物品才可打开）
    protected static void lockLootChests() {
        List<Location> chestLocations = survivalGame.getChestLocations();
        World world = Bukkit.getWorld(survivalGame.getWorldName());
        if (world == null)
            return;

        String lockKey = "MCE_LOCK"; // 不发放同名物品，即视为不可开
        int locked = 0;
        for (Location loc : chestLocations) {
            Block block = world.getBlockAt(loc);
            if (block.getState() instanceof Container container) {
                container.setLock(lockKey);
                container.update();
                locked++;
            }
        }
        plugin.getLogger().info("[SurvivalGame] 已上锁箱子数量=" + locked);
    }

    // 解除原版 Lock 上锁
    protected static void unlockLootChests() {
        List<Location> chestLocations = survivalGame.getChestLocations();
        World world = Bukkit.getWorld(survivalGame.getWorldName());
        if (world == null)
            return;

        int unlocked = 0;
        for (Location loc : chestLocations) {
            Block block = world.getBlockAt(loc);
            if (block.getState() instanceof Container container) {
                if (container.isLocked()) {
                    // 置为 null 清除锁（避免空字符串仍然判定为锁名）
                    container.setLock(null);
                    container.update();
                    unlocked++;
                }
            }
        }
        plugin.getLogger().info("[SurvivalGame] 已解除上锁箱子数量=" + unlocked);
    }

    // 设置世界边界（中心从配置读取，大小使用 initialBorderSize）
    protected static void setupWorldBorder() {
        World world = Bukkit.getWorld(survivalGame.getWorldName());
        if (world == null)
            return;

        WorldBorder border = world.getWorldBorder();
        // 固定世界中心
        border.setCenter(313, 495.5);
        border.setSize(Math.max(1, survivalGame.getInitialBorderSize()));
    }

    // 重置世界边界（中心从配置读取，大小使用 initialBorderSize）
    protected static void resetWorldBorder() {
        World world = Bukkit.getWorld(survivalGame.getWorldName());
        if (world == null)
            return;

        WorldBorder border = world.getWorldBorder();
        // 固定世界中心
        border.setCenter(313, 495.5);
        border.setSize(Math.max(1, survivalGame.getInitialBorderSize()));
    }

    // 开始第一次缩圈
    protected static void startFirstBorderShrink() {
        World world = Bukkit.getWorld(survivalGame.getWorldName());
        if (world == null)
            return;

        WorldBorder border = world.getWorldBorder();
        // 固定缩圈中心
        border.setCenter(313, 495.5);
        border.setSize(survivalGame.getFirstShrinkSize(), 120); // 120秒缩圈到20x20

        MCEMessenger.sendGlobalInfo("<red><bold>第一次缩圈开始！（用时2分钟）</bold></red>");
        MCEMessenger.sendGlobalInfo("<yellow>边界将在2分钟内收缩至 " + survivalGame.getFirstShrinkSize() + "x"
                + survivalGame.getFirstShrinkSize() + "！</yellow>");
    }

    // 开始最终决战
    protected static void startFinalBattle() {
        World world = Bukkit.getWorld(survivalGame.getWorldName());
        if (world == null)
            return;

        WorldBorder border = world.getWorldBorder();
        // 固定缩圈中心
        border.setCenter(313, 495.5);
        border.setSize(survivalGame.getFinalShrinkSize(), 60); // 60秒缩圈

        MCEMessenger.sendGlobalInfo("<red><bold>第二次缩圈开始！（用时1分钟）</bold></red>");
        MCEMessenger.sendGlobalInfo("<yellow>边界将在1分钟内收缩至 " + survivalGame.getFinalShrinkSize() + "x"
                + survivalGame.getFinalShrinkSize() + "！</yellow>");

        // 1.5分钟后检查胜利条件
        survivalGame.setDelayedTask(90, SurvivalGameFuncImpl::checkWinCondition);
    }

    // ======================== 战利品生成实现 ========================

    private static void fillChestInventory(Inventory inventory) {
        if (lootTableByType == null || lootTableByType.isEmpty())
            return;

        Random random = new Random();
        int itemCount = 4 + random.nextInt(3);
        for (int i = 0; i < itemCount; i++) {
            // 优先抽到非AIR的条目
            LootItem lootItem = null;
            for (int attempt = 0; attempt < 5; attempt++) {
                TypeLoot typeLoot = rollType(random);
                if (typeLoot == null)
                    continue;
                LootItem candidate = typeLoot.rollItem(random);
                if (candidate == null || candidate.material == Material.AIR)
                    continue;
                lootItem = candidate;
                break;
            }
            if (lootItem == null)
                continue;

            int amount = lootItem.minAmount >= lootItem.maxAmount ? lootItem.minAmount
                    : lootItem.minAmount + random.nextInt(lootItem.maxAmount - lootItem.minAmount + 1);

            ItemStack stack = new ItemStack(lootItem.material,
                    Math.max(1, Math.min(amount, lootItem.material.getMaxStackSize())));
            applyEffectsIfAny(stack, lootItem);
            applyPotionForTippedArrowIfAny(stack, lootItem);
            applyPotionBaseTypeIfAny(stack, lootItem);
            applyBookEnchantmentIfAny(stack, lootItem);

            // 随机空槽位优先放置，否则覆盖随机槽位
            int size = inventory.getSize();
            boolean placed = false;
            for (int attempt = 0; attempt < Math.min(size, 10); attempt++) {
                int slot = random.nextInt(size);
                ItemStack cur = inventory.getItem(slot);
                if (cur == null || cur.getType() == Material.AIR) {
                    inventory.setItem(slot, stack);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                int slot = random.nextInt(inventory.getSize());
                inventory.setItem(slot, stack);
            }
        }

    }

    private static TypeLoot rollType(Random random) {
        int r = random.nextInt(Math.max(1, lootTypeWeightTotal));
        int acc = 0;
        for (TypeLoot tl : lootTableByType.values()) {
            acc += tl.typeSelectedWeight;
            if (r < acc)
                return tl;
        }
        return null;
    }

    private static void loadLootTables() {
        lootTableByType = new LinkedHashMap<>();
        lootTypeWeightTotal = 0;

        Path root = plugin.getDataPath().resolve("MCEConfig").resolve("survival_game_loot_table");
        List<String> lootConfigLines = readTextLines(root.resolve("lootConfig.cfg"),
                "/MCEConfig/survival_game_loot_table/lootConfig.cfg");
        if (lootConfigLines.isEmpty())
            return;

        // 解析 type_mapping
        Map<Integer, String> indexToType = new LinkedHashMap<>();
        String section = null;
        for (String raw : lootConfigLines) {
            String line = raw.trim();
            if (line.isEmpty())
                continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1);
                continue;
            }
            if ("type_mapping".equals(section)) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    try {
                        int idx = Integer.parseInt(parts[0].trim());
                        String type = parts[1].trim();
                        indexToType.put(idx, type);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // 构建每种类型的战利品
        for (String type : indexToType.values()) {
            TypeLoot tl = new TypeLoot(type);

            // 读取 lootData/<type>.cfg
            List<String> dataLines = readTextLines(root.resolve("lootData").resolve(type + ".cfg"),
                    "/MCEConfig/survival_game_loot_table/lootData/" + type + ".cfg");
            parseTypeData(tl, dataLines);

            // 读取 lootPool/<type>.cfg
            List<String> poolLines = readTextLines(root.resolve("lootPool").resolve(type + ".cfg"),
                    "/MCEConfig/survival_game_loot_table/lootPool/" + type + ".cfg");
            parseTypePool(tl, poolLines);

            if (!tl.items.isEmpty() && tl.typeSelectedWeight > 0 && tl.totalItemChance > 0) {
                lootTableByType.put(type, tl);
                lootTypeWeightTotal += tl.typeSelectedWeight;
            }
        }
    }

    private static void parseTypeData(TypeLoot tl, List<String> lines) {
        String section = null;
        int currentIndex = -1;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty())
                continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1);
                try {
                    currentIndex = Integer.parseInt(section);
                } catch (NumberFormatException e) {
                    currentIndex = -1;
                }
                continue;
            }
            if ("meta".equals(section)) {
                if (line.startsWith("selected_chance=")) {
                    try {
                        tl.typeSelectedWeight = Integer.parseInt(line.substring("selected_chance=".length()).trim());
                    } catch (NumberFormatException ignored) {
                    }
                } else if (line.startsWith("total_chance=")) {
                    try {
                        tl.totalItemChance = Integer.parseInt(line.substring("total_chance=".length()).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
                continue;
            }
            if (currentIndex >= 0) {
                LootItem item = tl.items.computeIfAbsent(currentIndex, k -> new LootItem());
                if (line.startsWith("chance=")) {
                    try {
                        item.chance = Integer.parseInt(line.substring("chance=".length()).trim());
                    } catch (NumberFormatException ignored) {
                    }
                } else if (line.startsWith("min=")) {
                    try {
                        item.minAmount = Integer.parseInt(line.substring("min=".length()).trim());
                    } catch (NumberFormatException ignored) {
                    }
                } else if (line.startsWith("max=")) {
                    try {
                        item.maxAmount = Integer.parseInt(line.substring("max=".length()).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }

    private static void parseTypePool(TypeLoot tl, List<String> lines) {
        String section = null;
        int currentIndex = -1;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty())
                continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1);
                try {
                    currentIndex = Integer.parseInt(section);
                } catch (NumberFormatException e) {
                    currentIndex = -1;
                }
                continue;
            }
            if (currentIndex >= 0) {
                LootItem item = tl.items.computeIfAbsent(currentIndex, k -> new LootItem());
                // 解析物品定义
                String def = line;
                String[] parts = def.split("\\|");
                String materialName = parts[0].trim();
                Material material;
                try {
                    material = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    material = Material.AIR; // 未知物品，跳过
                }
                item.material = material;

                // 解析附加参数（如 effect=...）
                for (int i = 1; i < parts.length; i++) {
                    String kv = parts[i].trim();
                    int eq = kv.indexOf('=');
                    if (eq <= 0)
                        continue;
                    String key = kv.substring(0, eq).trim();
                    String value = kv.substring(eq + 1).trim();
                    if ("effect".equalsIgnoreCase(key)) {
                        // 格式: effect=regeneration:160:0 (效果名:时长tick:等级)
                        String[] ev = value.split(":");
                        if (ev.length >= 1)
                            item.effectName = ev[0].trim();
                        if (ev.length >= 2) {
                            try {
                                item.effectDuration = Integer.parseInt(ev[1].trim());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        if (ev.length >= 3) {
                            try {
                                item.effectAmplifier = Integer.parseInt(ev[2].trim());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    } else if ("potion".equalsIgnoreCase(key)) {
                        // 药水箭: potion=harming 等
                        item.potionKey = value.trim();
                    } else if ("ench".equalsIgnoreCase(key)) {
                        // 附魔书: ench=sharpness:1
                        String[] ev = value.split(":");
                        if (ev.length >= 1)
                            item.enchantName = ev[0].trim();
                        if (ev.length >= 2) {
                            try {
                                item.enchantLevel = Integer.parseInt(ev[1].trim());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        }
    }

    private static List<String> readTextLines(Path dataPath, String resourcePath) {
        try {
            if (Files.exists(dataPath)) {
                return Files.readAllLines(dataPath);
            }
        } catch (IOException ignored) {
        }

        // 资源回退
        try (InputStream in = plugin
                .getResource(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath)) {
            if (in == null)
                return Collections.emptyList();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                List<String> out = new ArrayList<>();
                String s;
                while ((s = br.readLine()) != null)
                    out.add(s);
                return out;
            }
        } catch (IOException ignored) {
        }
        return Collections.emptyList();
    }

    private static class TypeLoot {
        final String type;
        final Map<Integer, LootItem> items = new LinkedHashMap<>();
        int typeSelectedWeight = 0;
        int totalItemChance = 0;

        TypeLoot(String type) {
            this.type = type;
        }

        LootItem rollItem(Random random) {
            if (items.isEmpty() || totalItemChance <= 0)
                return null;
            int r = random.nextInt(totalItemChance);
            int acc = 0;
            for (Map.Entry<Integer, LootItem> e : items.entrySet()) {
                LootItem li = e.getValue();
                acc += Math.max(0, li.chance);
                if (r < acc)
                    return li;
            }
            return null;
        }
    }

    private static class LootItem {
        Material material = Material.AIR;
        int chance = 0;
        int minAmount = 1;
        int maxAmount = 1;
        String effectName = null;
        int effectDuration = 0;
        int effectAmplifier = 0;
        String potionKey = null;
        String enchantName = null;
        int enchantLevel = 0;
    }

    private static void applyEffectsIfAny(ItemStack stack, LootItem lootItem) {
        if (lootItem.effectName == null || lootItem.effectName.isEmpty())
            return;
        PotionEffectType type = PotionEffectType.getByName(normalizeEffectName(lootItem.effectName));
        if (type == null)
            return;
        int duration = Math.max(1, lootItem.effectDuration);
        int amplifier = Math.max(0, lootItem.effectAmplifier);
        PotionEffect effect = new PotionEffect(type, duration, amplifier, false, true, true);

        if (stack.getType() == Material.SUSPICIOUS_STEW) {
            var meta = stack.getItemMeta();
            if (meta instanceof SuspiciousStewMeta stewMeta) {
                stewMeta.addCustomEffect(effect, true);
                stack.setItemMeta(stewMeta);
            }
        } else if (stack.getType() == Material.POTION || stack.getType() == Material.SPLASH_POTION
                || stack.getType() == Material.LINGERING_POTION) {
            var meta = stack.getItemMeta();
            if (meta instanceof PotionMeta potionMeta) {
                potionMeta.addCustomEffect(effect, true);
                stack.setItemMeta(potionMeta);
            }
        }
    }

    private static String normalizeEffectName(String raw) {
        String s = raw.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        // 常见别名修正
        if ("JUMP_BOOST".equals(s))
            return "JUMP";
        return s;
    }

    private static void applyPotionForTippedArrowIfAny(ItemStack stack, LootItem lootItem) {
        if (stack.getType() != Material.TIPPED_ARROW)
            return;
        if (lootItem.potionKey == null || lootItem.potionKey.isEmpty())
            return;
        var meta = stack.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) {
            org.bukkit.potion.PotionType type = resolvePotionType(lootItem.potionKey);
            if (type != null) {
                potionMeta.setBasePotionType(type);
                stack.setItemMeta(potionMeta);
            }
        }
    }

    // 对普通/喷溅/滞留型药水应用基础药水类型（当 loot 指定 potion=xxx 时）
    private static void applyPotionBaseTypeIfAny(ItemStack stack, LootItem lootItem) {
        Material t = stack.getType();
        if (t != Material.POTION && t != Material.SPLASH_POTION && t != Material.LINGERING_POTION)
            return;
        if (lootItem.potionKey == null || lootItem.potionKey.isEmpty())
            return;
        var meta = stack.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) {
            org.bukkit.potion.PotionType type = resolvePotionType(lootItem.potionKey);
            if (type != null) {
                potionMeta.setBasePotionType(type);
                stack.setItemMeta(potionMeta);
            }
        }
    }

    private static org.bukkit.potion.PotionType resolvePotionType(String key) {
        String k = key.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        // 常见别名映射
        if ("HARMING".equals(k))
            k = "INSTANT_DAMAGE";
        if ("HEALING".equals(k))
            k = "INSTANT_HEAL";
        try {
            return org.bukkit.potion.PotionType.valueOf(k);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void applyBookEnchantmentIfAny(ItemStack stack, LootItem lootItem) {
        if (stack.getType() != Material.ENCHANTED_BOOK)
            return;
        if (lootItem.enchantName == null || lootItem.enchantName.isEmpty())
            return;
        Enchantment ench = Enchantment
                .getByKey(org.bukkit.NamespacedKey.minecraft(lootItem.enchantName.toLowerCase(java.util.Locale.ROOT)));
        if (ench == null) {
            ench = Enchantment.getByName(lootItem.enchantName.toUpperCase(java.util.Locale.ROOT));
        }
        if (ench == null)
            return;
        int level = Math.max(1, lootItem.enchantLevel);
        var meta = stack.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            bookMeta.addStoredEnchant(ench, level, true);
            stack.setItemMeta(bookMeta);
        }
    }

    // 预加载并输出所有条目（进入游戏前调用）
    public static void preloadAndDumpLootTables() {
        if (lootTableByType == null || lootTableByType.isEmpty()) {
            loadLootTables();
        }
        if (lootTableByType == null || lootTableByType.isEmpty()) {
            plugin.getLogger().warning("[SurvivalGame] No loot tables available to dump.");
            return;
        }
        plugin.getLogger().info("[SurvivalGame] ===== Loot Table Dump Start =====");
        for (Map.Entry<String, TypeLoot> e : lootTableByType.entrySet()) {
            String type = e.getKey();
            TypeLoot tl = e.getValue();
            plugin.getLogger().info("[SurvivalGame] <" + type + "> selected_chance=" + tl.typeSelectedWeight
                    + ", total_item_chance=" + tl.totalItemChance);
            for (Map.Entry<Integer, LootItem> ent : tl.items.entrySet()) {
                Integer idx = ent.getKey();
                LootItem li = ent.getValue();
                String eff = (li.effectName != null && !li.effectName.isEmpty())
                        ? (li.effectName + ":" + li.effectDuration + ":" + li.effectAmplifier)
                        : "-";
                plugin.getLogger().info(
                        "[SurvivalGame]   [" + idx + "] material=" + li.material + ", chance=" + li.chance +
                                ", amount=" + li.minAmount + "-" + li.maxAmount + ", effect=" + eff);
            }
        }
        plugin.getLogger().info("[SurvivalGame] ===== Loot Table Dump End =====");
    }

    // 开始倒计时（支持延迟若干秒后再开始10秒倒计时，使其贴合阶段末尾）
    public static void startCountdown(int startDelaySeconds) {
        int delay = Math.max(0, startDelaySeconds);
        for (int i = 10; i >= 1; i--) {
            int seconds = i;
            survivalGame.setDelayedTask(delay + (10 - i), () -> {
                MCEMessenger.sendGlobalTitle(
                        "<yellow><bold>" + seconds + "</bold></yellow>",
                        "<gray>准备开始...</gray>");
                MCEPlayerUtils.globalPlaySound("minecraft:block.note_block.hat");
            });
        }

        survivalGame.setDelayedTask(delay + 10, () -> {
            MCEMessenger.sendGlobalTitle(
                    "<green><bold>开始！</bold></green>",
                    "");
            MCEPlayerUtils.globalPlaySound("minecraft:entity.player.levelup");
        });
    }

    // 检查胜利条件
    public static void checkWinCondition() {
        Map<Team, Integer> teamPlayerCounts = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                Team team = MCETeamUtils.getTeam(player);
                if (team != null) {
                    teamPlayerCounts.put(team, teamPlayerCounts.getOrDefault(team, 0) + 1);
                }
            }
        }

        // 如果只剩一个或零个队伍，游戏结束
        if (teamPlayerCounts.size() <= 1) {
            survivalGame.getTimeline().nextState();
        }
    }

    // 清空本回合击杀统计（在回合开始前调用）
    protected static void clearKillStats() {
        playerKillCounts.clear();
    }

    // 记录一次击杀
    public static void registerKill(Player killer) {
        if (killer == null)
            return;
        java.util.UUID uid = killer.getUniqueId();
        playerKillCounts.put(uid, playerKillCounts.getOrDefault(uid, 0) + 1);
    }

    // 清空淘汰顺序（在回合开始前调用）
    protected static void clearEliminationOrder() {
        teamEliminationOrder.clear();
    }

    // 注册某队伍被淘汰（只登记一次）
    public static void registerTeamElimination(Team team) {
        if (team == null)
            return;
        if (!teamEliminationOrder.contains(team)) {
            teamEliminationOrder.add(team);
        }
    }

    // 发送本回合队伍排名（仅看淘汰先后：幸存队伍在前，已淘汰队伍按淘汰时间逆序）
    protected static void sendRoundRanking() {
        // 统计存活队伍
        Map<Team, Integer> teamSurvivors = new LinkedHashMap<>();
        List<Team> activeTeams = survivalGame.getActiveTeams();
        for (Team team : activeTeams)
            teamSurvivors.put(team, 0);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                Team team = MCETeamUtils.getTeam(player);
                if (team != null && teamSurvivors.containsKey(team)) {
                    teamSurvivors.put(team, teamSurvivors.get(team) + 1);
                }
            }
        }

        List<Team> survivors = new ArrayList<>();
        List<Team> eliminated = new ArrayList<>();
        for (Map.Entry<Team, Integer> e : teamSurvivors.entrySet()) {
            if (e.getValue() > 0)
                survivors.add(e.getKey());
            else
                eliminated.add(e.getKey());
        }

        // 已淘汰队伍按淘汰顺序逆序（最后淘汰者靠前）
        List<Team> eliminatedOrdered = new ArrayList<>();
        for (int i = teamEliminationOrder.size() - 1; i >= 0; i--) {
            Team t = teamEliminationOrder.get(i);
            if (eliminated.contains(t))
                eliminatedOrdered.add(t);
        }
        for (Team t : eliminated)
            if (!eliminatedOrdered.contains(t))
                eliminatedOrdered.add(t);

        // 广播排名
        MCEMessenger.sendGlobalInfo("<gold><bold>本回合队伍排名（按淘汰先后）：</bold></gold>");
        int idx = 1;
        for (Team t : survivors) {
            String teamName = MCETeamUtils.getTeamColoredName(t);
            MCEMessenger.sendGlobalInfo("<yellow>" + idx + ". </yellow>" + teamName + " <gray>(存活)</gray>");
            idx++;
        }
        for (Team t : eliminatedOrdered) {
            String teamName = MCETeamUtils.getTeamColoredName(t);
            MCEMessenger.sendGlobalInfo("<yellow>" + idx + ". </yellow>" + teamName + " <red>(已淘汰)</red>");
            idx++;
        }
    }

    // 发送本回合击杀榜（按击杀数降序）
    protected static void sendKillRanking() {
        if (playerKillCounts.isEmpty()) {
            MCEMessenger.sendGlobalInfo("<gold><bold>本回合击杀榜：</bold></gold> <gray>无人击杀</gray>");
            return;
        }
        java.util.List<java.util.Map.Entry<java.util.UUID, Integer>> list = new java.util.ArrayList<>(
                playerKillCounts.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        MCEMessenger.sendGlobalInfo("<gold><bold>本回合击杀榜：</bold></gold>");
        int idx = 1;
        for (java.util.Map.Entry<java.util.UUID, Integer> e : list) {
            Player p = Bukkit.getPlayer(e.getKey());
            String name = (p != null) ? MCEPlayerUtils.getColoredPlayerName(p) : "<gray>离线玩家</gray>";
            MCEMessenger
                    .sendGlobalInfo("<yellow>" + idx + ". </yellow>" + name + " <gray>(" + e.getValue() + ")</gray>");
            idx++;
            if (idx > 10)
                break; // 只显示前10名
        }
    }

    // 初始化游戏展示板
    protected static void resetGameBoard() {
        SurvivalGameGameBoard gameBoard = (SurvivalGameGameBoard) survivalGame.getGameBoard();
        int participants = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equals(survivalGame.getWorldName())
                    && p.getScoreboardTags().contains("Participant") && p.getGameMode() != GameMode.SPECTATOR)
                participants++;
        }
        gameBoard.updatePlayerRemainTitle(participants);
        gameBoard.setTeamRemainCount(survivalGame.getActiveTeams().size());

        for (int i = 0; i < survivalGame.getActiveTeams().size(); ++i) {
            gameBoard.getTeamRemain()[i] = 0;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().equals(survivalGame.getWorldName()))
                continue;
            if (!player.getScoreboardTags().contains("Participant") || player.getGameMode() == GameMode.SPECTATOR)
                continue;
            Team team = MCETeamUtils.getTeam(player);
            if (team != null) {
                int teamId = survivalGame.getTeamId(team);
                if (teamId >= 0 && teamId < gameBoard.getTeamRemain().length) {
                    gameBoard.getTeamRemain()[teamId]++;
                }
            }
        }

        gameBoard.updateTeamRemainTitle(null);
    }

    // =============== 玩家放置方块记录 ===============
    public static void registerPlacedBlock(org.bukkit.Location loc) {
        if (loc == null)
            return;
        org.bukkit.World w = loc.getWorld();
        if (w == null)
            return;
        if (survivalGame == null || !w.getName().equals(survivalGame.getWorldName()))
            return;
        // 归一化到整块坐标
        org.bukkit.Location key = new org.bukkit.Location(w, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        playerPlacedBlocks.add(key);
    }

    public static boolean isPlayerPlaced(org.bukkit.Location loc) {
        if (loc == null)
            return false;
        org.bukkit.World w = loc.getWorld();
        if (w == null)
            return false;
        org.bukkit.Location key = new org.bukkit.Location(w, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return playerPlacedBlocks.contains(key);
    }

    public static void clearPlacedBlocks() {
        playerPlacedBlocks.clear();
    }

    // 将记录的玩家放置方块一一清理为 AIR，并清空记录
    public static void restoreAndClearPlayerPlacedBlocks() {
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(survivalGame.getWorldName());
        if (world == null) {
            playerPlacedBlocks.clear();
            return;
        }
        int removed = 0;
        java.util.Iterator<org.bukkit.Location> it = playerPlacedBlocks.iterator();
        while (it.hasNext()) {
            org.bukkit.Location loc = it.next();
            if (loc == null || loc.getWorld() == null || !loc.getWorld().equals(world)) {
                it.remove();
                continue;
            }
            world.getBlockAt(loc).setType(org.bukkit.Material.AIR, false);
            it.remove();
            removed++;
        }
        plugin.getLogger().info("[SurvivalGame] 已清理玩家放置方块数量=" + removed);
    }

    // 发送获胜消息
    protected static void sendWinningMessage() {
        StringBuilder message = new StringBuilder();
        boolean isFirst = true;

        Team winningTeam = null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                if (isFirst) {
                    winningTeam = MCETeamUtils.getTeam(player);
                }
                message.append(isFirst ? MCEPlayerUtils.getColoredPlayerName(player)
                        : "<dark_aqua>, </dark_aqua>" +
                                MCEPlayerUtils.getColoredPlayerName(player));
                isFirst = false;
            }
        }

        if (isFirst) {
            message.append("<red>所有玩家已被淘汰！</red>");
        } else {
            if (winningTeam != null) {
                message.append(" <dark_aqua>(</dark_aqua>")
                        .append(MCETeamUtils.getTeamColoredName(winningTeam))
                        .append("<dark_aqua>)</dark_aqua>");
            }
            message.append("<dark_aqua>获得了胜利！</dark_aqua>");
        }

        MCEMessenger.sendGlobalInfo(message.toString());
        MCEMainController.setRunningGame(false);
    }

    // 创建玩家死亡掉落箱子（从玩家当前背包快照复制）
    public static void createDeathChest(Player player, Location location) {
        World world = location.getWorld();
        if (world == null)
            return;

        Block block = world.getBlockAt(location);
        block.setType(Material.CHEST);
        // 记录该位置为死亡箱位置（使用整块坐标）
        Location keyLoc = new Location(world, block.getX(), block.getY(), block.getZ());
        deathChestLocations.add(keyLoc);
        plugin.getLogger().info("[SG][DeathDebug] chest placed at " + keyLoc.getBlockX() + "," + keyLoc.getBlockY()
                + "," + keyLoc.getBlockZ());

        if (block.getState() instanceof Chest chest) {
            Inventory chestInv = chest.getBlockInventory();
            PlayerInventory playerInv = player.getInventory();

            // 主物品栏（不包含护甲/副手）
            ItemStack[] storage = playerInv.getStorageContents();
            if (storage != null) {
                for (ItemStack it : storage) {
                    if (it != null && it.getType() != Material.AIR) {
                        chestInv.addItem(it.clone());
                    }
                }
            }

            // 护甲
            ItemStack[] armor = playerInv.getArmorContents();
            if (armor != null) {
                for (ItemStack it : armor) {
                    if (it != null && it.getType() != Material.AIR) {
                        chestInv.addItem(it.clone());
                    }
                }
            }

            // 副手
            ItemStack off = playerInv.getItemInOffHand();
            if (off != null && off.getType() != Material.AIR) {
                chestInv.addItem(off.clone());
            }

            // 不调用 update(true) 以免覆盖库存

            // 清空玩家背包（主物品、护甲、副手）
            playerInv.clear();
            playerInv.setArmorContents(null);
            playerInv.setItemInOffHand(null);
        }
    }

    // 创建玩家死亡掉落箱子（从事件 drops 列表复制）
    public static void createDeathChest(Player player, Location location, java.util.Collection<ItemStack> drops) {
        World world = location.getWorld();
        if (world == null)
            return;

        Block block = world.getBlockAt(location);
        block.setType(Material.CHEST);
        Location keyLoc = new Location(world, block.getX(), block.getY(), block.getZ());
        deathChestLocations.add(keyLoc);
        plugin.getLogger().info("[SG][DeathDebug] chest placed at " + keyLoc.getBlockX() + "," + keyLoc.getBlockY()
                + "," + keyLoc.getBlockZ());

        if (block.getState() instanceof Chest chest) {
            Inventory chestInv = chest.getBlockInventory();

            if (drops != null) {
                for (ItemStack it : drops) {
                    if (it == null || it.getType() == Material.AIR)
                        continue;
                    plugin.getLogger().info("[SG][DeathDebug] add to chest: " + it.getType() + " x" + it.getAmount());
                    java.util.Map<Integer, ItemStack> leftover = chestInv.addItem(it.clone());
                    if (!leftover.isEmpty()) {
                        // 背包放不下的，丢在地上避免丢失
                        for (ItemStack rem : leftover.values()) {
                            if (rem != null && rem.getType() != Material.AIR) {
                                world.dropItemNaturally(location, rem);
                            }
                        }
                    }
                }
            }

            // 调试：统计箱子内容
            int nonAir = 0;
            for (ItemStack c : chestInv.getContents()) {
                if (c != null && c.getType() != Material.AIR)
                    nonAir++;
            }
            plugin.getLogger().info("[SG][DeathDebug] chest non-air count=" + nonAir);
        }
    }

    // 清理所有死亡箱子（移除方块）
    protected static void clearDeathChests() {
        World world = Bukkit.getWorld(survivalGame.getWorldName());
        if (world == null)
            return;

        int removed = 0;
        java.util.Iterator<Location> it = deathChestLocations.iterator();
        while (it.hasNext()) {
            Location loc = it.next();
            if (loc == null) {
                it.remove();
                continue;
            }
            Block block = world.getBlockAt(loc);
            if (block.getType() == Material.CHEST) {
                block.setType(Material.AIR, false);
                removed++;
            } else {
                // 无论如何尝试清理该位置，避免残留
                block.setType(Material.AIR, false);
            }
            it.remove();
        }
        plugin.getLogger().info("[SurvivalGame] 已清理死亡箱子数量=" + removed);
    }

    private static NamespacedKey deathChestKey() {
        return new NamespacedKey(plugin, "sg_death_chest");
    }
}
