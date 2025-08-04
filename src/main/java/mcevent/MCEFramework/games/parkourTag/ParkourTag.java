package mcevent.MCEFramework.games.parkourTag;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.parkourTag.customHandler.OpponentTeamGlowingHandler;
import mcevent.MCEFramework.games.parkourTag.customHandler.PlayerCaughtHandler;
import mcevent.MCEFramework.games.parkourTag.gameObject.ParkourTagGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Objects;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;
import static mcevent.MCEFramework.games.parkourTag.ParkourTagFuncImpl.*;

/*
ParkourTag: pkt的完整实现
 */
public class ParkourTag extends MCEGame {

    public int completeMatchesTot = 0;

    @Getter
    protected int[] completeTime = new int[MAX_TEAM_COUNT];

    @Getter
    protected ArrayList<Integer> survivePlayerTot = new ArrayList<>();

    @Getter
    protected boolean showSurvivePlayer = false;

    // 玩家被抓住事件监听器
    PlayerCaughtHandler playerCaughtHandler = new PlayerCaughtHandler();
    OpponentTeamGlowingHandler opponentTeamGlowingHandler = new OpponentTeamGlowingHandler();

    ParkourTagConfigParser parkourTagConfigParser = new ParkourTagConfigParser();

    public ParkourTag(String title, int id, String mapName, boolean isMultiGame, String configFileName,
                      int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration, int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration, cycleEndDuration, endDuration);
        MCETimerUtils.setFramedTask(opponentTeamGlowingHandler::toggleGlowing);
    }

    @Override
    public void onLaunch() {
        // 先关闭事件监听器
        playerCaughtHandler.suspend();
        setIntroTextList(parkourTagConfigParser.openAndParse(getConfigFileName()));
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameMode(GameMode.ADVENTURE);
        MCEPlayerUtils.globalHideNameTag();

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
        MCETeleporter.globalSwapWorld(this.getWorldName());

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) world.setGameRule(GameRule.FALL_DAMAGE, false);
        grantGlobalPotionEffect(saturation);
        this.setActiveTeams(MCETeamUtils.getActiveTeams());

        MCEPlayerUtils.clearGlobalTags();
    }

    @Override
    public void onCyclePreparation() {
        playerCaughtHandler.start();
        MCEWorldUtils.enablePVP();
        clearMatchCompleteState();
        getGameBoard().setStateTitle("<red><bold> 选择结束：</bold></red>");
        getGameBoard().updateRoundTitle(getCurrentRound());
        resetSurvivePlayerTot();

        MCEPlayerUtils.clearGlobalTags();
        MCEPlayerUtils.globalGrantTag("runner");
        MCEPlayerUtils.globalSetGameMode(GameMode.ADVENTURE);

        setActiveTeams(MCETeamUtils.rotateTeam(this.getActiveTeams())); // 更新本回合队伍匹配列表
        sendCurrentRoundMatchTitle();

        globalTeleportToChoiceRoom();
    }

    @Override
    public void onCycleStart() {
        opponentTeamGlowingHandler.start();
        this.getGameBoard().setStateTitle("<red><bold> 剩余时间：</bold></red>");
        resetChoiceRoom();
        showSurvivePlayer = true;

        globalTeleportToStadium();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0);
            Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0);
        }
        MCEMessenger.sendGlobalCountdown(10, "<aqua>游戏即将开始</aqua>");
        MCETimerUtils.setDelayedTask(10, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
                Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.42);
            }
            MCEMessenger.sendGlobalTitle(null, null); // clear title
        });

        this.setCurrentRound(this.getCurrentRound() + 1);
    }

    @Override
    public void onCycleEnd() {
        opponentTeamGlowingHandler.suspend();
        showSurvivePlayer = false;
        sendCurrentMatchState();
        this.getGameBoard().setStateTitle("<red><bold> 下一回合：</bold></red>");
    }

    @Override
    public void onEnd() {
        opponentTeamGlowingHandler.suspend();
        showSurvivePlayer = false;
        sendCurrentMatchState();
        this.getGameBoard().setStateTitle("<red><bold> 游戏结束：</bold></red>");
        MCEMainController.setRunningGame(false);

        // 结束游戏后停止监听器
        playerCaughtHandler.suspend();
        MCEPlayerUtils.globalShowNameTag();
    }

    @Override
    public void initGameBoard() {
        setRound(MCETeamUtils.getActiveTeamCount() - 1);
        setGameBoard(new ParkourTagGameBoard(getTitle(), getWorldName(), getRound()));
    }

    public void setTeamCompleteTime(Team team, int seconds) {
        int teamPos = getTeamId(team);
        completeTime[teamPos] = seconds;
    }

    public int getTeamCompleteTime(Team team) {
        int teamPos = getTeamId(team);
        return completeTime[teamPos];
    }

    // 获取当前回合的敌对队伍
    public Team getOpponentTeam(Team team) {
        int teamPos = getTeamId(team);
        return teamPos % 2 == 0 ? pkt.getActiveTeams().get(teamPos + 1) : pkt.getActiveTeams().get(teamPos - 1);
    }
}
