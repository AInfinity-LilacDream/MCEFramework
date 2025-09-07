package mcevent.MCEFramework.games.extractOwn.gameObject;

import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
ExtractOwnGameBoard: 暗矢狂潮游戏展示板
*/
public class ExtractOwnGameBoard extends MCEGameBoard {
    
    public ExtractOwnGameBoard(String gameName, String mapName, int round) {
        super(gameName, mapName, round);
    }
    
    @Override
    public void globalDisplay() {
        if (extractOwn == null) return;
        
        // 获取剩余时间并格式化
        int seconds = extractOwn.getTimeline().getCounter();
        int minute = seconds / 60;
        int second = seconds % 60;
        String time = String.format("%02d:%02d", minute, second);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboard() == null) continue;
            
            fr.mrmicky.fastboard.adventure.FastBoard board = new fr.mrmicky.fastboard.adventure.FastBoard(player);
            
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            
            board.updateLines(
                MiniMessage.miniMessage().deserialize(getGameTitle() != null ? getGameTitle() : ""),
                MiniMessage.miniMessage().deserialize(getMapTitle() != null ? getMapTitle() : ""),
                MiniMessage.miniMessage().deserialize(getRoundTitle() != null ? getRoundTitle() : ""),
                MiniMessage.miniMessage().deserialize(""),
                MiniMessage.miniMessage().deserialize(getStateTitle() + time)
            );
        }
    }
}