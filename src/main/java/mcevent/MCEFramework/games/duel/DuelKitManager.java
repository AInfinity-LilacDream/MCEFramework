package mcevent.MCEFramework.games.duel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;
import static mcevent.MCEFramework.customHandler.LobbyTeleportCompassHandler.createBackToLobbyCompass;

/**
 * DuelKitManager: duel 世界的套装选择与发放
 */
public class DuelKitManager implements Listener {

    private static final String WORLD = "duel";
    private static final org.bukkit.Location ENDERCHEST_POS = new org.bukkit.Location(Bukkit.getWorld(WORLD), 506, 122,
            -281);
    private static final String GUI_TITLE = "<gold><bold>选择套装</bold></gold>";
    private static final NamespacedKey KEY_INFINITE_CROSSBOW = new NamespacedKey(plugin, "duel_infinite_crossbow");
    private static final NamespacedKey KEY_SUMMON_OWNER = new NamespacedKey(plugin, "duel_summon_owner");
    private static final NamespacedKey KEY_ROCKET_LAUNCHER = new NamespacedKey(plugin, "duel_rocket_launcher");
    private static final NamespacedKey KEY_ROCKET_PROJECTILE = new NamespacedKey(plugin, "duel_rocket_proj");

    public enum Kit {
        SWORD, SPEED, ARCHER, SUMMONER, ELYTRA, AXE, ROCKET
    }

    private static final Map<UUID, Kit> playerKit = new HashMap<>();
    private static final Map<UUID, Long> summonCooldownUntil = new HashMap<>();
    private static final Map<UUID, java.util.List<UUID>> ownerToZombies = new HashMap<>();
    private static final Map<UUID, Long> rocketCooldownUntil = new HashMap<>();

    // 生命显示（名牌上方/下方）：使用 BELOW_NAME 槽位展示心形生命值，仅在 duel 世界生效
    private org.bukkit.scoreboard.Scoreboard duelHealthBoard;
    private org.bukkit.scoreboard.Objective duelHealthObjective;

    public DuelKitManager() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static Kit getSelectedKit(Player p) {
        return playerKit.getOrDefault(p.getUniqueId(), Kit.SWORD);
    }

    public static void setSelectedKit(Player p, Kit kit) {
        playerKit.put(p.getUniqueId(), kit == null ? Kit.SWORD : kit);
    }

    private boolean isInDuel(Player p) {
        return p != null && p.getWorld() != null && WORLD.equals(p.getWorld().getName());
    }

    private boolean isTargetEnderChest(org.bukkit.block.Block block) {
        if (block == null || block.getType() != Material.ENDER_CHEST)
            return false;
        org.bukkit.Location l = block.getLocation();
        return l.getBlockX() == ENDERCHEST_POS.getBlockX()
                && l.getBlockY() == ENDERCHEST_POS.getBlockY()
                && l.getBlockZ() == ENDERCHEST_POS.getBlockZ()
                && WORLD.equals(l.getWorld() != null ? l.getWorld().getName() : null);
    }

    // ===== 生命显示 =====
    private void ensureHealthBoard() {
        if (duelHealthBoard != null && duelHealthObjective != null)
            return;
        try {
            org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
            duelHealthBoard = sm.getNewScoreboard();
            try {
                duelHealthObjective = duelHealthBoard.registerNewObjective("duel_health",
                        org.bukkit.scoreboard.Criteria.DUMMY,
                        net.kyori.adventure.text.Component.text("❤"));
            } catch (Throwable t) {
                duelHealthObjective = duelHealthBoard.registerNewObjective("duel_health", "dummy");
                try {
                    duelHealthObjective.setDisplayName("❤");
                } catch (Throwable ignored) {
                }
            }
            duelHealthObjective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.BELOW_NAME);
        } catch (Throwable ignored) {
        }
    }

    private void assignHealthBoardIfInDuel(Player p) {
        if (!isInDuel(p))
            return;
        ensureHealthBoard();
        try {
            p.setScoreboard(duelHealthBoard);
        } catch (Throwable ignored) {
        }
        updateHealthScore(p);
    }

    private void clearHealthBoard(Player p) {
        try {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        } catch (Throwable ignored) {
        }
    }

    // 对外提供：隐藏所有玩家 BELOW_NAME 的生命显示（用于启动其他游戏时清理）
    public static void hideHealthBelowNameForAll() {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            try {
                pl.getScoreboard().clearSlot(org.bukkit.scoreboard.DisplaySlot.BELOW_NAME);
            } catch (Throwable ignored) {
            }
        }
    }

    private void updateHealthScore(Player p) {
        if (duelHealthObjective == null || p == null)
            return;
        double base = 20.0;
        try {
            var attr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (attr != null)
                base = attr.getBaseValue();
        } catch (Throwable ignored) {
        }
        int hp = (int) Math.ceil(Math.max(0.0, Math.min(base, p.getHealth())));
        if (hp <= 0 && p.getHealth() <= 0.0) {
            // 死亡显示0，否则在出生未受伤时默认显示满血
            if (p.isDead())
                hp = 0;
            else
                hp = (int) Math.round(base);
        }
        try {
            org.bukkit.scoreboard.Score s = duelHealthObjective.getScore(p.getName());
            s.setScore(hp);
        } catch (Throwable ignored) {
        }
    }

    @EventHandler
    public void onInteractEnderChest(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null)
            return;
        Player player = event.getPlayer();
        if (!isInDuel(player))
            return;
        if (!isTargetEnderChest(event.getClickedBlock()))
            return;
        // 右键打开 GUI 替代原生末影箱
        event.setCancelled(true);
        openKitGUI(player);
    }

    private void openKitGUI(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, MiniMessage.miniMessage().deserialize(GUI_TITLE));
        inv.setItem(10, createSwordIcon());
        inv.setItem(12, createSpeedIcon());
        inv.setItem(14, createArcherIcon());
        inv.setItem(16, createSummonerIcon());
        inv.setItem(19, createElytraIcon());
        inv.setItem(21, createAxeIcon());
        inv.setItem(23, createRocketIcon());
        player.openInventory(inv);
    }

    private ItemStack withNameAndLore(Material m, String name, String... loreLines) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            for (String l : loreLines)
                lore.add(MiniMessage.miniMessage().deserialize(l));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static void makeUnbreakable(ItemStack... items) {
        if (items == null)
            return;
        for (ItemStack it : items) {
            if (it == null)
                continue;
            ItemMeta m = it.getItemMeta();
            if (m == null)
                continue;
            try {
                m.setUnbreakable(true);
            } catch (Throwable ignored) {
            }
            m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            it.setItemMeta(m);
        }
    }

    private ItemStack createSwordIcon() {
        return withNameAndLore(Material.DIAMOND_SWORD, "<aqua><bold>Kit 1: Sword</bold></aqua>");
    }

    private ItemStack createSpeedIcon() {
        ItemStack it = new ItemStack(Material.POTION);
        ItemMeta base = it.getItemMeta();
        if (base instanceof org.bukkit.inventory.meta.PotionMeta pm) {
            try {
                pm.setBasePotionType(org.bukkit.potion.PotionType.SWIFTNESS);
            } catch (Throwable ignored) {
            }
            pm.displayName(MiniMessage.miniMessage().deserialize("<aqua><bold>Kit 2: Speed</bold></aqua>"));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize("<gray>钻头鞋+胸，铁裤保护II</gray>"));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>速度II（持续）</gray>"));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>锋利II钻石剑</gray>"));
            pm.lore(lore);
            it.setItemMeta(pm);
            return it;
        }
        return withNameAndLore(Material.POTION, "<aqua><bold>Kit 2: Speed</bold></aqua>");
    }

    private ItemStack createArcherIcon() {
        return withNameAndLore(Material.BOW, "<aqua><bold>Kit 3: Archer</bold></aqua>");
    }

    private ItemStack createSummonerIcon() {
        return withNameAndLore(Material.TRIDENT, "<aqua><bold>Kit 4: Summoner</bold></aqua>", "<gray>右键召唤亡灵士兵</gray>");
    }

    private ItemStack createElytraIcon() {
        return withNameAndLore(Material.ELYTRA, "<aqua><bold>Kit 5: Elytra</bold></aqua>");
    }

    private ItemStack createAxeIcon() {
        return withNameAndLore(Material.DIAMOND_AXE, "<aqua><bold>Kit 6: Axe</bold></aqua>");
    }

    private ItemStack createRocketIcon() {
        return withNameAndLore(Material.NETHERITE_SHOVEL, "<aqua><bold>Kit 7: Rocket</bold></aqua>",
                "<gray>右键发射风弹（2s冷却）</gray>");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!isInDuel(player))
            return;
        if (event.getView() == null || event.getView().title() == null)
            return;
        if (!MiniMessage.miniMessage().deserialize(GUI_TITLE).equals(event.getView().title()))
            return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == 10)
            setSelectedKit(player, Kit.SWORD);
        else if (slot == 12)
            setSelectedKit(player, Kit.SPEED);
        else if (slot == 14)
            setSelectedKit(player, Kit.ARCHER);
        else if (slot == 16)
            setSelectedKit(player, Kit.SUMMONER);
        else if (slot == 19)
            setSelectedKit(player, Kit.ELYTRA);
        else if (slot == 21)
            setSelectedKit(player, Kit.AXE);
        else if (slot == 23)
            setSelectedKit(player, Kit.ROCKET);
        applyKit(player, getSelectedKit(player));
        // 关闭界面并播放提示音效
        try {
            player.closeInventory();
        } catch (Throwable ignored) {
        }
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        } catch (Throwable ignored) {
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        if (!isInDuel(p))
            return;
        // 下一tick应用以确保背包准备就绪
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            applyKit(p, getSelectedKit(p));
            assignHealthBoardIfInDuel(p);
        });
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p))
            return;
        if (!isInDuel(p))
            return;
        plugin.getServer().getScheduler().runTask(plugin, () -> updateHealthScore(p));
    }

    @EventHandler
    public void onRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player p))
            return;
        if (!isInDuel(p))
            return;
        plugin.getServer().getScheduler().runTask(plugin, () -> updateHealthScore(p));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (!isInDuel(p))
            return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            applyKit(p, getSelectedKit(p));
            assignHealthBoardIfInDuel(p);
        });
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        String to = p.getWorld() != null ? p.getWorld().getName() : null;
        String from = event.getFrom() != null ? event.getFrom().getName() : null;
        if ("duel".equals(to)) {
            if (!playerKit.containsKey(p.getUniqueId())) {
                setSelectedKit(p, Kit.SWORD);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                applyKit(p, getSelectedKit(p));
                assignHealthBoardIfInDuel(p);
            });
        } else if ("duel".equals(from) && (to == null || !"duel".equals(to))) {
            stripKit(p);
            clearHealthBoard(p);
            // 清理该召唤者的僵尸
            java.util.List<UUID> ids = ownerToZombies.remove(p.getUniqueId());
            if (ids != null && !ids.isEmpty()) {
                for (UUID id : ids) {
                    org.bukkit.entity.Entity e = Bukkit.getEntity(id);
                    if (e != null && e.isValid()) {
                        e.remove();
                    }
                }
            }
        }
    }

    private static void stripKit(Player p) {
        if (p == null)
            return;
        p.removePotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
        p.getInventory().setHelmet(null);
        p.getInventory().setChestplate(null);
        p.getInventory().setLeggings(null);
        p.getInventory().setBoots(null);
        java.util.Set<Material> kitMaterials = java.util.Set.of(
                Material.DIAMOND_SWORD,
                Material.BOW,
                Material.CROSSBOW,
                Material.ARROW,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS);
        org.bukkit.inventory.PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && kitMaterials.contains(it.getType())) {
                inv.clear(i);
            }
        }
        p.updateInventory();
    }

    public static void applyKit(Player player, Kit kit) {
        if (player == null)
            return;
        if (kit == null)
            kit = Kit.SWORD;
        // 清空并给予返回主城指南针
        player.getInventory().clear();
        player.getInventory().setItem(8, createBackToLobbyCompass());

        switch (kit) {
            case SWORD -> giveSword(player);
            case SPEED -> giveSpeed(player);
            case ARCHER -> giveArcher(player);
            case SUMMONER -> giveSummoner(player);
            case ELYTRA -> giveElytra(player);
            case AXE -> giveAxe(player);
            case ROCKET -> giveRocket(player);
        }
        player.updateInventory();
    }

    private static void enchant(org.bukkit.inventory.ItemStack is, Enchantment ench, int level) {
        if (is == null)
            return;
        is.addUnsafeEnchantment(ench, level);
    }

    private static ItemStack randomArmorPiece(Material gold, Material leather, Material chain, Player owner) {
        int r = new java.util.Random().nextInt(3);
        Material m = (r == 0 ? gold : (r == 1 ? leather : chain));
        ItemStack piece = new ItemStack(m);
        if (m.name().contains("LEATHER")) {
            ItemMeta meta = piece.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta lam) {
                org.bukkit.Color c = resolveTeamColor(owner);
                lam.setColor(c);
                piece.setItemMeta(lam);
            }
        }
        return piece;
    }

    private static org.bukkit.Color resolveTeamColor(Player p) {
        org.bukkit.scoreboard.Team t = mcevent.MCEFramework.tools.MCETeamUtils.getTeam(p);
        if (t == null)
            return org.bukkit.Color.WHITE;
        String name = t.getName();
        if (name.contains("红"))
            return org.bukkit.Color.RED;
        if (name.contains("橙"))
            return org.bukkit.Color.ORANGE;
        if (name.contains("黄"))
            return org.bukkit.Color.YELLOW;
        if (name.contains("翠") || name.contains("绿"))
            return org.bukkit.Color.LIME;
        if (name.contains("青") || name.contains("缥"))
            return org.bukkit.Color.AQUA;
        if (name.contains("蓝"))
            return org.bukkit.Color.BLUE;
        if (name.contains("紫") || name.contains("粉"))
            return org.bukkit.Color.FUCHSIA;
        return org.bukkit.Color.WHITE;
    }

    private static void giveSword(Player p) {
        // 全套钻甲 保护2
        ItemStack helm = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        enchant(helm, Enchantment.PROTECTION, 2);
        enchant(chest, Enchantment.PROTECTION, 2);
        enchant(legs, Enchantment.PROTECTION, 2);
        enchant(boots, Enchantment.PROTECTION, 2);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        enchant(sword, Enchantment.SHARPNESS, 2);
        makeUnbreakable(helm, chest, legs, boots, sword);
        p.getInventory().setHelmet(helm);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);
        p.getInventory().addItem(sword);
    }

    private static void giveSpeed(Player p) {
        ItemStack helm = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack legs = new ItemStack(Material.IRON_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        enchant(helm, Enchantment.PROTECTION, 2);
        enchant(chest, Enchantment.PROTECTION, 2);
        enchant(legs, Enchantment.PROTECTION, 2);
        enchant(boots, Enchantment.PROTECTION, 2);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        enchant(sword, Enchantment.SHARPNESS, 2);
        makeUnbreakable(helm, chest, legs, boots, sword);
        p.getInventory().setHelmet(helm);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);
        p.getInventory().addItem(sword);
        // 速度II 持续效果
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED,
                Integer.MAX_VALUE, 1, false, false));
    }

    private static void giveSummoner(Player p) {
        // 全套铁甲 保护II
        ItemStack helm = new ItemStack(Material.IRON_HELMET);
        ItemStack chest = new ItemStack(Material.IRON_CHESTPLATE);
        ItemStack legs = new ItemStack(Material.IRON_LEGGINGS);
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        enchant(helm, Enchantment.PROTECTION, 2);
        enchant(chest, Enchantment.PROTECTION, 2);
        enchant(legs, Enchantment.PROTECTION, 2);
        enchant(boots, Enchantment.PROTECTION, 2);
        ItemStack trident = new ItemStack(Material.TRIDENT);
        enchant(trident, Enchantment.LOYALTY, 3);
        makeUnbreakable(helm, chest, legs, boots, trident);
        p.getInventory().setHelmet(helm);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);
        p.getInventory().addItem(trident);
    }

    private static void giveElytra(Player p) {
        ItemStack helm = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chest = new ItemStack(Material.ELYTRA);
        ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        enchant(helm, Enchantment.PROTECTION, 2);
        enchant(legs, Enchantment.PROTECTION, 2);
        enchant(boots, Enchantment.PROTECTION, 2);
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        enchant(sword, Enchantment.SHARPNESS, 3);
        makeUnbreakable(helm, chest, legs, boots, sword);
        p.getInventory().setHelmet(helm);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);
        p.getInventory().addItem(sword);
        p.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, 32));
    }

    private static void giveAxe(Player p) {
        ItemStack helm = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        enchant(helm, Enchantment.PROTECTION, 2);
        enchant(chest, Enchantment.PROTECTION, 2);
        enchant(legs, Enchantment.PROTECTION, 2);
        enchant(boots, Enchantment.PROTECTION, 2);
        ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
        makeUnbreakable(helm, chest, legs, boots, axe);
        p.getInventory().setHelmet(helm);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);
        p.getInventory().addItem(axe);
    }

    private static void giveRocket(Player p) {
        ItemStack helm = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        enchant(helm, Enchantment.PROTECTION, 2);
        enchant(chest, Enchantment.PROTECTION, 2);
        enchant(legs, Enchantment.PROTECTION, 2);
        enchant(boots, Enchantment.PROTECTION, 2);
        ItemStack launcher = new ItemStack(Material.NETHERITE_SHOVEL);
        ItemMeta meta = launcher.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>火箭发射器</bold></gold>"));
            meta.getPersistentDataContainer().set(KEY_ROCKET_LAUNCHER, PersistentDataType.BYTE, (byte) 1);
            launcher.setItemMeta(meta);
        }
        makeUnbreakable(helm, chest, legs, boots, launcher);
        p.getInventory().setHelmet(helm);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);
        p.getInventory().addItem(launcher);
    }

    @EventHandler
    public void onSummon(PlayerInteractEvent event) {
        if (event.getPlayer() == null)
            return;
        Player p = event.getPlayer();
        if (!isInDuel(p))
            return;
        ItemStack item = event.getItem();
        // 仅处理右键
        org.bukkit.event.block.Action a = event.getAction();
        if (a != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && a != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
            return;
        // 先处理火箭发射器（火焰弹）
        if (item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(KEY_ROCKET_LAUNCHER, PersistentDataType.BYTE)) {
            long now2 = System.currentTimeMillis();
            long until2 = rocketCooldownUntil.getOrDefault(p.getUniqueId(), 0L);
            if (now2 < until2)
                return;
            rocketCooldownUntil.put(p.getUniqueId(), now2 + 2000);
            boolean launched = false;
            try {
                org.bukkit.entity.SmallFireball fb = p.launchProjectile(org.bukkit.entity.SmallFireball.class);
                fb.setVelocity(p.getLocation().getDirection().normalize().multiply(1.2));
                try {
                    fb.setIsIncendiary(false);
                } catch (Throwable ignored) {
                }
                fb.getPersistentDataContainer().set(KEY_ROCKET_PROJECTILE, PersistentDataType.BYTE, (byte) 1);
                launched = true;
            } catch (Throwable ignored) {
            }
            if (!launched) {
                org.bukkit.entity.Snowball proj = p.launchProjectile(org.bukkit.entity.Snowball.class);
                proj.setVelocity(p.getLocation().getDirection().normalize().multiply(1.6));
                proj.getPersistentDataContainer().set(KEY_ROCKET_PROJECTILE, PersistentDataType.BYTE, (byte) 1);
            }
            try {
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1f, 1.0f);
            } catch (Throwable ignored) {
            }
            return;
        }
        // 召唤套装逻辑
        if (item == null || item.getType() != Material.TRIDENT)
            return;
        if (getSelectedKit(p) != Kit.SUMMONER)
            return;
        long now = System.currentTimeMillis();
        long until = summonCooldownUntil.getOrDefault(p.getUniqueId(), 0L);
        if (now < until)
            return;
        // 容量限制：每个玩家最多6个召唤物（每次召唤2个，因此仅当现有<=4时允许）
        java.util.List<UUID> existing = ownerToZombies.getOrDefault(p.getUniqueId(), java.util.Collections.emptyList());
        if (existing.size() > 4)
            return;

        org.bukkit.Location loc = p.getLocation().add(0, 0, 0);

        // 检查是否已有骷髅，决定本次召唤组合
        boolean hasSkeleton = false;
        {
            java.util.List<UUID> list = ownerToZombies.get(p.getUniqueId());
            if (list != null) {
                for (UUID id : list) {
                    org.bukkit.entity.Entity ent = Bukkit.getEntity(id);
                    if (ent instanceof org.bukkit.entity.Skeleton) {
                        hasSkeleton = true;
                        break;
                    }
                }
            }
        }

        java.util.List<org.bukkit.entity.Mob> spawned = new java.util.ArrayList<>();

        // 工具：创建强化僵尸
        java.util.function.Supplier<org.bukkit.entity.Zombie> spawnZombie = () -> p.getWorld().spawn(loc,
                org.bukkit.entity.Zombie.class, e -> {
                    e.setAdult();
                    // 使用字符串名称，避免不同服务端API差异
                    e.setCustomName(p.getName() + "'s Undead Soldier");
                    e.setCustomNameVisible(true);
                    e.getPersistentDataContainer().set(KEY_SUMMON_OWNER, PersistentDataType.STRING,
                            p.getUniqueId().toString());
                    // 速度V：放大等级4
                    e.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED,
                            Integer.MAX_VALUE, 4, false, false));
                    e.setRemoveWhenFarAway(false);
                    try {
                        e.setShouldBurnInDay(false);
                    } catch (Throwable ignored) {
                    }
                });

        // 设置初始目标：最近的其他玩家
        Player target = null;
        double best = Double.MAX_VALUE;
        for (Player other : p.getWorld().getPlayers()) {
            if (other.getUniqueId().equals(p.getUniqueId()))
                continue;
            double d = other.getLocation().distanceSquared(p.getLocation());
            if (d < best) {
                best = d;
                target = other;
            }
        }
        if (target != null) {
            for (org.bukkit.entity.Mob m : spawned) {
                if (m.getWorld().equals(target.getWorld())) {
                    try {
                        m.setTarget(target);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        // 工具：创建强化骷髅
        java.util.function.Supplier<org.bukkit.entity.Skeleton> spawnSkeleton = () -> p.getWorld().spawn(loc,
                org.bukkit.entity.Skeleton.class, e -> {
                    e.setCustomName(p.getName() + "'s Undead Soldier");
                    e.setCustomNameVisible(true);
                    e.getPersistentDataContainer().set(KEY_SUMMON_OWNER, PersistentDataType.STRING,
                            p.getUniqueId().toString());
                    // 可保留速度或不加，未指定则不强制
                    e.setRemoveWhenFarAway(false);
                    try {
                        e.setShouldBurnInDay(false);
                    } catch (Throwable ignored) {
                    }
                });
        // 给予骷髅 力量IV 冲击II 的弓
        ItemStack sbow = new ItemStack(Material.BOW);
        enchant(sbow, Enchantment.POWER, 4);
        enchant(sbow, Enchantment.PUNCH, 2);
        // 召唤组合
        if (hasSkeleton) {
            org.bukkit.entity.Zombie z1 = spawnZombie.get();
            org.bukkit.entity.Zombie z2 = spawnZombie.get();
            spawned.add(z1);
            spawned.add(z2);
        } else {
            org.bukkit.entity.Skeleton sk = spawnSkeleton.get();
            sk.getEquipment().setItemInMainHand(sbow);
            spawned.add(sk);
            org.bukkit.entity.Zombie z = spawnZombie.get();
            spawned.add(z);
        }

        // 将生成实体登记到 ownerToZombies
        for (org.bukkit.entity.Mob m : spawned) {
            ownerToZombies.computeIfAbsent(p.getUniqueId(), k -> new java.util.ArrayList<>()).add(m.getUniqueId());
        }
        // 僵尸手持 锋利III 钻石剑
        ItemStack zs = new ItemStack(Material.DIAMOND_SWORD);
        enchant(zs, Enchantment.SHARPNESS, 5);
        // 给所有生成的僵尸配置武器与护甲
        for (org.bukkit.entity.Mob m : spawned) {
            if (m instanceof org.bukkit.entity.Zombie zmb) {
                zmb.getEquipment().setItemInMainHand(zs.clone());
            }
        }

        // 僵尸全套金甲 保护II
        ItemStack zHelm = new ItemStack(Material.GOLDEN_HELMET);
        ItemStack zChest = new ItemStack(Material.GOLDEN_CHESTPLATE);
        ItemStack zLegs = new ItemStack(Material.GOLDEN_LEGGINGS);
        ItemStack zBoots = new ItemStack(Material.GOLDEN_BOOTS);
        enchant(zHelm, Enchantment.PROTECTION, 2);
        enchant(zChest, Enchantment.PROTECTION, 2);
        enchant(zLegs, Enchantment.PROTECTION, 2);
        enchant(zBoots, Enchantment.PROTECTION, 2);
        for (org.bukkit.entity.Mob m : spawned) {
            if (m instanceof org.bukkit.entity.Zombie zmb) {
                zmb.getEquipment().setHelmet(zHelm.clone());
                zmb.getEquipment().setChestplate(zChest.clone());
                zmb.getEquipment().setLeggings(zLegs.clone());
                zmb.getEquipment().setBoots(zBoots.clone());
            }
        }

        // 骷髅全套锁链 保护IV
        ItemStack sHelm = new ItemStack(Material.CHAINMAIL_HELMET);
        ItemStack sChest = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
        ItemStack sLegs = new ItemStack(Material.CHAINMAIL_LEGGINGS);
        ItemStack sBoots = new ItemStack(Material.CHAINMAIL_BOOTS);
        enchant(sHelm, Enchantment.PROTECTION, 4);
        enchant(sChest, Enchantment.PROTECTION, 4);
        enchant(sLegs, Enchantment.PROTECTION, 4);
        enchant(sBoots, Enchantment.PROTECTION, 4);
        for (org.bukkit.entity.Mob m : spawned) {
            if (m instanceof org.bukkit.entity.Skeleton skm) {
                skm.getEquipment().setHelmet(sHelm.clone());
                skm.getEquipment().setChestplate(sChest.clone());
                skm.getEquipment().setLeggings(sLegs.clone());
                skm.getEquipment().setBoots(sBoots.clone());
                // 确保弓已赋予
                if (skm.getEquipment().getItemInMainHand() == null
                        || skm.getEquipment().getItemInMainHand().getType() == Material.AIR) {
                    skm.getEquipment().setItemInMainHand(sbow.clone());
                }
            }
        }

        // 装备掉落几率设为0
        try {
            for (org.bukkit.entity.Mob m : spawned) {
                if (m instanceof org.bukkit.entity.Zombie zmb) {
                    zmb.getEquipment().setItemInMainHandDropChance(0f);
                    zmb.getEquipment().setHelmetDropChance(0f);
                    zmb.getEquipment().setChestplateDropChance(0f);
                    zmb.getEquipment().setLeggingsDropChance(0f);
                    zmb.getEquipment().setBootsDropChance(0f);
                } else if (m instanceof org.bukkit.entity.Skeleton skm) {
                    skm.getEquipment().setItemInMainHandDropChance(0f);
                    skm.getEquipment().setHelmetDropChance(0f);
                    skm.getEquipment().setChestplateDropChance(0f);
                    skm.getEquipment().setLeggingsDropChance(0f);
                    skm.getEquipment().setBootsDropChance(0f);
                }
            }
        } catch (Throwable ignored) {
        }

        // 设置冷却（仅在成功召唤后）
        summonCooldownUntil.put(p.getUniqueId(), now + 5000);
    }

    @EventHandler
    public void onRocketHit(ProjectileHitEvent event) {
        org.bukkit.entity.Projectile proj = event.getEntity();
        if (!proj.getPersistentDataContainer().has(KEY_ROCKET_PROJECTILE, PersistentDataType.BYTE))
            return;
        org.bukkit.Location loc = proj.getLocation();
        try {
            // 不破坏地形、不点燃，调整爆炸强度为 3.0
            loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 3.0f, false, false);
        } catch (Throwable ignored) {
        }
        proj.remove();
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        org.bukkit.entity.Entity ent = event.getEntity();
        if (!(ent instanceof org.bukkit.entity.LivingEntity le))
            return;
        PersistentDataContainer pdc = le.getPersistentDataContainer();
        if (!pdc.has(KEY_SUMMON_OWNER, PersistentDataType.STRING))
            return;
        String owner = pdc.get(KEY_SUMMON_OWNER, PersistentDataType.STRING);
        if (owner == null)
            return;
        if (event.getTarget() instanceof Player pl && pl.getUniqueId().toString().equals(owner)) {
            event.setCancelled(true);
            return;
        }
        if (event.getTarget() instanceof org.bukkit.entity.LivingEntity targetLe) {
            PersistentDataContainer tpdc = targetLe.getPersistentDataContainer();
            if (tpdc.has(KEY_SUMMON_OWNER, PersistentDataType.STRING)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 三叉戟命中时，所有召唤物立刻锁定被击中的实体
        if (event.getDamager() instanceof org.bukkit.entity.Trident trident
                && event.getEntity() instanceof org.bukkit.entity.LivingEntity hit) {
            org.bukkit.projectiles.ProjectileSource src = trident.getShooter();
            if (src instanceof Player owner && isInDuel(owner)) {
                // 仅当命中目标为玩家时，才让召唤物锁定该目标
                if (!(hit instanceof Player))
                    return;
                java.util.List<UUID> ids = ownerToZombies.get(owner.getUniqueId());
                if (ids != null) {
                    for (UUID id : new java.util.ArrayList<>(ids)) {
                        org.bukkit.entity.Entity ent = Bukkit.getEntity(id);
                        if (ent instanceof org.bukkit.entity.Mob mob && ent.getWorld().equals(hit.getWorld())) {
                            try {
                                mob.setTarget(hit);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                }
            }
        }
        // Melee damage by summoned mobs
        org.bukkit.entity.Entity damager = event.getDamager();
        if (damager instanceof org.bukkit.entity.LivingEntity le) {
            PersistentDataContainer pdc = le.getPersistentDataContainer();
            if (pdc.has(KEY_SUMMON_OWNER, PersistentDataType.STRING)) {
                String owner = pdc.get(KEY_SUMMON_OWNER, PersistentDataType.STRING);
                if (owner != null && event.getEntity() instanceof Player pl
                        && pl.getUniqueId().toString().equals(owner)) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getEntity() instanceof org.bukkit.entity.LivingEntity targetLe) {
                    PersistentDataContainer tpdc = targetLe.getPersistentDataContainer();
                    if (tpdc.has(KEY_SUMMON_OWNER, PersistentDataType.STRING)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
        // Projectile damage (e.g., skeleton arrows)
        if (damager instanceof org.bukkit.entity.Projectile proj) {
            org.bukkit.projectiles.ProjectileSource src = proj.getShooter();
            if (src instanceof org.bukkit.entity.LivingEntity le) {
                PersistentDataContainer pdc = le.getPersistentDataContainer();
                if (pdc.has(KEY_SUMMON_OWNER, PersistentDataType.STRING)) {
                    String owner = pdc.get(KEY_SUMMON_OWNER, PersistentDataType.STRING);
                    if (owner != null && event.getEntity() instanceof Player pl
                            && pl.getUniqueId().toString().equals(owner)) {
                        event.setCancelled(true);
                        return;
                    }
                    if (event.getEntity() instanceof org.bukkit.entity.LivingEntity targetLe) {
                        PersistentDataContainer tpdc = targetLe.getPersistentDataContainer();
                        if (tpdc.has(KEY_SUMMON_OWNER, PersistentDataType.STRING)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSummonedDeath(EntityDeathEvent event) {
        org.bukkit.entity.Entity e = event.getEntity();
        if (!(e instanceof org.bukkit.entity.LivingEntity le))
            return;
        PersistentDataContainer pdc = le.getPersistentDataContainer();
        if (!pdc.has(KEY_SUMMON_OWNER, PersistentDataType.STRING))
            return;
        try {
            event.getDrops().clear();
        } catch (Throwable ignored) {
        }
        try {
            event.setDroppedExp(0);
        } catch (Throwable ignored) {
        }
        // 从召唤物列表中移除，释放名额
        String owner = pdc.get(KEY_SUMMON_OWNER, PersistentDataType.STRING);
        if (owner != null) {
            try {
                UUID ou = UUID.fromString(owner);
                java.util.List<UUID> ids = ownerToZombies.get(ou);
                if (ids != null) {
                    ids.remove(le.getUniqueId());
                    if (ids.isEmpty())
                        ownerToZombies.remove(ou);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void giveArcher(Player p) {
        ItemStack helm = new ItemStack(Material.IRON_HELMET);
        ItemStack chest = new ItemStack(Material.IRON_CHESTPLATE);
        ItemStack legs = new ItemStack(Material.IRON_LEGGINGS);
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        enchant(helm, Enchantment.PROTECTION, 2);
        enchant(chest, Enchantment.PROTECTION, 2);
        enchant(legs, Enchantment.PROTECTION, 2);
        enchant(boots, Enchantment.PROTECTION, 2);
        ItemStack bow = new ItemStack(Material.BOW);
        enchant(bow, Enchantment.INFINITY, 1);
        enchant(bow, Enchantment.PUNCH, 1);
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        enchant(crossbow, Enchantment.QUICK_CHARGE, 2);
        // 为跨弓打上“无限”标记，并添加显示上的无限附魔（实际效果由事件保障）
        ItemMeta cbMeta = crossbow.getItemMeta();
        if (cbMeta != null) {
            PersistentDataContainer pdc = cbMeta.getPersistentDataContainer();
            pdc.set(KEY_INFINITE_CROSSBOW, PersistentDataType.BYTE, (byte) 1);
            // 为了提示，添加一个Unsafe的INFINITY附魔（不会真正生效在跨弓上）
            crossbow.setItemMeta(cbMeta);
            enchant(crossbow, Enchantment.INFINITY, 1);
        }
        ItemStack arrow = new ItemStack(Material.ARROW);
        makeUnbreakable(helm, chest, legs, boots, bow, crossbow);
        p.getInventory().setHelmet(helm);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);
        p.getInventory().addItem(bow);
        p.getInventory().addItem(crossbow);
        p.getInventory().addItem(arrow);
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player p))
            return;
        if (!isInDuel(p))
            return;
        ItemStack bow = event.getBow();
        if (bow == null || bow.getType() != Material.CROSSBOW)
            return;
        ItemMeta meta = bow.getItemMeta();
        if (meta == null)
            return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(KEY_INFINITE_CROSSBOW, PersistentDataType.BYTE))
            return;
        // 优先使用API阻止消耗；若不可用则回补一支箭
        try {
            event.setConsumeItem(false);
        } catch (Throwable ignore) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> p.getInventory().addItem(new ItemStack(Material.ARROW, 1)));
        }
    }

    // 击杀后回满血与饱食度
    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null)
            return;
        if (!isInDuel(killer))
            return;
        try {
            Objects.requireNonNull(killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).setBaseValue(20.0);
            killer.setHealth(20.0);
        } catch (Throwable ignored) {
        }
        try {
            killer.setFoodLevel(20);
            killer.setSaturation(20f);
        } catch (Throwable ignored) {
        }
        updateHealthScore(killer);
    }
}
