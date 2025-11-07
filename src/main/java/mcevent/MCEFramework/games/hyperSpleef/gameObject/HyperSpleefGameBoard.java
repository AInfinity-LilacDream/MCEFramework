package mcevent.MCEFramework.games.hyperSpleef.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.games.hyperSpleef.EventInfo;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
HyperSpleefGameBoard: 超级掘一死战游戏展示板
 */
@Setter
@Getter
public class HyperSpleefGameBoard extends MCEGameBoard {
    private int playerRemain;
    private int teamRemainCount = 0;
    private int[] teamRemain = new int[19];
    private String playerRemainTitle = "";
    private String teamRemainTitle = "";

    public HyperSpleefGameBoard(String gameName, String mapName) {
        super(gameName, mapName, 2); // 冰雪乱斗有2回合
    }

    public void updatePlayerRemainTitle(int playerRemain) {
        int alive = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipants();
        int total = 0;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if (hyperSpleef != null && p.getWorld().getName().equals(hyperSpleef.getWorldName())
                    && p.getScoreboardTags().contains("Participant"))
                total++;
        }
        this.playerRemain = alive;
        this.playerRemainTitle = "<green><bold> 剩余玩家：</bold></green>" + alive + "/" + total;
    }

    public void updateTeamRemainTitle(org.bukkit.scoreboard.Team team) {
        int aliveTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipantTeams();
        int totalTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countParticipantTeamsTotal();
        this.teamRemainCount = aliveTeams;
        this.teamRemainTitle = "<green><bold> 剩余队伍：</bold></green>" + aliveTeams + "/" + totalTeams;
    }

    @Override
    public void globalDisplay() {
        // 实时计算基于 Participant 的存活与总数
        int survivingPlayers = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipants();
        int totalPlayers = 0;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if (hyperSpleef != null && p.getWorld().getName().equals(hyperSpleef.getWorldName())
                    && p.getScoreboardTags().contains("Participant"))
                totalPlayers++;
        }
        int survivingTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipantTeams();
        int totalTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countParticipantTeamsTotal();

        // 更新标题
        this.playerRemainTitle = "<green><bold> 剩余玩家：</bold></green>" + survivingPlayers + "/" + totalPlayers;
        this.teamRemainTitle = "<green><bold> 剩余队伍：</bold></green>" + survivingTeams + "/" + totalTeams;

        // 获取当前时间和下一个事件信息
        // getCounter() 返回的是剩余时间（从最大值递减），需要转换为已过时间
        int elapsedTime = 0;
        if (hyperSpleef != null && hyperSpleef.getTimeline() != null) {
            elapsedTime = hyperSpleef.getTimeline().getCurrentTimelineNodeDuration();
        }

        // 获取状态标题
        String stateTitle = getStateTitle() != null ? getStateTitle() : "";

        // 获取下一个事件信息（仅在游戏进行中显示）
        String eventTitle = stateTitle;
        String timeDisplay = "";

        // 只在游戏进行中状态（状态标题包含"游戏进行中"）显示下一事件
        if (hyperSpleef != null && stateTitle.contains("游戏进行中")) {
            EventInfo nextEvent = hyperSpleef.getNextEvent();
            if (nextEvent != null) {
                // 计算距离下一个事件的倒计时（事件时间是已过时间）
                int timeUntilEvent = nextEvent.timeSeconds - elapsedTime;
                if (timeUntilEvent > 0) {
                    // 根据事件类型显示不同的格式
                    if ("随机事件".equals(nextEvent.eventName)) {
                        // 随机事件显示为"随机事件：30s"格式
                        eventTitle = "<green><bold> 随机事件：</bold></green>";
                        timeDisplay = timeUntilEvent + "s";
                    } else if ("地图变动".equals(nextEvent.eventName)) {
                        // 地图变动显示为"地图变动：XXs"格式
                        eventTitle = "<green><bold> 地图变动：</bold></green>";
                        timeDisplay = timeUntilEvent + "s";
                    } else {
                        // 其他事件（如游戏结束）显示为"事件名：XXs"格式
                        eventTitle = "<green><bold> " + nextEvent.eventName + "：</bold></green>";
                        timeDisplay = timeUntilEvent + "s";
                    }
                } else {
                    // 所有事件都已触发，显示距离回合结束的时间
                    int gameEndTime = 240; // 4分钟
                    int timeUntilEnd = gameEndTime - elapsedTime;
                    if (timeUntilEnd > 0) {
                        eventTitle = "<green><bold> 回合结束倒计时：</bold></green>";
                        timeDisplay = timeUntilEnd + "s";
                    } else {
                        eventTitle = stateTitle;
                        int minute = elapsedTime / 60;
                        int second = elapsedTime % 60;
                        timeDisplay = String.format(" %02d:%02d", minute, second);
                    }
                }
            }
        } else {
            // 非游戏进行中状态，显示状态标题和剩余时间
            int remainingTime = 0;
            if (hyperSpleef != null && hyperSpleef.getTimeline() != null) {
                remainingTime = hyperSpleef.getTimeline().getCounter();
            }
            int minute = remainingTime / 60;
            int second = remainingTime % 60;
            timeDisplay = String.format(" %02d:%02d", minute, second);
            eventTitle = stateTitle;
        }

        // 地图名称使用MiniMessage格式：<aqua><bold> 地图：</bold></aqua><red>Cake</red> by
        // <blue>PRAD</blue>
        String mapTitle = "<aqua><bold> 地图：</bold></aqua><red>Cake</red> by <blue>PRAD</blue>";

        // 获取回合标题
        String roundTitleText = getRoundTitle() != null ? getRoundTitle() : "<green><bold> 回合：</bold></green>1/2";

        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(mapTitle),
                    MiniMessage.miniMessage().deserialize(roundTitleText),
                    MiniMessage.miniMessage().deserialize(eventTitle + timeDisplay),
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(playerRemainTitle),
                    MiniMessage.miniMessage().deserialize(teamRemainTitle));
        }
    }
}
