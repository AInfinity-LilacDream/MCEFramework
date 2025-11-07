package mcevent.MCEFramework.games.hyperSpleef.gameObject;

import mcevent.MCEFramework.generalGameObject.MCESpecialItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * 飘浮之羽 - 使用后获得3s飘浮 X
 */
public class FloatingFeather extends MCESpecialItem {

    public FloatingFeather() {
        super("<white>飘浮之羽</white>", Material.FEATHER, 1200L, plugin); // 60秒冷却（1200 ticks）
    }

    @Override
    protected boolean executeAction(Player player, PlayerInteractEvent event) {
        // 给玩家飘浮 X 效果，持续3秒（60 ticks）
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 9, false, false));

        // 播放音效
        playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.2f);

        // 播放粒子效果
        Location loc = player.getLocation();
        for (int i = 0; i < 10; i++) {
            Location particleLoc = loc.clone().add(
                    (Math.random() - 0.5) * 2,
                    Math.random() * 2,
                    (Math.random() - 0.5) * 2);
            player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, particleLoc, 1);
        }

        sendActionBar(player, "<white>获得飘浮效果！</white>");

        return true;
    }
}
