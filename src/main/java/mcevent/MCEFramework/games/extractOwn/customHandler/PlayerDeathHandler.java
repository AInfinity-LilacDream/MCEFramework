package mcevent.MCEFramework.games.extractOwn.customHandler;

import mcevent.MCEFramework.games.extractOwn.ExtractOwn;
import mcevent.MCEFramework.games.extractOwn.ExtractOwnFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
PlayerDeathHandler: ExtractOwn玩家死亡监听器
*/
public class PlayerDeathHandler extends MCEResumableEventHandler implements Listener {

    private ExtractOwn extractOwn;

    public void register(ExtractOwn game) {
        this.extractOwn = game;
        setSuspended(true); // 默认挂起，游戏开始时启动
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void start() {
        setSuspended(false);
    }

    @Override
    public void suspend() {
        setSuspended(true);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (isSuspended())
            return;

        // 检查是否是玩家受到伤害
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        // 检查玩家是否在游戏中
        if (victim.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        Player attacker = null;

        // 确定攻击者
        if (event.getDamager() instanceof Player directAttacker) {
            attacker = directAttacker;
        } else if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }

        // 如果没有攻击者或攻击者不在游戏中，跳过
        if (attacker == null || attacker.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        Team attackerTeam = MCETeamUtils.getTeam(attacker);
        Team victimTeam = MCETeamUtils.getTeam(victim);

        // 防止同队误伤
        if (attackerTeam != null && victimTeam != null && attackerTeam.equals(victimTeam)) {
            event.setCancelled(true);
            return;
        }

        // 检查伤害是否会导致死亡 -> 直接交给全局淘汰处理器
        if (victim.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            mcevent.MCEFramework.customHandler.GlobalEliminationHandler.eliminateNow(victim);
        }
    }

    @EventHandler
    public void onPlayerGeneralDamage(EntityDamageEvent event) {
        if (isSuspended())
            return;

        // 检查是否是玩家受到伤害
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        // 检查玩家是否在游戏中
        if (victim.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        // 检查是否是世界边界伤害
        if (event.getCause() == EntityDamageEvent.DamageCause.WORLD_BORDER) {
            // 检查伤害是否会导致死亡
            if (victim.getHealth() - event.getFinalDamage() <= 0) {
                event.setCancelled(true);
                mcevent.MCEFramework.customHandler.GlobalEliminationHandler.eliminateNow(victim);
            }
        }
    }

    /**
     * 处理玩家击杀
     */
    private void handlePlayerKill(Player killer, Player victim) {
        String killerName = MCEPlayerUtils.getColoredPlayerName(killer);
        String victimName = MCEPlayerUtils.getColoredPlayerName(victim);

        // 向所有人发送聊天栏消息
        MCEMessenger.sendGlobalInfo(victimName + " 被 " + killerName + " 击杀");

        // 向击杀者发送大标题 (仿照PKT样式)
        MCEMessenger.sendTitleToPlayer(killer, "<red>⚔</red> " + victimName, null);

        // 调用ExtractOwnFuncImpl的击杀处理
        ExtractOwnFuncImpl.handlePlayerKill(killer, victim);

        // 立即检查是否只剩一队存活
        checkRoundEnd();
    }

    /**
     * 处理世界边界死亡（无击杀者）
     */
    private void handleWorldBorderDeath(Player victim) {
        String victimName = MCEPlayerUtils.getColoredPlayerName(victim);

        // 向所有人发送聊天栏消息
        MCEMessenger.sendGlobalInfo(victimName + " 脱离了世界！");

        // 立即检查是否只剩一队存活
        checkRoundEnd();
    }

    /**
     * 检查回合是否应该结束
     */
    private void checkRoundEnd() {
        if (extractOwn != null) {
            int survivingTeams = extractOwn.getSurvivingTeamCount();

            if (survivingTeams <= 1) {
                // 不调用nextState，让时间线自然过渡到cycleEnd阶段
                // 消息已经通过击杀处理显示完整
            }
        }
    }
}