package mcevent.MCEFramework.games.sandRun;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.sandRun.customHandler.SandFallHandler;
import mcevent.MCEFramework.games.sandRun.gameObject.SandRunGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
SandRun: Sand run 完整实现
 */
@Getter
@Setter
public class SandRun extends MCEGame {

    private SandFallHandler sandFallHandler = new SandFallHandler();
    private List<BukkitRunnable> gameTask = new ArrayList<>();
    private SandRunConfigParser sandRunConfigParser = new SandRunConfigParser();

    public SandRun(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration,
                cycleEndDuration, endDuration);
    }

    @Override
    public void onLaunch() {
        SandRunFuncImpl.loadConfig();
        MCEPlayerUtils.globalClearPotionEffects();

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
        }

        // 清理世界中的混凝土粉末
        SandRunFuncImpl.clearConcretePowder();

        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SURVIVAL, 5L);

        // 使用 SandRunFuncImpl 代替直接循环操作玩家
        SandRunFuncImpl.initializePlayers();

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
        grantGlobalPotionEffect(saturation);
        MCEPlayerUtils.clearGlobalTags();
    }

    @Override
    public void onCycleStart() {
        SandRunFuncImpl.resetGameBoard();
        this.getGameBoard().setStateTitle("<yellow><bold> 游戏进行中：</bold></yellow>");

        // 播放背景音乐
        MCEPlayerUtils.globalPlaySound("minecraft:sand_run");

        MCEWorldUtils.enablePVP();

        sandFallHandler.startSandFall();

        gameTask.add(MCETimerUtils.setDelayedTask(180, () -> this.getTimeline().nextState()));
    }

    @Override
    public void onEnd() {
        SandRunFuncImpl.sendWinningMessage();
        // 不在结束阶段修改玩家游戏模式

        // 进入结束阶段：设置标题并停止音乐与所有定时任务（保留返回主城的延时）
        this.getGameBoard().setStateTitle("<red><bold> 游戏结束：</bold></red>");
        MCEPlayerUtils.globalStopMusic();
        // 清理仅与玩法相关的自建任务，避免在结束阶段被意外触发 nextState
        SandRunFuncImpl.clearGameTasks(this);
        // 停止落沙任务
        sandFallHandler.stopSandFall();

        // onEnd结束后立即清理展示板和资源，然后启动投票系统
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop(); // 停止所有游戏资源
            MCEMainController.returnToLobbyOrLaunchVoting();
        });
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new SandRunGameBoard(getTitle(), getWorldName()));
    }

    @Override
    public void stop() {
        super.stop();

        // 停止背景音乐
        MCEPlayerUtils.globalStopMusic();

        SandRunFuncImpl.clearGameTasks(this);
        sandFallHandler.stopSandFall();
    }
}