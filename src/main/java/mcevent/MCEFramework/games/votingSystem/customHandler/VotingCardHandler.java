package mcevent.MCEFramework.games.votingSystem.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.games.votingSystem.VotingSystemFuncImpl;
import mcevent.MCEFramework.games.votingSystem.gameObject.VotingGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * VotingCardHandler: 投票卡交互处理器
 */
public class VotingCardHandler extends MCEResumableEventHandler implements Listener {

    public VotingCardHandler() {
        setSuspended(true); // 默认暂停
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isSuspended()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 检查是否右键点击投票卡
        if (item != null && item.getType() == Material.PAPER && 
            item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            
            String displayName = item.getItemMeta().getDisplayName();
            if ("§6§l投票卡".equals(displayName)) {
                event.setCancelled(true);
                
                // 确保投票已初始化（如果还没初始化）
                VotingSystemFuncImpl.ensureVotingInitialized();
                
                // 判断具体的动作类型
                Action action = event.getAction();
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    // 右键 - 打开投票GUI
                    VotingGUI.openVotingGUI(player);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isSuspended()) return;

        // 检查是否为投票GUI
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // 直接检查是否为我们的投票GUI，通过比较Component标题
        if (VotingGUI.isVotingGUI(player)) {
            event.setCancelled(true); // 阻止物品移动
            
            int slot = event.getSlot();
            int gameId = VotingGUI.getGameIdFromSlot(slot);
            
            if (gameId != -1) {
                // 玩家点击了有效的游戏选项
                VotingSystemFuncImpl.vote(player, gameId);
                // 无论成功失败都立即关闭GUI
                player.closeInventory();
            } else if (VotingGUI.isSkipIntroSlot(slot)) {
                // 玩家点击了跳过Intro选项
                VotingSystemFuncImpl.toggleSkipIntro();
                // 刷新当前玩家的GUI显示状态
                VotingGUI.refreshVotingGUI(player);
                // 刷新所有其他打开GUI的玩家
                VotingGUI.refreshAllVotingGUIs();
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isSuspended()) return;
        
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        // 检查是否是投票卡
        if (droppedItem.getType() == Material.PAPER && 
            droppedItem.hasItemMeta() && droppedItem.getItemMeta().hasDisplayName()) {
            
            String displayName = droppedItem.getItemMeta().getDisplayName();
            if ("§6§l投票卡".equals(displayName)) {
                // 取消丢弃投票卡
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void start() {
        // 初始化投票数据
        VotingSystemFuncImpl.initializeVoting();
        setSuspended(false);
    }

    @Override
    public void suspend() {
        setSuspended(true);
        // 清理投票数据
        VotingSystemFuncImpl.clearVotingData();
    }
}