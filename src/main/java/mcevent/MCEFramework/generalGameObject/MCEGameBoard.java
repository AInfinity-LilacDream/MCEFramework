package mcevent.MCEFramework.generalGameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Data;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/*
MCEGameBoard: 游戏展示板基类，定义了通用展示板接口属性
 */
@Data
public class MCEGameBoard {
    private String mainTitle = "<gradient:red:blue><bold>Lilac Games</bold></gradient>";

    private String gameTitle;
    private String mapTitle;
    private String roundTitle;
    private String stateTitle;
    private String scoreTitle;

    private int totalRound;

    public void updateRoundTitle(int currentRound) {
        this.roundTitle = "<green><bold> 回合：</bold></green>" + currentRound + "/" + totalRound;
    }

    public MCEGameBoard(String gameName, String mapName, int round) {
        this.totalRound = round;
        this.gameTitle = "<aqua><bold> 游戏：</bold></aqua>" + gameName;
        this.mapTitle = "<aqua><bold> 地图：</bold></aqua>" + mapName;
        this.roundTitle = "<green><bold> 回合：</bold></green>1/" + this.totalRound;
    }

    public void globalDisplay() {}
}
