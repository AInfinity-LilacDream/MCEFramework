package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashSet;
import java.util.Set;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * WorldDaylightFreezeHandler: 进入世界时强制关闭昼夜交替并设置为正午
 */
public class WorldDaylightFreezeHandler extends MCEResumableEventHandler implements Listener {

    private static final long NOON_TIME = 6000L; // 正午
    private static final Set<String> appliedWorlds = new HashSet<>();

    public WorldDaylightFreezeHandler() {
        setSuspended(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void applyRules(World world) {
        if (world == null)
            return;
        try {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setTime(NOON_TIME);
            appliedWorlds.add(world.getName());
        } catch (Throwable ignored) {
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isSuspended())
            return;
        Player p = event.getPlayer();
        World w = p != null ? p.getWorld() : null;
        if (w == null)
            return;
        // 下个tick应用，确保世界已就绪
        Bukkit.getScheduler().runTask(plugin, () -> applyRules(w));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (isSuspended())
            return;
        World to = event.getPlayer() != null ? event.getPlayer().getWorld() : null;
        if (to == null)
            return;
        applyRules(to);
    }
}
