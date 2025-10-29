package mcevent.MCEFramework.games.settings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * GameSettingsGUI: 主设置界面与子界面（全局 + 足球）
 */
public class GameSettingsGUI {

    private static final Component MAIN_TITLE = MiniMessage.miniMessage()
            .deserialize("<gold><bold>游戏设置</bold></gold>");
    private static final Component GLOBAL_TITLE = MiniMessage.miniMessage()
            .deserialize("<gold><bold>全局 设置</bold></gold>");
    private static final Component FOOTBALL_TITLE = MiniMessage.miniMessage()
            .deserialize("<gold><bold>少林足球 设置</bold></gold>");

    private static final int MAIN_SIZE = 27;
    private static final int GLOBAL_ICON_SLOT = 11;
    private static final int FOOTBALL_ICON_SLOT = 15;

    // 全局子界面
    private static final int GLOBAL_SIZE = 27;
    private static final int MANUAL_ON_SLOT = 11;
    private static final int MANUAL_OFF_SLOT = 15;

    // 足球子界面
    private static final int FOOTBALL_SIZE = 27;
    private static final int FOOTBALL_TEAMS_SLOT = 13;

    public static void openMainGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, MAIN_SIZE, MAIN_TITLE);
        // 全局图标
        inv.setItem(GLOBAL_ICON_SLOT, withNameLore(Material.COMMAND_BLOCK, "<yellow><bold>全局设置</bold></yellow>",
                "<gray>点击进入</gray>",
                "<gray>手动启动：" + (GameSettingsState.isManualStartEnabled() ? "<green>开</green>" : "<red>关</red>")
                        + "</gray>"));
        // 足球图标（使用史莱姆球作为图标）
        inv.setItem(FOOTBALL_ICON_SLOT, withNameLore(Material.SLIME_BALL, "<yellow><bold>少林足球</bold></yellow>",
                "<gray>点击进入设置</gray>"));
        fill(inv);
        player.openInventory(inv);
    }

    public static void openGlobalGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, GLOBAL_SIZE, GLOBAL_TITLE);
        inv.setItem(MANUAL_ON_SLOT, toggleItem("手动启动游戏：开", GameSettingsState.isManualStartEnabled()));
        inv.setItem(MANUAL_OFF_SLOT, toggleItem("手动启动游戏：关", !GameSettingsState.isManualStartEnabled()));
        fill(inv);
        player.openInventory(inv);
    }

    public static void openFootballGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, FOOTBALL_SIZE, FOOTBALL_TITLE);

        // 读取并展示队伍数量设置
        int teams = GameSettingsState.getFootballTeams();
        String value = teams == 4 ? "<aqua>4 队</aqua>" : "<green>2 队</green>";
        inv.setItem(FOOTBALL_TEAMS_SLOT, withNameLore(Material.PAPER,
                "<yellow><bold>队伍数</bold></yellow>",
                "<gray>当前: </gray>" + value,
                "<gray>点击在 2/4 之间切换</gray>"));

        fill(inv);
        player.openInventory(inv);
    }

    public static void handleInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null)
            return;
        Component title = event.getView().title();
        if (MAIN_TITLE.equals(title)) {
            event.setCancelled(true);
            if (event.getSlot() == GLOBAL_ICON_SLOT) {
                openGlobalGUI((Player) event.getWhoClicked());
            } else if (event.getSlot() == FOOTBALL_ICON_SLOT) {
                openFootballGUI((Player) event.getWhoClicked());
            }
            return;
        }
        if (GLOBAL_TITLE.equals(title)) {
            event.setCancelled(true);
            if (event.getSlot() == MANUAL_ON_SLOT) {
                GameSettingsState.setManualStartEnabled(true);
                openGlobalGUI((Player) event.getWhoClicked());
            } else if (event.getSlot() == MANUAL_OFF_SLOT) {
                GameSettingsState.setManualStartEnabled(false);
                openGlobalGUI((Player) event.getWhoClicked());
            }
            return;
        }
        if (FOOTBALL_TITLE.equals(title)) {
            event.setCancelled(true);
            if (event.getSlot() == FOOTBALL_TEAMS_SLOT) {
                // 切换 2/4 队
                int teams = GameSettingsState.getFootballTeams();
                GameSettingsState.setFootballTeams(teams == 2 ? 4 : 2);
                openFootballGUI((Player) event.getWhoClicked());
            }
        }
    }

    private static ItemStack withNameLore(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            java.util.List<Component> lines = new java.util.ArrayList<>();
            for (String l : lore)
                lines.add(MiniMessage.miniMessage().deserialize(l));
            meta.lore(lines);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack toggleItem(String title, boolean active) {
        ItemStack item = new ItemStack(active ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>" + title + "</bold></gold>"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gray> "));
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, pane);
        }
    }
}
