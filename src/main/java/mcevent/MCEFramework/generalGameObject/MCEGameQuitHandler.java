package mcevent.MCEFramework.generalGameObject;

import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Set;

/**
 * MCEGameQuitHandler: 游戏退出处理工具类
 * 提供统一的玩家退出处理逻辑：只有在游戏进行中（onCycleStart 阶段）退出才判定为死亡
 */
public class MCEGameQuitHandler {

    /**
     * 检查当前是否处于游戏进行中（onCycleStart 阶段）
     * 时间线结构：
     * - 0: onLaunch
     * - 1 (可选): intro
     * - 2: onPreparation
     * - 后续: onCyclePreparation (如果 duration > 0), onCycleStart, onCycleEnd (如果还有下一回合)
     * - 最后一个状态: onEnd
     * 
     * 注意：onCycleStart 节点的 canSuspend = false，而 onCyclePreparation 和 onCycleEnd 的 canSuspend = true
     */
    public static boolean isInCycleStartPhase(MCEGame game) {
        if (game == null || game.getTimeline() == null) {
            return false;
        }
        
        var timeline = game.getTimeline();
        var timelineState = timeline.getTimelineState();
        if (timelineState == null || timelineState.isEmpty()) {
            return false;
        }
        
        int currentState = timeline.getCurrentState();
        
        // 如果状态小于2，说明在 onLaunch 或 intro 阶段
        if (currentState < 2) {
            return false;
        }
        
        // 检查是否是最后一个状态（onEnd）
        if (currentState >= timelineState.size() - 1) {
            return false;
        }
        
        // 直接检查当前时间线节点的属性
        // onCycleStart 节点的特征：canSuspend = false，且不是第一个节点（onLaunch）
        // 注意：onLaunch 也是 canSuspend = false，但它是第一个节点，已经被上面的 currentState < 2 排除了
        try {
            var currentNode = timelineState.get(currentState);
            if (currentNode != null && !currentNode.isCanSuspend()) {
                // 当前节点是 canSuspend = false，且不是第一个和最后一个节点
                // 这应该是 onCycleStart 节点
                return true;
            }
        } catch (Exception e) {
            // 如果无法访问节点，回退到原来的逻辑
            // 检查是否在 onCycleStart 阶段：每个回合的第二个状态
            // (currentState - 2) % 3 == 1 表示在 onCycleStart
            int cycleState = currentState - 2;
            return cycleState % 3 == 1;
        }
        
        return false;
    }

    /**
     * 处理玩家退出时的逻辑
     * 只有在游戏进行中（onCycleStart 阶段）退出才判定为死亡
     * 
     * @param game 游戏实例
     * @param player 退出的玩家
     * @param onDeathCallback 死亡时的回调函数（用于处理游戏特定的死亡逻辑，如队伍淘汰检查等）
     */
    public static void handlePlayerQuit(MCEGame game, Player player, Runnable onDeathCallback) {
        // 只有在游戏进行中（onCycleStart 阶段）退出才判定为死亡
        // 在回合准备中、游戏开始前、游戏结束后退出重进都可以继续进行游戏
        if (!isInCycleStartPhase(game)) {
            // 不在游戏进行中，撤销全局处理器的死亡标记，恢复玩家状态
            // scoreboard tags 是持久化的，即使玩家退出也能修改
            // 这样玩家重新加入时可以继续游戏
            player.removeScoreboardTag("dead");
            if (player.getScoreboardTags().contains("Participant")) {
                player.addScoreboardTag("Active");
            }
            // 注意：玩家已退出，无法设置游戏模式，但会在重新加入时由 DefaultGamePlayerJoinHandler 处理
            return;
        }
        
        // 全局处理器 GamePlayerQuitHandler 已经标记玩家为死亡
        // 发送淘汰消息和音效（发送给所有在线玩家）
        String pname = MCEPlayerUtils.getColoredPlayerName(player);
        MCEMessenger.sendGlobalInfo(pname + " <gray>已被淘汰！</gray>");
        MCEPlayerUtils.globalPlaySound("minecraft:player_eliminated");
        
        // 使用延迟任务确保在玩家完全退出后执行死亡处理逻辑
        // 这样可以确保队伍淘汰检查等逻辑能正确执行
        game.setDelayedTask(0.1, () -> {
            if (onDeathCallback != null) {
                onDeathCallback.run();
            }
        });
    }

    /**
     * 检查队伍淘汰：如果该玩家是队伍最后一个存活者，则登记队伍淘汰
     * 
     * @param playerName 玩家名称
     * @param playerTeam 玩家队伍
     * @param teamEliminationOrder 队伍淘汰顺序列表
     * @return 是否触发了队伍淘汰
     */
    public static boolean checkTeamElimination(String playerName, Team playerTeam, List<Team> teamEliminationOrder) {
        if (playerTeam == null || teamEliminationOrder.contains(playerTeam)) {
            return false;
        }
        
        boolean anyAliveSameTeam = false;
        Set<Team> aliveTeamsProbe = new java.util.HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            // 玩家已退出，不需要排除
            if (p.getScoreboardTags().contains("Active") && !p.getScoreboardTags().contains("dead")) {
                Team pt = MCETeamUtils.getTeam(p);
                if (pt != null) {
                    aliveTeamsProbe.add(pt);
                    if (pt.equals(playerTeam))
                        anyAliveSameTeam = true;
                }
            }
        }
        
        if (!anyAliveSameTeam && aliveTeamsProbe.size() >= 1) {
            teamEliminationOrder.add(playerTeam);
            String tname = MCETeamUtils.getTeamColoredName(playerTeam);
            MCEMessenger.sendGlobalInfo(tname + " <gray>已被团灭！</gray>");
            MCEPlayerUtils.globalPlaySound("minecraft:team_eliminated");
            return true;
        }
        
        return false;
    }
}

