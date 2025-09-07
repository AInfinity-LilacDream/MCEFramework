package mcevent.MCEFramework.games.football.customHandler;

import mcevent.MCEFramework.games.football.Football;
import mcevent.MCEFramework.games.football.FootballFuncImpl;
import mcevent.MCEFramework.tools.MCEMessenger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Armadillo;
import org.bukkit.scheduler.BukkitRunnable;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
BallTrackingHandler: 监听犰狳（球）的位置，检测进球
*/
public class BallTrackingHandler {
    
    private BukkitRunnable trackingTask;
    private Football game;
    private boolean isActive = false;

    public void start(Football football) {
        this.game = football;
        this.isActive = true;
        
        trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive || game.getBall() == null || game.getBall().isDead()) {
                    return;
                }
                
                checkGoal();
            }
        };
        
        // 每1个tick检查一次（20次/秒）- 提高检测频率
        trackingTask.runTaskTimer(plugin, 0L, 1L);
    }
    
    public void suspend() {
        this.isActive = false;
        if (trackingTask != null) {
            trackingTask.cancel();
        }
    }
    
    private void checkGoal() {
        Armadillo ball = game.getBall();
        Location ballLocation = ball.getLocation();
        
        // 调试信息：输出球的位置（每20tick输出一次，避免日志过多）
        if (System.currentTimeMillis() % 1000 < 50) { // 大约每秒输出一次
            plugin.getLogger().info("球的位置: " + String.format("%.2f, %.2f, %.2f", 
                ballLocation.getX(), ballLocation.getY(), ballLocation.getZ()));
            plugin.getLogger().info("红门范围: " + game.getRedGoalMin() + " 到 " + game.getRedGoalMax());
            plugin.getLogger().info("蓝门范围: " + game.getBlueGoalMin() + " 到 " + game.getBlueGoalMax());
        }
        
        // 检查红队球门
        if (FootballFuncImpl.isInGoal(ballLocation, game.getRedGoalMin(), game.getRedGoalMax())) {
            // 蓝队进球（球进入红队球门）
            plugin.getLogger().info("检测到蓝队进球！球位置: " + ballLocation.getX() + ", " + ballLocation.getY() + ", " + ballLocation.getZ());
            onGoalScored(false);
            return;
        }
        
        // 检查蓝队球门
        if (FootballFuncImpl.isInGoal(ballLocation, game.getBlueGoalMin(), game.getBlueGoalMax())) {
            // 红队进球（球进入蓝队球门）
            plugin.getLogger().info("检测到红队进球！球位置: " + ballLocation.getX() + ", " + ballLocation.getY() + ", " + ballLocation.getZ());
            onGoalScored(true);
            return;
        }
    }
    
    private void onGoalScored(boolean redTeamScored) {
        // 暂停跟踪以防止重复检测
        isActive = false;
        
        // 调用游戏的进球处理方法
        game.onGoal(redTeamScored);
        
        // 3秒后重新激活跟踪
        game.setDelayedTask(3, () -> {
            isActive = true;
        });
    }
}