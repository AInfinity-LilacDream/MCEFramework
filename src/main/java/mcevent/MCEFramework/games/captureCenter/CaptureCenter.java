package mcevent.MCEFramework.games.captureCenter;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.captureCenter.customHandler.PlayerFallHandler;
import mcevent.MCEFramework.games.captureCenter.gameObject.CaptureCenterGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

@Getter @Setter
public class CaptureCenter extends MCEGame {
    
    private PlayerFallHandler playerFallHandler = new PlayerFallHandler();
    private List<BukkitRunnable> gameTask = new ArrayList<>();
    private CaptureCenterConfigParser captureCenterConfigParser = new CaptureCenterConfigParser();
    
    public CaptureCenter(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
                        int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration, 
                        int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration, cycleEndDuration, endDuration);
    }
    
    @Override
    public void onLaunch() {
        CaptureCenterFuncImpl.loadConfig();
        
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
        }
        
        // 恢复地图原状
        CaptureCenterFuncImpl.resetMap();
        
        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCETeleporter.globalSwapWorld(this.getWorldName());
        
        // 传送所有玩家到指定位置
        CaptureCenterFuncImpl.teleportPlayersToSpawn();
        
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameMode(GameMode.ADVENTURE);
        
        CaptureCenterFuncImpl.initializePlayers();
        
        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
        grantGlobalPotionEffect(saturation);
        MCEPlayerUtils.clearGlobalTags();
        
        // 注册事件处理器
        playerFallHandler.register();
    }
    
    @Override
    public void onCycleStart() {
        CaptureCenterFuncImpl.resetGameBoard();
        this.getGameBoard().setStateTitle("<red><bold> 剩余时间：</bold></red>");
        
        // 确保所有玩家都有Active标签
        MCEPlayerUtils.globalGrantTag("Active");
        
        // 开启全局PVP
        MCEWorldUtils.enablePVP();
        
        // 移除游戏开始平台
        CaptureCenterFuncImpl.removeStartPlatform();
        
        // 给玩家击退棒
        CaptureCenterFuncImpl.giveKnockbackStick();
        
        // 10秒后解锁得分点
        gameTask.add(MCETimerUtils.setDelayedTask(10, () -> {
            MCEMessenger.sendGlobalTitle("<gold><bold>山顶得分点已解锁！</bold></gold>", "");
            CaptureCenterFuncImpl.enableScoring(this);
        }));
        
        // 30秒后开始平台收缩
        gameTask.add(MCETimerUtils.setDelayedTask(30, () -> {
            CaptureCenterFuncImpl.startPlatformShrinking(this);
        }));
        
        // 180秒后游戏结束
        gameTask.add(MCETimerUtils.setDelayedTask(180, () -> {
            this.getTimeline().nextState();
        }));
    }
    
    @Override
    public void onEnd() {
        CaptureCenterFuncImpl.clearGameTasks(this);
        CaptureCenterFuncImpl.sendWinningMessage();
        MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);
        playerFallHandler.unregister();
        
        // 等待onEnd阶段完成后再启动投票系统（endDuration + 2秒缓冲）
        long delayTicks = (getEndDuration() + 2) * 20L; // 转换为ticks
        Bukkit.getScheduler().runTaskLater(plugin, MCEMainController::launchVotingSystem, delayTicks);
    }
    
    @Override
    public void initGameBoard() {
        setGameBoard(new CaptureCenterGameBoard(getTitle(), getWorldName()));
    }
    
    @Override
    public void stop() {
        super.stop();
        CaptureCenterFuncImpl.clearGameTasks(this);
        playerFallHandler.unregister();
    }
}