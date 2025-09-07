package mcevent.MCEFramework.generalGameObject;

import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * 默认的游戏中玩家加入处理器
 * 提供通用的处理逻辑：让新加入玩家离开队伍，设置为观察者模式，不参与游戏
 */
public class DefaultGamePlayerJoinHandler implements GamePlayerJoinHandler {
    
    private final MCEGame game;
    
    public DefaultGamePlayerJoinHandler(MCEGame game) {
        this.game = game;
    }
    
    @Override
    public void handlePlayerJoinDuringGame(Player player) {
        plugin.getLogger().info("处理游戏中新加入的玩家: " + player.getName());
        
        // 检查玩家是否已经有队伍（可能是断线重连的游戏参与者）
        Team currentTeam = MCETeamUtils.getTeam(player);
        boolean wasGameParticipant = (currentTeam != null && game.getActiveTeams().contains(currentTeam));
        
        if (wasGameParticipant) {
            // 如果玩家原本就在游戏队伍中，保留其队伍成员身份
            plugin.getLogger().info("玩家 " + player.getName() + " 是游戏参与者，保留队伍成员身份: " + currentTeam.getName());
            
            // 延迟设置为观察者模式（因为已经被判定为死亡）
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.setGameMode(GameMode.SPECTATOR);
                    }
                }
            }.runTaskLater(plugin, 10L);
            
        } else {
            // 如果玩家不是游戏参与者，按原逻辑处理
            // 1. 让玩家离开当前队伍（如果有）
            removePlayerFromTeam(player);
            
            // 2. 确保玩家没有Active标签（不参与游戏）
            player.removeScoreboardTag("Active");
            
            // 3. 延迟设置为观察者模式，确保传送完成后再设置
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && !isGameParticipant(player)) {
                        player.setGameMode(getDefaultJoinGameMode());
                        player.removeScoreboardTag("Active"); // 再次确保移除
                        
                        // 发送提示消息
                        player.sendMessage("§c游戏正在进行中，你已设置为观察者模式。");
                        plugin.getLogger().info("已将玩家 " + player.getName() + " 设置为观察者模式");
                    }
                }
            }.runTaskLater(plugin, 10L); // 10 ticks = 0.5秒延迟
        }
    }
    
    @Override
    public boolean isGameParticipant(Player player) {
        // 玩家有Active标签且在游戏世界中才算参与者
        return player.getScoreboardTags().contains("Active") && 
               player.getWorld().getName().equals(game.getWorldName());
    }
    
    /**
     * 让玩家离开当前队伍
     */
    private void removePlayerFromTeam(Player player) {
        Team currentTeam = MCETeamUtils.getTeam(player);
        if (currentTeam != null) {
            currentTeam.removeEntry(player.getName());
            plugin.getLogger().info("已将玩家 " + player.getName() + " 从队伍 " + currentTeam.getName() + " 中移除");
            
            // 通知玩家
            player.sendMessage("§e你已自动离开队伍: §f" + currentTeam.getName());
        }
    }
}