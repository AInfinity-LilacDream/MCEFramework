package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.captureCenter.CaptureCenter;
import mcevent.MCEFramework.games.crazyMiner.CrazyMiner;
import mcevent.MCEFramework.games.discoFever.DiscoFever;
import mcevent.MCEFramework.games.sandRun.SandRun;
import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl;
import mcevent.MCEFramework.games.survivalGame.gameObject.SurvivalGameGameBoard;
import mcevent.MCEFramework.games.tntTag.TNTTag;
import mcevent.MCEFramework.games.underworldGame.UnderworldGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scoreboard.Team;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.Set;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
GlobalEliminationHandler: 统一处理玩家与队伍淘汰的全局监听器
*/
public class GlobalEliminationHandler extends MCEResumableEventHandler implements Listener {

    public GlobalEliminationHandler() {
        setSuspended(false); // 全局常驻
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!MCEMainController.isRunningGame())
            return;

        MCEGame current = MCEMainController.getCurrentRunningGame();
        // DiscoFever / ParkourTag / CrazyMiner 使用自定义淘汰与结算逻辑，不走全局死亡淘汰
        // UnderworldGame 现在使用全局死亡处理
        if (current instanceof DiscoFever
                || current instanceof mcevent.MCEFramework.games.parkourTag.ParkourTag
                || current instanceof CrazyMiner)
            return;

        Player victim = event.getEntity();
        // 非参与者（未持有 Active 或不在游戏世界）完全忽略
        if (current != null && !current.isGameParticipant(victim))
            return;
        // SG 特化：用事件drops创建死亡箱并清空掉落，避免玩家背包被系统先清空
        if (current instanceof SurvivalGame) {
            // 调试：打印玩家背包与事件掉落
            try {
                PlayerInventory inv = victim.getInventory();
                plugin.getLogger().info("[SG][DeathDebug] Player=" + victim.getName());
                ItemStack[] storage = inv.getStorageContents();
                int idx = 0;
                for (ItemStack it : storage) {
                    if (it != null && it.getType() != org.bukkit.Material.AIR) {
                        plugin.getLogger()
                                .info("[SG][DeathDebug] storage[" + idx + "]=" + it.getType() + " x" + it.getAmount());
                    }
                    idx++;
                }
                ItemStack[] armor = inv.getArmorContents();
                int aidx = 0;
                for (ItemStack it : armor) {
                    if (it != null && it.getType() != org.bukkit.Material.AIR) {
                        plugin.getLogger()
                                .info("[SG][DeathDebug] armor[" + aidx + "]=" + it.getType() + " x" + it.getAmount());
                    }
                    aidx++;
                }
                ItemStack off = inv.getItemInOffHand();
                if (off != null && off.getType() != org.bukkit.Material.AIR) {
                    plugin.getLogger().info("[SG][DeathDebug] offhand=" + off.getType() + " x" + off.getAmount());
                }
            } catch (Throwable ignored) {
            }

            java.util.List<ItemStack> snapshot = new java.util.ArrayList<>(event.getDrops());
            event.getDrops().clear();
            org.bukkit.Location chestLoc = victim.getLocation().clone();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    plugin.getLogger().info("[SG][DeathDebug] placing chest with drops=" + snapshot.size());
                    SurvivalGameFuncImpl.createDeathChest(victim, chestLoc, snapshot);
                } catch (Throwable t) {
                    plugin.getLogger().warning("[SG][DeathDebug] createDeathChest failed: " + t.getMessage());
                }
            }, 1L);
        }

        // 统一处理
        eliminateNow(victim);
    }

    public static void eliminateNow(Player victim) {
        if (!MCEMainController.isRunningGame())
            return;
        MCEGame current = MCEMainController.getCurrentRunningGame();

        // 统一标记与旁观：仅标记 dead，不移除 Active（Active 代表本局参赛资格）
        victim.addScoreboardTag("dead");
        victim.setGameMode(GameMode.SPECTATOR);

        // 阴间游戏：清除床/重生锚重生点并设置重生点到游戏世界出生点（防止死亡后重生到主城）
        if (current instanceof UnderworldGame) {
            UnderworldGame underworldGame = (UnderworldGame) current;
            String worldName = underworldGame.getWorldName();
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world != null) {
                org.bukkit.Location spawnLoc = world.getSpawnLocation();
                
                // 关键修复：先清除玩家的床/重生锚重生点，避免 Minecraft 优先使用主城的床/重生锚
                // setRespawnLocation(null) 会清除床/重生锚的重生点
                victim.setRespawnLocation(null);
                
                // 然后设置游戏世界的重生点
                victim.setRespawnLocation(spawnLoc);
                
                plugin.getLogger().info("[UnderworldGame][DeathDebug] 玩家 " + victim.getName() + 
                    " 死亡，已清除床/重生锚重生点，设置重生点到游戏世界: " + worldName + 
                    ", 位置: " + spawnLoc.getX() + "," + spawnLoc.getY() + "," + spawnLoc.getZ() +
                    ", 当前世界: " + (victim.getWorld() != null ? victim.getWorld().getName() : "null"));
            } else {
                plugin.getLogger().warning("[UnderworldGame][DeathDebug] 玩家 " + victim.getName() + 
                    " 死亡，但无法找到游戏世界: " + worldName);
            }
            
            // 更新存活玩家数并刷新展示板
            underworldGame.updateAlivePlayerCount();
            if (underworldGame.getGameBoard() != null) {
                underworldGame.getGameBoard().globalDisplay();
            }
        }

        // 玩家淘汰提示与音效
        String pname = MCEPlayerUtils.getColoredPlayerName(victim);
        MCEMessenger.sendGlobalInfo(pname + " <gray>已被淘汰！</gray>");
        MCEPlayerUtils.globalPlaySound("minecraft:player_eliminated");

        // 若是饥饿游戏，记录击杀并更新展示板计数
        if (current instanceof SurvivalGame sg) {
            Player killer = victim.getKiller();
            if (killer != null) {
                SurvivalGameFuncImpl.registerKill(killer);
            }
            if (sg.getGameBoard() instanceof SurvivalGameGameBoard board) {
                Team team = MCETeamUtils.getTeam(victim);
                if (team != null) {
                    board.updateTeamRemainTitle(team);
                }
                int remaining = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getGameMode() != GameMode.SPECTATOR)
                        remaining++;
                }
                board.updatePlayerRemainTitle(remaining);
            }
        }
        // 暗矢狂潮使用自身死亡监听器结算积分与淘汰流程，这里不做任何处理

        // 队伍团灭检测（TNTTag 不参与队伍团灭判定）
        if (!(current instanceof TNTTag)) {
            Team vteam = MCETeamUtils.getTeam(victim);
            if (vteam != null) {
                boolean anyAlive = false;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Team pt = MCETeamUtils.getTeam(p);
                    if (pt != null && vteam != null && java.util.Objects.equals(pt.getName(), vteam.getName())
                            && p.getGameMode() != GameMode.SPECTATOR) {
                        anyAlive = true;
                        break;
                    }
                }
                if (!anyAlive) {
                    String tname = MCETeamUtils.getTeamColoredName(vteam);
                    MCEMessenger.sendGlobalInfo(tname + " <gray>已被团灭！</gray>");
                    MCEPlayerUtils.globalPlaySound("minecraft:team_eliminated");
                    if (current instanceof SurvivalGame) {
                        SurvivalGameFuncImpl.registerTeamElimination(vteam);
                    }
                }
            }
        }

        // 每次淘汰后评估是否应当结束当前回合
        evaluateRoundEnd(current);
    }

    private static void evaluateRoundEnd(MCEGame current) {
        if (current == null)
            return;

        // 模式一：只剩一队结束（由各自游戏控制是否在此处推进）
        // 注意：ExtractOwn 自行处理回合结束与存活分统计，这里不推进
        if (current instanceof CaptureCenter
                || current instanceof SurvivalGame
                || current instanceof UnderworldGame) { // UnderworldGame 使用全局死亡处理，只剩一队时结束
            if (countAliveTeams() <= 1) {
                current.getTimeline().nextState();
            }
            return;
        }

        // 模式二：所有人都死了才结束（无存活玩家）
        // 注意：DiscoFever 改为自身坠落监听，不使用全局死亡监听淘汰，但仍沿用此处的回合结束评估
        if (current instanceof DiscoFever || current instanceof SandRun) {
            if (countAlivePlayers() == 0) {
                current.getTimeline().nextState();
            }
            return;
        }

        // 模式三：只剩一个人结束
        if (current instanceof TNTTag) {
            if (countAlivePlayers() <= 1) {
                current.getTimeline().nextState();
            }
        }
    }

    private static int countAlivePlayers() {
        int alive = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SPECTATOR)
                alive++;
        }
        return alive;
    }

    private static int countAliveTeams() {
        Set<Team> aliveTeams = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                Team t = MCETeamUtils.getTeam(p);
                if (t != null)
                    aliveTeams.add(t);
            }
        }
        return aliveTeams.size();
    }
}
