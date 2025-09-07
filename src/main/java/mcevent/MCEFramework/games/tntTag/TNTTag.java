package mcevent.MCEFramework.games.tntTag;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.tntTag.customHandler.TNTExplodeHandler;
import mcevent.MCEFramework.games.tntTag.customHandler.TNTTransferHandler;
import mcevent.MCEFramework.games.tntTag.gameObject.TNTTagGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.miscellaneous.Constants;
import mcevent.MCEFramework.miscellaneous.TeamWithDetails;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static mcevent.MCEFramework.games.tntTag.TNTTagFuncImpl.*;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
TNTTag: 丢锅大战的完整实现
*/
@Getter @Setter
public class TNTTag extends MCEGame {

    private TNTExplodeHandler tntExplodeHandler = new TNTExplodeHandler();
    private TNTTransferHandler tntTransferHandler = new TNTTransferHandler();
    private TNTTagConfigParser tntTagConfigParser = new TNTTagConfigParser();
    
    private List<Player> tntCarriers = new ArrayList<>();
    private List<Player> alivePlayers = new ArrayList<>();
    private List<String> deathOrder = new ArrayList<>();
    private int currentPhase = 0;
    private boolean inTransition = false;
    
    private BossBar bossBar = Bukkit.createBossBar(
            "阶段倒计时",
            BarColor.RED,
            BarStyle.SOLID
    );

    public TNTTag(String title, int id, String mapName, boolean isMultiGame, String configFileName,
                  int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration, int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration, cycleEndDuration, endDuration);
    }

    @Override
    public void handlePlayerQuitDuringGame(org.bukkit.entity.Player player) {
        // 从存活玩家列表中移除
        alivePlayers.remove(player);
        // 从TNT携带者列表中移除
        tntCarriers.remove(player);
    }

    @Override
    public void onLaunch() {
        setIntroTextList(tntTagConfigParser.openAndParse(getConfigFileName()));
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.enablePVP(); // 启用PVP

        // 确保队伍存在并将所有玩家分到翠队
        Team cyanTeam = getCyanTeam();
        if (cyanTeam != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                cyanTeam.addEntry(player.getName());
            }
        }
        
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.ADVENTURE, 5L);
        MCEPlayerUtils.globalHideNameTag();

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
        }
        
        // 启用友伤和PVP
        MCETeamUtils.enableFriendlyFire();
        
        // 设置玩家血量为10颗心（20.0血量）
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
            player.setGlowing(true); // 所有人发光
        }
        
        grantGlobalPotionEffect(saturation);
        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCEPlayerUtils.clearGlobalTags();
        
        // 初始化存活玩家列表
        alivePlayers.clear();
        deathOrder.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            alivePlayers.add(player);
        }
        
        currentPhase = 0;
        inTransition = false;
    }

    @Override
    public void onCycleStart() {
        this.getGameBoard().setStateTitle("<green><bold> 游戏进行中</bold></green>");
        
        // 播放背景音乐
        MCEPlayerUtils.globalPlaySound("minecraft:tnttag");
        
        // 启动事件处理器
        tntExplodeHandler.start();
        tntTransferHandler.start();
        
        // 传送所有人到出生点
        Location spawnLocation = Bukkit.getWorld(this.getWorldName()).getSpawnLocation();
        for (Player player : alivePlayers) {
            player.teleport(spawnLocation);
        }
        
        // 开始第一个阶段
        startNewPhase();
    }

    @Override
    public void onEnd() {
        TNTTagFuncImpl.sendFinalResults();
        MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);
        
        // onEnd结束后立即清理展示板和资源，然后启动投票系统
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop(); // 停止所有游戏资源
            MCEMainController.launchVotingSystem(); // 立即启动投票系统
        });
    }

    @Override
    public void initGameBoard() {
        setRound(1); // TNTTag只有一个回合
        setGameBoard(new TNTTagGameBoard(getTitle(), getWorldName(), getRound()));
    }

    @Override
    public void stop() {
        super.stop();

        // 停止背景音乐
        MCEPlayerUtils.globalStopMusic();
        
        // 清除所有BossBar
        clearAllBossBars();
        
        tntExplodeHandler.suspend();
        tntTransferHandler.suspend();
        MCEPlayerUtils.globalShowNameTag();
        
        // 关闭友伤
        MCETeamUtils.disableFriendlyFire();
        
        // 清理TNT物品和发光效果
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGlowing(false);
            player.getInventory().clear();
        }
    }
    
    // 清除所有BossBar
    private void clearAllBossBars() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
    
    // 开始新的阶段
    public void startNewPhase() {
        if (shouldGameEnd()) {
            this.getTimeline().nextState(); // 结束游戏
            return;
        }
        
        currentPhase++;
        inTransition = false;
        
        // 选择TNT携带者
        selectTNTCarriers();
        
        // 开始30秒倒计时 - 使用MCETimerUtils的标准BossBar显示
        MCETimerUtils.showGlobalDurationOnBossBar(bossBar, 30.0, false);
        
        // 30秒后爆炸
        MCETimerUtils.setDelayedTask(30, this::explodeTNTCarriers); // 30秒后执行
    }
    
    // 爆炸TNT携带者
    public void explodeTNTCarriers() {
        inTransition = true;
        
        for (Player tntCarrier : new ArrayList<>(tntCarriers)) {
            if (alivePlayers.contains(tntCarrier)) {
                // 播放爆炸效果
                TNTTagFuncImpl.explodeTNTCarrier(tntCarrier);
                // 记录死亡
                eliminatePlayer(tntCarrier);
            }
        }
        
        tntCarriers.clear();
        
        // 5秒过渡时间 - 使用MCETimerUtils的标准BossBar显示
        MCETimerUtils.showGlobalDurationOnBossBar(bossBar, 5.0, true);
        MCETimerUtils.setDelayedTask(5, this::startNewPhase); // 5秒后执行
    }
    
    // 选择TNT携带者
    private void selectTNTCarriers() {
        tntCarriers.clear();
        
        if (alivePlayers.isEmpty()) return;
        
        // 先清除所有玩家的队伍分配
        for (Player player : alivePlayers) {
            Team currentTeam = MCETeamUtils.getTeam(player);
            if (currentTeam != null) {
                currentTeam.removeEntry(player.getName());
            }
        }
        
        Random random = new Random();
        int carrierCount = alivePlayers.size() >= 5 ? 2 : 1;
        
        List<Player> availablePlayers = new ArrayList<>(alivePlayers);
        
        for (int i = 0; i < carrierCount && !availablePlayers.isEmpty(); i++) {
            Player selected = availablePlayers.remove(random.nextInt(availablePlayers.size()));
            tntCarriers.add(selected);
            
            // 分到红队
            Team redTeam = getRedTeam();
            if (redTeam != null) {
                redTeam.addEntry(selected.getName());
            }
            
            // 给予TNT头盔（带绑定诅咒）
            selected.getInventory().setHelmet(createTNTHelmet());
        }
        
        // 其他玩家分到翠队
        Team cyanTeam = getCyanTeam();
        if (cyanTeam != null) {
            for (Player player : alivePlayers) {
                if (!tntCarriers.contains(player)) {
                    cyanTeam.addEntry(player.getName());
                }
            }
        }
        
        // 发送TNT携带者信息
        for (Player carrier : tntCarriers) {
            String coloredPlayerName = MCEPlayerUtils.getColoredPlayerName(carrier);
            MCEMessenger.sendGlobalText(coloredPlayerName + " <yellow>是TNT携带者！</yellow>");
        }
        
        // 更新游戏板
        if (getGameBoard() instanceof TNTTagGameBoard tntTagGameBoard) {
            tntTagGameBoard.updateAlivePlayersTitle(alivePlayers.size());
            tntTagGameBoard.globalDisplay();
        }
    }
    
    // 转移TNT
    public void transferTNT(Player from, Player to) {
        if (!tntCarriers.contains(from) || tntCarriers.contains(to) || !alivePlayers.contains(to)) {
            return;
        }
        
        // 移除原携带者
        tntCarriers.remove(from);
        from.getInventory().setHelmet(null);
        Team cyanTeam = getCyanTeam();
        if (cyanTeam != null) {
            cyanTeam.addEntry(from.getName());
        }
        
        // 新的携带者
        tntCarriers.add(to);
        to.getInventory().setHelmet(createTNTHelmet());
        Team redTeam = getRedTeam();
        if (redTeam != null) {
            redTeam.addEntry(to.getName());
        }
        
        MCEMessenger.sendGlobalText("<yellow>" + from.getName() + " 将TNT传递给了 " + to.getName() + "！</yellow>");
    }
    
    // 淘汰玩家
    public void eliminatePlayer(Player player) {
        if (!alivePlayers.contains(player)) return;
        
        alivePlayers.remove(player);
        deathOrder.add(player.getName());
        
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();
        
        MCEMessenger.sendGlobalText("<red>" + player.getName() + " 爆炸了！</red>");
        
        // 更新游戏板
        if (getGameBoard() instanceof TNTTagGameBoard tntTagGameBoard) {
            tntTagGameBoard.updateAlivePlayersTitle(alivePlayers.size());
            tntTagGameBoard.globalDisplay();
        }
    }
    
    // 创建带有绑定诅咒的TNT头盔
    private ItemStack createTNTHelmet() {
        ItemStack tntHelmet = new ItemStack(Material.TNT);
        ItemMeta meta = tntHelmet.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // 隐藏魔咒显示
            tntHelmet.setItemMeta(meta);
        }
        return tntHelmet;
    }
    
    // 检查游戏是否应该结束
    private boolean shouldGameEnd() {
        return alivePlayers.size() <= 1;
    }
    
    // 获取红队
    private Team getRedTeam() {
        for (TeamWithDetails teamDetails : Constants.teams) {
            if ("红队".equals(teamDetails.alias())) {
                return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamDetails.teamName());
            }
        }
        return null;
    }
    
    // 获取翠队
    private Team getCyanTeam() {
        for (TeamWithDetails teamDetails : Constants.teams) {
            if ("翠队".equals(teamDetails.alias())) {
                return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamDetails.teamName());
            }
        }
        return null;
    }
}