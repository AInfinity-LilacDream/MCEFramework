package mcevent.MCEFramework.customHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
WelcomeMessageHandler: 欢迎标语处理器
在无游戏运行时显示动画欢迎标语
*/
public class WelcomeMessageHandler implements Listener {

    private static BukkitTask globalAnimationTask;
    private static final Map<UUID, BukkitTask> playerAnimationTasks = new HashMap<>();
    private static boolean isWelcomeMessageActive = false;
    private static BossBar lobbyBossBar;

    // 欢迎标语内容
    private static final String WELCOME_MESSAGE = "欢迎来到Lilac Games！游戏稍后开始...";

    public WelcomeMessageHandler() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 启动全局欢迎标语动画
     */
    public static void startWelcomeMessage() {
        if (isWelcomeMessageActive)
            return;

        isWelcomeMessageActive = true;

        // 为所有在主城的玩家启动动画
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInLobby(player)) {
                startPlayerAnimation(player);
                showBossBar(player);
            }
        }

        // 初始化 BossBar（只初始化一次）
        if (lobbyBossBar == null) {
            lobbyBossBar = BossBar.bossBar(buildBossBarTitle(), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        }
    }

    /**
     * 停止全局欢迎标语动画
     */
    public static void stopWelcomeMessage() {
        isWelcomeMessageActive = false;

        // 停止所有玩家的动画任务
        for (BukkitTask task : playerAnimationTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        playerAnimationTasks.clear();

        if (globalAnimationTask != null && !globalAnimationTask.isCancelled()) {
            globalAnimationTask.cancel();
            globalAnimationTask = null;
        }

        // 隐藏 BossBar 并清理
        hideBossBarFromAll();
        lobbyBossBar = null;
    }

    /**
     * 玩家加入服务器时处理
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joinedPlayer = event.getPlayer();

        // 延迟检查，确保玩家完全加载
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isWelcomeMessageActive && isInLobby(joinedPlayer)) {
                // 给新加入的玩家显示动画欢迎消息
                startPlayerAnimation(joinedPlayer);
                showBossBar(joinedPlayer);

                // 给其他在线玩家发送带新玩家名字的欢迎消息
                Component welcomeMessage = MiniMessage.miniMessage().deserialize("<gold><bold>欢迎</bold></gold>")
                        .append(MiniMessage.miniMessage()
                                .deserialize("<yellow>" + joinedPlayer.getName() + "</yellow>"))
                        .append(MiniMessage.miniMessage().deserialize(
                                "<gold><bold>来到</bold></gold><gradient:red:blue>Lilac Games</gradient><gold><bold>！</bold></gold>"));

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    // 不给新加入的玩家发送这个消息，只给其他玩家
                    if (!onlinePlayer.equals(joinedPlayer) && isInLobby(onlinePlayer)) {
                        onlinePlayer.sendMessage(welcomeMessage);
                    }
                }
            }
        }, 20L); // 1秒后检查
    }

    /**
     * 玩家切换世界时处理
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 如果玩家离开主城，停止其动画
        if (!isInLobby(player)) {
            BukkitTask task = playerAnimationTasks.get(playerId);
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
            playerAnimationTasks.remove(playerId);
            hideBossBar(player);
        }
        // 如果玩家进入主城且欢迎标语激活，开始动画
        else if (isWelcomeMessageActive) {
            startPlayerAnimation(player);
            showBossBar(player);
        }
    }

    /**
     * 为单个玩家启动动画
     */
    private static void startPlayerAnimation(Player player) {
        UUID playerId = player.getUniqueId();

        // 停止该玩家之前的动画任务
        BukkitTask existingTask = playerAnimationTasks.get(playerId);
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }

        // 启动新的动画任务
        BukkitRunnable runnable = new BukkitRunnable() {
            private int animationStep = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !isInLobby(player) || !isWelcomeMessageActive) {
                    playerAnimationTasks.remove(playerId);
                    this.cancel();
                    return;
                }

                Component message = createAnimatedMessage(animationStep);
                player.sendActionBar(message);

                animationStep++;
                // 动画循环：滚动效果 + 等待 + 全部变黄 + 等待
                int messageLength = WELCOME_MESSAGE.length();
                int totalSteps = messageLength + 3 + 20 + 20 + 60; // 滚动(长度+3) + 等待 + 全黄 + 等待
                if (animationStep >= totalSteps) {
                    animationStep = 0;
                }
            }
        };

        BukkitTask animationTask = runnable.runTaskTimer(plugin, 0L, 1L); // 每tick执行一次
        playerAnimationTasks.put(playerId, animationTask);
    }

    /**
     * 创建动画消息
     */
    private static Component createAnimatedMessage(int step) {
        TextComponent.Builder builder = Component.text();
        int messageLength = WELCOME_MESSAGE.length();

        // 动画阶段划分
        int rollPhaseEnd = messageLength + 3; // 滚动阶段结束
        int waitPhase1End = rollPhaseEnd + 20; // 第一次等待结束
        int yellowPhaseEnd = waitPhase1End + 20; // 全黄阶段结束

        if (step < rollPhaseEnd) {
            // 阶段1：滚动效果 - 同时有3个连续字符是黄色
            int centerIndex = step - 1; // 中心黄色字符的索引（从-1开始，这样第0步时centerIndex=-1）

            for (int i = 0; i < messageLength; i++) {
                char c = WELCOME_MESSAGE.charAt(i);
                // 检查当前字符是否在黄色范围内（centerIndex-1 到 centerIndex+1）
                if (i >= centerIndex - 1 && i <= centerIndex + 1 && centerIndex >= 0) {
                    builder.append(Component.text(c, NamedTextColor.YELLOW));
                } else {
                    builder.append(Component.text(c, NamedTextColor.GREEN));
                }
            }
        } else if (step < waitPhase1End) {
            // 阶段2：等待 - 全部绿色
            builder.append(Component.text(WELCOME_MESSAGE, NamedTextColor.GREEN));
        } else if (step < yellowPhaseEnd) {
            // 阶段3：全部变黄
            builder.append(Component.text(WELCOME_MESSAGE, NamedTextColor.YELLOW));
        } else {
            // 阶段4：等待 - 全部绿色
            builder.append(Component.text(WELCOME_MESSAGE, NamedTextColor.GREEN));
        }

        return builder.build();
    }

    /**
     * 检查玩家是否在主城
     */
    private static boolean isInLobby(Player player) {
        World world = player.getWorld();
        return world.getName().equals("lobby"); // 主城世界名为 "lobby"
    }

    /**
     * 检查欢迎标语是否激活
     */
    public static boolean isWelcomeMessageActive() {
        return isWelcomeMessageActive;
    }

    private static Component buildBossBarTitle() {
        return Component.empty()
                .append(Component.text("@复旦大学基岩社 ", NamedTextColor.AQUA))
                .append(Component.text("@AInfinity_Dream", NamedTextColor.GREEN))
                .append(Component.space())
                .append(Component.text("倾情奉献", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true));
    }

    private static void showBossBar(Player player) {
        if (lobbyBossBar == null) {
            lobbyBossBar = BossBar.bossBar(buildBossBarTitle(), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        }
        try {
            player.showBossBar(lobbyBossBar);
        } catch (Throwable ignored) {
        }
    }

    private static void hideBossBar(Player player) {
        if (lobbyBossBar == null)
            return;
        try {
            player.hideBossBar(lobbyBossBar);
        } catch (Throwable ignored) {
        }
    }

    private static void hideBossBarFromAll() {
        if (lobbyBossBar == null)
            return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                p.hideBossBar(lobbyBossBar);
            } catch (Throwable ignored) {
            }
        }
    }
}