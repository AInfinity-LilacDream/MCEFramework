package mcevent.MCEFramework;

import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.commands.*;
import mcevent.MCEFramework.customHandler.GlobalPVPHandler;
import mcevent.MCEFramework.games.captureCenter.CaptureCenter;
import mcevent.MCEFramework.games.discoFever.DiscoFever;
import mcevent.MCEFramework.games.football.Football;
import mcevent.MCEFramework.games.musicDodge.MusicDodge;
import mcevent.MCEFramework.games.parkourTag.ParkourTag;
import mcevent.MCEFramework.games.sandRun.SandRun;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCETimeline;
import mcevent.MCEFramework.miscellaneous.Constants;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.List;
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

    @Getter @Setter
    private static MCEGame currentRunningGame = null;

    @Getter
    private static GlobalPVPHandler globalPVPHandler;

    @Override
    public void onEnable() {

        // 初始化全局游戏单例 - 只读取地图名称，完整配置将在onLaunch时读取
        // 先读取配置文件获取地图名称
        String pktMapName = readMapNameFromConfig("MCEConfig/ParkourTag.cfg", mapNames[0]);
        String dfMapName = readMapNameFromConfig("MCEConfig/DiscoFever.cfg", mapNames[1]); 
        String mdMapName = readMapNameFromConfig("MCEConfig/MusicDodge.cfg", mapNames[2]);
        String srMapName = readMapNameFromConfig("MCEConfig/SandRun.cfg", mapNames[3]);
        String ccMapName = readMapNameFromConfig("MCEConfig/CaptureCenter.cfg", mapNames[4]);
        String footballMapName = readMapNameFromConfig("MCEConfig/Football.cfg", mapNames[5]);
        
        // 使用配置文件中的地图名称创建游戏实例
        pkt = new ParkourTag("瓮中捉鳖", 0, pktMapName, true, "MCEConfig/ParkourTag.cfg",
                5, 35, 15, 15, 70, 25, 25);
        discoFever = new DiscoFever("色盲狂热", 1, dfMapName, 1, false, "MCEConfig/DiscoFever.cfg",
                5, 55, 15, 0, 215, 25, 25);
        musicDodge = new MusicDodge("跃动音律", 2, mdMapName, 1, false, "MCEConfig/MusicDodge.cfg",
                5, 55, 15, 0, 215, 25, 25);
        sandRun = new SandRun("落沙漫步", 3, srMapName, 1, false, "MCEConfig/SandRun.cfg",
                5, 55, 15, 0, 180, 25, 25);
        captureCenter = new CaptureCenter("占山为王", 4, ccMapName, 1, false, "MCEConfig/CaptureCenter.cfg",
                5, 55, 15, 0, 180, 25, 25);
        football = new Football("少林足球", 5, footballMapName, 1, false, "MCEConfig/Football.cfg",
                3, 25, 10, 5, Integer.MAX_VALUE, 15, 20);

        // 初始化游戏列表
        gameList.add(pkt);
        gameList.add(discoFever);
        gameList.add(musicDodge);
        gameList.add(sandRun);
        gameList.add(captureCenter);
        gameList.add(football);

        // 注册全局事件监听器
        globalPVPHandler = new GlobalPVPHandler();

        // ACF command manager
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new ShuffleTeam()); // shuffleteam
        commandManager.registerCommand(new DivideTeams()); // divideteams
        commandManager.registerCommand(new Launch()); // launch
        commandManager.registerCommand(new Stop()); // stop
        commandManager.registerCommand(new Suspend()); // suspend
        commandManager.registerCommand(new Resume()); // resume
        commandManager.registerCommand(new SendInfo()); // sendInfo
        commandManager.registerCommand(new PKTSelectChaser()); // pktselectchaser
        commandManager.registerCommand(new Party()); // party
        commandManager.registerCommand(new TogglePVP()); // togglepvp
        
        getLogger().info("合合启动了");
    }

    /**
     * 从配置文件中快速读取地图名称，不进行完整配置解析
     */
    private String readMapNameFromConfig(String configFileName, String defaultMapName) {
        try {
            Path configPath = plugin.getDataPath().resolve(configFileName);
            
            // 如果配置文件不存在，使用默认值
            if (!java.nio.file.Files.exists(configPath)) {
                return defaultMapName;
            }
            
            List<String> lines = java.nio.file.Files.readAllLines(configPath);
            boolean inMapNameSection = false;
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                if (trimmedLine.equals("[map_name]")) {
                    inMapNameSection = true;
                    continue;
                }
                
                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                    inMapNameSection = false;
                    continue;
                }
                
                if (inMapNameSection && !trimmedLine.isEmpty()) {
                    return trimmedLine;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("读取地图名称失败: " + configFileName + ", 使用默认值: " + defaultMapName);
        }
        
        return defaultMapName;
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
        setCurrentRunningGame(nextGame);
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

    // 停止当前运行的游戏
    public static boolean stopCurrentGame() {
        if (currentRunningGame != null && isRunningGame()) {
            currentRunningGame.stop();
            setRunningGame(false);
            setCurrentRunningGame(null);
            return true;
        }
        return false;
    }
}
