package mcevent.MCEFramework.games.survivalGame.customHandler;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
PvPControlHandler: PvP控制处理器（前45秒禁止PvP，之后开启无友伤PvP）
 */
@Getter @Setter
public class PvPControlHandler extends MCEResumableEventHandler implements Listener {

    private boolean pvpEnabled = false;

    public PvPControlHandler() {
        setSuspended(false); // 默认启用处理器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (isSuspended()) return;

        Player damager = null;
        Player victim = null;

        // 处理玩家对玩家的直接伤害
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            damager = (Player) event.getDamager();
            victim = (Player) event.getEntity();
        }
        // 处理投射物伤害
        else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile && event.getEntity() instanceof Player) {
            if (projectile.getShooter() instanceof Player) {
                damager = (Player) projectile.getShooter();
                victim = (Player) event.getEntity();
            }
        }

        if (damager == null || victim == null) {
            return;
        }

        // 如果PvP未开启，取消所有玩家间伤害
        if (!pvpEnabled) {
            event.setCancelled(true);
            return;
        }

        // PvP已开启，检查友伤
        Team damagerTeam = MCETeamUtils.getTeam(damager);
        Team victimTeam = MCETeamUtils.getTeam(victim);

        // 如果两个玩家在同一队伍，取消伤害（无友伤）
        if (damagerTeam != null && victimTeam != null && damagerTeam.equals(victimTeam)) {
            event.setCancelled(true);
        }
    }
}
