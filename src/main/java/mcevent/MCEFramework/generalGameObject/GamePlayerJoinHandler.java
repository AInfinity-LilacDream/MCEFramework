package mcevent.MCEFramework.generalGameObject;

import org.bukkit.entity.Player;

/**
 * 游戏中玩家加入处理接口
 * 定义当新玩家在游戏进行中加入服务器时的处理逻辑
 */
public interface GamePlayerJoinHandler {
    
    /**
     * 处理新加入游戏的玩家
     * 确保新玩家不会干扰正在进行的游戏
     * 
     * @param player 新加入的玩家
     */
    void handlePlayerJoinDuringGame(Player player);
    
    /**
     * 检查玩家是否应该被视为游戏参与者
     * 
     * @param player 要检查的玩家
     * @return true表示玩家是游戏参与者，false表示是观察者
     */
    boolean isGameParticipant(Player player);
    
    /**
     * 获取新加入玩家的默认游戏模式
     * 
     * @return 新加入玩家应该设置的游戏模式
     */
    default org.bukkit.GameMode getDefaultJoinGameMode() {
        return org.bukkit.GameMode.SPECTATOR;
    }
}