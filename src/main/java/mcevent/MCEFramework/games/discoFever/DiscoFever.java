package mcevent.MCEFramework.games.discoFever;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.discoFever.customHandler.ActionBarMessageHandler;
import mcevent.MCEFramework.games.discoFever.customHandler.PlayerFallHandler;
import mcevent.MCEFramework.games.discoFever.gameObject.DiscoFeverGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static mcevent.MCEFramework.games.discoFever.DiscoFeverFuncImpl.*;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
DiscoFever: disco fever的完整实现
 */
@Getter @Setter
public class DiscoFever extends MCEGame {

    private PlayerFallHandler playerFallHandler = new PlayerFallHandler();
    private ActionBarMessageHandler actionBarMessageHandler = new ActionBarMessageHandler();
    private DiscoFeverConfigParser discoFeverConfigParser = new DiscoFeverConfigParser();
    private List<BukkitRunnable> bossBarTasks = new ArrayList<>();

    private List<Double> timeList = new ArrayList<>();
    private List<Material> materialList = new ArrayList<>();
    private int currentState = 0;
    private int maxState = 0;

    private Location currentPlatformLocation = discoFeverPlatformLocation;
    private BossBar bossBar = Bukkit.createBossBar(
            "平台时间",
            BarColor.GREEN,
            BarStyle.SOLID
    );

    public DiscoFever(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
                      int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration, int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration, cycleEndDuration, endDuration);
        MCETimerUtils.setFramedTask(() -> actionBarMessageHandler.showMessage());
    }

    @Override
    public void onLaunch() {
        resetPlatform();
        loadConfig();
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) world.setGameRule(GameRule.FALL_DAMAGE, false);
        setActiveTeams(MCETeamUtils.getActiveTeams());
        playerFallHandler.start();
        playerFallHandler.setInGame(false);
        MCETeleporter.globalSwapWorld(this.getWorldName());
        currentState = 0;
        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameMode(GameMode.ADVENTURE);

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
        MCETeleporter.globalSwapWorld(this.getWorldName());

        grantGlobalPotionEffect(saturation);

        MCEPlayerUtils.clearGlobalTags();
    }

    @Override
    public void onCycleStart() {
        resetGameBoard();
        this.getGameBoard().setStateTitle("<red><bold> 剩余时间：</bold></red>");

        currentPlatformLocation = discoFeverPlatformLocation;
        playerFallHandler.setInGame(true);
        actionBarMessageHandler.start();

        double time = 0;
        bossBarTasks.clear();
        Random rand = new Random();

        for (int i = 0; i < timeList.size(); ++i) {
            Double duration = timeList.get(i);
            Material material = materialList.get(rand.nextInt(materialList.size()));
            int id = i + 1;
            bossBarTasks.add(MCETimerUtils.setDelayedTask(time, () -> {
                fillPlayerInventoryWithBlock(material);
                MCETimerUtils.showGlobalDurationOnBossBar(bossBar, duration, false);
                actionBarMessageHandler.setActionBarMessage("<dark_aqua>平台：</dark_aqua><aqua>" + id +
                        "</aqua><dark_aqua>/</dark_aqua><aqua>" + maxState + "</aqua> <dark_aqua>(</dark_aqua><aqua>" +
                        String.format("%.2f", duration) + "</aqua><dark_aqua>秒)</dark_aqua>");
                generateNewPlatform(currentPlatformLocation);
            }));
            time += duration;
            if (i != timeList.size() - 1) {
                bossBarTasks.add(MCETimerUtils.setDelayedTask(time, () -> {
                    fillPlayerInventoryWithBlock(Material.AIR);
                    MCETimerUtils.showGlobalDurationOnBossBar(bossBar, 2, true);
                    updatePlatform(currentPlatformLocation, material);
                    updateCurrentPlatformLocation();
                }));
                time += 2;
            }
            else {
                bossBarTasks.add(MCETimerUtils.setDelayedTask(time, () -> this.getTimeline().nextState()));
            }
        }
    }

    @Override
    public void onEnd() {
        bossBar.removeAll();
        discoFever.clearBossBarTask();
        actionBarMessageHandler.suspend();
        playerFallHandler.suspend();
        sendWinningMessage();
        MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);
        MCEMainController.setRunningGame(false);
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new DiscoFeverGameBoard(getTitle(), getWorldName()));
    }

    public void clearBossBarTask() {
        for (BukkitRunnable bossBarTask : bossBarTasks) {
            bossBarTask.cancel();
        }
        bossBarTasks.clear();
    }

}
