package mcevent.MCEFramework.games.discoFever;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.discoFever.customHandler.ActionBarMessageHandler;
import mcevent.MCEFramework.games.discoFever.customHandler.InventoryHandler;
import mcevent.MCEFramework.games.discoFever.customHandler.PlayerFallHandler;
import mcevent.MCEFramework.games.discoFever.gameObject.DiscoFeverGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static mcevent.MCEFramework.games.discoFever.DiscoFeverFuncImpl.*;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
DiscoFever: disco fever的完整实现
 */
@Getter
@Setter
public class DiscoFever extends MCEGame {

    private ActionBarMessageHandler actionBarMessageHandler = new ActionBarMessageHandler();
    private InventoryHandler inventoryHandler = new InventoryHandler();
    private PlayerFallHandler playerFallHandler = new PlayerFallHandler();
    private DiscoFeverConfigParser discoFeverConfigParser = new DiscoFeverConfigParser();
    private List<BukkitRunnable> bossBarTasks = new ArrayList<>();

    private List<Double> timeList = new ArrayList<>();
    private List<Material> materialList = new ArrayList<>();
    private int currentState = 0;
    private int maxState = 0;

    private Location currentPlatformLocation;
    private BossBar bossBar = Bukkit.createBossBar(
            "平台时间",
            BarColor.GREEN,
            BarStyle.SOLID);

    public DiscoFever(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration,
                cycleEndDuration, endDuration);
        MCETimerUtils.setFramedTask(() -> actionBarMessageHandler.showMessage());
        inventoryHandler.register(this);
        playerFallHandler.register(this);
    }

    @Override
    public void onLaunch() {
        MCEPlayerUtils.globalClearPotionEffects();
        currentPlatformLocation = getDiscoFeverPlatformLocation(this.getWorldName());
        resetPlatform(this.getWorldName());
        loadConfig();
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null)
            world.setGameRule(GameRule.FALL_DAMAGE, false);
        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCETeleporter.globalSwapWorld(this.getWorldName());
        currentState = 0;
        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SURVIVAL, 5L);

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        // 设置玩家血量为10颗心（20.0血量）
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
        }

        grantGlobalPotionEffect(saturation);

        MCEPlayerUtils.clearGlobalTags();
        // 启动监听器，防止玩家丢弃靴子
        inventoryHandler.start();
        // 启动监听器并设为准备期（Y<=3 回出生点）
        playerFallHandler.setPreparationPhase(true);
        playerFallHandler.start();

        // 关闭玩家间碰撞（仅在 DiscoFever 生效）
        setTeamsCollision(false);

        // 进入 DiscoFever 时给予队伍颜色皮靴
        equipTeamBootsForAllPlayers();
    }

    @Override
    public void onCycleStart() {
        resetGameBoard();
        this.getGameBoard().setStateTitle("<red><bold> 剩余时间：</bold></red>");

        // 播放背景音乐
        MCEPlayerUtils.globalPlaySound("minecraft:disco_fever");

        currentPlatformLocation = getDiscoFeverPlatformLocation(this.getWorldName());
        actionBarMessageHandler.start();

        inventoryHandler.start();
        // 进行阶段：切换为淘汰模式
        playerFallHandler.setPreparationPhase(false);
        playerFallHandler.start();

        // 正式开始后给予无粒子隐身
        grantGlobalPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 600, 0, false, false, false));

        // 再次确保玩家穿戴队伍颜色皮靴
        equipTeamBootsForAllPlayers();

        double time = 0;
        bossBarTasks.clear();
        Random rand = new Random();

        for (int i = 0; i < timeList.size(); ++i) {
            Double duration = timeList.get(i);
            Material material = materialList.get(rand.nextInt(materialList.size()));
            int id = i + 1;
            bossBarTasks.add(MCETimerUtils.setDelayedTask(time, () -> {
                fillPlayerInventoryWithBlock(material);
                MCETimerUtils.showGlobalDurationOnBossBar(bossBar, duration, false);
                actionBarMessageHandler.setActionBarMessage("<dark_aqua>平台：</dark_aqua><aqua>" + id +
                        "</aqua><dark_aqua>/</dark_aqua><aqua>" + maxState + "</aqua> <dark_aqua>(</dark_aqua><aqua>" +
                        String.format("%.2f", duration) + "</aqua><dark_aqua>秒)</dark_aqua>");
                generateNewPlatform(currentPlatformLocation);
            }));
            time += duration;
            if (i != timeList.size() - 1) {
                bossBarTasks.add(MCETimerUtils.setDelayedTask(time, () -> {
                    fillPlayerInventoryWithBlock(Material.AIR);
                    MCETimerUtils.showGlobalDurationOnBossBar(bossBar, 2, true);
                    updatePlatform(currentPlatformLocation, material, this.getWorldName());
                    updateCurrentPlatformLocation();
                }));
                time += 2;
            } else {
                bossBarTasks.add(MCETimerUtils.setDelayedTask(time, () -> this.getTimeline().nextState()));
            }
        }
    }

    @Override
    public void onEnd() {
        sendWinningMessage();
        // 不在结束阶段修改玩家游戏模式

        // 进入结束阶段：停止音乐、停止BossBar与倒计时、清理平台任务、暂停消息与坠落处理器，并显示“游戏结束”标题
        this.getGameBoard().setStateTitle("<red><bold> 游戏结束：</bold></red>");
        MCEPlayerUtils.globalStopMusic();
        if (bossBar != null) {
            bossBar.removeAll();
        }
        clearBossBarTask();
        actionBarMessageHandler.suspend();
        inventoryHandler.suspend();
        playerFallHandler.suspend();

        // onEnd结束后立即清理展示板和资源，然后启动投票系统
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop(); // 停止所有游戏资源
            MCEMainController.returnToLobbyOrLaunchVoting();
        });
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new DiscoFeverGameBoard(getTitle(), getWorldName()));
    }

    @Override
    public void stop() {
        super.stop();

        // 停止背景音乐
        MCEPlayerUtils.globalStopMusic();

        bossBar.removeAll();
        clearBossBarTask();
        actionBarMessageHandler.suspend();
        inventoryHandler.suspend();
        playerFallHandler.suspend();

        // 恢复玩家间碰撞到开启
        setTeamsCollision(true);
    }

    public void clearBossBarTask() {
        for (BukkitRunnable bossBarTask : bossBarTasks) {
            bossBarTask.cancel();
        }
        bossBarTasks.clear();
    }

    private void setTeamsCollision(boolean enabled) {
        java.util.ArrayList<Team> teams = getActiveTeams();
        if (teams == null || teams.isEmpty())
            teams = MCETeamUtils.getActiveTeams();
        if (teams == null)
            return;
        for (Team team : teams) {
            if (team == null)
                continue;
            try {
                team.setOption(Team.Option.COLLISION_RULE,
                        enabled ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER);
            } catch (Throwable ignore) {
                // 兼容性考虑：若服务器不支持该API，则忽略
            }
        }
    }

    private void equipTeamBootsForAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            Color color = mapTeamToLeatherColor(team);
            if (color == null)
                continue;
            ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
            LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
            if (meta != null) {
                meta.setColor(color);
                meta.setUnbreakable(true);
                boots.setItemMeta(meta);
            }
            player.getInventory().setBoots(boots);
        }
    }

    private Color mapTeamToLeatherColor(Team team) {
        String name = team.getName();
        if (name.contains("红"))
            return Color.RED;
        if (name.contains("橙"))
            return Color.ORANGE;
        if (name.contains("黄"))
            return Color.YELLOW;
        if (name.contains("翠") || name.contains("绿"))
            return Color.LIME;
        if (name.contains("青") || name.contains("缥"))
            return Color.AQUA;
        if (name.contains("蓝"))
            return Color.BLUE;
        if (name.contains("紫") || name.contains("粉"))
            return Color.FUCHSIA;
        return Color.WHITE;
    }

}
