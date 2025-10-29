package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.games.settings.GameSettingsGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * GameSettingsHandler: 全局“游戏设置”物品与GUI入口
 */
public class GameSettingsHandler extends MCEResumableEventHandler implements Listener {

    private static final Component SETTINGS_NAME = MiniMessage.miniMessage()
            .deserialize("<gold><bold>游戏设置</bold></gold>");

    public GameSettingsHandler() {
        setSuspended(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static ItemStack createSettingsItem() {
        ItemStack item = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(SETTINGS_NAME);
            java.util.List<Component> lore = java.util.Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<yellow>右键打开游戏设置</yellow>"),
                    MiniMessage.miniMessage().deserialize("<gray>配置每个游戏的参数</gray>"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static boolean isSettingsItem(ItemStack stack) {
        if (stack == null || stack.getType() != Material.COMMAND_BLOCK)
            return false;
        if (!stack.hasItemMeta())
            return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName())
            return false;
        Component name = meta.displayName();
        return SETTINGS_NAME.equals(name);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (isSuspended())
            return;
        if (event.getHand() == EquipmentSlot.OFF_HAND)
            return;
        Action a = event.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK)
            return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!isSettingsItem(item))
            return;
        event.setCancelled(true);
        GameSettingsGUI.openMainGUI(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isSuspended())
            return;
        GameSettingsGUI.handleInventoryClick(event);
    }
}
