package mcevent.MCEFramework.tools;

import mcevent.MCEFramework.miscellaneous.Constants;
import mcevent.MCEFramework.miscellaneous.TeamWithDetails;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
MCETeamUtils: 获取队伍信息以及操作队伍的工具类
 */
public class MCETeamUtils {

    // 获得当前队伍数量
    public static int getActiveTeamCount() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        int activeTeamsCount = 0;

        for (Team team : scoreboard.getTeams()) {
            if (!team.getEntries().isEmpty()) {
                activeTeamsCount++;
            }
        }

        return activeTeamsCount;
    }

    // 获得当前队伍列表
    public static ArrayList<Team> getActiveTeams() {
        ArrayList<Team> teams = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            if (!teams.contains(team))
                teams.add(team);
        }

        return teams;
    }

    public static ArrayList<Team> rotateTeam(ArrayList<Team> teams) {
        ArrayList<Team> newTeams = new ArrayList<>();
        newTeams.add(teams.getFirst());
        newTeams.add(teams.getLast());
        for (int i = 1; i < teams.size() - 1; i++)
            newTeams.add(teams.get(i));
        return newTeams;
    }

    // 获得当前队伍的玩家列表
    public static ArrayList<Player> getPlayers(Team team) {
        ArrayList<Player> players = new ArrayList<>();
        if (team == null)
            return players;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.getEntries().contains(player.getName())) {
                players.add(player);
            }
        }

        return players;
    }

    // 获得某个玩家所在的队伍
    public static Team getTeam(Player player) {
        for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
            if (team.getEntries().contains(player.getName())) {
                return team;
            }
        }

        return null; // never reached
    }

    public static String[] getTeamColor(Team team) {
        for (TeamWithDetails teamWithDetails : Constants.teams) {
            if (Objects.equals(teamWithDetails.teamName(), team.getName())) {
                return new String[] {
                        teamWithDetails.textColorPre(),
                        teamWithDetails.textColorPost()
                };
            }
        }
        return new String[] { "<black>", "</black>" };
    }

    public static String getUncoloredTeamName(Team team) {
        for (TeamWithDetails teamWithDetails : Constants.teams) {
            if (Objects.equals(teamWithDetails.teamName(), team.getName())) {
                return "<gray>" + teamWithDetails.teamNameNoColor() + "</gray>";
            }
        }
        return team.getName();
    }

    public static String getTeamColoredName(Team team) {
        for (TeamWithDetails teamWithDetails : Constants.teams) {
            if (Objects.equals(teamWithDetails.teamName(), team.getName())) {
                return teamWithDetails.textColorPre() + teamWithDetails.teamNameNoColor()
                        + teamWithDetails.textColorPost();
            }
        }
        return team.getName();
    }

    // 返回队伍中在线玩家的数量
    public static int getTeamOnlinePlayers(Team team) {
        int players = 0;

        for (Player player : Bukkit.getOnlinePlayers())
            if (team.getEntries().contains(player.getName()))
                players++;

        return players;
    }

    // 友伤系统控制方法
    public static void enableFriendlyFire() {
        mcevent.MCEFramework.MCEMainController.getFriendlyFireHandler().suspend(); // suspend=true 激活友伤
    }

    public static void disableFriendlyFire() {
        mcevent.MCEFramework.MCEMainController.getFriendlyFireHandler().start(); // suspended=false 关闭友伤
    }

    public static boolean isFriendlyFireEnabled() {
        return mcevent.MCEFramework.MCEMainController.getFriendlyFireHandler().isSuspended(); // suspended=true 代表友伤启用
    }

    // ========== 队伍发光效果工具方法（使用 NMS） ==========
    
    /**
     * 使用 NMS 为指定玩家设置发光效果（对特定观察者可见）
     * 根据 SpigotMC 论坛的正确实现方式
     * @param target 目标玩家（要发光的玩家）
     * @param viewer 观察者（能看到发光的玩家）
     * @param glowing 是否发光
     */
    public static void setPlayerGlowingNMS(Player target, Player viewer, boolean glowing) {
        if (target == null || viewer == null) {
            return;
        }
        
        try {
            // 获取 NMS 玩家对象
            CraftPlayer craftTarget = (CraftPlayer) target;
            CraftPlayer craftViewer = (CraftPlayer) viewer;
            ServerPlayer nmsTarget = craftTarget.getHandle();
            ServerPlayer nmsViewer = craftViewer.getHandle();
            
            // 设置发光字节值（0x40 = 第6位，表示发光）
            byte glowingByte = glowing ? (byte) 0x40 : (byte) 0x00;
            
            // 创建数据值列表
            List<SynchedEntityData.DataValue<?>> eData = new ArrayList<>();
            
            // 使用 DataValue.create() 方法创建数据值
            // 实体标志在索引 0，使用 BYTE 序列化器
            eData.add(SynchedEntityData.DataValue.create(
                new EntityDataAccessor<>(0, EntityDataSerializers.BYTE),
                glowingByte
            ));
            
            // 创建并发送实体元数据包
            ClientboundSetEntityDataPacket metadata = new ClientboundSetEntityDataPacket(
                nmsTarget.getId(),
                eData
            );
            
            // 检查连接是否有效
            if (nmsViewer.connection == null) {
                return;
            }
            
            // 发送数据包给观察者
            nmsViewer.connection.send(metadata);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 为队伍设置发光效果（队伍内的玩家可以看到队友发光）
     * @param team 目标队伍
     * @param glowing 是否发光
     */
    public static void setTeamGlowing(Team team, boolean glowing) {
        if (team == null) return;
        
        List<Player> teamPlayers = getPlayers(team);
        
        // 为队伍内的每个玩家设置发光效果
        // 让队伍内的每个玩家都能看到其他队友发光
        for (Player target : teamPlayers) {
            for (Player viewer : teamPlayers) {
                if (!target.equals(viewer)) {
                    setPlayerGlowingNMS(target, viewer, glowing);
                }
            }
        }
    }
    
    /**
     * 清除所有队伍的发光效果
     */
    public static void clearAllTeamGlowing() {
        for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
            setTeamGlowing(team, false);
        }
    }
}
