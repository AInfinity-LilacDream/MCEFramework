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

    private BukkitRunnable pktSaturationTask;

    // 玩家被抓住事件监听器
    PlayerCaughtHandler playerCaughtHandler = new PlayerCaughtHandler();
    OpponentTeamGlowingHandler opponentTeamGlowingHandler = new OpponentTeamGlowingHandler();

    @Getter
    ParkourTagConfigParser parkourTagConfigParser = new ParkourTagConfigParser();

    public ParkourTag(String title, int id, String mapName, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration,
                cycleEndDuration, endDuration);
        MCETimerUtils.setFramedTask(opponentTeamGlowingHandler::toggleGlowing);
    }

    @Override
    public void handlePlayerQuitDuringGame(org.bukkit.entity.Player player) {
        // 更新存活玩家数
        Team playerTeam = MCETeamUtils.getTeam(player);
        if (playerTeam != null) {
            int teamIndex = this.getActiveTeams().indexOf(playerTeam);
            if (teamIndex >= 0 && teamIndex < survivePlayerTot.size()) {
                survivePlayerTot.set(teamIndex, Math.max(0, survivePlayerTot.get(teamIndex) - 1));
            }
        }

        // 检查游戏结束条件
        checkGameEndCondition();
    }

    @Override
    protected void checkGameEndCondition() {
        // 检查是否有队伍的跑者全部出局
        int activeTeamCount = 0;
        for (int survivePlayers : survivePlayerTot) {
            if (survivePlayers > 0) {
                activeTeamCount++;
            }
        }

        if (activeTeamCount <= 1) {
            // 只剩一队或没队了，游戏应该结束
            // 让时间线自然过渡，不主动干预
        }
    }

    @Override
    public void onLaunch() {
        // 先关闭事件监听器
        playerCaughtHandler.suspend();
        MCEPlayerUtils.globalClearPotionEffects();
        setIntroTextList(parkourTagConfigParser.openAndParse(getConfigFileName()));
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SURVIVAL, 5L);
        MCEPlayerUtils.globalHideNameTag();

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null)
            world.setGameRule(GameRule.FALL_DAMAGE, false);

        // 设置玩家血量为10颗心（20.0血量）
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
        }

        grantGlobalPotionEffect(saturation);
        this.setActiveTeams(MCETeamUtils.getActiveTeams());

        MCEPlayerUtils.clearGlobalTags();
    }

    @Override
    public void onCyclePreparation() {
        // 选队阶段不允许抓捕与PVP，防止误判导致被切旁观
        playerCaughtHandler.suspend();
        MCEWorldUtils.disablePVP();
        clearMatchCompleteState();
        getGameBoard().setStateTitle("<red><bold> 选择结束：</bold></red>");
        getGameBoard().updateRoundTitle(getCurrentRound());
        resetSurvivePlayerTot();

        // Active/Participant 由基类管理，不在此阶段清全局标签或统一改模式
        grantGlobalPotionEffect(saturation);

        // 开始回合背景音乐
        MCEPlayerUtils.globalStopMusic();
        MCEPlayerUtils.globalPlaySound("minecraft:parkour_tag");

        setActiveTeams(MCETeamUtils.rotateTeam(this.getActiveTeams())); // 更新本回合队伍匹配列表
        sendCurrentRoundMatchTitle();

        globalTeleportToChoiceRoom(parkourTagConfigParser);
    }

    @Override
    public void onCycleStart() {
        // 正式开始小局，开启抓捕与PVP
        playerCaughtHandler.start();
        MCEWorldUtils.enablePVP();
        opponentTeamGlowingHandler.start();
        this.getGameBoard().setStateTitle("<red><bold> 剩余时间：</bold></red>");
        resetChoiceRoom(parkourTagConfigParser);
        showSurvivePlayer = true;

        // 在传送到场地前，确保每队必须有且只有一名抓捕者；若未选择则随机指定并提示双方
        ensureChasersSelectedAndNotify();

        globalTeleportToStadium(parkourTagConfigParser);
        grantGlobalPotionEffect(saturation);

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
        // 结束回合时停止背景音乐
        MCEPlayerUtils.globalStopMusic();
        sendCurrentMatchState();
        this.getGameBoard().setStateTitle("<red><bold> 下一回合：</bold></red>");
    }

    @Override
    public void onEnd() {
        sendCurrentMatchState();
        this.getGameBoard().setStateTitle("<red><bold> 游戏结束：</bold></red>");
        // 不在结束阶段修改玩家游戏模式

        // onEnd结束后立即清理展示板和资源，然后启动投票系统
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop(); // 停止所有游戏资源
            MCEMainController.returnToLobbyOrLaunchVoting();
        });
    }

    @Override
    public void initGameBoard() {
        setRound(MCETeamUtils.getActiveTeamCount() - 1);
        setGameBoard(new ParkourTagGameBoard(getTitle(), getWorldName(), getRound()));
    }

    @Override
    public void stop() {
        super.stop();
        opponentTeamGlowingHandler.suspend();
        playerCaughtHandler.suspend();
        showSurvivePlayer = false;
        if (pktSaturationTask != null) {
            pktSaturationTask.cancel();
            pktSaturationTask = null;
        }
        MCEPlayerUtils.globalStopMusic();
        MCEPlayerUtils.globalShowNameTag();
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
