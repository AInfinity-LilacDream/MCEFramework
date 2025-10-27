package mcevent.MCEFramework.generalGameObject;

import lombok.Data;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/*
MCEGameBoard: 游戏展示板基类，定义了通用展示板接口属性
 */
@Data
public class MCEGameBoard {
    private String mainTitle = "<gradient:red:blue><bold>Lilac Games</bold></gradient>";

    private String gameTitle;
    private String mapTitle;
    private String roundTitle;
    private String stateTitle;
    private String scoreTitle;

    private int totalRound;

    public void updateRoundTitle(int currentRound) {
        this.roundTitle = "<green><bold> 回合：</bold></green>" + currentRound + "/" + totalRound;
    }

    public MCEGameBoard(String gameName, String mapName, int round) {
        this.totalRound = round;
        this.gameTitle = "<aqua><bold> 游戏：</bold></aqua>" + gameName;
        this.mapTitle = "<aqua><bold> 地图：</bold></aqua>" + mapName;
        this.roundTitle = "<green><bold> 回合：</bold></green>1/" + this.totalRound;
    }

    public MCEGameBoard(String gameName, String mapName) {
        this.gameTitle = "<aqua><bold> 游戏：</bold></aqua>" + gameName;
        this.mapTitle = "<aqua><bold> 地图：</bold></aqua>" + mapName;
    }

    public void globalDisplay() {
    }

    /**
     * 统计当前运行中游戏世界的“剩余玩家”数量：仅统计拥有 Participant 标签且非旁观的玩家。
     */
    public static int countRemainingParticipants() {
        MCEGame current = MCEMainController.getCurrentRunningGame();
        if (current == null)
            return 0;
        String worldName = current.getWorldName();
        int alive = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(worldName))
                continue;
            if (!p.getScoreboardTags().contains("Participant"))
                continue;
            if (p.getGameMode() == GameMode.SPECTATOR)
                continue;
            alive++;
        }
        return alive;
    }

    /**
     * 统计当前运行中游戏世界的“剩余队伍”数量：仅统计拥有 Participant 标签且非旁观的玩家所属队伍（去重）。
     */
    public static int countRemainingParticipantTeams() {
        MCEGame current = MCEMainController.getCurrentRunningGame();
        if (current == null)
            return 0;
        String worldName = current.getWorldName();
        java.util.Set<Team> teams = new java.util.HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(worldName))
                continue;
            if (!p.getScoreboardTags().contains("Participant"))
                continue;
            if (p.getGameMode() == GameMode.SPECTATOR)
                continue;
            Team t = MCETeamUtils.getTeam(p);
            if (t != null)
                teams.add(t);
        }
        return teams.size();
    }

    /**
     * 统计当前运行中游戏世界的“参与玩家总数”：仅统计拥有 Participant 标签（无视是否旁观）。
     */
    public static int countParticipantsTotal() {
        MCEGame current = MCEMainController.getCurrentRunningGame();
        if (current == null)
            return 0;
        String worldName = current.getWorldName();
        int total = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(worldName))
                continue;
            if (!p.getScoreboardTags().contains("Participant"))
                continue;
            total++;
        }
        return total;
    }

    /**
     * 统计当前运行中游戏世界的“参与队伍总数”：仅统计拥有 Participant 标签（无视是否旁观）的玩家所属队伍（去重）。
     */
    public static int countParticipantTeamsTotal() {
        MCEGame current = MCEMainController.getCurrentRunningGame();
        if (current == null)
            return 0;
        String worldName = current.getWorldName();
        java.util.Set<Team> teams = new java.util.HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(worldName))
                continue;
            if (!p.getScoreboardTags().contains("Participant"))
                continue;
            Team t = MCETeamUtils.getTeam(p);
            if (t != null)
                teams.add(t);
        }
        return teams.size();
    }
}
