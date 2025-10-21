package mcevent.MCEFramework.games.extractOwn.customHandler;

import mcevent.MCEFramework.games.extractOwn.ExtractOwn;
import mcevent.MCEFramework.games.extractOwn.ExtractOwnFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
CrossbowAttackHandler: 弩箭攻击处理器
*/
public class CrossbowAttackHandler extends MCEResumableEventHandler implements Listener {

    private ExtractOwn extractOwn;

    public void register(ExtractOwn game) {
        this.extractOwn = game;
        setSuspended(true); // 默认挂起，游戏开始时启动
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void start() {
        setSuspended(false);
    }

    @Override
    public void suspend() {
        setSuspended(true);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (isSuspended())
            return;

        // 检查是否是箭击中玩家
        if (event.getDamager() instanceof Arrow arrow && event.getEntity() instanceof Player victim) {
            // 检查射箭者是否是玩家
            if (arrow.getShooter() instanceof Player shooter) {
                Team shooterTeam = MCETeamUtils.getTeam(shooter);
                Team victimTeam = MCETeamUtils.getTeam(victim);

                // 防止同队误伤
                if (shooterTeam != null && victimTeam != null && shooterTeam.equals(victimTeam)) {
                    event.setCancelled(true);
                    return;
                }

                // 设置弩的伤害为2.5颗心 (5.0 HP)
                event.setDamage(5.0);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isSuspended())
            return;

        // 阻止掉落物品和经验
        event.getDrops().clear();
        event.setDroppedExp(0);

        // 死亡处理由PlayerDeathHandler负责，这里只处理掉落物
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (isSuspended())
            return;

        if (event.getEntity() instanceof Arrow arrow) {
            // 如果击中玩家，立即移除箭
            if (event.getHitEntity() instanceof Player) {
                arrow.remove();
            } else {
                // 击中方块后延迟移除
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!arrow.isDead()) {
                        arrow.remove();
                    }
                }, 100L); // 5秒后移除
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isSuspended())
            return;

        Player player = event.getPlayer();
        Material droppedItem = event.getItemDrop().getItemStack().getType();

        // 检查玩家是否在游戏中
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // 防止丢弃弩和箭
        if (droppedItem == Material.CROSSBOW || droppedItem == Material.ARROW) {
            event.setCancelled(true);
        }
    }
}