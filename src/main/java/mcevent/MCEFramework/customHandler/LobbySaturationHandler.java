package mcevent.MCEFramework.customHandler;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
LobbySaturationHandler: 保证玩家在主城(lobby)始终拥有饱和效果
离开主城（进入duel或其他游戏世界）时移除饱和效果。
并有周期性任务以抵消其他逻辑的全局清除效果。
*/
public class LobbySaturationHandler implements Listener {

    private final BukkitTask reapplyTask;

    public LobbySaturationHandler() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // 每2秒检查一次：在主城则确保饱和，不在主城则移除
        this.reapplyTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isInLobby(p)) {
                    applySaturation(p);
                }
            }
        }, 0L, 40L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isInLobby(player)) {
            applySaturation(player);
        } else {
            removeSaturation(player);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (isInLobby(player)) {
            applySaturation(player);
        } else {
            removeSaturation(player);
        }
    }

    private boolean isInLobby(Player player) {
        World world = player.getWorld();
        return world != null && "lobby".equals(world.getName());
    }

    private void applySaturation(Player player) {
        try {
            // 给予较长时间无粒子饱和效果；周期任务会刷新时间
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 0, false, false, false));
        } catch (Throwable ignored) {
        }
    }

    private void removeSaturation(Player player) {
        try {
            player.removePotionEffect(PotionEffectType.SATURATION);
        } catch (Throwable ignored) {
        }
    }
}
