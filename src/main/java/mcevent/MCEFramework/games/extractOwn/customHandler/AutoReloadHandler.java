package mcevent.MCEFramework.games.extractOwn.customHandler;

import mcevent.MCEFramework.games.extractOwn.ExtractOwn;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * AutoReloadHandler: 自动拉弩处理器
 * 模拟原版拉弦动画但移除减速效果
 */
public class AutoReloadHandler extends MCEResumableEventHandler implements Listener {
    
    private ExtractOwn extractOwn;
    private final Map<UUID, BukkitRunnable> chargingTasks = new HashMap<>();
    
    public void register(ExtractOwn game) {
        this.extractOwn = game;
        setSuspended(true); // 默认挂起，游戏开始时启动
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    public void start() {
        setSuspended(false);
    }
    
    @Override
    public void suspend() {
        setSuspended(true);
        // 清理所有任务
        clearAllTasks();
    }
    
    private void clearAllTasks() {
        for (BukkitRunnable task : chargingTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        chargingTasks.clear();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isSuspended()) return;
        
        Player player = event.getPlayer();
        
        // 检查玩家是否在游戏中
        if (player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.CROSSBOW) {
            return;
        }
        
        // 检查是否右键使用弩
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // 如果弩没有装填，开始拉弦过程
            if (item.getItemMeta() instanceof CrossbowMeta crossbowMeta && !crossbowMeta.hasChargedProjectiles()) {
                event.setCancelled(true); // 取消原版行为
                startChargingCrossbow(player);
            }
        }
    }
    
    @EventHandler
    public void onCrossbowShoot(EntityShootBowEvent event) {
        if (isSuspended()) return;
        
        if (event.getEntity() instanceof Player player) {
            // 检查玩家是否在游戏中
            if (player.getGameMode() != GameMode.ADVENTURE) {
                return;
            }
            
            // 射击后自动开始拉弦
            if (event.getBow() != null && event.getBow().getType() == Material.CROSSBOW) {
                UUID playerId = player.getUniqueId();
                stopChargingCrossbow(playerId);
                
                // 延迟5tick开始自动拉弦
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.getInventory().getItemInMainHand().getType() == Material.CROSSBOW) {
                        startChargingCrossbow(player);
                    }
                }, 5L);
            }
        }
    }
    
    /**
     * 开始拉弦过程（模拟原版动画但无减速）
     */
    private void startChargingCrossbow(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 停止之前的任务
        stopChargingCrossbow(playerId);
        
        // 快速装填I的充能时间：约0.75秒（15 ticks）
        BukkitRunnable chargingTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 检查玩家状态
                if (player.getGameMode() != GameMode.ADVENTURE || 
                    player.getInventory().getItemInMainHand().getType() != Material.CROSSBOW) {
                    stopChargingCrossbow(playerId);
                    return;
                }
                
                ItemStack crossbow = player.getInventory().getItemInMainHand();
                if (crossbow.getItemMeta() instanceof CrossbowMeta crossbowMeta) {
                    // 检查是否有箭
                    if (player.getInventory().contains(Material.ARROW)) {
                        // 完成装填
                        ItemStack arrow = new ItemStack(Material.ARROW);
                        crossbowMeta.addChargedProjectile(arrow);
                        crossbow.setItemMeta(crossbowMeta);
                        player.updateInventory();
                        
                        // 播放装填完成音效
                        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.0f);
                    }
                }
                
                stopChargingCrossbow(playerId);
            }
        };
        
        chargingTasks.put(playerId, chargingTask);
        // 15 ticks后完成装填（对应快速装填I的时间）
        chargingTask.runTaskLater(plugin, 15L);
        
        // 播放开始装填音效
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_CROSSBOW_LOADING_START, 1.0f, 1.0f);
    }
    
    /**
     * 停止拉弦过程
     */
    private void stopChargingCrossbow(UUID playerId) {
        BukkitRunnable chargingTask = chargingTasks.get(playerId);
        if (chargingTask != null && !chargingTask.isCancelled()) {
            chargingTask.cancel();
        }
        chargingTasks.remove(playerId);
    }
}