package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.votingSystem.VotingSystem;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * LobbyDoubleJumpHandler: 主城/投票阶段二段跳
 */
public class LobbyDoubleJumpHandler extends MCEResumableEventHandler implements Listener {

    private final Map<UUID, Long> lastJumpAt = new HashMap<>();
    private static final long COOLDOWN_MS = 900; // 0.9秒冷却，手感更顺滑

    public LobbyDoubleJumpHandler() {
        setSuspended(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean isInLobbyDoubleJumpPhase(Player player) {
        if (player == null)
            return false;
        if (!"lobby".equals(player.getWorld().getName()))
            return false;
        // 在未运行任何游戏，或当前为投票系统时启用
        if (!MCEMainController.isRunningGame())
            return true;
        return MCEMainController.getCurrentRunningGame() instanceof VotingSystem;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (!isInLobbyDoubleJumpPhase(player))
            return;
        if (player.getGameMode() != GameMode.SURVIVAL)
            return;
        // 在地面时允许飞行，以触发 PlayerToggleFlightEvent 作为二段跳
        if (player.isOnGround() && !player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (!isInLobbyDoubleJumpPhase(player))
            return;
        if (player.getGameMode() != GameMode.SURVIVAL)
            return;

        // 将飞行键作为二段跳触发
        event.setCancelled(true);
        player.setAllowFlight(false);

        long now = System.currentTimeMillis();
        long last = lastJumpAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS)
            return;
        lastJumpAt.put(player.getUniqueId(), now);

        Vector dir = player.getLocation().getDirection();
        dir.setY(0).normalize();
        Vector boost = dir.multiply(0.9).setY(0.95);
        player.setVelocity(boost);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.6f, 1.2f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.1, 0), 12, 0.3, 0.2, 0.3, 0.01);

        // 少许延迟后在地面再次允许飞行
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline())
                return;
            if (isInLobbyDoubleJumpPhase(player) && player.getGameMode() == GameMode.SURVIVAL && player.isOnGround()) {
                player.setAllowFlight(true);
            }
        }, 20L);
    }
}
