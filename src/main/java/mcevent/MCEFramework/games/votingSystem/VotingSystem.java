package mcevent.MCEFramework.games.votingSystem;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.games.votingSystem.customHandler.VotingCardHandler;
import mcevent.MCEFramework.games.votingSystem.gameObject.VotingSystemGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.miscellaneous.Constants;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

import static mcevent.MCEFramework.games.votingSystem.VotingSystemFuncImpl.*;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/**
 * VotingSystem: 游戏投票系统
 * 在游戏结束后让玩家投票选择下一个游戏
 */
@Getter @Setter
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
        
        // 传送不在主城的玩家到主城并清理发光效果
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 清理玩家的发光效果
            MCEGlowingEffectManager.clearPlayerGlowingEffect(player);
            
            if (!player.getWorld().getName().equals("lobby")) {
                player.teleport(Objects.requireNonNull(Bukkit.getWorld("lobby")).getSpawnLocation());
            }
        }
        MCEWorldUtils.disablePVP();
        
        // 添加延时任务设置冒险模式，确保传送完成后再设置游戏模式
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.ADVENTURE, 5L); // 0.25秒延时
        
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

        grantGlobalPotionEffect(saturation);
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
        
        MCEMessenger.sendGlobalTitle("<gold><bold>投票开始！</bold></gold>", 
                                   "<yellow>右键投票卡选择下一个游戏</yellow>");

        // 处理玩家选队
        if (Constants.enableTeamSelection) {
            giveTeamSelectionCard();
        }
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
            // 保存烈焰棒
            ItemStack blazeRod = null;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.BLAZE_ROD && 
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    "§c§l风弹发射器".equals(item.getItemMeta().getDisplayName())) {
                    blazeRod = item.clone();
                    break;
                }
            }
            
            player.getInventory().clear();
            player.getInventory().setItem(4, votingCard); // 放在第5个槽位（中间）
            
            // 恢复烈焰棒
            if (blazeRod != null) {
                player.getInventory().setItem(0, blazeRod); // 放在第一个槽位
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
            meta.setDisplayName("§6§l投票卡");
            meta.setLore(java.util.Arrays.asList(
                "§e右键点击打开投票界面",
                "§7选择您想要游玩的下一个游戏"
            ));
            card.setItemMeta(meta);
        }
        return card;
    }

    /**
     * 给所有玩家发放选队卡
     */
    private void giveTeamSelectionCard() {
        ItemStack votingCard = createVotingCard();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.getInventory().setItem(5, votingCard); // 放在第5个槽位（中间）
            player.updateInventory();
        }
    }

    /**
     * 创建投票卡物品
     * @return 投票卡 ItemStack
     */
    private ItemStack createTeamSelectionCard() {
        ItemStack selectionCard = new ItemStack(Material.PAPER);    // 或者换成别的
        ItemMeta meta = selectionCard.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l选队卡");
            meta.setLore(java.util.Arrays.asList(
                    "§e右键点击打开选队界面",
                    "§7选择您想要加入的队伍"
            ));
            selectionCard.setItemMeta(meta);
        }
        return selectionCard;
    }

    /**
     * 创建投票倒计时BossBar
     */
    private void createVotingBossBar() {
        // 创建BossBar
        votingBossBar = Bukkit.createBossBar("§6§l投票倒计时", BarColor.YELLOW, BarStyle.SOLID);
        
        // 为所有在线玩家显示BossBar
        for (Player player : Bukkit.getOnlinePlayers()) {
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