package mcevent.MCEFramework.games.underworldGame.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static mcevent.MCEFramework.miscellaneous.Constants.underworldGame;

/*
UnderworldGameGameBoard: 阴间游戏展示板
 */
@Setter
@Getter
public class UnderworldGameGameBoard extends MCEGameBoard {
    
    public UnderworldGameGameBoard(String gameName, String mapName) {
        super(gameName, mapName);
    }

    @Override
    public void globalDisplay() {
        // 在游戏进行中不显示倒计时，只显示"游戏进行中"
        // 其他阶段（包括游戏结束）都显示倒计时
        String stateTitle = getStateTitle() == null ? "" : getStateTitle();
        String stateDisplay;
        if (stateTitle.contains("游戏进行中")) {
            // 游戏进行中，不显示时间
            stateDisplay = stateTitle;
        } else {
            // 其他阶段（包括游戏结束）显示倒计时
            int seconds = underworldGame.getTimeline().getCounter();
            int minute = seconds / 60;
            int second = seconds % 60;
            String time = String.format("%02d:%02d", minute, second);
            stateDisplay = stateTitle + time;
        }
        
        int aliveCount = underworldGame.getAlivePlayerCount();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(stateDisplay),
                    MiniMessage.miniMessage().deserialize("<green><bold> 存活玩家: </bold></green>" + aliveCount)
            );
        }
    }
}

