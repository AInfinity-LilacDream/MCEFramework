package mcevent.MCEFramework.games.spleef.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.games.spleef.SpleefFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
SpleefGameBoard: 冰雪掘战游戏展示板
 */
@Setter
@Getter
public class SpleefGameBoard extends MCEGameBoard {
    private int playerRemain;
    private int teamRemainCount = 0;
    private int[] teamRemain = new int[19];
    private String playerRemainTitle = "";
    private String teamRemainTitle = "";

    public SpleefGameBoard(String gameName, String mapName) {
        super(gameName, mapName);
    }

    public void updatePlayerRemainTitle(int playerRemain) {
        int alive = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipants();
        int total = 0;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equals(spleef.getWorldName()) && p.getScoreboardTags().contains("Participant"))
                total++;
        }
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
        // 实时计算基于 Participant 的存活与总数
        int survivingPlayers = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipants();
        int totalPlayers = 0;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equals(spleef.getWorldName()) && p.getScoreboardTags().contains("Participant"))
                totalPlayers++;
        }
        int survivingTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipantTeams();
        int totalTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countParticipantTeamsTotal();

        // 更新标题
        this.playerRemainTitle = "<green><bold> 剩余玩家：</bold></green>" + survivingPlayers + "/" + totalPlayers;
        this.teamRemainTitle = "<green><bold> 剩余队伍：</bold></green>" + survivingTeams + "/" + totalTeams;

        int seconds = spleef.getTimeline().getCounter();
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
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(playerRemainTitle),
                    MiniMessage.miniMessage().deserialize(teamRemainTitle));
        }
    }
}