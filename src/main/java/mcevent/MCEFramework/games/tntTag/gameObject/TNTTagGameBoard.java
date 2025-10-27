package mcevent.MCEFramework.games.tntTag.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
TNTTagGameBoard: TNTTag游戏展示板
*/
@Setter
@Getter
public class TNTTagGameBoard extends MCEGameBoard {
    private int alivePlayers;
    private String alivePlayersTitle = "";

    public TNTTagGameBoard(String gameName, String mapName, int round) {
        super(gameName, mapName, round);
        // 避免早期计时任务刷新时标题为null
        setStateTitle("<yellow><bold> 准备中：</bold></yellow>");
        updateAlivePlayersTitle(0);
    }

    public void updateAlivePlayersTitle(int alivePlayers) {
        this.alivePlayers = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipants();
        this.alivePlayersTitle = "<green><bold> 剩余玩家：</bold></green>" + this.alivePlayers;
    }

    @Override
    public void globalDisplay() {
        // 在游戏进行阶段不显示倒计时，只在准备阶段显示
        String stateTitle = getStateTitle() == null ? "" : getStateTitle();
        String stateDisplay;
        if (stateTitle.contains("游戏进行中")) {
            stateDisplay = stateTitle;
        } else {
            int seconds = tnttag.getTimeline().getCounter();
            int minute = seconds / 60;
            int second = seconds % 60;
            String time = String.format("%02d:%02d", minute, second);
            stateDisplay = stateTitle + time;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(getMapTitle()),
                    MiniMessage.miniMessage().deserialize(stateDisplay),
                    MiniMessage.miniMessage().deserialize(getAlivePlayersTitle()),
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize("<gray>丢锅大战 - 生存到最后！</gray>"));
        }
    }
}