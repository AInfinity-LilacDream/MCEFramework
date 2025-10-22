package mcevent.MCEFramework.tools;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import fr.mrmicky.fastboard.adventure.FastBoard;
import java.util.Set;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
MCEPlayerUtils: 工具类，提供修改玩家属性及药水效果的方法
 */
public class MCEPlayerUtils {
    public static void grantGlobalPotionEffect(PotionEffect effect) {
        for (Player player : Bukkit.getOnlinePlayers())
            player.addPotionEffect(effect);
    }

    public static void clearGlobalTags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearTag(player);
        }
    }

    public static void clearTag(Player player) {
        Set<String> playerTags = player.getScoreboardTags();
        for (String tag : playerTags)
            player.removeScoreboardTag(tag);
    }

    public static void globalGrantTag(String tag) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addScoreboardTag(tag);
        }
    }

    public static void globalSetGameMode(GameMode gamemode) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(gamemode);
        }
    }

    public static void globalSetGameModeDelayed(GameMode gamemode, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setGameMode(gamemode);
            }
        }, delayTicks);
    }

    public static void globalClearFastBoard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 创建一个空的 FastBoard 来清除现有的计分板
            FastBoard board = new FastBoard(player);
            board.updateTitle(net.kyori.adventure.text.Component.empty());
            board.updateLines();
            board.delete();
        }
    }

    public static String getColoredPlayerName(Player player) {
        String name = player.getName();
        String coloredName = "";
        Team team = MCETeamUtils.getTeam(player);
        if (team != null) {
            String[] textColor = MCETeamUtils.getTeamColor(team);
            coloredName = textColor[0] + name + textColor[1];
        }
        return coloredName;
    }

    public static void globalHideNameTag() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
    }

    public static void globalChangeTeamNameTag() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            // 仅队友可见名牌：对其他队伍隐藏
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        }
    }

    public static void globalShowNameTag() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
    }

    public static void globalPlaySound(String music) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(
                    player,
                    music,
                    SoundCategory.AMBIENT,
                    1.0f,
                    1.0f);
        }
    }

    public static void globalStopAllSounds() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.stopAllSounds();
        }
    }

    public static void globalStopMusic() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.stopSound(SoundCategory.AMBIENT);
        }
    }

    public static void globalClearInventory() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
        }
    }

    public static void globalClearPotionEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        }
    }
}
