package mcevent.MCEFramework;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.commands.*;
import mcevent.MCEFramework.customHandler.ChatFormatHandler;
import mcevent.MCEFramework.customHandler.FriendlyFireHandler;
import mcevent.MCEFramework.customHandler.GlobalPVPHandler;
import mcevent.MCEFramework.customHandler.LobbyBounceHandler;
import mcevent.MCEFramework.customHandler.LobbyHandler;
import mcevent.MCEFramework.customHandler.PlayerJoinHandler;
import mcevent.MCEFramework.customHandler.GamePlayerQuitHandler;
import mcevent.MCEFramework.customHandler.WelcomeMessageHandler;
import mcevent.MCEFramework.games.captureCenter.CaptureCenter;
import mcevent.MCEFramework.games.crazyMiner.CrazyMiner;
import mcevent.MCEFramework.games.discoFever.DiscoFever;
import mcevent.MCEFramework.games.extractOwn.ExtractOwn;
import mcevent.MCEFramework.games.football.Football;
import mcevent.MCEFramework.games.musicDodge.MusicDodge;
import mcevent.MCEFramework.games.parkourTag.ParkourTag;
import mcevent.MCEFramework.games.sandRun.SandRun;
import mcevent.MCEFramework.games.spleef.Spleef;
import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.games.tntTag.TNTTag;
import mcevent.MCEFramework.games.votingSystem.VotingSystem;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCETimeline;
import mcevent.MCEFramework.tools.MCEGlowingEffectManager;
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
    @Getter
    @Setter
    private static MCETimeline currentTimeline;
    private static final ArrayList<MCEGame> gameList = new ArrayList<>();

    @Getter
    @Setter
    private static boolean isRunningGame = false;

    @Getter
    @Setter
    private static MCEGame currentRunningGame = null;

    @Getter
    private static GlobalPVPHandler globalPVPHandler;

    @Getter
    private static FriendlyFireHandler friendlyFireHandler;

    @Getter
    private static PlayerJoinHandler playerJoinHandler;

    @Getter
    private static GamePlayerQuitHandler gamePlayerQuitHandler;

    @Getter
    private static LobbyBounceHandler lobbyBounceHandler;

    @Getter
    private static LobbyHandler lobbyHandler;

    @Getter
    private static ChatFormatHandler chatFormatHandler;

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
        String crazyMinerMapName = readMapNameFromConfig("MCEConfig/CrazyMiner.cfg", mapNames[6]);
        String extractOwnMapName = readMapNameFromConfig("MCEConfig/ExtractOwn.cfg", mapNames[7]);
        String tntTagMapName = readMapNameFromConfig("MCEConfig/TNTTag.cfg", mapNames[8]);
        String spleefMapName = readMapNameFromConfig("MCEConfig/Spleef.cfg", mapNames[9]);
        String survivalGameMapName = readMapNameFromConfig("MCEConfig/SurvivalGame.cfg", mapNames[10]);
        String votingMapName = mapNames[11]; // 投票系统直接使用lobby

        // 使用配置文件中的地图名称创建游戏实例
        pkt = new ParkourTag("瓮中捉鳖", PARKOUR_TAG_ID, pktMapName, true, "MCEConfig/ParkourTag.cfg",
                5, 35, 15, 15, 70, 25, 25);
        discoFever = new DiscoFever("色盲狂热", DISCO_FEVER_ID, dfMapName, 1, false, "MCEConfig/DiscoFever.cfg",
                5, 55, 15, 0, 215, 25, 25);
        musicDodge = new MusicDodge("跃动音律", MUSIC_DODGE_ID, mdMapName, 1, false, "MCEConfig/MusicDodge.cfg",
                5, 55, 15, 0, 215, 25, 25);
        sandRun = new SandRun("落沙漫步", SAND_RUN_ID, srMapName, 1, false, "MCEConfig/SandRun.cfg",
                5, 55, 15, 0, 180, 25, 25);
        captureCenter = new CaptureCenter("占山为王", CAPTURE_CENTER_ID, ccMapName, 1, false, "MCEConfig/CaptureCenter.cfg",
                5, 55, 15, 0, 180, 25, 25);
        football = new Football("少林足球", FOOTBALL_ID, footballMapName, 5, true, "MCEConfig/Football.cfg",
                3, 25, 10, 5, Integer.MAX_VALUE, 1, 20);
        crazyMiner = new CrazyMiner("惊天矿工团", CRAZY_MINER_ID, crazyMinerMapName, 1, false, "MCEConfig/CrazyMiner.cfg",
                5, 55, 15, 0, 1080, 25, 25);
        extractOwn = new ExtractOwn("暗矢狂潮", EXTRACT_OWN_ID, extractOwnMapName, 3, true, "MCEConfig/ExtractOwn.cfg",
                5, 55, 15, 5, 360, 15, 25);
        tnttag = new TNTTag("丢锅大战", TNT_TAG_ID, tntTagMapName, false, "MCEConfig/TNTTag.cfg",
                5, 60, 15, 0, Integer.MAX_VALUE, 5, 25);
        spleef = new Spleef("冰雪掘战", SPLEEF_ID, spleefMapName, 3, true, "MCEConfig/Spleef.cfg",
                5, 55, 15, 5, 180, 15, 25);
        survivalGame = new SurvivalGame("饥饿游戏", SURVIVAL_GAME_ID, survivalGameMapName, 2, true,
                "MCEConfig/SurvivalGame.cfg",
                5, 55, 15, 15, 450, 25, 25);
        votingSystem = new VotingSystem("投票系统", VOTING_SYSTEM_ID, votingMapName, 1, false, "MCEConfig/VotingSystem.cfg",
                2, 0, 0, 0, 30, 0, 3);

        // 初始化游戏列表
        gameList.add(pkt);
        gameList.add(discoFever);
        gameList.add(musicDodge);
        gameList.add(sandRun);
        gameList.add(captureCenter);
        gameList.add(football);
        gameList.add(crazyMiner);
        gameList.add(extractOwn);
        gameList.add(tnttag);
        gameList.add(spleef);
        gameList.add(survivalGame);
        gameList.add(votingSystem);

        // 全面清理所有玩家状态（在线和离线）
        cleanupAllPlayersOnStartup();

        // 注册全局事件监听器
        globalPVPHandler = new GlobalPVPHandler();
        friendlyFireHandler = new FriendlyFireHandler();
        playerJoinHandler = new PlayerJoinHandler();
        gamePlayerQuitHandler = new GamePlayerQuitHandler();
        lobbyBounceHandler = new LobbyBounceHandler();
        lobbyHandler = new LobbyHandler();
        chatFormatHandler = new ChatFormatHandler();
        new mcevent.MCEFramework.customHandler.GlobalEliminationHandler(); // 全局淘汰监听器
        new WelcomeMessageHandler(); // 欢迎标语处理器
        Bukkit.getPluginManager()
                .registerEvents(new mcevent.MCEFramework.games.survivalGame.customHandler.ChestSelectorHandler(), this); // 箱子标注器

        // 确保 survival_game_loot_table 资源已复制到数据目录
        ensureSurvivalGameLootTable();

        // 延迟给所有在线玩家烈焰棒（确保所有初始化完成）
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if ("lobby".equals(player.getWorld().getName())) {
                    lobbyHandler.giveBlazeRod(player);
                }
            }
            getLogger().info("已为所有在主城的玩家给予烈焰棒");
        }, 10L); // 延迟10tick (0.5秒)

        // ACF command manager
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new ShuffleTeam()); // shuffleteam
        commandManager.registerCommand(new Launch()); // launch
        commandManager.registerCommand(new Stop()); // stop
        commandManager.registerCommand(new Suspend()); // suspend
        commandManager.registerCommand(new Resume()); // resume
        commandManager.registerCommand(new SendInfo()); // sendInfo
        commandManager.registerCommand(new PKTSelectChaser()); // pktselectchaser
        commandManager.registerCommand(new Party()); // party
        commandManager.registerCommand(new TogglePVP()); // togglepvp
        commandManager.registerCommand(new ToggleFriendlyFire()); // togglefriendlyfire
        commandManager.registerCommand(new GiveSpecialItem()); // giveSpecialItem

        getLogger().info("合合启动了");

        // 预加载并输出生存游戏战利品信息（进入游戏前）
        try {
            mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl.preloadAndDumpLootTables();
        } catch (Throwable t) {
            getLogger().warning("Failed to dump SurvivalGame loot tables: " + t.getMessage());
        }

        // 启动欢迎标语动画（插件启动时没有游戏在运行）
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!isRunningGame()) {
                startWelcomeMessage();
            }
        }, 20L); // 延迟1秒启动，确保所有初始化完成
    }

    private void ensureSurvivalGameLootTable() {
        try {
            java.nio.file.Path root = plugin.getDataPath().resolve("MCEConfig").resolve("survival_game_loot_table");
            java.nio.file.Files.createDirectories(root);

            copyIfMissing(root.resolve("lootConfig.cfg"), "MCEConfig/survival_game_loot_table/lootConfig.cfg");

            java.nio.file.Path lootPool = root.resolve("lootPool");
            java.nio.file.Path lootData = root.resolve("lootData");
            java.nio.file.Files.createDirectories(lootPool);
            java.nio.file.Files.createDirectories(lootData);

            String[] files = new String[] {
                    "ammo.cfg", "armor.cfg", "empty.cfg", "enchantment.cfg", "food.cfg", "material.cfg", "others.cfg",
                    "potion.cfg", "weapon.cfg"
            };
            for (String f : files) {
                copyIfMissing(lootPool.resolve(f), "MCEConfig/survival_game_loot_table/lootPool/" + f);
                copyIfMissing(lootData.resolve(f), "MCEConfig/survival_game_loot_table/lootData/" + f);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to ensure survival_game_loot_table resources: " + e.getMessage());
        }
    }

    private void copyIfMissing(java.nio.file.Path target, String resourcePath) throws java.io.IOException {
        if (java.nio.file.Files.exists(target))
            return;
        try (java.io.InputStream in = getResource(resourcePath)) {
            if (in != null) {
                java.nio.file.Files.copy(in, target);
            }
        }
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
        // 停止欢迎标语动画
        stopWelcomeMessage();

        MCEGame nextGame = gameList.get(gameID);
        setCurrentRunningGame(nextGame);
        nextGame.init(intro); // 在开始游戏之前，先初始化游戏的时间线

        nextGame.start();
    }

    public static void launchVotingSystem() {
        // 启动投票系统
        immediateLaunchGame(VOTING_SYSTEM_ID, false);
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
        // return currentTimeline != null && currentTimeline == eventTimeline;
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
        // 传送不在主城的玩家到主城
        if (!"lobby".equals(player.getWorld().getName())) {
            if (Bukkit.getWorld("lobby") != null) {
                player.teleport(Bukkit.getWorld("lobby").getSpawnLocation());
            }
        }

        // 清空药水效果
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        // 清空scoreboard标签
        player.getScoreboardTags().clear();

        // 清除发光效果
        MCEGlowingEffectManager.clearPlayerGlowingEffect(player);

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

    /**
     * 启动欢迎标语动画（当无游戏运行时）
     */
    public static void startWelcomeMessage() {
        WelcomeMessageHandler.startWelcomeMessage();
    }

    /**
     * 停止欢迎标语动画
     */
    public static void stopWelcomeMessage() {
        WelcomeMessageHandler.stopWelcomeMessage();
    }
}
