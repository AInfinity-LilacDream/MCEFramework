package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.votingSystem.VotingSystem;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * WindLauncherHandler: 主城/投票阶段“风弹发射器”（烈焰棒）
 */
public class WindLauncherHandler extends MCEResumableEventHandler implements Listener {

    private static final Component DISPLAY_NAME = MiniMessage.miniMessage()
            .deserialize("<red><bold>风弹发射器</bold></red>");
    private static final long COOLDOWN_MS = 3000;

    public WindLauncherHandler() {
        setSuspended(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean isInLobbyOrVoting(Player player) {
        if (player == null)
            return false;
        if (!"lobby".equals(player.getWorld().getName()))
            return false;
        if (!MCEMainController.isRunningGame())
            return true;
        return MCEMainController.getCurrentRunningGame() instanceof VotingSystem;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (isSuspended())
            return;
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        Player player = event.getPlayer();
        if (!isInLobbyOrVoting(player))
            return;
        if (player.getGameMode() != GameMode.SURVIVAL)
            return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.BLAZE_ROD)
            return;
        if (!item.hasItemMeta())
            return;
        var meta = item.getItemMeta();
        boolean matches = false;
        if (meta.displayName() != null) {
            matches = DISPLAY_NAME.equals(meta.displayName());
        }
        if (!matches)
            return;

        // 冷却
        long now = System.currentTimeMillis();
        String key = "wind_last_use";
        Object metaVal = player.getMetadata(key).stream().findFirst().map(v -> v.value()).orElse(null);
        if (metaVal instanceof Long last && now - last < COOLDOWN_MS) {
            // 冷却中：不再显示动作栏提示，仅取消默认交互
            event.setCancelled(true);
            return;
        }
        player.setMetadata(key, new FixedMetadataValue(plugin, now));

        // 发射“风弹” - 使用 1.21 的 WindCharge 弹体
        event.setCancelled(true);
        try {
            WindCharge wc = player.launchProjectile(WindCharge.class);
            wc.setVelocity(player.getLocation().getDirection().normalize().multiply(1.25));
            wc.setMetadata("mce_wind", new FixedMetadataValue(plugin, true));
        } catch (Throwable ignore) {
            // 兼容性回退：若服务器不支持 WindCharge，则使用雪球模拟
            Snowball proj = player.launchProjectile(Snowball.class);
            proj.setItem(new ItemStack(Material.SNOWBALL));
            proj.setVelocity(player.getLocation().getDirection().normalize().multiply(1.4));
            proj.setMetadata("mce_wind", new FixedMetadataValue(plugin, true));
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.2f);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (isSuspended())
            return;
        if (!(event.getEntity() instanceof Snowball || event.getEntity() instanceof WindCharge))
            return;
        if (!event.getEntity().hasMetadata("mce_wind"))
            return;
        ProjectileSource src = event.getEntity().getShooter();
        if (!(src instanceof Player shooter))
            return;
        if (!isInLobbyOrVoting(shooter))
            return;

        if (event.getHitEntity() instanceof Player victim) {
            // 击退并发光3秒（对所有人可见）
            Vector push = victim.getLocation().toVector().subtract(shooter.getLocation().toVector()).normalize()
                    .multiply(0.9).setY(0.45);
            victim.setVelocity(push);
            victim.setGlowing(true);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.8f, 1.0f);
            victim.getWorld().spawnParticle(Particle.GUST, victim.getLocation().add(0, 1.0, 0), 15, 0.4, 0.4, 0.4,
                    0.02);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (victim.isOnline())
                    victim.setGlowing(false);
            }, 60L);
        }
    }
}
