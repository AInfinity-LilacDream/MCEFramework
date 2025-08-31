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
import org.bukkit.inventory.ItemStack;

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
                
                // 打开投票GUI
                VotingGUI.openVotingGUI(player);
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