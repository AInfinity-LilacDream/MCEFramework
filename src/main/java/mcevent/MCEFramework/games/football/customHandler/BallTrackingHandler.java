package mcevent.MCEFramework.games.football.customHandler;

import mcevent.MCEFramework.games.football.Football;
import mcevent.MCEFramework.games.football.FootballFuncImpl;
import mcevent.MCEFramework.games.football.gameObject.FootballMatch;
import org.bukkit.Location;
import org.bukkit.entity.Armadillo;
import org.bukkit.scheduler.BukkitRunnable;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
BallTrackingHandler: 监听犰狳（球）的位置，检测进球
*/
public class BallTrackingHandler {

    private BukkitRunnable trackingTask;
    private Football game;
    private FootballMatch match; // 可选：用于子对局解耦
    private boolean isActive = false;

    public void start(Football football) {
        this.game = football;
        this.isActive = true;
        if (trackingTask != null) {
            trackingTask.cancel();
        }

        trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive || game.getBall() == null || game.getBall().isDead()) {
                    return;
                }

                checkGoal();
            }
        };

        // 每1个tick检查一次（20次/秒）- 提高检测频率
        trackingTask.runTaskTimer(plugin, 0L, 1L);
    }

    // 针对第二球场的进球检测（青/黄）
    public void startSecond(Football football) {
        this.game = football;
        this.isActive = true;
        if (trackingTask != null) {
            trackingTask.cancel();
        }

        trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive || game.getBall2() == null || game.getBall2().isDead()) {
                    return;
                }
                checkGoalSecond();
            }
        };
        trackingTask.runTaskTimer(plugin, 0L, 1L);
    }

    // === 新增：以 FootballMatch 为上下文（第一球场）===
    public void start(FootballMatch match) {
        this.match = match;
        this.game = null;
        this.isActive = true;
        if (trackingTask != null) {
            trackingTask.cancel();
        }
        trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now % 1000 < 50) {
                    boolean ballNull = (match.getBallEntity() == null);
                    boolean dead = !ballNull && match.getBallEntity().isDead();
                    if (!ballNull) {
                        org.bukkit.Location l = match.getBallEntity().getLocation();
                        plugin.getLogger().info("[Track-Match-RB] active=" + isActive + " ballNull=" + ballNull +
                                " dead=" + dead + " loc="
                                + String.format("%.2f,%.2f,%.2f", l.getX(), l.getY(), l.getZ()));
                    } else {
                        plugin.getLogger().info("[Track-Match-RB] active=" + isActive + " ballNull=true");
                    }
                }
                if (match.getBallEntity() == null || match.getBallEntity().isDead())
                    return;
                checkGoalForMatch();
            }
        };
        trackingTask.runTaskTimer(plugin, 0L, 1L);
    }

    // === 新增：以 FootballMatch 为上下文（第二球场）===
    public void startSecond(FootballMatch match) {
        this.match = match;
        this.game = null;
        this.isActive = true;
        if (trackingTask != null) {
            trackingTask.cancel();
        }
        trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now % 1000 < 50) {
                    boolean ballNull = (match.getBallEntity() == null);
                    boolean dead = !ballNull && match.getBallEntity().isDead();
                    if (!ballNull) {
                        org.bukkit.Location l = match.getBallEntity().getLocation();
                        plugin.getLogger().info("[Track-Match-CY] active=" + isActive + " ballNull=" + ballNull +
                                " dead=" + dead + " loc="
                                + String.format("%.2f,%.2f,%.2f", l.getX(), l.getY(), l.getZ()));
                    } else {
                        plugin.getLogger().info("[Track-Match-CY] active=" + isActive + " ballNull=true");
                    }
                }
                if (match.getBallEntity() == null || match.getBallEntity().isDead())
                    return;
                checkGoalSecondForMatch();
            }
        };
        trackingTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void suspend() {
        this.isActive = false;
        if (trackingTask != null) {
            trackingTask.cancel();
        }
    }

    private void checkGoal() {
        Armadillo ball = game.getBall();
        Location ballLocation = ball.getLocation();

        // 调试信息：输出球的位置（每20tick输出一次，避免日志过多）
        if (System.currentTimeMillis() % 1000 < 50) { // 大约每秒输出一次
            plugin.getLogger().info("球的位置: " + String.format("%.2f, %.2f, %.2f",
                    ballLocation.getX(), ballLocation.getY(), ballLocation.getZ()));
            plugin.getLogger().info("红门范围: " + game.getRedGoalMin() + " 到 " + game.getRedGoalMax());
            plugin.getLogger().info("蓝门范围: " + game.getBlueGoalMin() + " 到 " + game.getBlueGoalMax());
        }

        // 检查红队球门
        if (FootballFuncImpl.isInGoal(ballLocation, game.getRedGoalMin(), game.getRedGoalMax())) {
            // 蓝队进球（球进入红队球门）
            plugin.getLogger().info(
                    "检测到蓝队进球！球位置: " + ballLocation.getX() + ", " + ballLocation.getY() + ", " + ballLocation.getZ());
            onGoalScored(false);
            return;
        }

        // 检查蓝队球门
        if (FootballFuncImpl.isInGoal(ballLocation, game.getBlueGoalMin(), game.getBlueGoalMax())) {
            // 红队进球（球进入蓝队球门）
            plugin.getLogger().info(
                    "检测到红队进球！球位置: " + ballLocation.getX() + ", " + ballLocation.getY() + ", " + ballLocation.getZ());
            onGoalScored(true);
            return;
        }
    }

    private void onGoalScored(boolean redTeamScored) {
        // 暂停跟踪以防止重复检测
        isActive = false;

        // 调用游戏的进球处理方法
        game.onGoal(redTeamScored);

        // 3秒后由游戏时间线推进到下一节点；不再在此处全局传送，避免影响另一场
        game.setDelayedTask(3, () -> {
            isActive = true;
        });
    }

    private void checkGoalSecond() {
        Armadillo ball = game.getBall2();
        Location loc = ball.getLocation();
        // 青方进黄门
        if (mcevent.MCEFramework.games.football.FootballFuncImpl.isInGoal(loc, game.getYellowGoalMin(),
                game.getYellowGoalMax())) {
            game.onGoalSecond(true);
            isActive = false;
            game.setDelayedTask(3, () -> {
                isActive = true;
            });
            return;
        }
        // 黄方进青门
        if (mcevent.MCEFramework.games.football.FootballFuncImpl.isInGoal(loc, game.getCyanGoalMin(),
                game.getCyanGoalMax())) {
            game.onGoalSecond(false);
            isActive = false;
            game.setDelayedTask(3, () -> {
                isActive = true;
            });
        }
    }

    // === 新增：Match 版本进球检测（第一球场）===
    private void checkGoalForMatch() {
        Armadillo ball = match.getBallEntity();
        Location loc = ball.getLocation();
        // 进入B方球门：A方得分
        if (FootballFuncImpl.isInGoal(loc, match.getGoalMinB(), match.getGoalMaxB())) {
            match.onGoalByA();
            return;
        }
        // 进入A方球门：B方得分
        if (FootballFuncImpl.isInGoal(loc, match.getGoalMinA(), match.getGoalMaxA())) {
            match.onGoalByB();
        }
    }

    // === 新增：Match 版本进球检测（第二球场）===
    private void checkGoalSecondForMatch() {
        Armadillo ball = match.getBallEntity();
        Location loc = ball.getLocation();
        // 第二球场同理：以设定的门框范围判定
        if (FootballFuncImpl.isInGoal(loc, match.getGoalMinB(), match.getGoalMaxB())) {
            plugin.getLogger().info("[Goal-Match-CY] A scores at "
                    + String.format("%.2f,%.2f,%.2f", loc.getX(), loc.getY(), loc.getZ()));
            match.onGoalByA();
            return;
        }
        if (FootballFuncImpl.isInGoal(loc, match.getGoalMinA(), match.getGoalMaxA())) {
            plugin.getLogger().info("[Goal-Match-CY] B scores at "
                    + String.format("%.2f,%.2f,%.2f", loc.getX(), loc.getY(), loc.getZ()));
            match.onGoalByB();
        }
    }
}