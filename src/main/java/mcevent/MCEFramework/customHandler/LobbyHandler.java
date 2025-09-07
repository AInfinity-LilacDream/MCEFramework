package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * LobbyHandler: 主城功能处理器
 * 处理烈焰棒风弹发射和二段跳功能
 */
public class LobbyHandler extends MCEResumableEventHandler implements Listener {
    
    // 风弹冷却时间跟踪 (玩家UUID -> 冷却结束时间)
    private final Map<UUID, Long> windChargeCooldown = new HashMap<>();
    
    // 二段跳状态跟踪 (玩家UUID -> 是否可以二段跳)
    private final Map<UUID, Boolean> doubleJumpAvailable = new HashMap<>();
    
    // 饱和效果任务
    private BukkitRunnable saturationTask;
    
    public LobbyHandler() {
        setSuspended(false); // 默认启用
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startSaturationTask();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 如果玩家在主城，给予烈焰棒和飞行能力
        if (isInLobby(player)) {
            giveBlazeRod(player);
            enableDoubleJump(player);
            
            // 在主城时关闭PVP并开启友伤
            mcevent.MCEFramework.tools.MCEWorldUtils.disablePVP();
            mcevent.MCEFramework.tools.MCETeamUtils.enableFriendlyFire();
        }
    }
    
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        if (isInLobby(player)) {
            // 进入主城，给予烈焰棒和飞行能力
            giveBlazeRod(player);
            enableDoubleJump(player);
            
            // 进入主城时关闭PVP并开启友伤
            mcevent.MCEFramework.tools.MCEWorldUtils.disablePVP();
            mcevent.MCEFramework.tools.MCETeamUtils.enableFriendlyFire();
        } else {
            // 离开主城，移除飞行能力和二段跳状态
            disableDoubleJump(player);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isSuspended()) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // 检查是否在主城
        if (!isInLobby(player)) return;
        
        // 检查是否右键使用烈焰棒
        if (item != null && item.getType() == Material.BLAZE_ROD && 
            item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            
            String displayName = item.getItemMeta().getDisplayName();
            if ("§c§l风弹发射器".equals(displayName)) {
                Action action = event.getAction();
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    
                    // 检查冷却时间
                    UUID playerUUID = player.getUniqueId();
                    long currentTime = System.currentTimeMillis();
                    
                    if (windChargeCooldown.containsKey(playerUUID) && 
                        windChargeCooldown.get(playerUUID) > currentTime) {
                        // 还在冷却中
                        return;
                    }
                    
                    // 发射风弹
                    WindCharge windCharge = player.launchProjectile(WindCharge.class);
                    windCharge.setVelocity(player.getLocation().getDirection().multiply(2.0));
                    
                    // 设置冷却时间 (3秒)
                    windChargeCooldown.put(playerUUID, currentTime + 3000);
                }
            }
        }
    }
    
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (isSuspended()) return;
        
        // 检查是否是风弹击中玩家
        if (event.getEntity() instanceof WindCharge && event.getHitEntity() instanceof Player) {
            Player hitPlayer = (Player) event.getHitEntity();
            
            // 给被击中的玩家添加3秒发光效果
            hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, true));
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isSuspended()) return;
        
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        // 检查是否在主城
        if (!isInLobby(player)) return;
        
        // 检查是否是烈焰棒
        if (droppedItem.getType() == Material.BLAZE_ROD && 
            droppedItem.hasItemMeta() && droppedItem.getItemMeta().hasDisplayName()) {
            
            String displayName = droppedItem.getItemMeta().getDisplayName();
            if ("§c§l风弹发射器".equals(displayName)) {
                // 取消丢弃烈焰棒
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (isSuspended()) return;
        
        Player player = event.getPlayer();
        
        // 检查是否在主城且不在创造模式
        if (!isInLobby(player) || player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        
        // 检查是否可以二段跳
        UUID playerUUID = player.getUniqueId();
        if (doubleJumpAvailable.getOrDefault(playerUUID, false)) {
            event.setCancelled(true);
            
            // 执行二段跳
            Vector velocity = player.getVelocity();
            Vector direction = player.getLocation().getDirection();
            
            // 增强推力强度
            velocity.setY(1.0); // 向上的推力（从0.6增加到1.0）
            velocity.add(direction.multiply(1.2).setY(0)); // 前方的推力（从0.8增加到1.2）
            
            player.setVelocity(velocity);
            
            // 禁用二段跳直到下次着地
            doubleJumpAvailable.put(playerUUID, false);
            player.setAllowFlight(false);
            
            // 播放音效
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isSuspended()) return;
        
        Player player = event.getPlayer();
        
        // 检查是否在主城
        if (!isInLobby(player)) return;
        
        UUID playerUUID = player.getUniqueId();
        
        // 检查玩家是否着地且之前没有二段跳能力
        if (player.isOnGround() && !doubleJumpAvailable.getOrDefault(playerUUID, false)) {
            // 重置二段跳
            doubleJumpAvailable.put(playerUUID, true);
            player.setAllowFlight(true);
            player.setFlying(false);
        }
        // 检查玩家是否在空中但仍有二段跳能力（确保飞行权限开启）
        else if (!player.isOnGround() && doubleJumpAvailable.getOrDefault(playerUUID, false) && !player.getAllowFlight()) {
            player.setAllowFlight(true);
            player.setFlying(false);
        }
    }
    
    /**
     * 检查玩家是否在主城
     */
    private boolean isInLobby(Player player) {
        return "lobby".equals(player.getWorld().getName());
    }
    
    /**
     * 给予玩家烈焰棒（先清空物品栏）
     */
    public void giveBlazeRod(Player player) {
        // 先清空物品栏
        player.getInventory().clear();
        
        // 创建烈焰棒并放到第一个位置
        ItemStack blazeRod = createBlazeRod();
        player.getInventory().setItem(0, blazeRod);
    }
    
    /**
     * 创建烈焰棒物品
     */
    private ItemStack createBlazeRod() {
        ItemStack blazeRod = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = blazeRod.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§l风弹发射器");
            meta.setLore(java.util.Arrays.asList(
                "§e右键发射风弹",
                "§7击中玩家给予发光效果",
                "§c冷却时间: 3秒"
            ));
            blazeRod.setItemMeta(meta);
        }
        return blazeRod;
    }
    
    /**
     * 启用二段跳功能
     */
    private void enableDoubleJump(Player player) {
        UUID playerUUID = player.getUniqueId();
        doubleJumpAvailable.put(playerUUID, true);
        
        // 确保玩家在冒险模式下也能使用飞行
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            player.setAllowFlight(true);
            player.setFlying(false);
        }
    }
    
    /**
     * 禁用二段跳功能
     */
    private void disableDoubleJump(Player player) {
        UUID playerUUID = player.getUniqueId();
        doubleJumpAvailable.remove(playerUUID);
        player.setAllowFlight(false);
        player.setFlying(false);
    }
    
    /**
     * 当玩家着地时重置二段跳
     */
    public void resetDoubleJump(Player player) {
        if (isInLobby(player)) {
            UUID playerUUID = player.getUniqueId();
            doubleJumpAvailable.put(playerUUID, true);
            player.setAllowFlight(true);
            player.setFlying(false);
        }
    }
    
    @Override
    public void start() {
        setSuspended(false);
        
        // 为所有在线玩家启用主城功能
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInLobby(player)) {
                giveBlazeRod(player);
                enableDoubleJump(player);
            }
        }
        
        // 启动饱和效果任务
        if (saturationTask == null || saturationTask.isCancelled()) {
            startSaturationTask();
        }
    }
    
    @Override
    public void suspend() {
        setSuspended(true);
        
        // 清理所有冷却和状态数据
        windChargeCooldown.clear();
        doubleJumpAvailable.clear();
        
        // 停止饱和效果任务
        if (saturationTask != null && !saturationTask.isCancelled()) {
            saturationTask.cancel();
        }
    }
    
    /**
     * 启动饱和效果任务
     */
    private void startSaturationTask() {
        saturationTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isInLobby(player)) {
                        // 给予饱和效果 (10秒，等级255，无粒子效果)
                        player.addPotionEffect(new PotionEffect(
                            PotionEffectType.SATURATION, 
                            200, // 10秒 (20 ticks * 10)
                            255, // 最高等级
                            true, // 环境效果
                            false // 无粒子
                        ));
                    }
                }
            }
        };
        
        // 每5秒执行一次 (100 ticks)
        saturationTask.runTaskTimer(plugin, 0L, 100L);
    }
}