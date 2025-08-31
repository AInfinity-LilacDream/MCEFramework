package mcevent.MCEFramework;

import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.commands.*;
import mcevent.MCEFramework.customHandler.GlobalPVPHandler;
import mcevent.MCEFramework.customHandler.PlayerJoinHandler;
import mcevent.MCEFramework.games.captureCenter.CaptureCenter;
import mcevent.MCEFramework.games.discoFever.DiscoFever;
import mcevent.MCEFramework.games.football.Football;
import mcevent.MCEFramework.games.musicDodge.MusicDodge;
import mcevent.MCEFramework.games.parkourTag.ParkourTag;
import mcevent.MCEFramework.games.sandRun.SandRun;
import mcevent.MCEFramework.games.votingSystem.VotingSystem;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCETimeline;
import mcevent.MCEFramework.miscellaneous.Constants;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
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
    
    @Getter
    private static PlayerJoinHandler playerJoinHandler;

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
        String votingMapName = mapNames[6]; // 投票系统直接使用lobby
        
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
        votingSystem = new VotingSystem("投票系统", 6, votingMapName, 1, false, "MCEConfig/VotingSystem.cfg",
                2, 0, 0, 0, 30, 0, 3);

        // 初始化游戏列表
        gameList.add(pkt);
        gameList.add(discoFever);
        gameList.add(musicDodge);
        gameList.add(sandRun);
        gameList.add(captureCenter);
        gameList.add(football);
        gameList.add(votingSystem);

        // 全面清理所有玩家状态（在线和离线）
        cleanupAllPlayersOnStartup();
        
        // 注册全局事件监听器
        globalPVPHandler = new GlobalPVPHandler();
        playerJoinHandler = new PlayerJoinHandler();

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
    
    public static void launchVotingSystem() {
        // 启动投票系统（游戏ID 6）
        immediateLaunchGame(6, false);
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
    
    /**
     * 插件启动时全面清理所有玩家状态
     * 清空效果、标签、物品栏
     */
    private void cleanupAllPlayersOnStartup() {
        getLogger().info("开始清理所有玩家状态...");
        
        // 清理在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanupPlayer(player);
        }
        
        // 清理离线玩家 - 获取所有曾经加入过服务器的玩家
        OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
        int cleanedCount = 0;
        
        for (OfflinePlayer offlinePlayer : offlinePlayers) {
            if (offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                // 对于离线玩家，我们主要清理可持久化的数据
                Player player = offlinePlayer.getPlayer();
                if (player != null) {
                    cleanupPlayer(player);
                    cleanedCount++;
                }
            }
        }
        
        getLogger().info("清理完成！在线玩家: " + Bukkit.getOnlinePlayers().size() + 
                        ", 离线玩家: " + cleanedCount);
    }
    
    /**
     * 清理单个玩家的状态
     */
    private void cleanupPlayer(Player player) {
        // 清空药水效果
        player.getActivePotionEffects().forEach(effect -> 
            player.removePotionEffect(effect.getType())
        );
        
        // 清空scoreboard标签
        player.getScoreboardTags().clear();
        
        // 清空物品栏
        player.getInventory().clear();
        player.getEnderChest().clear();
        
        // 重置经验
        player.setExp(0);
        player.setLevel(0);
        
        // 重置食物和生命值
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setHealth(player.getMaxHealth());
    }
}
