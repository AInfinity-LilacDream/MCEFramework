package mcevent.MCEFramework.games.spleef;

import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;

import static mcevent.MCEFramework.miscellaneous.Constants.spleef;

/*
SpleefFuncImpl: 冰雪掘战游戏逻辑实现
*/
public class SpleefFuncImpl {

    private static final Random random = new Random();
    
    /**
     * 处理雪块破坏事件
     */
    public static void handleSnowBreak(Player player, Material brokenBlock) {
        if (!player.getScoreboardTags().contains("Active") || 
            player.getScoreboardTags().contains("dead")) {
            return;
        }
        
        // 只有雪块和雪层可以给予雪球
        if (brokenBlock == Material.SNOW_BLOCK || brokenBlock == Material.SNOW) {
            int snowballAmount = getSnowballAmount(brokenBlock);
            spleef.addPlayerSnowballs(player, snowballAmount);
        }
    }
    
    /**
     * 根据破坏的方块类型决定雪球数量
     */
    private static int getSnowballAmount(Material material) {
        return switch (material) {
            case SNOW_BLOCK -> 4; // 固定4个雪球
            case SNOW -> 2; // 固定2个雪球  
            default -> 0;
        };
    }

    /**
     * 检查玩家是否有雪球
     */
    private static boolean hasSnowballs(Player player) {
        return player.getInventory().contains(Material.SNOWBALL);
    }

    /**
     * 投掷雪球
     */
    private static void throwSnowball(Player player) {
        // 创建雪球实体
        Snowball snowball = player.launchProjectile(Snowball.class);

        // 设置雪球速度
        Vector direction = player.getLocation().getDirection();
        snowball.setVelocity(direction.multiply(1.5)); // 增加投掷速度

        // 设置自定义标记，用于识别这是玩家投掷的雪球
        snowball.setCustomName("spleef_snowball:" + player.getName());
    }

    /**
     * 消耗一个雪球
     */
    private static void consumeSnowball(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.SNOWBALL) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                break;
            }
        }
        player.updateInventory();
    }
    
    
    
    /**
     * 处理玩家掉落事件
     */
    public static void handlePlayerFall(Player player) {
        // PlayerFallHandler已经检查过Y坐标，直接调用处理方法
        spleef.handlePlayerFall(player);
    }
    
    /**
     * 获取存活队伍数量
     */
    public static int getSurvivingTeamCount() {
        Set<Team> aliveTeams = new HashSet<>();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 检查玩家是否存活：有Active标签且没有dead标签
            // 不限制游戏模式，因为在不同阶段玩家模式会变化
            if (player.getScoreboardTags().contains("Active") && 
                !player.getScoreboardTags().contains("dead")) {
                Team playerTeam = MCETeamUtils.getTeam(player);
                if (playerTeam != null) {
                    aliveTeams.add(playerTeam);
                }
            }
        }
        
        return aliveTeams.size();
    }
    
    /**
     * 获取存活玩家数量
     */
    public static int getSurvivingPlayerCount() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 检查玩家是否存活：有Active标签且没有dead标签
            // 不限制游戏模式，因为在不同阶段玩家模式会变化
            if (player.getScoreboardTags().contains("Active") && 
                !player.getScoreboardTags().contains("dead")) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 检查游戏是否应该结束
     */
    public static boolean shouldGameEnd() {
        return getSurvivingTeamCount() <= 1;
    }
    
    /**
     * 获取获胜队伍
     */
    public static Team getWinningTeam() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL && 
                player.getScoreboardTags().contains("Active") && 
                !player.getScoreboardTags().contains("dead")) {
                return MCETeamUtils.getTeam(player);
            }
        }
        return null;
    }
}