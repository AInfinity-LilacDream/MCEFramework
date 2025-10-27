package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * LobbyTeleportCompassHandler: 主城“前往Duel”指南针
 */
public class LobbyTeleportCompassHandler extends MCEResumableEventHandler implements Listener {

    private static final Component NAME_TO_DUEL = MiniMessage.miniMessage()
            .deserialize("<green><bold>前往Duel</bold></green>");
    private static final Component NAME_BACK_LOBBY = MiniMessage.miniMessage()
            .deserialize("<green><bold>返回主城</bold></green>");

    public LobbyTeleportCompassHandler() {
        setSuspended(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // 防抖：在一次罗盘传送后，短时间内忽略再次右键，避免“传过去又被拉回”
    private static final java.util.Map<java.util.UUID, Long> compassCooldownUntil = new java.util.HashMap<>();

    private static boolean inCooldown(Player p) {
        Long until = compassCooldownUntil.get(p.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    private static void setCooldown(Player p, long millis) {
        compassCooldownUntil.put(p.getUniqueId(), System.currentTimeMillis() + millis);
    }

    private boolean isInLobbyPhase(Player player) {
        if (player == null)
            return false;
        if (!"lobby".equals(player.getWorld().getName()))
            return false;
        // 未运行任何游戏 或 正在投票时可用（投票期间允许进入 Duel）
        boolean isVoting = MCEMainController
                .getCurrentRunningGame() instanceof mcevent.MCEFramework.games.votingSystem.VotingSystem;
        return !MCEMainController.isRunningGame() || isVoting;
    }

    private boolean isInDuelWorld(Player player) {
        return player != null && "duel".equals(player.getWorld().getName());
    }

    private static boolean nameMatches(org.bukkit.inventory.meta.ItemMeta meta, Component expected) {
        if (meta.displayName() == null)
            return false;
        Component actual = meta.displayName();
        if (actual.equals(expected))
            return true;
        // 兼容比较：序列化后比对
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage
                .miniMessage();
        String a = mm.serialize(actual);
        String b = mm.serialize(expected);
        if (a.equals(b))
            return true;
        // 兜底：legacy 序列化对比
        String la = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .serialize(actual);
        String lb = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .serialize(expected);
        return la.equals(lb);
    }

    public static ItemStack createBackToLobbyCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        var meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(NAME_BACK_LOBBY);
            java.util.List<Component> lore = java.util.Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<yellow>右键传送回 <aqua>lobby</aqua> 世界</yellow>"),
                    MiniMessage.miniMessage().deserialize("<gray>仅在Duel可用</gray>"));
            meta.lore(lore);
            compass.setItemMeta(meta);
        }
        return compass;
    }

    public static ItemStack createToDuelCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        var meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(NAME_TO_DUEL);
            java.util.List<Component> lore = java.util.Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<yellow>右键传送到 <aqua>duel</aqua> 世界</yellow>"),
                    MiniMessage.miniMessage().deserialize("<gray>仅在主城可用</gray>"));
            meta.lore(lore);
            compass.setItemMeta(meta);
        }
        return compass;
    }

    private static ItemStack createVotingCard() {
        ItemStack card = new ItemStack(Material.PAPER);
        org.bukkit.inventory.meta.ItemMeta meta = card.getItemMeta();
        if (meta != null) {
            // 与 VotingCardHandler 兼容：使用相同的 legacy 名称
            meta.setDisplayName("§6§l投票卡");
            meta.setLore(java.util.Arrays.asList(
                    "§e右键点击打开投票界面",
                    "§7选择您想要游玩的下一个游戏"));
            card.setItemMeta(meta);
        }
        return card;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (isSuspended())
            return;
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.COMPASS)
            return;
        if (!item.hasItemMeta())
            return;
        if (inCooldown(player))
            return;
        var meta = item.getItemMeta();

        // 分两种情况处理：
        // 1) 主城：前往Duel
        if (isInLobbyPhase(player) && nameMatches(meta, NAME_TO_DUEL)) {
            World duel = Bukkit.getWorld("duel");
            if (duel == null)
                return;
            event.setCancelled(true);
            setCooldown(player, 1200); // 1.2s 防抖
            player.teleport(duel.getSpawnLocation());
            // 清空背包并给予返回主城指南针
            player.getInventory().clear();
            player.getInventory().setItem(8, createBackToLobbyCompass());
            player.updateInventory();
            // 进入 duel 后，清空其已投的票
            try {
                mcevent.MCEFramework.games.votingSystem.VotingSystemFuncImpl.clearVotingDataForPlayer(player);
            } catch (Throwable ignored) {
            }
            return;
        }

        // 2) Duel：返回主城
        if (isInDuelWorld(player) && nameMatches(meta, NAME_BACK_LOBBY)) {
            World lobby = Bukkit.getWorld("lobby");
            if (lobby == null)
                return;
            event.setCancelled(true);
            setCooldown(player, 1200); // 1.2s 防抖
            player.teleport(lobby.getSpawnLocation());
            // 若正在投票，回到主城后补发投票卡与前往Duel指南针
            boolean isVoting = MCEMainController
                    .getCurrentRunningGame() instanceof mcevent.MCEFramework.games.votingSystem.VotingSystem;
            if (isVoting) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.getInventory().clear();
                    player.getInventory().setItem(4, createVotingCard());
                    player.getInventory().setItem(8, createToDuelCompass());
                    player.updateInventory();
                });
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (isSuspended())
            return;
        Player p = event.getPlayer();
        String to = p.getWorld() != null ? p.getWorld().getName() : null;
        String from = event.getFrom() != null ? event.getFrom().getName() : null;
        boolean isVoting = MCEMainController
                .getCurrentRunningGame() instanceof mcevent.MCEFramework.games.votingSystem.VotingSystem;
        if (isVoting && "lobby".equals(to) && "duel".equals(from)) {
            // 投票期间从 duel 返回主城：补发投票卡与前往Duel指南针
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                p.getInventory().clear();
                p.getInventory().setItem(4, createVotingCard());
                p.getInventory().setItem(8, createToDuelCompass());
                p.updateInventory();
            });
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (isSuspended())
            return;
        World world = event.getRespawnLocation() != null ? event.getRespawnLocation().getWorld() : null;
        if (world == null)
            return;
        // 由 DuelKitManager 统一处理 duel 世界的装备与指针发放
        if ("duel".equals(world.getName()))
            return;
        // 下个tick执行，确保背包就绪
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player p = event.getPlayer();
            if (!p.isOnline())
                return;
            p.getInventory().clear();
            // 在主城则给予“前往Duel”指南针
            if ("lobby".equals(world.getName())) {
                p.getInventory().setItem(8, createToDuelCompass());
            } else {
                // 其他非 duel 世界保持原逻辑（给予返回主城指南针）
                p.getInventory().setItem(8, createBackToLobbyCompass());
            }
            p.updateInventory();
        });
    }
}
