package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * GamePlayerQuitHandler: 处理游戏中玩家退出的事件
 * 根据面向对象设计原则，提供统一的退出处理接口
 */
public class GamePlayerQuitHandler extends MCEResumableEventHandler implements Listener {

    public GamePlayerQuitHandler() {
        setSuspended(false); // 始终启用，不需要暂停
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (isSuspended()) return;

        Player player = event.getPlayer();
        
        // 清理玩家的发光效果
        clearPlayerGlowingEffect(player);
        
        // 检查是否有正在进行的游戏
        if (MCEMainController.isRunningGame()) {
            MCEGame currentGame = MCEMainController.getCurrentRunningGame();
            
            if (currentGame != null) {
                // 检查玩家是否是游戏参与者
                if (currentGame.isGameParticipant(player)) {
                    // 如果是游戏参与者，处理游戏中退出逻辑
                    handlePlayerQuitDuringGame(player, currentGame);
                }
            }
        }
    }
    
    /**
     * 处理玩家在游戏中退出的逻辑
     * 遵循开闭原则，允许子类扩展具体游戏的退出处理逻辑
     */
    private void handlePlayerQuitDuringGame(Player player, MCEGame game) {
        // 检查玩家是否处于活跃游戏状态
        if (player.getGameMode() == GameMode.ADVENTURE && 
            player.getScoreboardTags().contains("Active") && 
            !player.getScoreboardTags().contains("dead")) {
            
            // 将退出的活跃玩家标记为死亡
            markPlayerAsDead(player);
            
            // 发送全局消息通知玩家退出
            String playerName = MCEPlayerUtils.getColoredPlayerName(player);
            
            // 通知游戏处理玩家退出
            game.handlePlayerQuitDuringGame(player);
        }
    }
    
    /**
     * 标记玩家为死亡状态
     * 统一的死亡标记逻辑，符合单一职责原则
     * 注意：不移除玩家的队伍成员身份，保证重新加入时仍在原队伍
     */
    private void markPlayerAsDead(Player player) {
        // 设置为旁观模式
        player.setGameMode(GameMode.SPECTATOR);
        
        // 添加死亡标签
        player.addScoreboardTag("dead");
        
        // 移除活跃标签
        player.removeScoreboardTag("Active");
        
        // 清理玩家的发光效果
        clearPlayerGlowingEffect(player);
        
        // 重要：不移除玩家的队伍成员身份
        // 这样玩家重新加入时仍然在原队伍中
    }
    
    /**
     * 清理玩家的发光效果
     * 遵循单一职责原则，专门负责发光效果清理
     */
    private void clearPlayerGlowingEffect(Player player) {
        // 清除玩家自身的发光效果
        player.setGlowing(false);
        
        // 如果玩家有发光相关的scoreboard tags，也清理掉
        player.getScoreboardTags().removeIf(tag -> 
            tag.toLowerCase().contains("glow") || 
            tag.toLowerCase().contains("glowing"));
    }
}