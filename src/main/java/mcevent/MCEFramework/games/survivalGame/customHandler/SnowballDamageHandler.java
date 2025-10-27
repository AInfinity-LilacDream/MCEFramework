package mcevent.MCEFramework.games.survivalGame.customHandler;

import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
SnowballDamageHandler: 让雪球在饥饿游戏中对玩家造成伤害
 */
public class SnowballDamageHandler extends MCEResumableEventHandler implements Listener {

    private SurvivalGame game;

    public void register(SurvivalGame game) {
        this.game = game;
        setSuspended(true);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (isSuspended())
            return;
        if (!(event.getDamager() instanceof Snowball))
            return;
        if (!(event.getEntity() instanceof Player target))
            return;
        if (!isGameWorld(target.getWorld() != null ? target.getWorld().getName() : null))
            return;
        // 仅对参与者生效
        if (game == null || !game.isGameParticipant(target))
            return;
        // 仅当投掷者为玩家时生效
        if (!(event.getDamager() instanceof Snowball sb) || !(sb.getShooter() instanceof Player))
            return;
        // 给予轻微伤害（半颗心）
        event.setDamage(1.0);
    }

    private boolean isGameWorld(String worldName) {
        return game != null && game.getWorldName().equals(worldName);
    }
}
