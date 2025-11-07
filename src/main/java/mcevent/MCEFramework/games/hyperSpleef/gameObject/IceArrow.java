package mcevent.MCEFramework.games.hyperSpleef.gameObject;

import mcevent.MCEFramework.generalGameObject.MCESpecialItem;
import mcevent.MCEFramework.tools.MCEMessenger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * 寒冰箭 - 射出一支箭，触碰方块后1s爆炸，随机将周围的雪块变成冰
 */
public class IceArrow extends MCESpecialItem {

    private static final Random random = new Random();
    private static final Set<Arrow> trackedArrows = new HashSet<>();

    public IceArrow() {
        super("<aqua>寒冰箭</aqua>", Material.BOW, 100L, plugin); // 5秒冷却（100 ticks）
    }

    @Override
    protected boolean executeAction(Player player, PlayerInteractEvent event) {
        if (!player.getInventory().contains(Material.ARROW)) {
            sendActionBar(player, "<red>你需要箭才能使用寒冰箭！</red>");
            return false;
        }

        // 移除一个箭
        player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(Material.ARROW, 1));

        // 发射箭
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(player.getEyeLocation().getDirection().multiply(2.0));

        // 标记这支箭
        arrow.setCustomName("hyper_spleef_ice_arrow");
        trackedArrows.add(arrow);

        // 播放音效
        playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);

        // 注册箭头命中监听器
        registerArrowHitListener(arrow);

        return true;
    }

    private void registerArrowHitListener(Arrow arrow) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || !arrow.isValid()) {
                    trackedArrows.remove(arrow);
                    this.cancel();
                    return;
                }

                // 检查箭头是否击中方块
                if (arrow.isOnGround() || arrow.getLocation().getBlock().getType() != Material.AIR) {
                    Location hitLocation = arrow.getLocation();

                    // 不立即清除箭，1秒后爆炸并清除
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // 爆炸
                            explode(hitLocation);
                            // 清除箭
                            if (!arrow.isDead() && arrow.isValid()) {
                                arrow.remove();
                            }
                            trackedArrows.remove(arrow);
                        }
                    }.runTaskLater(plugin, 20L); // 1秒 = 20 ticks

                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explode(Location location) {
        // 播放爆炸效果
        location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1);
        playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // 随机将周围的雪块变成冰（半径3格）
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = location.clone().add(x, y, z).getBlock();

                    // 随机概率（30%）
                    if (random.nextDouble() < 0.3 && block.getType() == Material.SNOW_BLOCK) {
                        block.setType(Material.ICE);
                    }
                }
            }
        }
    }
}
