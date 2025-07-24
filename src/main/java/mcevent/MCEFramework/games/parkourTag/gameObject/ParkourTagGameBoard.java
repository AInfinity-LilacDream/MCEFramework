package mcevent.MCEFramework.games.parkourTag.gameObject;

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
ParkourTagGameBoard: pkt游戏展示板
 */
@Setter @Getter
public class ParkourTagGameBoard extends MCEGameBoard {
    private ArrayList<Integer> playerRemain;
    private String playerRemainTitle;

    public ParkourTagGameBoard(String gameName, String mapName, int round) {
        super(gameName, mapName, round);
    }

    @Override
    public void globalDisplay() {
        int seconds = pkt.getTimeline().getCounter();

        int minute = seconds / 60;
        int second = seconds % 60;
        String time = String.format("%02d:%02d", minute, second);
        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);

            int teamPos = pkt.getTeamId(MCETeamUtils.getTeam(player));
            Team opponentTeam = teamPos % 2 == 0 ? pkt.getActiveTeams().get(teamPos + 1) : pkt.getActiveTeams().get(teamPos - 1);

            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(getMapTitle()),
                    MiniMessage.miniMessage().deserialize(getRoundTitle()),
                    MiniMessage.miniMessage().deserialize(getStateTitle() + time),
                    pkt.isShowSurvivePlayer() ? MiniMessage.miniMessage().deserialize("<green><bold> 剩余玩家: </bold></green>" +
                            pkt.getSurvivePlayerTot().get(pkt.getTeamId(opponentTeam))) : null
            );
        }
    }
}
