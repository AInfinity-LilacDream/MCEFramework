package mcevent.MCEFramework.games.musicDodge;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;

/**
 * Dust粒子包拦截器 - 拦截所有Dust粒子包以减少网络流量
 */
public class DustParticleInterceptor extends PacketAdapter {
    
    private boolean isEnabled = false;
    
    public DustParticleInterceptor(Plugin plugin) {
        super(plugin, ListenerPriority.HIGH, PacketType.Play.Server.WORLD_PARTICLES);
        
        // 立即注册监听器到ProtocolLib，但保持禁用状态
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }
    
    @Override
    public void onPacketSending(PacketEvent event) {
        try {
            // 检查是否为粒子包
            if (event.getPacketType() == PacketType.Play.Server.WORLD_PARTICLES) {
                var packet = event.getPacket();
                
                // 根据1.21协议结构读取粒子包
                // 字段顺序：Long Distance(Boolean), Always Visible(Boolean), X(Double), Y(Double), Z(Double), 
                // Offset X(Float), Offset Y(Float), Offset Z(Float), Max Speed(Float), Particle Count(Int), Particle ID(VarInt), Data(Varies)
                
                try {
                    // 检查粒子类型 - 在新版ProtocolLib中，粒子信息在Object字段中
                    boolean isDustParticle = false;
                    
                    // 检查Object字段中的粒子信息
                    if (packet.getSpecificModifier(Object.class).size() > 0) {
                        Object particleData = packet.getSpecificModifier(Object.class).read(0);
                        if (particleData != null) {
                            String particleType = particleData.getClass().getSimpleName();
                            
                            // 检查是否为DUST粒子
                            isDustParticle = particleType.equals("DustParticleOptions") || 
                                           particleType.contains("Dust") || 
                                           particleData.getClass().getName().contains("DustParticle");
                        }
                    }
                    
                    if (isEnabled && isDustParticle) {
                        event.setCancelled(true);
                    }
                    
                } catch (Exception e1) {
                    // 备用策略：如果无法确定粒子类型且启用拦截，则拦截所有粒子
                    if (isEnabled) {
                        event.setCancelled(true);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error while intercepting particle packet: " + e.getMessage());
        }
    }
    
    /**
     * 检查粒子ID是否为DUST类型
     */
    private boolean isDustParticle(int particleId) {
        // 根据Minecraft 1.21协议文档：
        // minecraft:dust 的粒子ID是 13
        return particleId == 13;
    }
    
    /**
     * 启用粒子拦截
     */
    public void enable() {
        if (!isEnabled) {
            isEnabled = true;
        }
    }
    
    /**
     * 禁用粒子拦截
     */
    public void disable() {
        if (isEnabled) {
            isEnabled = false;
            ProtocolLibrary.getProtocolManager().removePacketListener(this);
        }
    }
    
    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return isEnabled;
    }
    
    /**
     * 静态工厂方法
     */
    public static DustParticleInterceptor create(Plugin plugin) {
        return new DustParticleInterceptor(plugin);
    }
}