package mcevent.MCEFramework.tools;

import mcevent.MCEFramework.miscellaneous.Constants;
import mcevent.MCEFramework.miscellaneous.TeamWithDetails;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Objects;

/*
MCETeamUtils: 获取队伍信息以及操作队伍的工具类
 */
public class MCETeamUtils {

    // 获得当前队伍数量
    public static int getActiveTeamCount() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        int activeTeamsCount = 0;

        for (Team team : scoreboard.getTeams()) {
            if (!team.getEntries().isEmpty()) {
                activeTeamsCount++;
            }
        }

        return activeTeamsCount;
    }

    // 获得当前队伍列表
    public static ArrayList<Team> getActiveTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        ArrayList<Team> teams = new ArrayList<>();

        for (Team team : scoreboard.getTeams()) {
            if (!team.getEntries().isEmpty()) {
                teams.add(team);
            }
        }

        return teams;
    }

    public static ArrayList<Team> rotateTeam(ArrayList<Team> teams) {
        ArrayList<Team> newTeams = new ArrayList<>();
        newTeams.add(teams.getFirst());
        newTeams.add(teams.getLast());
        for (int i = 1; i < teams.size() - 1; i++) newTeams.add(teams.get(i));
        return newTeams;
    }

    // 获得当前队伍的玩家列表
    public static ArrayList<Player> getPlayers(Team team) {
        ArrayList<Player> players = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.getEntries().contains(player.getName())) {
                players.add(player);
            }
        }

        return players;
    }

    // 获得某个玩家所在的队伍
    public static Team getTeam(Player player) {
        for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
            if (team.getEntries().contains(player.getName())) {
                return team;
            }
        }

        return null; // never reached
    }

    public static String[] getTeamColor(Team team) {
        for (TeamWithDetails teamWithDetails : Constants.teams) {
            if (Objects.equals(teamWithDetails.teamName(), team.getName())) {
                return new String[] {
                        teamWithDetails.textColorPre(),
                        teamWithDetails.textColorPost()
                };
            }
        }
        return new String[] {"<black>", "</black>"};
    }

    public static String getUncoloredTeamName(Team team) {
        for (TeamWithDetails teamWithDetails : Constants.teams) {
            if (Objects.equals(teamWithDetails.teamName(), team.getName())) {
                return "<gray>" + teamWithDetails.teamNameNoColor() + "</gray>";
            }
        }
        return team.getName();
    }

    // 返回队伍中在线玩家的数量
    public static int getTeamOnlinePlayers(Team team) {
        int players = 0;

        for (Player player : Bukkit.getOnlinePlayers())
            if (team.getEntries().contains(player.getName()))
                players++;

        return players;
    }
}
