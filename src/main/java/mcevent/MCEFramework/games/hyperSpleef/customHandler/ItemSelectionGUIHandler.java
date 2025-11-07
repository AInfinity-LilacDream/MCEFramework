package mcevent.MCEFramework.games.hyperSpleef.customHandler;

import mcevent.MCEFramework.games.hyperSpleef.HyperSpleef;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
ItemSelectionGUIHandler: 道具选择GUI处理器
*/
public class ItemSelectionGUIHandler extends MCEResumableEventHandler implements Listener {

    private static final net.kyori.adventure.text.Component GUI_TITLE = MiniMessage.miniMessage()
            .deserialize("<gold>选择特殊道具</gold>");
    private static final Set<UUID> playersWithOpenGUI = new HashSet<>();
    private static final Map<UUID, String> playerSelectedItems = new HashMap<>();

    public void register(HyperSpleef game) {
        setSuspended(true);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void start() {
        setSuspended(false);
    }

    @Override
    public void suspend() {
        setSuspended(true);
        // 关闭所有打开的GUI
        for (UUID uuid : new HashSet<>(playersWithOpenGUI)) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                if (isItemSelectionGUI(player)) {
                    player.closeInventory();
                }
            }
        }
        playersWithOpenGUI.clear();
    }

    public static boolean isItemSelectionGUI(Player player) {
        if (player.getOpenInventory() == null)
            return false;
        return GUI_TITLE.equals(player.getOpenInventory().title());
    }

    public static String getPlayerSelectedItem(UUID uuid) {
        return playerSelectedItems.get(uuid);
    }

    public static void setPlayerSelectedItem(UUID uuid, String item) {
        playerSelectedItems.put(uuid, item);
    }

    public static void openItemSelectionGUI(Player player) {
        Inventory gui = plugin.getServer().createInventory(null, 9, GUI_TITLE);

        // 寒冰箭
        ItemStack iceArrow = new ItemStack(Material.BOW);
        ItemMeta iceArrowMeta = iceArrow.getItemMeta();
        if (iceArrowMeta != null) {
            iceArrowMeta.displayName(MiniMessage.miniMessage().deserialize("<aqua>寒冰箭</aqua>"));
            iceArrowMeta.lore(Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<gray>射出一支箭，触碰方块后1s爆炸</gray>"),
                    MiniMessage.miniMessage().deserialize("<gray>随机将周围的雪块变成冰</gray>")));
        }
        iceArrow.setItemMeta(iceArrowMeta);
        gui.setItem(2, iceArrow);

        // 暴雪法杖
        ItemStack blizzardStaff = new ItemStack(Material.BLAZE_ROD);
        ItemMeta blizzardStaffMeta = blizzardStaff.getItemMeta();
        if (blizzardStaffMeta != null) {
            blizzardStaffMeta.displayName(MiniMessage.miniMessage().deserialize("<blue>暴雪法杖</blue>"));
            blizzardStaffMeta.lore(Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<gray>向前发射大量雪球</gray>")));
        }
        blizzardStaff.setItemMeta(blizzardStaffMeta);
        gui.setItem(4, blizzardStaff);

        // 飘浮之羽
        ItemStack floatingFeather = new ItemStack(Material.FEATHER);
        ItemMeta floatingFeatherMeta = floatingFeather.getItemMeta();
        if (floatingFeatherMeta != null) {
            floatingFeatherMeta.displayName(MiniMessage.miniMessage().deserialize("<white>飘浮之羽</white>"));
            floatingFeatherMeta.lore(Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<gray>使用后获得3s飘浮 X</gray>")));
        }
        floatingFeather.setItemMeta(floatingFeatherMeta);
        gui.setItem(6, floatingFeather);

        player.openInventory(gui);
        playersWithOpenGUI.add(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isSuspended())
            return;

        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!isItemSelectionGUI(player))
            return;

        event.setCancelled(true);

        // 只允许点击GUI内的物品（槽位2, 4, 6）
        int slot = event.getSlot();
        if (slot != 2 && slot != 4 && slot != 6) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String itemName = clicked.getItemMeta() != null && clicked.getItemMeta().displayName() != null
                ? MiniMessage.miniMessage().serialize(clicked.getItemMeta().displayName())
                : null;

        if (itemName != null) {
            String selectedItem = null;
            String message = "";

            if (itemName.contains("寒冰箭")) {
                selectedItem = "iceArrow";
                message = "<green>你选择了 <aqua>寒冰箭</aqua>！</green>";
            } else if (itemName.contains("暴雪法杖")) {
                selectedItem = "blizzardStaff";
                message = "<green>你选择了 <blue>暴雪法杖</blue>！</green>";
            } else if (itemName.contains("飘浮之羽")) {
                selectedItem = "floatingFeather";
                message = "<green>你选择了 <white>飘浮之羽</white>！</green>";
            }

            if (selectedItem != null) {
                setPlayerSelectedItem(player.getUniqueId(), selectedItem);
                player.sendMessage(MiniMessage.miniMessage().deserialize(message));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.closeInventory();
                playersWithOpenGUI.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isSuspended())
            return;

        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!isItemSelectionGUI(player))
            return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (isSuspended())
            return;

        if (!(event.getPlayer() instanceof Player player))
            return;
        if (!isItemSelectionGUI(player))
            return;

        playersWithOpenGUI.remove(player.getUniqueId());

        // 如果玩家还未选择物品，重新打开GUI
        if (!playerSelectedItems.containsKey(player.getUniqueId())) {
            // 延迟1 tick重新打开，避免立即关闭
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && !isSuspended()) {
                        openItemSelectionGUI(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isSuspended())
            return;

        Player player = event.getPlayer();
        if (!isItemSelectionGUI(player))
            return;

        event.setCancelled(true);
    }

    public static void clearPlayerSelection(UUID uuid) {
        playerSelectedItems.remove(uuid);
    }

    public static void ensureDefaultSelection(Player player) {
        if (!playerSelectedItems.containsKey(player.getUniqueId())) {
            setPlayerSelectedItem(player.getUniqueId(), "iceArrow");
            plugin.getLogger().info("HyperSpleef: 玩家 " + player.getName() + " 未选择物品，默认选择第一个");
        }
    }
}
