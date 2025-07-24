package mcevent.MCEFramework.generalGameObject;

import lombok.Data;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;

/*
MCEGame: 游戏基类，定义通用游戏接口属性与游戏流程框架
 */
@Data
public class MCEGame {
    private String title;
    private int id;

    private ArrayList<Team> activeTeams;

    private String worldName;

    private MCETimeline timeline = new MCETimeline();

    private MCEGameBoard gameBoard;

    private int round = 0;
    private int currentRound = 0;
    private boolean isMultiGame = false;

    public MCEGame(String title, int id, String worldName, int round, boolean isMultiGame) {
        setTitle(title);
        setId(id);
        setWorldName(worldName);
        setMultiGame(isMultiGame);
        setRound(round);
    }

    public MCEGame(String title, int id, String worldName, boolean isMultiGame) {
        setTitle(title);
        setId(id);
        setWorldName(worldName);
        setMultiGame(isMultiGame);
    }

    public void init(boolean intro) {
        this.initGameBoard();
        this.setCurrentRound(1);

        this.setTimeline(new MCETimeline());
        this.getTimeline().addTimelineNode(
                new MCETimelineNode(5, false, this::onLaunch, this.getTimeline(), this.getGameBoard())
        );
        if (intro) {
            this.getTimeline().addTimelineNode(
                    new MCETimelineNode(35, false, this::intro, this.getTimeline(), this.getGameBoard())
            );
        }
        this.getTimeline().addTimelineNode(
                new MCETimelineNode(15, true, this::onPreparation, this.getTimeline(), this.getGameBoard())
        );

        for (int i = 1; i <= round; ++i) {
            this.getTimeline().addTimelineNode(
                    new MCETimelineNode(15, true, this::onCyclePreparation, this.getTimeline(), this.getGameBoard())
            );
            this.getTimeline().addTimelineNode(
                    new MCETimelineNode(70, false, this::onCycleStart, this.getTimeline(), this.getGameBoard())
            );
            if (i < round) {
                this.getTimeline().addTimelineNode(
                        new MCETimelineNode(25, true, this::onCycleEnd, this.getTimeline(), this.getGameBoard())
                );
            }
        }

        this.getTimeline().addTimelineNode(
                new MCETimelineNode(25, false, this::onEnd, this.getTimeline(), this.getGameBoard())
        );
    }

    public void start() {
        MCEMainController.setRunningGame(true);
        MCEMainController.setCurrentTimeline(this.getTimeline());
        timeline.start();
    }

    public void onLaunch() {}
    public void intro() {}
    public void onPreparation() {}
    public void onCyclePreparation() {}
    public void onCycleStart() {}
    public void onCycleEnd() {}
    public void onEnd() {}
    public void initGameBoard() {}
}
