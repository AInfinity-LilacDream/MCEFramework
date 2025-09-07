package mcevent.MCEFramework.tools;

import mcevent.MCEFramework.miscellaneous.Constants;
import mcevent.MCEFramework.miscellaneous.IntroTexts;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;

import static mcevent.MCEFramework.miscellaneous.IntroTexts.blankLine;
import static mcevent.MCEFramework.miscellaneous.IntroTexts.divider;

/*
MCEMessenger: 向玩家发送信息的工具类
 */
public class MCEMessenger {

    public static void sendMatchTitleToPlayer(Player player, String ownTeamName, String opponentTeamName, int currentRound) {
        sendTitleToPlayer(player, "第" + currentRound + "回合", ownTeamName + " VS " + opponentTeamName);
    }

    public static void sendGlobalText(String message) {
        Component parsed = MiniMessage.miniMessage().deserialize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(parsed);
        }
    }

    public static void sendGlobalInfo(String message) {
        String infoStr = "<blue>[Info]</blue> ";
        message = infoStr + message;
        Component parsed = MiniMessage.miniMessage().deserialize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(parsed);
        }
    }

    public static void sendIntroText(String gameName, ArrayList<Component> introTexts) {
        MCEMessenger.sendGlobalTitle("<gradient:red:gold:yellow>" + gameName + "</gradient>", null);

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < introTexts.size(); i++) {
                Component text = introTexts.get(i);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage(divider);
                        player.sendMessage(blankLine);
                        player.sendMessage(text);
                        player.sendMessage(blankLine);
                        player.sendMessage(divider);
                    }
                }.runTaskLater(Constants.plugin, 20L * 10 * i + 10L * 10);
            }
        }
    }

    public static void sendInfoToPlayer(String message, Player player) {
        String infoStr = "<blue>[Info]</blue> ";
        message = infoStr + message;
        Component parsed = MiniMessage.miniMessage().deserialize(message);
        player.sendMessage(parsed);
    }

    private static Title parseTitle(String titleText, String subtitleText) {
        if (titleText == null) titleText = "";
        if (subtitleText == null) subtitleText = "";

        // 解析 MiniMessage 格式的文本（支持颜色、渐变等）
        var title = MiniMessage.miniMessage().deserialize(titleText);
        var subtitle = MiniMessage.miniMessage().deserialize(subtitleText);

        // 创建 Title 对象（可自定义淡入、停留、淡出时间）
        Title.Times times = Title.Times.times(
                java.time.Duration.ofMillis(150), // 淡入时间
                java.time.Duration.ofMillis(7000), // 停留时间
                java.time.Duration.ofMillis(500)  // 淡出时间
        );

        return Title.title(title, subtitle, times);
    }

    public static void sendGlobalTitle(String titleText, String subtitleText) {
        Title titleObj = parseTitle(titleText, subtitleText);
        for (Player player : Bukkit.getOnlinePlayers())
            player.showTitle(titleObj);
    }

    public static void sendTitleToPlayer(Player player, String titleText, String subtitleText) {
        Title titleObj = parseTitle(titleText, subtitleText);
        player.showTitle(titleObj);
    }

    // 向指定队伍的所有玩家发送消息
    public static void sendInfoToTeam(Team team, String message) {
        ArrayList<Player> players = MCETeamUtils.getPlayers(team);
        for (int i = 0; i < players.size(); i++) {
            sendInfoToPlayer(message, players.get(i));
        }
    }

    // 向所有玩家发送一个游戏开始倒计时
    public static void sendGlobalCountdown(int seconds, String hintMessage) {
        BukkitRunnable task;

        task = new BukkitRunnable() {
            int counter = seconds;

            @Override
            public void run() {
                counter--;

                switch (counter) {
                    case 3:
                        sendGlobalTitle(hintMessage, "<green>>" + counter + "<</green>");
                        break;
                    case 2:
                        sendGlobalTitle(hintMessage, "<yellow>>" + counter + "<</yellow>");
                        break;
                    case 1:
                        sendGlobalTitle(hintMessage, "<red>>" + counter + "<</red>");
                        this.cancel();
                        break;
                    default:
                        sendGlobalTitle(hintMessage, ">" + counter + "<");
                }
            }
        };
        task.runTaskTimer(Constants.plugin, 0L, 20L);
    }

    // 向所有玩家发送动作栏信息
    public static void sendGlobalActionBarMessage(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendActionBarMessageToPlayer(player, message);
        }
    }

    // 向指定玩家发送动作栏信息
    public static void sendActionBarMessageToPlayer(Player player, String message) {
        Component parsed = MiniMessage.miniMessage().deserialize(message);
        player.sendActionBar(parsed);
    }
}
