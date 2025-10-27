package mcevent.MCEFramework.games.musicDodge.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCETeamUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
MusicDodgeGameBoard: music dodge游戏展示板
 */
@Setter
@Getter
public class MusicDodgeGameBoard extends MCEGameBoard {
    private int playerRemain;
    private int teamRemainCount = 0;
    private int[] teamRemain = new int[19];
    private String playerRemainTitle = "";
    private String teamRemainTitle = "";

    public MusicDodgeGameBoard(String gameName, String mapName) {
        super(gameName, mapName);
    }

    public void updatePlayerRemainTitle(int playerRemain) {
        int alive = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipants();
        int total = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countParticipantsTotal();
        this.playerRemain = alive;
        this.playerRemainTitle = "<green><bold> 剩余玩家：</bold></green>" + alive + "/" + total;
    }

    public void updateTeamRemainTitle(Team team) {

        int aliveTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipantTeams();
        int totalTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countParticipantTeamsTotal();
        this.teamRemainCount = aliveTeams;
        this.teamRemainTitle = "<green><bold> 剩余队伍：</bold></green>" + aliveTeams + "/" + totalTeams;
    }

    @Override
    public void globalDisplay() {
        int seconds = musicDodge.getTimeline().getCounter();

        int minute = seconds / 60;
        int second = seconds % 60;
        String time = String.format("%02d:%02d", minute, second);
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
