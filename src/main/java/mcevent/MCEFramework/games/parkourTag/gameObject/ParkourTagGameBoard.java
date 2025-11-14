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
            Team playerTeam = MCETeamUtils.getTeam(player);
            Team opponentTeam = pkt.getOpponentTeam(playerTeam);

            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            
            // 构建剩余玩家显示行（仅在显示时）
            net.kyori.adventure.text.Component survivePlayerLine = null;
            if (pkt.isShowSurvivePlayer() && playerTeam != null) {
                boolean isChaser = player.getScoreboardTags().contains("chaser");
                boolean isRunner = player.getScoreboardTags().contains("runner");
                
                int teamId = -1;
                if (isChaser && opponentTeam != null) {
                    // 抓捕者显示对手队伍的剩余玩家数
                    teamId = pkt.getTeamId(opponentTeam);
                } else if (isRunner) {
                    // 逃脱者显示自己队伍的剩余玩家数
                    teamId = pkt.getTeamId(playerTeam);
                }
                
                if (teamId >= 0 && teamId < pkt.getSurvivePlayerTot().size()) {
                    survivePlayerLine = MiniMessage.miniMessage().deserialize("<green><bold> 剩余玩家: </bold></green>" +
                            pkt.getSurvivePlayerTot().get(teamId));
                }
            }
            
            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(getMapTitle()),
                    MiniMessage.miniMessage().deserialize(getRoundTitle()),
                    MiniMessage.miniMessage().deserialize(getStateTitle() + time),
                    survivePlayerLine
            );
        }
    }
}
