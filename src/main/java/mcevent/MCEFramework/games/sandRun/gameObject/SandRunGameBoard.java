package mcevent.MCEFramework.games.sandRun.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
SandRunGameBoard: sand run 游戏展示板
 */
@Setter @Getter
public class SandRunGameBoard extends MCEGameBoard {
    private int playerRemain;
    private int teamRemainCount = 0;
    private int[] teamRemain = new int[19];
    private String playerRemainTitle = "";
    private String teamRemainTitle = "";

    public SandRunGameBoard(String gameName, String mapName) {
        super(gameName, mapName);
    }

    public void updatePlayerRemainTitle(int playerRemain) {
        this.playerRemain = playerRemain;
        this.playerRemainTitle = "<green><bold> 剩余玩家：</bold></green>" +
                playerRemain + "/" + Bukkit.getOnlinePlayers().size();
    }

    public void updateTeamRemainTitle(Team team) {

        // null为初始化
        if (team == null) {
            this.teamRemainTitle = "<green><bold> 剩余队伍：</bold></green>" +
                    teamRemainCount + "/" + sandRun.getActiveTeams().size();
            return;
        }

        this.teamRemain[sandRun.getTeamId(team)]--;
        if (teamRemain[sandRun.getTeamId(team)] == 0) {
            this.teamRemainCount--;
            this.teamRemainTitle = "<green><bold> 剩余队伍：</bold></green>" +
                    teamRemainCount + "/" + sandRun.getActiveTeams().size();
        }
    }

    @Override
    public void globalDisplay() {
        int seconds = sandRun.getTimeline().getCounter();

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
                    MiniMessage.miniMessage().deserialize(getTeamRemainTitle())
            );
        }
    }
}
