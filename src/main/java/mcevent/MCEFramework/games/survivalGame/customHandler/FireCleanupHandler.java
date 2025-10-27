package mcevent.MCEFramework.games.survivalGame.customHandler;

import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * FireCleanupHandler: 记录并清理回合内产生的火焰方块
 */
public class FireCleanupHandler extends MCEResumableEventHandler implements Listener {

    private final Set<Location> fireLocations = new HashSet<>();
    private SurvivalGame game;

    public void register(SurvivalGame game) {
        this.game = game;
        setSuspended(true);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean isGameWorld(World w) {
        return w != null && game != null && game.getWorldName().equals(w.getName());
    }

    private static boolean isFire(Material m) {
        return m == Material.FIRE || m == Material.SOUL_FIRE;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (isSuspended())
            return;
        if (event.getItem() == null || event.getItem().getType() != Material.FIRE_CHARGE)
            return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
            return;
        org.bukkit.block.Block clicked = event.getClickedBlock();
        if (clicked == null)
            return;
        org.bukkit.block.Block target = clicked.getRelative(event.getBlockFace());
        if (!isGameWorld(target.getWorld()))
            return;
        // 记录预计将点燃的方块位置（服务端紧随其后会在该处生成 FIRE）
        org.bukkit.Location bl = target.getLocation().toBlockLocation();
        fireLocations.add(bl);
        try {
            String wn = bl.getWorld() != null ? bl.getWorld().getName() : "null";
            plugin.getLogger().info("[SG][FireDebug] record fire(interact) @ " + wn +
                    " (" + bl.getBlockX() + "," + bl.getBlockY() + "," + bl.getBlockZ() + ")");
        } catch (Throwable ignored) {
        }
    }

    public void clearFires() {
        World world = Bukkit.getWorld(game.getWorldName());
        if (world == null) {
            fireLocations.clear();
            return;
        }
        for (Location loc : fireLocations) {
            try {
                if (loc.getWorld() != null && loc.getWorld().equals(world)) {
                    Material t = world.getBlockAt(loc).getType();
                    if (t == Material.FIRE || t == Material.SOUL_FIRE) {
                        world.getBlockAt(loc).setType(Material.AIR, false);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        fireLocations.clear();
    }
}
