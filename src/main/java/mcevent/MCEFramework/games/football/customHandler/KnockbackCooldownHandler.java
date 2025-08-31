package mcevent.MCEFramework.games.football.customHandler;

import mcevent.MCEFramework.games.football.Football;
import mcevent.MCEFramework.tools.MCEMessenger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
KnockbackCooldownHandler: 处理击退棒的冷却系统
*/
public class KnockbackCooldownHandler implements Listener {
    
    private Football game;
    private Map<UUID, Long> blaze_rod_cooldowns = new HashMap<>(); // 击退3冷却
    private Map<UUID, Long> breeze_rod_cooldowns = new HashMap<>(); // 击退7冷却
    private static final long BLAZE_ROD_COOLDOWN = 5000L; // 5秒冷却（毫秒）
    private static final long BREEZE_ROD_COOLDOWN = 20000L; // 20秒冷却（毫秒）
    
    public void start(Football football) {
        this.game = football;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void suspend() {
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        blaze_rod_cooldowns.clear();
        breeze_rod_cooldowns.clear();
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (game == null) return;
        
        Entity damager = event.getDamager();
        if (!(damager instanceof Player player)) return;
        
        ItemStack item = player.getInventory().getItemInMainHand();
        Material itemType = item.getType();
        
        // 只处理有冷却的武器：BLAZE_ROD（击退3）和 BREEZE_ROD（击退7）
        if (itemType != Material.BLAZE_ROD && itemType != Material.BREEZE_ROD) return;
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (itemType == Material.BLAZE_ROD) {
            // 处理击退3烈焰棒冷却
            if (!item.hasItemMeta() || item.getEnchantmentLevel(Enchantment.KNOCKBACK) != 3) return;
            
            if (blaze_rod_cooldowns.containsKey(playerId)) {
                long lastUse = blaze_rod_cooldowns.get(playerId);
                long timeDiff = currentTime - lastUse;
                
                if (timeDiff < BLAZE_ROD_COOLDOWN) {
                    event.setCancelled(true);
                    return;
                }
            }
            
            // 记录使用时间并设置冷却
            blaze_rod_cooldowns.put(playerId, currentTime);
            player.setCooldown(Material.BLAZE_ROD, 100); // 5秒 = 100 ticks
            startCooldownTimer(player, Material.BLAZE_ROD, 5);
            
        } else {
            // 处理击退7旋风棒冷却
            if (!item.hasItemMeta() || item.getEnchantmentLevel(Enchantment.KNOCKBACK) != 7) return;
            
            if (breeze_rod_cooldowns.containsKey(playerId)) {
                long lastUse = breeze_rod_cooldowns.get(playerId);
                long timeDiff = currentTime - lastUse;
                
                if (timeDiff < BREEZE_ROD_COOLDOWN) {
                    event.setCancelled(true);
                    return;
                }
            }
            
            // 记录使用时间并设置冷却
            breeze_rod_cooldowns.put(playerId, currentTime);
            player.setCooldown(Material.BREEZE_ROD, 400); // 20秒 = 400 ticks
            startCooldownTimer(player, Material.BREEZE_ROD, 20);
        }
    }
    
    private void startCooldownTimer(Player player, Material weaponType, int totalCooldownSeconds) {
        new BukkitRunnable() {
            int remainingSeconds = totalCooldownSeconds;
            
            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    // 冷却完成，恢复原名称
                    updateWeaponName(player, weaponType, 0);
                    cancel();
                    return;
                }
                
                // 更新物品名称显示剩余冷却时间
                updateWeaponName(player, weaponType, remainingSeconds);
                remainingSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每秒执行一次
    }
    
    private void updateWeaponName(Player player, Material weaponType, int cooldownSeconds) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != weaponType) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (weaponType == Material.BLAZE_ROD) {
                if (cooldownSeconds > 0) {
                    meta.displayName(MiniMessage.miniMessage().deserialize(
                        "<red><bold>强力击退棒（冷却: " + cooldownSeconds + "s）</bold></red>"));
                } else {
                    meta.displayName(MiniMessage.miniMessage().deserialize(
                        "<red><bold>强力击退棒（击退3）</bold></red>"));
                }
            } else if (weaponType == Material.BREEZE_ROD) {
                if (cooldownSeconds > 0) {
                    meta.displayName(MiniMessage.miniMessage().deserialize(
                        "<gold><bold>超级旋风棒（冷却: " + cooldownSeconds + "s）</bold></gold>"));
                } else {
                    meta.displayName(MiniMessage.miniMessage().deserialize(
                        "<gold><bold>超级旋风棒（击退7）</bold></gold>"));
                }
            }
            item.setItemMeta(meta);
        }
    }
}