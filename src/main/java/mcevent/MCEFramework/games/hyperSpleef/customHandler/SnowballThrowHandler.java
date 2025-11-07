package mcevent.MCEFramework.games.hyperSpleef.customHandler;

import mcevent.MCEFramework.games.hyperSpleef.HyperSpleef;
import mcevent.MCEFramework.games.hyperSpleef.HyperSpleefFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
SnowballThrowHandler: 超级掘一死战雪球投掷处理器
右键金铲可以直接丢弃雪球
*/
public class SnowballThrowHandler extends MCEResumableEventHandler implements Listener {

    private HyperSpleef hyperSpleef;

    public void register(HyperSpleef game) {
        this.hyperSpleef = game;
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
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isSuspended())
            return;

        Player player = event.getPlayer();

        // 仅参与者且未死亡
        if (!player.getScoreboardTags().contains("Participant") ||
                player.getScoreboardTags().contains("dead")) {
            return;
        }

        // 如果手持金铲子右键，自动发射雪球
        if (player.getInventory().getItemInMainHand().getType() == Material.GOLDEN_SHOVEL &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true); // 防止金铲子的右键行为

            // 检查玩家是否有雪球
            if (player.getInventory().contains(Material.SNOWBALL)) {
                // 移除一个雪球并发射
                player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(Material.SNOWBALL, 1));

                // 发射雪球，标记为游戏中的雪球
                Snowball snowball = player.launchProjectile(Snowball.class);
                snowball.setVelocity(player.getEyeLocation().getDirection().multiply(2.2));
                snowball.setCustomName("hyper_spleef_snowball:" + player.getName());
                plugin.getLogger().info("调试 - " + player.getName() + " 使用金铲子发射了雪球");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (isSuspended())
            return;

        // 检查是否是雪球击中玩家
        if (!(event.getDamager() instanceof Snowball snowball) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        // 检查是否是玩家发射的雪球
        if (!(snowball.getShooter() instanceof Player shooter)) {
            return;
        }

        // 检查玩家是否在游戏中
        if (!target.getScoreboardTags().contains("Participant") || target.getScoreboardTags().contains("dead") ||
                !shooter.getScoreboardTags().contains("Participant") || shooter.getScoreboardTags().contains("dead")) {
            event.setCancelled(true);
            return;
        }

        // 检查友伤
        Team shooterTeam = MCETeamUtils.getTeam(shooter);
        Team targetTeam = MCETeamUtils.getTeam(target);
        if (shooterTeam != null && targetTeam != null && shooterTeam.equals(targetTeam)) {
            event.setCancelled(true);
            return;
        }

        // 设置雪球伤害为半颗心（1.0）
        event.setDamage(1.0);
        // 允许击退效果
    }
}
