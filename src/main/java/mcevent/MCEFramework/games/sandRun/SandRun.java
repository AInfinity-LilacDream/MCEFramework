package mcevent.MCEFramework.games.sandRun;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.games.sandRun.customHandler.SandFallHandler;
import mcevent.MCEFramework.games.sandRun.gameObject.SandRunGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
SandRun: Sand run 完整实现
 */
@Getter @Setter
public class SandRun extends MCEGame {

    private SandFallHandler sandFallHandler = new SandFallHandler();
    private List<BukkitRunnable> gameTask = new ArrayList<>();
    private SandRunConfigParser sandRunConfigParser = new SandRunConfigParser();

    public SandRun(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
                   int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration, int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration, cycleEndDuration, endDuration);
    }

    @Override
    public void onLaunch() {
        SandRunFuncImpl.loadConfig();
        
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
        MCEPlayerUtils.globalSetGameMode(GameMode.ADVENTURE);
        
        // 使用 SandRunFuncImpl 代替直接循环操作玩家
        SandRunFuncImpl.initializePlayers();

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
        grantGlobalPotionEffect(saturation);
        MCEPlayerUtils.clearGlobalTags();
    }

    @Override
    public void onCycleStart() {
        SandRunFuncImpl.resetGameBoard();
        this.getGameBoard().setStateTitle("<red><bold> 剩余时间：</bold></red>");
        
        sandFallHandler.startSandFall();
        
        gameTask.add(MCETimerUtils.setDelayedTask(180, () -> {
            this.getTimeline().nextState();
        }));
    }

    @Override
    public void onEnd() {
        SandRunFuncImpl.clearGameTasks(this);
        sandFallHandler.stopSandFall();
        SandRunFuncImpl.sendWinningMessage();
        MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new SandRunGameBoard(getTitle(), getWorldName()));
    }

    @Override
    public void stop() {
        super.stop();
        SandRunFuncImpl.clearGameTasks(this);
        sandFallHandler.stopSandFall();
    }
}