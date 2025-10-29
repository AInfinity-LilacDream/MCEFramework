package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * LobbyItemHandler: 仅负责在主城发放烈焰棒的轻量处理器（不包含饱和、二段跳等功能）
 */
public class LobbyItemHandler extends MCEResumableEventHandler implements Listener {

    public LobbyItemHandler() {
        setSuspended(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (isInLobby(player)) {
            // 进入主城默认打开PVP与友伤（投票期间除外）
            try {
                mcevent.MCEFramework.MCEMainController.getGlobalPVPHandler().suspend();
            } catch (Throwable ignored) {
            }
            try {
                mcevent.MCEFramework.MCEMainController.getFriendlyFireHandler().suspend();
            } catch (Throwable ignored) {
            }
            giveLobbyItems(player);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (isInLobby(player)) {
            // 进入主城默认打开PVP与友伤（投票期间除外）
            try {
                mcevent.MCEFramework.MCEMainController.getGlobalPVPHandler().suspend();
            } catch (Throwable ignored) {
            }
            try {
                mcevent.MCEFramework.MCEMainController.getFriendlyFireHandler().suspend();
            } catch (Throwable ignored) {
            }
            giveLobbyItems(player);
        }
    }

    private boolean isInLobby(Player player) {
        return "lobby".equals(player.getWorld().getName());
    }

    public void giveLobbyItems(Player player) {
        // 不在投票阶段时发放；投票阶段由 VotingSystem 发卡
        if (mcevent.MCEFramework.MCEMainController.isRunningGame())
            return;

        // 先清空物品栏后发放主城物品（保持原先行为）
        player.getInventory().clear();

        // 风弹发射器（烈焰棒）
        ItemStack blazeRod = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = blazeRod.getItemMeta();
        if (meta != null) {
            Component name = MiniMessage.miniMessage().deserialize("<red><bold>风弹发射器</bold></red>");
            java.util.List<Component> lore = java.util.Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<yellow>右键发射风弹</yellow>"),
                    MiniMessage.miniMessage().deserialize("<gray>击中玩家给予发光效果</gray>"),
                    MiniMessage.miniMessage().deserialize("<red>冷却时间: 3秒</red>"));
            meta.displayName(name);
            meta.lore(lore);
            blazeRod.setItemMeta(meta);
        }
        player.getInventory().setItem(0, blazeRod);

        // 游戏设置（命令方块）
        try {
            if (mcevent.MCEFramework.MCEMainController.getAdminList().contains(player.getName())) {
                player.getInventory().setItem(7,
                        mcevent.MCEFramework.customHandler.GameSettingsHandler.createSettingsItem());
            }
        } catch (Throwable ignored) {
        }

        // 按当前世界发放对应指南针：主城->前往Duel；Duel->返回主城
        if ("lobby".equals(player.getWorld().getName())) {
            player.getInventory().setItem(8,
                    mcevent.MCEFramework.customHandler.LobbyTeleportCompassHandler.createToDuelCompass());
        } else if ("duel".equals(player.getWorld().getName())) {
            player.getInventory().setItem(8,
                    mcevent.MCEFramework.customHandler.LobbyTeleportCompassHandler.createBackToLobbyCompass());
        }

        player.updateInventory();
    }
}
