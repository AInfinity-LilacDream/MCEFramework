package mcevent.MCEFramework.generalGameObject;

import lombok.Data;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static mcevent.MCEFramework.miscellaneous.Constants.discoFever;

/*
MCEGame: 游戏基类，定义通用游戏接口属性与游戏流程框架
 */
@Data
public class MCEGame {
    private ArrayList<Component> introTextList = new ArrayList<>();

    private String title;
    private int id;

    private ArrayList<Team> activeTeams;

    private String worldName;

    private MCETimeline timeline = new MCETimeline();

    private MCEGameBoard gameBoard;

    private String configFileName;

    private int round = 0;
    private int currentRound = 0;
    private boolean isMultiGame = false;

    private int launchDuration;
    private int introDuration;
    private int preparationDuration;
    private int cyclePreparationDuration;
    private int cycleStartDuration;
    private int cycleEndDuration;
    private int endDuration;

    public MCEGame(String title, int id, String worldName, int round, boolean isMultiGame, String configFileName,
                   int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration, int cycleStartDuration, int cycleEndDuration, int endDuration) {
        setTitle(title);
        setId(id);
        setWorldName(worldName);
        setMultiGame(isMultiGame);
        setRound(round);
        setConfigFileName(configFileName);
        setLaunchDuration(launchDuration);
        setIntroDuration(introDuration);
        setPreparationDuration(preparationDuration);
        setCyclePreparationDuration(cyclePreparationDuration);
        setCycleStartDuration(cycleStartDuration);
        setCycleEndDuration(cycleEndDuration);
        setEndDuration(endDuration);
    }

    public MCEGame(String title, int id, String worldName, boolean isMultiGame, String configFileName
    , int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration, int cycleStartDuration, int cycleEndDuration, int endDuration) {
        setTitle(title);
        setId(id);
        setWorldName(worldName);
        setMultiGame(isMultiGame);
        setConfigFileName(configFileName);
        setLaunchDuration(launchDuration);
        setIntroDuration(introDuration);
        setPreparationDuration(preparationDuration);
        setCyclePreparationDuration(cyclePreparationDuration);
        setCycleStartDuration(cycleStartDuration);
        setCycleEndDuration(cycleEndDuration);
        setEndDuration(endDuration);
    }

    public void init(boolean intro) {
        this.initGameBoard();
        this.setCurrentRound(1);


        this.setTimeline(new MCETimeline());
        this.getTimeline().addTimelineNode(
                new MCETimelineNode(launchDuration, false, this::onLaunch, this.getTimeline(), this.getGameBoard())
        );
        if (intro) {
            this.getTimeline().addTimelineNode(
                    new MCETimelineNode(introDuration, false, this::intro, this.getTimeline(), this.getGameBoard())
            );
        }
        this.getTimeline().addTimelineNode(
                new MCETimelineNode(preparationDuration, true, this::onPreparation, this.getTimeline(), this.getGameBoard())
        );

        for (int i = 1; i <= round; ++i) {
            this.getTimeline().addTimelineNode(
                    new MCETimelineNode(cyclePreparationDuration, true, this::onCyclePreparation, this.getTimeline(), this.getGameBoard())
            );
            this.getTimeline().addTimelineNode(
                    new MCETimelineNode(cycleStartDuration, false, this::onCycleStart, this.getTimeline(), this.getGameBoard())
            );
            if (i < round) {
                this.getTimeline().addTimelineNode(
                        new MCETimelineNode(cycleEndDuration, true, this::onCycleEnd, this.getTimeline(), this.getGameBoard())
                );
            }
        }

        this.getTimeline().addTimelineNode(
                new MCETimelineNode(endDuration, false, this::onEnd, this.getTimeline(), this.getGameBoard())
        );
    }

    public int getTeamId(Team team) {
        return activeTeams.indexOf(team);
    }

    public void start() {
        MCEMainController.setRunningGame(true);
        MCEMainController.setCurrentTimeline(this.getTimeline());
        
        timeline.start();
    }

    public void stop() {
        MCEMainController.setRunningGame(false);
        
        // 清空所有玩家的scoreboard tags
        MCEPlayerUtils.clearGlobalTags();
        
        if (timeline != null) {
            timeline.suspend();
        }
    }

    public void onLaunch() {}

    public void intro() {
        this.getGameBoard().setStateTitle("<red><bold> 游戏介绍：</bold></red>");
        MCEMessenger.sendIntroText(getTitle(), getIntroTextList());
    }

    public void onPreparation() { 
        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
        
        // 给所有在线玩家添加Active标签，标记为活跃游戏玩家
        MCEPlayerUtils.globalGrantTag("Active");
    }
    public void onCyclePreparation() {
        // 确保Active标签存在（如果子类没有覆盖此方法）
        MCEPlayerUtils.globalGrantTag("Active");
    }
    public void onCycleStart() {}
    public void onCycleEnd() {}
    public void onEnd() {}
    public void initGameBoard() {}
}
