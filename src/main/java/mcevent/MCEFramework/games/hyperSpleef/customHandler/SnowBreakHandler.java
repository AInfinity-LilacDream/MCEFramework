package mcevent.MCEFramework.games.hyperSpleef.customHandler;

import mcevent.MCEFramework.games.hyperSpleef.HyperSpleef;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
SnowBreakHandler: 超级掘一死战雪块破坏处理器
*/
public class SnowBreakHandler extends MCEResumableEventHandler implements Listener {

    private HyperSpleef hyperSpleef;

    public void register(HyperSpleef game) {
        this.hyperSpleef = game;
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
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDamage(BlockDamageEvent event) {
        if (isSuspended())
            return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material brokenBlock = block.getType();

        // 仅参与者且未死亡
        if (!player.getScoreboardTags().contains("Participant") ||
                player.getScoreboardTags().contains("dead")) {
            return;
        }

        // 检查 hyperSpleef 是否已注册
        if (hyperSpleef == null) {
            return;
        }

        // 检查是否在游戏世界中
        if (!player.getWorld().getName().equals(hyperSpleef.getWorldName())) {
            return;
        }

        // 让浮冰和冰块可以秒挖（无论用什么工具或空手）
        if (brokenBlock == Material.ICE || brokenBlock == Material.PACKED_ICE || brokenBlock == Material.BLUE_ICE) {
            // 设置即时破坏，无论用什么工具
            event.setInstaBreak(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isSuspended())
            return;

        Player player = event.getPlayer();
        Material brokenBlock = event.getBlock().getType();

        // 仅参与者且未死亡
        if (!player.getScoreboardTags().contains("Participant") ||
                player.getScoreboardTags().contains("dead")) {
            return;
        }

        // 检查 hyperSpleef 是否已注册
        if (hyperSpleef == null) {
            return;
        }

        // 检查是否在游戏世界中
        if (!player.getWorld().getName().equals(hyperSpleef.getWorldName())) {
            return;
        }

        // 处理浮冰（ICE）破坏，防止生成水
        if (brokenBlock == Material.ICE) {
            // 取消原事件，防止生成水
            event.setCancelled(true);
            // 直接设置为空气，不生成水
            event.getBlock().setType(Material.AIR);
            return;
        }

        // 允许所有方块破坏（像spleef一样，GlobalBlockInteractionHandler通过检查SnowBreakHandler是否挂起来控制）
        // 只对雪块和雪层进行特殊处理（掉落雪球）
        if (brokenBlock == Material.SNOW_BLOCK || brokenBlock == Material.SNOW) {
            // 取消默认掉落，直接给玩家雪球
            event.setDropItems(false);
            // 处理雪块破坏（直接give玩家雪球）
            int snowballAmount = getSnowballAmount(brokenBlock);
            hyperSpleef.addPlayerSnowballs(player, snowballAmount);
        }
        // 其他方块直接允许破坏，不做任何处理
    }

    /**
     * 根据破坏的方块类型决定雪球数量
     */
    private int getSnowballAmount(Material material) {
        return switch (material) {
            case SNOW_BLOCK -> 4; // 固定4个雪球
            case SNOW -> 2; // 固定2个雪球
            default -> 0;
        };
    }
}
