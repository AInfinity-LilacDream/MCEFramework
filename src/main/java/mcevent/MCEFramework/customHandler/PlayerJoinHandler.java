package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEGlowingEffectManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * PlayerJoinHandler: 处理玩家加入服务器的事件
 * 发送欢迎信息，根据当前游戏状态传送玩家到对应位置
 */
public class PlayerJoinHandler extends MCEResumableEventHandler implements Listener {

    public PlayerJoinHandler() {
        setSuspended(false); // 始终启用，不需要暂停
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isSuspended()) return;

        // 迎新信息在 WelcomeMessageHandler 中发送
        event.joinMessage(null);

        Player player = event.getPlayer();
        
        // 清理玩家的发光效果
        MCEGlowingEffectManager.clearPlayerGlowingEffect(player);
        
        // 根据游戏状态决定传送位置
        if (MCEMainController.isRunningGame()) {
            MCEGame currentGame = MCEMainController.getCurrentRunningGame();
            
            if (currentGame != null) {
                // 检查玩家是否是游戏参与者
                if (currentGame.isGameParticipant(player)) {
                    // 如果是游戏参与者，传送到游戏世界出生点
                    teleportToGameWorld(player);
                } else {
                    // 如果不是游戏参与者，传送到游戏世界并使用游戏的处理器处理
                    teleportToGameWorld(player);
                    
                    // 使用游戏的统一处理器处理新加入的玩家
                    currentGame.handlePlayerJoinDuringGame(player);
                }
            } else {
                // 安全回退：传送到主城
                teleportToLobby(player);
            }
        } else {
            // 如果没有正在进行的游戏，传送到主城
            teleportToLobby(player);
        }
    }
    
    /**
     * 传送玩家到主城出生点
     */
    private void teleportToLobby(Player player) {
        // 清理玩家的发光效果
        MCEGlowingEffectManager.clearPlayerGlowingEffect(player);
        
        World lobbyWorld = Bukkit.getWorld("lobby");
        
        if (lobbyWorld != null) {
            Location lobbySpawn = lobbyWorld.getSpawnLocation();
            player.teleport(lobbySpawn);
        }
    }
    
    /**
     * 传送玩家到当前游戏世界出生点
     */
    private void teleportToGameWorld(Player player) {
        MCEGame currentGame = MCEMainController.getCurrentRunningGame();
        if (currentGame != null) {
            String gameWorldName = currentGame.getWorldName();
            World gameWorld = Bukkit.getWorld(gameWorldName);
            
            if (gameWorld != null) {
                Location spawnLocation = gameWorld.getSpawnLocation();
                player.teleport(spawnLocation);
                
                Component gameMessage = Component.text("当前正在进行游戏: ")
                        .color(NamedTextColor.AQUA)
                        .append(Component.text(currentGame.getTitle())
                                .color(NamedTextColor.YELLOW));
                player.sendMessage(gameMessage);
            } else {
                // 如果游戏世界不存在，传送到主城
                teleportToLobby(player);
            }
        } else {
            // 如果获取不到当前游戏，传送到主城
            teleportToLobby(player);
        }
    }
}