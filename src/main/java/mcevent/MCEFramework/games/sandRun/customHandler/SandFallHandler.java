package mcevent.MCEFramework.games.sandRun.customHandler;

import mcevent.MCEFramework.games.sandRun.SandRunFuncImpl;
import mcevent.MCEFramework.miscellaneous.TeamWithDetails;
import mcevent.MCEFramework.tools.MCETeamUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;

import static mcevent.MCEFramework.miscellaneous.Constants.teams;
import static mcevent.MCEFramework.miscellaneous.Constants.plugin;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class SandFallHandler implements Listener {
    
    private BukkitTask sandFallTask;
    private final Map<String, Material> teamColorMap = new HashMap<>();
    private long sandFallInterval = 10L; // 默认每0.5秒掉落一次 (10 ticks)
    
    public SandFallHandler() {
        initTeamColorMap();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void initTeamColorMap() {
        for (TeamWithDetails teamDetails : teams) {
            Material material = getConcreteByColor(teamDetails.teamColor());
            teamColorMap.put(teamDetails.teamName(), material);
        }
    }
    
    private Material getConcreteByColor(NamedTextColor color) {
        if (color == RED) return Material.RED_CONCRETE_POWDER;
        if (color == GOLD) return Material.ORANGE_CONCRETE_POWDER;
        if (color == YELLOW) return Material.YELLOW_CONCRETE_POWDER;
        if (color == GREEN) return Material.LIME_CONCRETE_POWDER;
        if (color == DARK_GREEN) return Material.GREEN_CONCRETE_POWDER;
        if (color == AQUA) return Material.CYAN_CONCRETE_POWDER;
        if (color == DARK_AQUA) return Material.LIGHT_BLUE_CONCRETE_POWDER;
        if (color == DARK_BLUE) return Material.BLUE_CONCRETE_POWDER;
        if (color == DARK_PURPLE) return Material.PURPLE_CONCRETE_POWDER;
        if (color == LIGHT_PURPLE) return Material.MAGENTA_CONCRETE_POWDER;
        return Material.SAND;
    }
    
    public void startSandFall() {
        startSandFall(sandFallInterval);
    }
    
    public void startSandFall(long intervalTicks) {
        if (sandFallTask != null) {
            sandFallTask.cancel();
        }
        
        
        this.sandFallInterval = intervalTicks;
        sandFallTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnSandForAllPlayers();
            }
        }.runTaskTimer(plugin, 0L, intervalTicks);
    }
    
    public void setSandFallSpeed(long intervalTicks) {
        this.sandFallInterval = intervalTicks;
        if (sandFallTask != null) {
            startSandFall(intervalTicks);
        }
    }
    
    public void stopSandFall() {
        if (sandFallTask != null) {
            sandFallTask.cancel();
            sandFallTask = null;
        }
    }
    
    private void spawnSandForAllPlayers() {
        int activePlayerCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.ADVENTURE && player.getScoreboardTags().contains("Active")) {
                activePlayerCount++;
                spawnSandAbovePlayer(player);
            }
        }
        
        if (activePlayerCount == 0) {
            plugin.getLogger().warning("落沙漫步：没有找到Active玩家，停止沙子掉落");
            stopSandFall();
        }
    }
    
    private void spawnSandAbovePlayer(Player player) {
        Team playerTeam = MCETeamUtils.getTeam(player);
        if (playerTeam == null) return;
        
        Material sandMaterial = teamColorMap.getOrDefault(playerTeam.getName(), Material.SAND);
        
        Location playerLoc = player.getLocation();
        World world = player.getWorld();
        
        // 获取玩家所在的完整方块坐标
        int blockX = playerLoc.getBlockX();
        int blockZ = playerLoc.getBlockZ();
        int spawnHeight = Math.min(playerLoc.getBlockY() + 8, world.getMaxHeight() - 1);
        
        // 使用完整方块坐标的中心位置生成
        Location spawnLoc = new Location(world, blockX + 0.5, spawnHeight + 0.5, blockZ + 0.5);
        
        // 使用正确的方法创建 FallingBlock
        FallingBlock fallingBlock = world.spawnFallingBlock(spawnLoc, sandMaterial.createBlockData());
        fallingBlock.setDropItem(false);
        fallingBlock.setHurtEntities(false); // 不直接伤害玩家，通过窒息来实现伤害
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            // 只处理有Active标签的游戏玩家
            if (!player.getScoreboardTags().contains("Active")) {
                return;
            }
            
            if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
                // 检查玩家受到窒息伤害后是否会死亡
                double finalHealth = player.getHealth() - event.getFinalDamage();
                if (finalHealth <= 0) {
                    // 取消伤害事件，避免真正死亡
                    event.setCancelled(true);
                    
                    // 直接切换为旁观模式
                    player.setGameMode(GameMode.SPECTATOR);
                    player.setHealth(player.getMaxHealth()); // 恢复生命值
                    
                    // 更新游戏计分板
                    SandRunFuncImpl.updateGameBoardOnPlayerDeath(player);
                }
                return;
            }
            if (event.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
                event.setCancelled(true);
            }
        }
    }
}