package mcevent.MCEFramework.games.captureCenter.customHandler;

import mcevent.MCEFramework.games.captureCenter.CaptureCenterFuncImpl;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

public class PlayerFallHandler implements Listener {
    
    private boolean isRegistered = false;
    
    public void register() {
        if (!isRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            isRegistered = true;
        }
    }
    
    public void unregister() {
        if (isRegistered) {
            HandlerList.unregisterAll(this);
            isRegistered = false;
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否掉落到虚空（只处理活跃游戏玩家）
        if (player.getLocation().getY() <= -65 && player.getGameMode() == GameMode.ADVENTURE && 
            player.getScoreboardTags().contains("Active")) {
            // 玩家被击溃
            Team playerTeam = MCETeamUtils.getTeam(player);
            String teamName = playerTeam != null ? MCETeamUtils.getUncoloredTeamName(playerTeam) : "未知队伍";
            
            MCEMessenger.sendGlobalInfo(
                MCEPlayerUtils.getColoredPlayerName(player) + 
                " <gray>(" + teamName + ") 被击溃了！</gray>"
            );
            
            CaptureCenterFuncImpl.handlePlayerFallIntoVoid(player);
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 处理PVP击杀奖励（只处理活跃游戏玩家）
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            if (victim.getGameMode() != GameMode.ADVENTURE || attacker.getGameMode() != GameMode.ADVENTURE ||
                !victim.getScoreboardTags().contains("Active") || !attacker.getScoreboardTags().contains("Active")) {
                return;
            }
            
            // 检查是否会导致玩家掉落虚空死亡
            double knockbackForce = event.getFinalDamage();
            if (knockbackForce > 0 && victim.getLocation().getY() <= -60) {
                // 如果这次攻击可能导致玩家掉入虚空，给攻击者击杀奖励
                Team attackerTeam = MCETeamUtils.getTeam(attacker);
                Team victimTeam = MCETeamUtils.getTeam(victim);
                
                // 确保不是队友攻击
                if (attackerTeam != null && victimTeam != null && !attackerTeam.equals(victimTeam)) {
                    // 延迟检查玩家是否真的掉入虚空
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (victim.getLocation().getY() <= -65 || victim.getGameMode() == GameMode.SPECTATOR) {
                            CaptureCenterFuncImpl.handlePlayerKill(attacker);
                            
                            MCEMessenger.sendGlobalInfo(
                                MCEPlayerUtils.getColoredPlayerName(attacker) + 
                                " <yellow>击败了</yellow> " + 
                                MCEPlayerUtils.getColoredPlayerName(victim) + 
                                " <gold>+50分！</gold>"
                            );
                        }
                    }, 2L); // 延迟2tick检查
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // 取消摔落伤害，因为已经设置了游戏规则FALL_DAMAGE为false
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }
}