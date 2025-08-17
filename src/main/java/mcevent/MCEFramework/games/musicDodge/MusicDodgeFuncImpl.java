package mcevent.MCEFramework.games.musicDodge;

import mcevent.MCEFramework.games.discoFever.gameObject.DiscoFeverGameBoard;
import mcevent.MCEFramework.games.musicDodge.gameObject.MusicDodgeGameBoard;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.miscellaneous.Constants.discoFever;

public class MusicDodgeFuncImpl {

    private static final MusicDodgeConfigParser musicDodgeConfigParser = musicDodge.getMusicDodgeConfigParser();

    // 从配置文件加载数据
    protected static void loadConfig() {
        musicDodge.setIntroTextList(musicDodge.getMusicDodgeConfigParser().openAndParse(musicDodge.getConfigFileName()));
    }

    // 初始化游戏展示板
    protected static void resetGameBoard() {
        MusicDodgeGameBoard gameBoard = (MusicDodgeGameBoard) musicDodge.getGameBoard();
        gameBoard.updatePlayerRemainTitle(Bukkit.getOnlinePlayers().size());
        gameBoard.setTeamRemainCount(musicDodge.getActiveTeams().size());
        for (int i = 0; i < musicDodge.getActiveTeams().size(); ++i)
            gameBoard.getTeamRemain()[i] = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            gameBoard.getTeamRemain()[musicDodge.getTeamId(team)]++;
        }
        gameBoard.updateTeamRemainTitle(null);
    }
}
