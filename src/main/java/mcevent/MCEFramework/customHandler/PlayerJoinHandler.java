package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
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

        event.joinMessage(MiniMessage.miniMessage().deserialize(
                "<gold><bold>欢迎来到</bold></gold><gradient:red:blue>Lilac Games</gradient><gold><bold>！</bold></gold>"
        ));
        Player player = event.getPlayer();
        
        // 根据游戏状态决定传送位置
        if (MCEMainController.isRunningGame()) {
            // 检查玩家是否有Active标签（活跃游戏玩家）
            if (player.getScoreboardTags().contains("Active")) {
                // 如果是活跃玩家，传送到游戏世界出生点
                teleportToGameWorld(player);
            } else {
                // 如果不是活跃玩家，传送到游戏世界出生点并延迟设置旁观模式
                teleportToGameWorld(player);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline() && MCEMainController.isRunningGame() &&
                                !player.getScoreboardTags().contains("Active")) {
                            player.setGameMode(GameMode.SPECTATOR);
                        }
                    }
                }.runTaskLater(plugin, 5);
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