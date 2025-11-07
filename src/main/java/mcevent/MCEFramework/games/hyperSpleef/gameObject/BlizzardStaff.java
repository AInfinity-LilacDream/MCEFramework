package mcevent.MCEFramework.games.hyperSpleef.gameObject;

import mcevent.MCEFramework.generalGameObject.MCESpecialItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * 暴雪法杖 - 向前发射大量雪球
 */
public class BlizzardStaff extends MCESpecialItem {

    public BlizzardStaff() {
        super("<blue>暴雪法杖</blue>", Material.BLAZE_ROD, 100L, plugin); // 5秒冷却（100 ticks）
    }

    @Override
    protected boolean executeAction(Player player, PlayerInteractEvent event) {
        if (!player.getInventory().contains(Material.SNOWBALL)) {
            sendActionBar(player, "<red>你需要雪球才能使用暴雪法杖！</red>");
            return false;
        }

        Location playerLoc = player.getEyeLocation();
        Vector direction = playerLoc.getDirection();

        // 发射15个雪球
        int snowballCount = 0;
        for (int i = 0; i < 15; i++) {
            if (player.getInventory().contains(Material.SNOWBALL)) {
                player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(Material.SNOWBALL, 1));

                Snowball snowball = player.launchProjectile(Snowball.class);

                // 添加随机偏移
                Vector offset = direction.clone();
                offset.add(new Vector(
                        (Math.random() - 0.5) * 0.3,
                        (Math.random() - 0.5) * 0.3,
                        (Math.random() - 0.5) * 0.3));

                snowball.setVelocity(offset.multiply(2.0));
                snowball.setCustomName("hyper_spleef_blizzard_snowball");

                snowballCount++;
            }
        }

        // 播放音效
        playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.8f);

        if (snowballCount > 0) {
            sendActionBar(player, "<blue>发射了 " + snowballCount + " 个雪球！</blue>");
        }

        return snowballCount > 0;
    }
}
