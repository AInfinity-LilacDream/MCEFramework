package mcevent.MCEFramework;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.commands.*;
import mcevent.MCEFramework.games.discoFever.DiscoFever;
import mcevent.MCEFramework.games.parkourTag.ParkourTag;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCETimeline;
import mcevent.MCEFramework.miscellaneous.Constants;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
MCEMainController: 插件入口点，全局控制器
 */
public final class MCEMainController extends JavaPlugin {

    @Getter
    private static MCETimeline eventTimeline = new MCETimeline(true);
    @Getter @Setter
    private static MCETimeline currentTimeline;
    private static final ArrayList<MCEGame> gameList = new ArrayList<>();

    @Getter @Setter
    private static boolean isRunningGame = false;

    @Override
    public void onEnable() {

        // 初始化全局游戏单例
        pkt = new ParkourTag("瓮中捉鳖", 0, mapNames[0], true);
        discoFever = new DiscoFever("色盲派对", 1, "world", 1, false);

        // 初始化游戏列表
        gameList.add(Constants.pkt);
        gameList.add(Constants.discoFever);

        // ACF command manager
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new ShuffleTeam()); // shuffleteam
        commandManager.registerCommand(new Launch()); // launch
        commandManager.registerCommand(new Suspend()); // suspend
        commandManager.registerCommand(new Resume()); // resume
        commandManager.registerCommand(new SendInfo()); // sendInfo
        commandManager.registerCommand(new PKTSelectChaser()); // pktselectchaser
        commandManager.registerCommand(new Party()); // party
        
        getLogger().info("合合启动了");
    }

    @Override
    public void onDisable() {
        getLogger().info("合合卸载了");
    }

    public static void resetTimeline() {
        eventTimeline.reset();
    }

    public static void immediateLaunchGame(int gameID, boolean intro) {
        MCEGame nextGame = gameList.get(gameID);
        nextGame.init(intro); // 在开始游戏之前，先初始化游戏的时间线

        nextGame.start();
    }

    // 切换到子时间线之后会自动切换回主时间线
    public static void switchToTimeline(MCETimeline timeline, MCETimeline parentTimeline) {
        currentTimeline.suspend();

        currentTimeline = timeline;
        currentTimeline.start();
        currentTimeline = parentTimeline;
    }

    // 检测当前是否正在运行游戏
    public static boolean checkGameRunning() {
        return isRunningGame();
//        return currentTimeline != null && currentTimeline == eventTimeline;
    }
}
