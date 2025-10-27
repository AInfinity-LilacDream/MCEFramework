package mcevent.MCEFramework.games.discoFever.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
// imports retained as needed elsewhere
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

// no local collections used here

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
DiscoFeverGameBoard: disco fever游戏展示板
 */
@Setter
@Getter
public class DiscoFeverGameBoard extends MCEGameBoard {
    private int playerRemain;
    private int teamRemainCount = 0;
    private int[] teamRemain = new int[19];
    private String playerRemainTitle = "";
    private String teamRemainTitle = "";

    public DiscoFeverGameBoard(String gameName, String mapName) {
        super(gameName, mapName);
    }

    public void updatePlayerRemainTitle(int playerRemain) {
        int alive = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipants();
        int total = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countParticipantsTotal();
        this.playerRemain = alive;
        this.playerRemainTitle = "<green><bold> 剩余玩家：</bold></green>" + alive + "/" + total;
    }

    public void updateTeamRemainTitle(Team team) {
        // 无论是否传入 team，都改为基于 Participant 实时统计
        int aliveTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipantTeams();
        int totalTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countParticipantTeamsTotal();
        this.teamRemainCount = aliveTeams;
        this.teamRemainTitle = "<green><bold> 剩余队伍：</bold></green>" +
                aliveTeams + "/" + totalTeams;
    }

    @Override
    public void globalDisplay() {
        int seconds = discoFever.getTimeline().getCounter();

        int minute = seconds / 60;
        int second = seconds % 60;
        String time = String.format("%02d:%02d", minute, second);

        // 在渲染前基于 Participant 重新统计剩余玩家/队伍
        String worldName = discoFever.getWorldName();
        int totalParticipants = 0;
        int aliveParticipants = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(worldName))
                continue;
            if (!p.getScoreboardTags().contains("Participant"))
                continue;
            totalParticipants++;
            if (p.getGameMode() != org.bukkit.GameMode.SPECTATOR)
                aliveParticipants++;
        }
        this.playerRemain = aliveParticipants;
        this.playerRemainTitle = "<green><bold> 剩余玩家：</bold></green>" + aliveParticipants + "/" + totalParticipants;

        int aliveTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipantTeams();
        int totalTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countParticipantTeamsTotal();
        this.teamRemainCount = aliveTeams;
        this.teamRemainTitle = "<green><bold> 剩余队伍：</bold></green>" + aliveTeams + "/" + totalTeams;

        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(getMapTitle()),
                    MiniMessage.miniMessage().deserialize(getStateTitle() + time),
                    MiniMessage.miniMessage().deserialize(getPlayerRemainTitle()),
                    MiniMessage.miniMessage().deserialize(getTeamRemainTitle()));
        }
    }
}
