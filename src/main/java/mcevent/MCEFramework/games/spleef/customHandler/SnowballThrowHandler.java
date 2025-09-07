package mcevent.MCEFramework.games.spleef.customHandler;

import mcevent.MCEFramework.games.spleef.Spleef;
import mcevent.MCEFramework.games.spleef.SpleefFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import mcevent.MCEFramework.tools.MCETimerUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
SnowballThrowHandler: 冰雪掘战雪球投掷处理器
*/
public class SnowballThrowHandler extends MCEResumableEventHandler implements Listener {
    
    private Spleef spleef;
    private Map<String, Boolean> playerHoldingRightClick = new HashMap<>();
    private BukkitRunnable snowballTask;
    
    public void register(Spleef game) {
        this.spleef = game;
        setSuspended(true); // 默认挂起，游戏开始时启动
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    public void start() {
        setSuspended(false);
        
        // 启动每帧检测右键的任务
        snowballTask = MCETimerUtils.setFramedTask(() -> {
            if (!isSuspended()) {
                checkPlayersRightClick();
            }
        });
    }
    
    @Override
    public void suspend() {
        setSuspended(true);
        
        // 停止每帧检测任务
        if (snowballTask != null && !snowballTask.isCancelled()) {
            snowballTask.cancel();
        }
        
        // 清理右键状态
        playerHoldingRightClick.clear();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isSuspended()) return;
        
        Player player = event.getPlayer();
        
        // 检查玩家是否在游戏中
        if (!player.getScoreboardTags().contains("Active") || 
            player.getScoreboardTags().contains("dead")) {
            return;
        }
        
        // 检查是否手持雪球并且右键
        if (player.getInventory().getItemInMainHand().getType() == Material.SNOWBALL && 
            (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            // 允许正常的雪球投掷，使用原版机制
            return;
        }
        
        // 如果手持金铲子右键，自动发射雪球
        if (player.getInventory().getItemInMainHand().getType() == Material.GOLDEN_SHOVEL && 
            (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true); // 防止金铲子的右键行为
            
            // 检查玩家是否有雪球
            if (player.getInventory().contains(Material.SNOWBALL)) {
                // 移除一个雪球并发射
                player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(Material.SNOWBALL, 1));
                
                // 发射雪球
                Snowball snowball = player.launchProjectile(Snowball.class);
                snowball.setVelocity(player.getEyeLocation().getDirection().multiply(2.2));
                plugin.getLogger().info("调试 - " + player.getName() + " 使用金铲子发射了雪球");
            }
        }
    }
    
    /**
     * 简化的右键检测（不再需要每帧检测）
     */
    private void checkPlayersRightClick() {
        // 不再需要每帧检测，使用默认的雪球机制
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        plugin.getLogger().info("调试 - SnowballThrowHandler收到伤害事件, suspended=" + isSuspended() + 
            ", damager=" + event.getDamager().getClass().getSimpleName() + 
            ", entity=" + event.getEntity().getClass().getSimpleName() +
            ", cancelled=" + event.isCancelled());
            
        if (isSuspended()) return;
        
        // 检查是否是雪球击中玩家
        if (!(event.getDamager() instanceof Snowball snowball) || !(event.getEntity() instanceof Player target)) {
            return;
        }
        
        // 检查是否是玩家发射的雪球
        if (!(snowball.getShooter() instanceof Player shooter)) {
            return;
        }
        
        plugin.getLogger().info("调试 - 雪球击中: " + shooter.getName() + " -> " + target.getName() + ", 伤害: " + event.getDamage());
        
        // 检查玩家是否在游戏中
        if (!target.getScoreboardTags().contains("Active") || target.getScoreboardTags().contains("dead") ||
            !shooter.getScoreboardTags().contains("Active") || shooter.getScoreboardTags().contains("dead")) {
            plugin.getLogger().info("调试 - 雪球击中但玩家不在活跃状态");
            event.setCancelled(true);
            return;
        }
        
        // 检查友伤
        Team shooterTeam = MCETeamUtils.getTeam(shooter);
        Team targetTeam = MCETeamUtils.getTeam(target);
        if (shooterTeam != null && targetTeam != null && shooterTeam.equals(targetTeam)) {
            plugin.getLogger().info("调试 - 友伤，取消雪球伤害");
            event.setCancelled(true);
            return;
        }
        
        plugin.getLogger().info("调试 - 雪球伤害允许，设置为半颗心伤害");
        // 设置雪球伤害为半颗心（1.0）
        event.setDamage(1.0);
        // 设置元数据标记，防止其他处理器干扰
        event.getEntity().setMetadata("spleef_snowball_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        // 允许击退效果
    }
    
}