package mcevent.MCEFramework.games.spleef.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.games.spleef.SpleefFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import mcevent.MCEFramework.tools.MCETeamUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
SpleefGameBoard: 冰雪掘战游戏展示板
 */
@Setter @Getter
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
        this.playerRemain = playerRemain;
        this.playerRemainTitle = "<green><bold> 剩余玩家：</bold></green>" +
                playerRemain + "/" + Bukkit.getOnlinePlayers().size();
    }

    public void updateTeamRemainTitle(Team team) {
        // null为初始化
        if (team == null) {
            this.teamRemainCount = SpleefFuncImpl.getSurvivingTeamCount();
            int totalTeams = spleef.getActiveTeams() != null ? spleef.getActiveTeams().size() : 0;
            this.teamRemainTitle = "<green><bold> 剩余队伍：</bold></green>" +
                    teamRemainCount + "/" + totalTeams;
            return;
        }

        // 这个方法不再需要复杂的逻辑，直接更新即可
        this.teamRemainCount = SpleefFuncImpl.getSurvivingTeamCount();
        int totalTeams = spleef.getActiveTeams() != null ? spleef.getActiveTeams().size() : 0;
        this.teamRemainTitle = "<green><bold> 剩余队伍：</bold></green>" +
                teamRemainCount + "/" + totalTeams;
    }

    @Override
    public void globalDisplay() {
        // 实时计算存活玩家和队伍数量
        int survivingPlayers = SpleefFuncImpl.getSurvivingPlayerCount();
        int survivingTeams = SpleefFuncImpl.getSurvivingTeamCount();
        int totalPlayers = Bukkit.getOnlinePlayers().size();
        int totalTeams = spleef.getActiveTeams() != null ? spleef.getActiveTeams().size() : 0;
        
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
                    MiniMessage.miniMessage().deserialize(teamRemainTitle)
            );
        }
    }
}