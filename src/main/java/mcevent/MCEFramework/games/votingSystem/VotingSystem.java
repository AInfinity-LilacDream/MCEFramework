package mcevent.MCEFramework.games.votingSystem;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.games.votingSystem.customHandler.VotingCardHandler;
import mcevent.MCEFramework.games.votingSystem.gameObject.VotingSystemGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import mcevent.MCEFramework.tools.MCEGlowingEffectManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

import static mcevent.MCEFramework.games.votingSystem.VotingSystemFuncImpl.*;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
// import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/**
 * VotingSystem: 游戏投票系统
 * 在游戏结束后让玩家投票选择下一个游戏
 */
@Getter
@Setter
public class VotingSystem extends MCEGame {

    private VotingCardHandler votingCardHandler = new VotingCardHandler();
    private VotingSystemConfigParser votingSystemConfigParser = new VotingSystemConfigParser();
    private BossBar votingBossBar;
    private BukkitRunnable bossBarTask;

    public VotingSystem(String title, int id, String worldName, int round, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, worldName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration,
                cycleStartDuration, cycleEndDuration, endDuration);
    }

    @Override
    public void onLaunch() {
        loadConfig();
        MCEPlayerUtils.globalClearPotionEffects();

        // 传送非主城的玩家到主城，并清理发光效果
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 清理玩家的发光效果
            MCEGlowingEffectManager.clearPlayerGlowingEffect(player);

            String wn = player.getWorld().getName();
            if (!"lobby".equals(wn) ) {
                player.teleport(Objects.requireNonNull(Bukkit.getWorld("lobby")).getSpawnLocation());
            }
        }
        MCEWorldUtils.disablePVP();

        // 添加延时任务设置冒险模式，确保传送完成后再设置游戏模式
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SURVIVAL, 5L); // 0.25秒延时

        // 重新启用主城二段跳功能（延迟执行确保游戏模式设置完成）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if ("lobby".equals(player.getWorld().getName())) {
                    // 重新启用二段跳
                    player.setAllowFlight(true);
                    player.setFlying(false);
                }
            }
        }, 10L); // 延迟10tick确保游戏模式设置完成

        // 设置玩家血量为10颗心（20.0血量）
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
        }
        MCEPlayerUtils.clearGlobalTags();
    }

    @Override
    public void onCycleStart() {
        // 播放背景音乐
        MCEPlayerUtils.globalPlaySound("minecraft:vote");

        // 给所有玩家投票卡
        giveVotingCards();

        // 启动投票系统
        votingCardHandler.start();

        // 创建并启动BossBar倒计时
        createVotingBossBar();

        // 回到主城投票时，发放主城物品（风弹发射器 + 前往Duel指南针）
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            mcevent.MCEFramework.customHandler.LobbyItemHandler lih = mcevent.MCEFramework.MCEMainController
                    .getLobbyItemHandler();
            if (lih != null && ("lobby".equals(p.getWorld().getName()) || "duel".equals(p.getWorld().getName()))) {
                lih.giveLobbyItems(p);
            }
        }

        MCEMessenger.sendGlobalTitle("<gold><bold>投票开始！</bold></gold>",
                "<yellow>右键投票卡选择下一个游戏</yellow>");
    }

    @Override
    public void onEnd() {
        // 投票结束，统计结果并启动获胜游戏
        cleanupBossBar();
        VotingSystemFuncImpl.processVotingResults();
        votingCardHandler.suspend(); // 处理完结果后再清理数据
    }

    @Override
    public void stop() {
        super.stop();

        // 停止背景音乐
        MCEPlayerUtils.globalStopMusic();

        votingCardHandler.suspend();
        cleanupBossBar();
    }

    /**
     * 给所有在线玩家发放投票卡
     */
    private void giveVotingCards() {
        ItemStack votingCard = createVotingCard();

        for (Player player : Bukkit.getOnlinePlayers()) {
            // 保存或准备恢复烈焰棒（风弹发射器）
            ItemStack blazeRod = null;
            Component blazeName = MiniMessage.miniMessage().deserialize("<red><bold>风弹发射器</bold></red>");
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.BLAZE_ROD &&
                        item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    Component dn = item.getItemMeta().displayName();
                    if (dn != null && dn.equals(blazeName)) {
                        blazeRod = item.clone();
                        break;
                    }
                }
            }

            player.getInventory().clear();
            player.getInventory().setItem(4, votingCard); // 放在第5个槽位（中间）

            // 恢复烈焰棒；如未找到则补发一个
            if (blazeRod != null) {
                player.getInventory().setItem(0, blazeRod);
            } else {
                ItemStack newRod = new ItemStack(Material.BLAZE_ROD);
                ItemMeta meta = newRod.getItemMeta();
                if (meta != null) {
                    meta.displayName(blazeName);
                    java.util.List<Component> lore = java.util.Arrays.asList(
                            MiniMessage.miniMessage().deserialize("<yellow>右键发射风弹</yellow>"),
                            MiniMessage.miniMessage().deserialize("<gray>击中玩家给予发光效果</gray>"),
                            MiniMessage.miniMessage().deserialize("<red>冷却时间: 3秒</red>"));
                    meta.lore(lore);
                    newRod.setItemMeta(meta);
                }
                player.getInventory().setItem(0, newRod);
            }

            // 始终补发指南针：主城->前往Duel（投票期也可进入Duel）
            try {
                if (player.getWorld() != null && "lobby".equals(player.getWorld().getName())) {
                    player.getInventory().setItem(8,
                            mcevent.MCEFramework.customHandler.LobbyTeleportCompassHandler.createToDuelCompass());
                }
            } catch (Throwable ignored) {
            }

            player.updateInventory();
        }
    }

    /**
     * 创建投票卡物品
     */
    private ItemStack createVotingCard() {
        ItemStack card = new ItemStack(Material.PAPER);
        ItemMeta meta = card.getItemMeta();
        if (meta != null) {
            Component name = MiniMessage.miniMessage().deserialize("<gold><bold>投票卡</bold></gold>");
            java.util.List<Component> lore = java.util.Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<yellow>右键点击打开投票界面</yellow>"),
                    MiniMessage.miniMessage().deserialize("<gray>选择您想要游玩的下一个游戏</gray>"));
            meta.displayName(name);
            meta.lore(lore);
            card.setItemMeta(meta);
        }
        return card;
    }

    /**
     * 创建投票倒计时BossBar
     */
    private void createVotingBossBar() {
        // 创建BossBar
        votingBossBar = Bukkit.createBossBar("§6§l投票倒计时", BarColor.YELLOW, BarStyle.SOLID);

        // 为所有在线玩家显示BossBar（排除 duel 世界玩家）
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != null && "duel".equals(player.getWorld().getName()))
                continue;
            votingBossBar.addPlayer(player);
        }

        // 启动倒计时任务 (30秒)
        final int totalSeconds = 30;
        bossBarTask = new BukkitRunnable() {
            int remainingSeconds = totalSeconds;

            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    this.cancel();
                    return;
                }

                // 更新BossBar
                double progress = (double) remainingSeconds / totalSeconds;
                votingBossBar.setProgress(progress);
                votingBossBar.setTitle("§6§l投票倒计时: " + remainingSeconds + "秒");

                // 根据时间改变颜色
                if (remainingSeconds <= 10) {
                    votingBossBar.setColor(BarColor.RED);
                } else if (remainingSeconds <= 20) {
                    votingBossBar.setColor(BarColor.YELLOW);
                } else {
                    votingBossBar.setColor(BarColor.GREEN);
                }

                remainingSeconds--;
            }
        };

        bossBarTask.runTaskTimer(plugin, 0L, 20L); // 每秒更新一次
    }

    /**
     * 清理BossBar
     */
    private void cleanupBossBar() {
        if (votingBossBar != null) {
            votingBossBar.removeAll();
            votingBossBar = null;
        }

        if (bossBarTask != null && !bossBarTask.isCancelled()) {
            bossBarTask.cancel();
            bossBarTask = null;
        }
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new VotingSystemGameBoard(getTitle(), getWorldName(), getRound()));
    }
}