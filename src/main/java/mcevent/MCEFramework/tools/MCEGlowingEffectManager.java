package mcevent.MCEFramework.tools;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * MCEGlowingEffectManager: 发光效果管理器
 * 遵循单一职责原则，专门负责管理所有发光效果
 * 提供统一的发光效果接口，符合开闭原则
 */
public class MCEGlowingEffectManager {
    
    private static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    
    /**
     * 为指定玩家设置发光效果（对特定观察者可见）
     * 使用ProtocolLib发送数据包实现客户端发光效果
     */
    public static void setPlayerGlowing(Player target, Player viewer, boolean glowing) {
        if (target == null || viewer == null) return;
        
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, target.getEntityId());

            List<WrappedDataValue> dataValues = new ArrayList<>();
            byte glowingFlag = glowing ? (byte) 0x40 : (byte) 0x00;
            
            dataValues.add(
                    new WrappedDataValue(
                            0,
                            WrappedDataWatcher.Registry.get(Byte.class),
                            glowingFlag
                    )
            );
            packet.getDataValueCollectionModifier().write(0, dataValues);

            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 清除指定玩家的发光效果
     * 对所有在线玩家清除目标玩家的发光效果
     */
    public static void clearPlayerGlowingEffect(Player target) {
        if (target == null) return;
        
        // 清除原生发光效果
        target.setGlowing(false);
        
        // 清除基于ProtocolLib的发光效果
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) {
                setPlayerGlowing(target, viewer, false);
            }
        }
        
        // 清除发光相关的scoreboard tags
        target.getScoreboardTags().removeIf(tag -> 
            tag.toLowerCase().contains("glow") || 
            tag.toLowerCase().contains("glowing"));
    }
    
    /**
     * 全局清除所有玩家的发光效果
     * 用于游戏结束或返回主城时的全面清理
     */
    public static void clearAllGlowingEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearPlayerGlowingEffect(player);
        }
    }
    
    /**
     * 为玩家设置团队发光效果
     * 让特定团队的玩家对另一团队发光
     */
    public static void setTeamGlowingEffect(List<Player> targetTeam, List<Player> viewerTeam, boolean glowing) {
        if (targetTeam == null || viewerTeam == null) return;
        
        for (Player target : targetTeam) {
            for (Player viewer : viewerTeam) {
                if (!target.equals(viewer)) {
                    setPlayerGlowing(target, viewer, glowing);
                }
            }
        }
    }
    
    /**
     * 清除团队间的发光效果
     */
    public static void clearTeamGlowingEffect(List<Player> targetTeam, List<Player> viewerTeam) {
        setTeamGlowingEffect(targetTeam, viewerTeam, false);
    }
    
    /**
     * 检查玩家是否有发光效果标记
     */
    public static boolean hasGlowingTag(Player player) {
        if (player == null) return false;
        
        return player.getScoreboardTags().stream()
                .anyMatch(tag -> tag.toLowerCase().contains("glow") || 
                                tag.toLowerCase().contains("glowing"));
    }
}