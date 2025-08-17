package mcevent.MCEFramework.games.musicDodge;

import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;

/**
 * MusicDodge Payload处理器 - 服务端版本
 * 用于发送攻击数据到客户端
 */
public class MusicDodgePayload {
    
    public static final String CHANNEL_NAME = "mce:musicdodge";
    
    /**
     * 创建攻击数据Payload（直接使用字符串）
     * 1.21.4版本的客户端会使用PacketByteBuf.writeString/readString来处理
     */
    public static byte[] createPayload(String attackData) {
        try {
            // 使用Minecraft的字符串编码格式
            // 这需要模拟PacketByteBuf.writeString的行为
            byte[] stringBytes = attackData.getBytes(StandardCharsets.UTF_8);
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            
            // writeString格式：VarInt长度 + UTF-8字节
            writeVarInt(dos, stringBytes.length);
            dos.write(stringBytes);
            
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            // 返回空字符串的编码
            return new byte[]{0}; // VarInt 0
        }
    }
    
    /**
     * 写入VarInt（模拟Minecraft的VarInt编码）
     */
    private static void writeVarInt(java.io.DataOutputStream dos, int value) throws java.io.IOException {
        while ((value & -128) != 0) {
            dos.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        dos.writeByte(value);
    }
    
    /**
     * 发送攻击数据到玩家
     */
    public static void sendToPlayer(Player player, String attackData) {
        try {
            byte[] payload = createPayload(attackData);
            player.sendPluginMessage(
                mcevent.MCEFramework.miscellaneous.Constants.plugin, 
                CHANNEL_NAME, 
                payload
            );
        } catch (Exception e) {
            // 如果发送失败，静默忽略（玩家可能没有对应的客户端Mod）
            // System.err.println("Failed to send attack data to player: " + e.getMessage());
        }
    }
    
    /**
     * 发送攻击数据到所有在线玩家
     */
    public static void sendToAllPlayers(String attackData) {
        try {
            byte[] payload = createPayload(attackData);
            
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                try {
                    player.sendPluginMessage(
                        mcevent.MCEFramework.miscellaneous.Constants.plugin, 
                        CHANNEL_NAME, 
                        payload
                    );
                } catch (Exception e) {
                    // 忽略单个玩家的发送失败
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send attack data to all players: " + e.getMessage());
        }
    }
    
    /**
     * 创建空Payload（清除客户端显示）
     */
    public static void sendClearToAllPlayers() {
        sendToAllPlayers("");
    }
}