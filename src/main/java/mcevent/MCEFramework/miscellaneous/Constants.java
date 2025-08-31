package mcevent.MCEFramework.miscellaneous;

import mcevent.MCEFramework.games.captureCenter.CaptureCenter;
import mcevent.MCEFramework.games.discoFever.DiscoFever;
import mcevent.MCEFramework.games.football.Football;
import mcevent.MCEFramework.games.musicDodge.MusicDodge;
import mcevent.MCEFramework.games.parkourTag.ParkourTag;
import mcevent.MCEFramework.games.sandRun.SandRun;
import mcevent.MCEFramework.games.votingSystem.VotingSystem;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/*
Constants: 存放全局静态常量
 */
public class Constants {
    public static final TeamWithDetails[] teams = {
            new TeamWithDetails("<red>红色山楂</red>", "红色山楂", "红队", NamedTextColor.RED, "<red>", "</red>"),
            new TeamWithDetails("<gold>橙色柑橘</gold>", "橙色柑橘", "橙队", NamedTextColor.GOLD, "<gold>", "</gold>"),
            new TeamWithDetails("<yellow>黄色香蕉</yellow>", "黄色香蕉", "黄队", NamedTextColor.YELLOW, "<yellow>", "</yellow>"),
            new TeamWithDetails("<green>翠色酸橙</green>", "翠色酸橙", "翠队", NamedTextColor.GREEN, "<green>", "</green>"),
            new TeamWithDetails("<dark_green>绿色鳄梨</dark_green>", "绿色鳄梨", "绿队", NamedTextColor.DARK_GREEN, "<dark_green>", "</dark_green>"),
            new TeamWithDetails("<aqua>青色柠檬</aqua>", "青色柠檬", "青队", NamedTextColor.AQUA, "<aqua>", "</aqua>"),
            new TeamWithDetails("<dark_aqua>缥色莓果</dark_aqua>", "缥色莓果", "缥队", NamedTextColor.DARK_AQUA, "<dark_aqua>", "</dark_aqua>"),
            new TeamWithDetails("<dark_blue>蓝色葡萄</dark_blue>", "蓝色葡萄", "蓝队", NamedTextColor.DARK_BLUE, "<dark_blue>", "</dark_blue>"),
            new TeamWithDetails("<dark_purple>紫色杏李</dark_purple>", "紫色杏李", "紫队", NamedTextColor.DARK_PURPLE, "<dark_purple>", "</dark_purple>"),
            new TeamWithDetails("<light_purple>粉色石榴</light_purple>", "粉色石榴", "粉队", NamedTextColor.LIGHT_PURPLE, "<light_purple>", "</light_purple>"),
    };

    public static Plugin plugin = Bukkit.getPluginManager().getPlugin("MCEFramework"); // 全局的插件实例

    public final static int MAX_TEAM_COUNT = 10;

    // 游戏地图名称
    public static String[] mapNames = new String[] {
            "pkt_concrete",
            "discofever_classic",
            "musicdodge_classic",
            "sand_run_classic",
            "capture_classic",
            "football_field",
            "lobby", // 投票系统使用主城
    };

    // 全局的游戏单例
    public static ParkourTag pkt;
    public static DiscoFever discoFever;
    public static MusicDodge musicDodge;
    public static SandRun sandRun;
    public static CaptureCenter captureCenter;
    public static Football football;
    public static VotingSystem votingSystem;

    // ParkourTag位置现在从配置文件动态获取，不再使用硬编码

    // DiscoFever平台位置现在动态获取，不再使用硬编码地图名称
    public static Location getDiscoFeverPlatformLocation(String worldName) {
        return new Location(Bukkit.getWorld(worldName), 4, 6, 0);
    }

    public static PotionEffect saturation = new PotionEffect(
            PotionEffectType.SATURATION,
            Integer.MAX_VALUE,
            255,
            true,
            false,
            false
    );
}
