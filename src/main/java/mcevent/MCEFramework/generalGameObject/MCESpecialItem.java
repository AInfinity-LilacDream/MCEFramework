package mcevent.MCEFramework.generalGameObject;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEMessenger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MCESpecialItem: 特殊物品基类
 * 具有自定义名字、右键触发动作和冷却机制的物品基类
 */
@Getter @Setter
public abstract class MCESpecialItem {
    private String itemName; // 物品显示名称
    private Material material; // 物品材质
    private long cooldownTicks; // 冷却时间（tick）
    private Plugin plugin;
    
    // 玩家冷却状态管理
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();
    
    public MCESpecialItem(String itemName, Material material, long cooldownTicks, Plugin plugin) {
        this.itemName = itemName;
        this.material = material;
        this.cooldownTicks = cooldownTicks;
        this.plugin = plugin;
    }
    
    /**
     * 创建物品实例
     * @return 配置好的ItemStack
     */
    public ItemStack createItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(itemName));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 处理玩家右键交互事件
     * @param event 玩家交互事件
     * @param player 玩家
     * @return 是否成功处理
     */
    public boolean handleRightClick(PlayerInteractEvent event, Player player) {
        // 检查是否是右键
        if (!event.getAction().toString().contains("RIGHT_CLICK")) {
            return false;
        }
        
        // 检查手持物品是否匹配
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isMatchingItem(item)) {
            return false;
        }
        
        // 如果是匹配的物品，总是取消默认事件（如末影珍珠的投掷）
        event.setCancelled(true);
        
        // 检查冷却时间
        if (isOnCooldown(player)) {
            onCooldownMessage(player);
            return true; // 返回true表示事件被处理了（即使是冷却状态）
        }
        
        // 执行特殊动作
        boolean success = executeAction(player, event);
        
        // 如果成功执行，设置冷却
        if (success) {
            setCooldown(player);
            // 同时设置物品的原版冷却时间
            setItemCooldown(player);
        }
        
        return true; // 总是返回true，因为事件已被处理
    }
    
    /**
     * 检查物品是否匹配此特殊物品
     * @param item 要检查的物品
     * @return 是否匹配
     */
    protected boolean isMatchingItem(ItemStack item) {
        if (item == null || item.getType() != material) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        return MiniMessage.miniMessage().deserialize(itemName).equals(meta.displayName());
    }
    
    /**
     * 检查玩家是否在冷却中
     * @param player 玩家
     * @return 是否在冷却中
     */
    protected boolean isOnCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerCooldowns.containsKey(playerId)) {
            return false;
        }
        
        long lastUseTime = playerCooldowns.get(playerId);
        long currentTime = System.currentTimeMillis();
        long cooldownMs = cooldownTicks * 50; // tick转毫秒
        
        return (currentTime - lastUseTime) < cooldownMs;
    }
    
    /**
     * 设置玩家冷却
     * @param player 玩家
     */
    protected void setCooldown(Player player) {
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * 获取剩余冷却时间（秒）
     * @param player 玩家
     * @return 剩余冷却时间，如果没有冷却返回0
     */
    protected double getRemainingCooldown(Player player) {
        if (!isOnCooldown(player)) {
            return 0.0;
        }
        
        UUID playerId = player.getUniqueId();
        long lastUseTime = playerCooldowns.get(playerId);
        long currentTime = System.currentTimeMillis();
        long cooldownMs = cooldownTicks * 50;
        
        return Math.max(0, (cooldownMs - (currentTime - lastUseTime)) / 1000.0);
    }
    
    /**
     * 清除玩家冷却
     * @param player 玩家
     */
    public void clearCooldown(Player player) {
        playerCooldowns.remove(player.getUniqueId());
    }
    
    /**
     * 清除所有冷却
     */
    public void clearAllCooldowns() {
        playerCooldowns.clear();
    }
    
    /**
     * 设置物品的原版冷却时间
     * @param player 玩家
     */
    protected void setItemCooldown(Player player) {
        int cooldownTicks = (int) this.cooldownTicks;
        player.setCooldown(material, cooldownTicks);
    }
    
    /**
     * 执行特殊动作 - 子类需要实现
     * @param player 使用物品的玩家
     * @param event 交互事件
     * @return 是否成功执行
     */
    protected abstract boolean executeAction(Player player, PlayerInteractEvent event);
    
    /**
     * 冷却时的提示消息 - 子类可以重写
     * @param player 玩家
     */
    protected void onCooldownMessage(Player player) {
        double remainingTime = getRemainingCooldown(player);
        player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>该物品还在冷却中！</red>"));
    }
    
    /**
     * 播放声音效果的通用方法
     * @param location 播放位置
     * @param sound 声音类型
     * @param volume 音量
     * @param pitch 音调
     */
    protected void playSound(Location location, Sound sound, float volume, float pitch) {
        location.getWorld().playSound(location, sound, volume, pitch);
    }
    
    /**
     * 生成粒子效果的通用方法
     * @param location 位置
     * @param particle 粒子类型
     * @param count 粒子数量
     * @param offsetX X轴偏移
     * @param offsetY Y轴偏移
     * @param offsetZ Z轴偏移
     * @param extra 额外数据
     */
    protected void spawnParticles(Location location, Particle particle, int count, 
                                double offsetX, double offsetY, double offsetZ, double extra) {
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }
    
    /**
     * 发送动作栏消息的通用方法
     * @param player 玩家
     * @param message MiniMessage格式的消息
     */
    protected void sendActionBar(Player player, String message) {
        player.sendActionBar(MiniMessage.miniMessage().deserialize(message));
    }
}