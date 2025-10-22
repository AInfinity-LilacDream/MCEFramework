package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
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

    private boolean isInLobbyPhase(Player player) {
        if (player == null)
            return false;
        if (!"lobby".equals(player.getWorld().getName()))
            return false;
        // 仅在未运行任何游戏时可用（不在投票/游戏中）
        return !MCEMainController.isRunningGame();
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
        var meta = item.getItemMeta();

        // 分两种情况处理：
        // 1) 主城：前往Duel
        if (isInLobbyPhase(player) && nameMatches(meta, NAME_TO_DUEL)) {
            World duel = Bukkit.getWorld("duel");
            if (duel == null)
                return;
            event.setCancelled(true);
            player.teleport(duel.getSpawnLocation());
            // 清空背包并给予返回主城指南针
            player.getInventory().clear();
            player.getInventory().setItem(8, createBackToLobbyCompass());
            player.updateInventory();
            return;
        }

        // 2) Duel：返回主城
        if (isInDuelWorld(player) && nameMatches(meta, NAME_BACK_LOBBY)) {
            World lobby = Bukkit.getWorld("lobby");
            if (lobby == null)
                return;
            event.setCancelled(true);
            player.teleport(lobby.getSpawnLocation());
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
            p.getInventory().setItem(8, createBackToLobbyCompass());
            p.updateInventory();
        });
    }
}
