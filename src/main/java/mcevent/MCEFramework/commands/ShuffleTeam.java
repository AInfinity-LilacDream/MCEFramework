package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import mcevent.MCEFramework.miscellaneous.Constants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
ShuffleTeam: 随机分队
usage: shuffleteam
 */
@CommandAlias("shuffleteam")
@CommandPermission("shuffleteam.use")
public class ShuffleTeam extends BaseCommand {

    private static final Scoreboard teamBoard = Bukkit.getScoreboardManager().getMainScoreboard();

    @Default
    public void onShuffle() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(onlinePlayers);
        Collections.shuffle(Arrays.asList(Constants.teams));

        clearExistingTeams();

        for (int i = 0; i < 10; ++i) {
            Team team = teamBoard.registerNewTeam(Constants.teams[i].teamName());
            team.color(Constants.teams[i].teamColor());
        }

        List<Team> teams = new ArrayList<>(teamBoard.getTeams());
        int teamCount = Math.min(onlinePlayers.size(), teams.size());
        for (int i = 0; i < onlinePlayers.size(); ++i) {
            Player player = onlinePlayers.get(i);
            Team team = teams.get(i % teamCount);
            team.addEntry(player.getName());
        }
    }

    private void clearExistingTeams() {
        teamBoard.getTeams().forEach(Team::unregister);
    }
}
