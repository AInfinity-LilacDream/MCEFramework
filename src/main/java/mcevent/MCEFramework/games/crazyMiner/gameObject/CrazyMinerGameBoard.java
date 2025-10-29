package mcevent.MCEFramework.games.crazyMiner.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.GameMode;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
CrazyMinerGameBoard: CrazyMiner 游戏展示板
*/
@Setter
@Getter
public class CrazyMinerGameBoard extends MCEGameBoard {
    private int playerRemain;
    private int teamRemainCount = 0;
    private int[] teamRemain = new int[19];
    private String playerRemainTitle = "";
    private String teamRemainTitle = "";

    public CrazyMinerGameBoard(String gameName, String mapName) {
        super(gameName, mapName);
    }

    public void updatePlayerRemainTitle(int playerRemain) {
        int alive = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipants();
        int total = 0;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equals(crazyMiner.getWorldName())
                    && p.getScoreboardTags().contains("Participant"))
                total++;
        }
        this.playerRemain = alive;
        this.playerRemainTitle = "<yellow><bold> 剩余人数：</bold></yellow>" + alive + "/" + total;
    }

    public void updateTeamRemainTitle(Team team) {
        // 重新统计每支队伍的存活人数（仅统计当前CrazyMiner世界、带有Participant、且非旁观）
        java.util.List<Team> activeTeams = crazyMiner.getActiveTeams();
        if (activeTeams == null)
            activeTeams = java.util.Collections.emptyList();

        // 清零当前计数（按activeTeams大小）
        int size = Math.min(this.teamRemain.length, activeTeams.size());
        for (int i = 0; i < size; i++) {
            this.teamRemain[i] = 0;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(crazyMiner.getWorldName()))
                continue;
            if (!p.getScoreboardTags().contains("Participant"))
                continue;
            if (p.getGameMode() == GameMode.SPECTATOR)
                continue;

            Team pt = MCETeamUtils.getTeam(p);
            if (pt == null)
                continue;
            // 映射到activeTeams索引
            int idx = -1;
            for (int i = 0; i < size; i++) {
                Team at = activeTeams.get(i);
                if (at != null && at.getName() != null && pt.getName() != null && at.getName().equals(pt.getName())) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0 && idx < size)
                this.teamRemain[idx]++;
        }

        // 计算存活队伍数与总队伍数
        int aliveTeams = 0;
        for (int i = 0; i < size; i++) {
            if (this.teamRemain[i] > 0)
                aliveTeams++;
        }
        int totalTeams = activeTeams.size();

        this.teamRemainCount = aliveTeams;
        this.teamRemainTitle = "<yellow><bold> 剩余队伍：</bold></yellow>" + aliveTeams + "/" + totalTeams;
    }

    @Override
    public void globalDisplay() {
        int seconds = crazyMiner.getTimeline().getCounter();

        int minute = seconds / 60;
        int second = seconds % 60;
        String time = String.format("%02d:%02d", minute, second);

        // Calculate world border size if available
        String borderInfo = "";
        try {
            if (Bukkit.getWorld(crazyMiner.getWorldName()) != null) {
                double borderSize = Bukkit.getWorld(crazyMiner.getWorldName()).getWorldBorder().getSize();
                borderInfo = "<gold><bold> 边界大小：</bold></gold>" + String.format("%.0f", borderSize);
            }
        } catch (Exception e) {
            borderInfo = "<gold><bold> 边界大小：</bold></gold>未知";
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(getMapTitle()),
                    MiniMessage.miniMessage().deserialize(getStateTitle() + time),
                    MiniMessage.miniMessage().deserialize(getPlayerRemainTitle()),
                    MiniMessage.miniMessage().deserialize(getTeamRemainTitle()),
                    MiniMessage.miniMessage().deserialize(borderInfo),
                    MiniMessage.miniMessage().deserialize(" "));
        }
    }
}