package mcevent.MCEFramework.games.musicDodge;

import mcevent.MCEFramework.games.musicDodge.gameObject.MusicDodgeGameBoard;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.miscellaneous.Constants.discoFever;

public class MusicDodgeFuncImpl {

    private static final MusicDodgeConfigParser musicDodgeConfigParser = musicDodge.getMusicDodgeConfigParser();

    // 从配置文件加载数据
    protected static void loadConfig() {
        musicDodge
                .setIntroTextList(musicDodge.getMusicDodgeConfigParser().openAndParse(musicDodge.getConfigFileName()));
    }

    // 初始化游戏展示板
    protected static void resetGameBoard() {
        MusicDodgeGameBoard gameBoard = (MusicDodgeGameBoard) musicDodge.getGameBoard();
        gameBoard.updatePlayerRemainTitle(0);
        int teamSize = musicDodge.getActiveTeams() != null ? musicDodge.getActiveTeams().size() : 0;
        gameBoard.setTeamRemainCount(
                mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipantTeams());
        for (int i = 0; i < teamSize; ++i)
            gameBoard.getTeamRemain()[i] = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getScoreboardTags().contains("Participant") || player.getGameMode() == GameMode.SPECTATOR)
                continue;
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            int idx = musicDodge.getTeamId(team);
            if (idx >= 0 && idx < gameBoard.getTeamRemain().length)
                gameBoard.getTeamRemain()[idx]++;
        }
        gameBoard.updateTeamRemainTitle(null);
    }
}
