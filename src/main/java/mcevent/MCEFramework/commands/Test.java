package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;

/*
Test: 测试命令
usage: /test setTeamGlowing [on|off]
 */
@CommandAlias("test")
@CommandPermission("test.use")
public class Test extends BaseCommand {
    
    // 记录每个队伍的发光状态
    private static final Map<String, Boolean> teamGlowingStates = new HashMap<>();
    
    @Subcommand("setTeamGlowing")
    @CommandPermission("test.setTeamGlowing")
    public void onSetTeamGlowing(CommandSender sender, @Optional String state) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令只能由玩家执行！");
            return;
        }
        
        // 获取玩家所在的队伍
        Team team = MCETeamUtils.getTeam(player);
        if (team == null) {
            MCEMessenger.sendInfoToPlayer("<red>你不在任何队伍中！</red>", player);
            return;
        }
        
        // 确定要设置的状态
        boolean glowing;
        if (state == null) {
            // 如果没有指定状态，则切换状态
            boolean currentState = teamGlowingStates.getOrDefault(team.getName(), false);
            glowing = !currentState;
        } else if (state.equalsIgnoreCase("on") || state.equalsIgnoreCase("true") || state.equalsIgnoreCase("enable")) {
            glowing = true;
        } else if (state.equalsIgnoreCase("off") || state.equalsIgnoreCase("false") || state.equalsIgnoreCase("disable")) {
            glowing = false;
        } else {
            MCEMessenger.sendInfoToPlayer("<red>无效的参数！使用: /test setTeamGlowing [on|off]</red>", player);
            return;
        }
        
        // 设置队伍发光效果
        MCETeamUtils.setTeamGlowing(team, glowing);
        teamGlowingStates.put(team.getName(), glowing);
        
        // 发送反馈消息
        String status = glowing ? "<green>开启</green>" : "<red>关闭</red>";
        MCEMessenger.sendInfoToPlayer("<yellow>队伍发光效果已" + status + "！</yellow>", player);
        
        // 通知队伍内所有玩家
        for (Player teamPlayer : MCETeamUtils.getPlayers(team)) {
            if (!teamPlayer.equals(player)) {
                MCEMessenger.sendInfoToPlayer("<yellow>队伍发光效果已" + status + "！</yellow>", teamPlayer);
            }
        }
    }
}

