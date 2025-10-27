package mcevent.MCEFramework.games.discoFever.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.discoFever.DiscoFever;
import mcevent.MCEFramework.games.discoFever.gameObject.DiscoFeverGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
PlayerFallHandler: DiscoFever 专用坠落监听器
准备阶段：Y<=3 传送回世界出生点
进行阶段：Y<=3 视为淘汰，执行与全局一致的淘汰、团灭检测与展示板更新
*/
public class PlayerFallHandler extends MCEResumableEventHandler implements Listener {

    private DiscoFever discoFever;

    // 阶段标志：true 准备期；false 进行期
    private boolean preparationPhase = true;

    public void register(DiscoFever game) {
        this.discoFever = game;
        setSuspended(true);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setPreparationPhase(boolean preparation) {
        this.preparationPhase = preparation;
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
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isSuspended())
            return;
        if (!MCEMainController.isRunningGame())
            return;
        MCEGame current = MCEMainController.getCurrentRunningGame();
        if (!(current instanceof DiscoFever))
            return;

        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals(discoFever.getWorldName()))
            return;

        double y = player.getLocation().getY();
        if (y > 3.0)
            return;

        if (preparationPhase) {
            World world = Bukkit.getWorld(discoFever.getWorldName());
            if (world == null)
                return;
            Location spawn = world.getSpawnLocation();
            player.teleport(spawn);
            return;
        }

        // 进行期：仅对参与者（Participant）生效
        if (!player.getScoreboardTags().contains("Participant") || player.getScoreboardTags().contains("dead"))
            return;

        // 标记淘汰并旁观
        player.addScoreboardTag("dead");
        player.removeScoreboardTag("Active");
        player.setGameMode(GameMode.SPECTATOR);

        String pname = MCEPlayerUtils.getColoredPlayerName(player);
        MCEMessenger.sendGlobalInfo(pname + " <gray>已被淘汰！</gray>");
        MCEPlayerUtils.globalPlaySound("minecraft:player_eliminated");

        // 更新展示板：玩家与队伍剩余
        if (discoFever.getGameBoard() instanceof DiscoFeverGameBoard board) {
            Team team = MCETeamUtils.getTeam(player);
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

        // 团灭检测
        Team vteam = MCETeamUtils.getTeam(player);
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
            }
        }

        // 全局结束检测：仅当无存活玩家时，提前结束本局
        int alivePlayers = 0;
        java.util.Set<String> aliveTeamNames = new java.util.HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(discoFever.getWorldName()))
                continue;
            if (!p.getScoreboardTags().contains("Active") || p.getScoreboardTags().contains("dead"))
                continue;
            if (p.getGameMode() == GameMode.SPECTATOR)
                continue;
            alivePlayers++;
            Team pt = MCETeamUtils.getTeam(p);
            if (pt != null)
                aliveTeamNames.add(pt.getName());
        }
        if (alivePlayers == 0) {
            try {
                // 停止后续平台调度
                discoFever.clearBossBarTask();
            } catch (Throwable ignored) {
            }
            // 跳转到时间线下一阶段（onEnd）
            if (discoFever.getTimeline() != null) {
                discoFever.getTimeline().nextState();
            }
        }
    }
}
