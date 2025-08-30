package mcevent.MCEFramework.tools;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import java.util.Set;

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
                    1.0f
            );
        }
    }

    public static void globalStopAllSounds() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.stopAllSounds();
        }
    }

    public static void globalStopMusic() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.stopSound(SoundCategory.MUSIC);
        }
    }
}
