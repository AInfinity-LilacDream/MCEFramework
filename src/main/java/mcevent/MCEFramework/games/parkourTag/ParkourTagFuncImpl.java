package mcevent.MCEFramework.games.parkourTag;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCETeamUtils.*;
import static mcevent.MCEFramework.tools.MCEWorldUtils.teleportLocation;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import java.util.ArrayList;

/*
ParkourTagFuncImpl: 封装ParkourTag游戏逻辑函数
 */
public class ParkourTagFuncImpl {

    // 清空提前结束的对局数量以及结束时间
    protected static void clearMatchCompleteState() {
        pkt.completeMatchesTot = 0;
        for (int i = 0; i < pkt.getActiveTeams().size(); ++i)
            pkt.completeTime[i] = 0;
    }

    // 重置队伍存活玩家数
    protected static void resetSurvivePlayerTot() {
        pkt.survivePlayerTot.clear();
        for (int i = 0; i < pkt.getActiveTeams().size(); i++) {
            // PKT游戏中每个队伍都有一个抓人者，所以剩余玩家数 = 队伍总人数 - 1
            int totalPlayers = MCETeamUtils.getTeamOnlinePlayers(pkt.getActiveTeams().get(i));
            int survivePlayers = Math.max(0, totalPlayers - 1); // 确保不会是负数
            pkt.survivePlayerTot.add(survivePlayers);
        }
    }

    // 全局发送回合对阵标题
    protected static void sendCurrentRoundMatchTitle() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null) {
                continue; // 跳过没有队伍的玩家
            }
            Team opponentTeam = pkt.getOpponentTeam(team);
            if (opponentTeam != null) {
                MCEMessenger.sendMatchTitleToPlayer(player, team.getName(), opponentTeam.getName(),
                        pkt.getCurrentRound());
            }
        }
    }

    // 传送玩家到开始房间
    protected static void globalTeleportToChoiceRoom(ParkourTagConfigParser config) {
        int teamTotal = MCETeamUtils.getActiveTeamCount();
        for (int i = 0; i < teamTotal; i += 2) {
            int offset = i / 2;
            Team team1 = pkt.getActiveTeams().get(i);
            Team team2 = pkt.getActiveTeams().get(i + 1);

            ArrayList<Player> team1Players = MCETeamUtils.getPlayers(team1);
            ArrayList<Player> team2Players = MCETeamUtils.getPlayers(team2);

            Location pktTeamLocation1 = config.getLocation("pktTeamLocation1");
            Location pktTeamLocation2 = config.getLocation("pktTeamLocation2");
            Location pktOffset = config.getLocation("pktOffset");

            for (Player player : team1Players)
                player.teleport(teleportLocation(pktTeamLocation1, pktOffset, offset));
            for (Player player : team2Players)
                player.teleport(teleportLocation(pktTeamLocation2, pktOffset, offset));
        }
    }

    // 重置选择抓捕者房间
    protected static void resetChoiceRoom(ParkourTagConfigParser config) {
        Location pktDoorLocation1down = config.getLocation("pktDoorLocation1down");
        Location pktDoorLocation1up = config.getLocation("pktDoorLocation1up");
        Location pktDoorLocation2down = config.getLocation("pktDoorLocation2down");
        Location pktDoorLocation2up = config.getLocation("pktDoorLocation2up");
        Location pktOffset = config.getLocation("pktOffset");

        for (int offset = 0; offset < 20; ++offset) {
            Location loc1Down = MCEWorldUtils.teleportLocation(pktDoorLocation1down, pktOffset, offset);
            Location loc1Up = MCEWorldUtils.teleportLocation(pktDoorLocation1up, pktOffset, offset);
            Location loc2Down = MCEWorldUtils.teleportLocation(pktDoorLocation2down, pktOffset, offset);
            Location loc2Up = MCEWorldUtils.teleportLocation(pktDoorLocation2up, pktOffset, offset);

            Block block1Down = loc1Down.getBlock();
            Block block1Up = loc1Up.getBlock();
            Block block2Down = loc2Down.getBlock();
            Block block2Up = loc2Up.getBlock();

            block1Down.setType(Material.AIR);
            block1Up.setType(Material.AIR);
            block2Down.setType(Material.AIR);
            block2Up.setType(Material.AIR);
        }
    }

    // 传送玩家到比赛场地并且开始游戏
    protected static void globalTeleportToStadium(ParkourTagConfigParser config) {
        Location pktChaserStartLocation = config.getLocation("pktChaserStartLocation");
        Location pktRunnerStartLocation = config.getLocation("pktRunnerStartLocation");
        Location pktOffset = config.getLocation("pktOffset");

        int teamTotal = MCETeamUtils.getActiveTeamCount();
        for (int i = 0; i < teamTotal; i += 2) {
            Team team1 = pkt.getActiveTeams().get(i);
            Team team2 = pkt.getActiveTeams().get(i + 1);

            ArrayList<Player> team1Players = MCETeamUtils.getPlayers(team1);
            ArrayList<Player> team2Players = MCETeamUtils.getPlayers(team2);

            for (Player player : team1Players) {
                if (player.getScoreboardTags().contains("chaser"))
                    player.teleport(teleportLocation(pktChaserStartLocation, pktOffset, i + 1));
                else
                    player.teleport(teleportLocation(pktRunnerStartLocation, pktOffset, i));
            }

            for (Player player : team2Players) {
                if (player.getScoreboardTags().contains("chaser"))
                    player.teleport(teleportLocation(pktChaserStartLocation, pktOffset, i));
                else
                    player.teleport(teleportLocation(pktRunnerStartLocation, pktOffset, i + 1));
            }
        }
    }

    // 确保每个队伍有且只有一个抓捕者；若无则随机指定，并提示双方队伍
    protected static void ensureChasersSelectedAndNotify() {
        int teamTotal = MCETeamUtils.getActiveTeamCount();
        for (int i = 0; i < teamTotal; i += 2) {
            Team team1 = pkt.getActiveTeams().get(i);
            Team team2 = pkt.getActiveTeams().get(i + 1);

            Player team1Chaser = MCETeamUtils.getPlayers(team1).stream()
                    .filter(p -> p.getScoreboardTags().contains("chaser")).findFirst().orElse(null);
            Player team2Chaser = MCETeamUtils.getPlayers(team2).stream()
                    .filter(p -> p.getScoreboardTags().contains("chaser")).findFirst().orElse(null);

            if (team1Chaser == null) {
                ArrayList<Player> t1Players = MCETeamUtils.getPlayers(team1);
                if (!t1Players.isEmpty()) {
                    Player selected = t1Players.get((int) (Math.random() * t1Players.size()));
                    // 仅清理PKT角色相关标签，保留 Participant/Active 等全局标签
                    selected.removeScoreboardTag("chaser");
                    selected.removeScoreboardTag("runner");
                    selected.removeScoreboardTag("caught");
                    selected.removeScoreboardTag("dead");
                    selected.addScoreboardTag("chaser");
                    MCEMessenger.sendInfoToTeam(team1,
                            "你们未选择抓捕者，系统已随机指派 " + MCEPlayerUtils.getColoredPlayerName(selected));
                    MCEMessenger.sendInfoToTeam(team2,
                            team1.getName() + " 未选择抓捕者，系统已指派 " + MCEPlayerUtils.getColoredPlayerName(selected));
                }
            }

            if (team2Chaser == null) {
                ArrayList<Player> t2Players = MCETeamUtils.getPlayers(team2);
                if (!t2Players.isEmpty()) {
                    Player selected = t2Players.get((int) (Math.random() * t2Players.size()));
                    // 仅清理PKT角色相关标签，保留 Participant/Active 等全局标签
                    selected.removeScoreboardTag("chaser");
                    selected.removeScoreboardTag("runner");
                    selected.removeScoreboardTag("caught");
                    selected.removeScoreboardTag("dead");
                    selected.addScoreboardTag("chaser");
                    MCEMessenger.sendInfoToTeam(team2,
                            "你们未选择抓捕者，系统已随机指派 " + MCEPlayerUtils.getColoredPlayerName(selected));
                    MCEMessenger.sendInfoToTeam(team1,
                            team2.getName() + " 未选择抓捕者，系统已指派 " + MCEPlayerUtils.getColoredPlayerName(selected));
                }
            }
        }
    }

    // 发送当前回合对局结果信息
    protected static void sendCurrentMatchState() {
        MCEMessenger.sendGlobalTitle("<red><bold>游戏结束！</bold></red>", null);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("chaser") &&
                    pkt.getTeamCompleteTime(MCETeamUtils.getTeam(player)) == 0) {
                MCEMessenger.sendInfoToPlayer("<red>[⚠] 你没有抓完所有猎物TAT</red>", player);
            }
        }

        MCETimerUtils.setDelayedTask(5, () -> MCEMessenger.sendGlobalText("本回合胜负情况："));
        MCETimerUtils.setDelayedTask(10, () -> {
            MCEMessenger.sendGlobalText("<newline>");
            for (int i = 0; i < getActiveTeams().size(); i += 2) {
                Team team1 = getActiveTeams().get(i);
                Team team2 = getActiveTeams().get(i + 1);

                int team1time = pkt.getTeamCompleteTime(team1);
                int team2time = pkt.getTeamCompleteTime(team2);

                MCEMessenger.sendGlobalText("     " +
                        ((team1time > team2time) ? MCETeamUtils.getUncoloredTeamName(team1) : team1.getName()) +
                        " VS " +
                        ((team1time > team2time) ? team2.getName() : MCETeamUtils.getUncoloredTeamName(team2)) +
                        "<newline>");
            }
        });
        MCETimerUtils.setDelayedTask(15, () -> MCEMessenger.sendGlobalText("本回合存活队伍："));
        MCETimerUtils.setDelayedTask(20, () -> {
            MCEMessenger.sendGlobalText("<newline>");
            for (int i = 0; i < getActiveTeams().size(); ++i)
                if (pkt.getSurvivePlayerTot().get(i) > 0)
                    MCEMessenger.sendGlobalText("     " + getActiveTeams().get(i).getName() + "<newline>");
        });

        MCEMainController.setRunningGame(false);
    }

    // 开启对方逃脱者发光
    protected static void EnableTeamGlowing() {

    }

    // 关闭对方逃脱者发光
    protected static void DisableTeamGlowing() {
    }
}
