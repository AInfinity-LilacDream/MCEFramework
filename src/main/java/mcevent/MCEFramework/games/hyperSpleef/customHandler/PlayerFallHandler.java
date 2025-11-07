package mcevent.MCEFramework.games.hyperSpleef.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.hyperSpleef.HyperSpleef;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
PlayerFallHandler: 超级掘一死战玩家掉落处理器
参照DiscoFever的实现，直接在事件处理器中处理死亡逻辑
*/
public class PlayerFallHandler extends MCEResumableEventHandler implements Listener {

    private HyperSpleef hyperSpleef;

    public void register(HyperSpleef game) {
        this.hyperSpleef = game;
        setSuspended(true); // 默认挂起，游戏开始时启动
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void start() {
        setSuspended(false);
    }

    @Override
    public void suspend() {
        setSuspended(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isSuspended())
            return;
        if (!MCEMainController.isRunningGame())
            return;
        MCEGame current = MCEMainController.getCurrentRunningGame();
        if (!(current instanceof HyperSpleef))
            return;

        Player player = event.getPlayer();
        if (hyperSpleef == null || !player.getWorld().getName().equals(hyperSpleef.getWorldName()))
            return;

        // 仅参与者且未死亡
        if (!player.getScoreboardTags().contains("Participant") || player.getScoreboardTags().contains("dead"))
            return;

        // 检查Y坐标是否低于等于阈值
        double y = player.getLocation().getY();
        int threshold = HyperSpleef.getFallYThreshold();

        // 当玩家接近阈值时打印调试信息
        if (y <= threshold + 5) {
            plugin.getLogger()
                    .info("PlayerFallHandler: 玩家 " + player.getName() + " Y坐标=" + String.format("%.2f", y) + ", 阈值="
                            + threshold +
                            ", Participant=" + player.getScoreboardTags().contains("Participant") +
                            ", Active=" + player.getScoreboardTags().contains("Active") +
                            ", dead=" + player.getScoreboardTags().contains("dead") +
                            ", 世界=" + player.getWorld().getName());
        }

        if (y > threshold)
            return;

        // 标记淘汰并旁观（参照DiscoFever的方式，立即标记避免重复触发）
        player.addScoreboardTag("dead");
        player.removeScoreboardTag("Active");
        player.setGameMode(GameMode.SPECTATOR);

        String pname = MCEPlayerUtils.getColoredPlayerName(player);
        MCEMessenger.sendGlobalInfo(pname + " <gray>已被淘汰！</gray>");
        MCEPlayerUtils.globalPlaySound("minecraft:player_eliminated");

        plugin.getLogger().info("PlayerFallHandler: 玩家 " + player.getName() + " 掉落死亡判定完成！Y=" + String.format("%.2f", y)
                + " <= " + threshold);

        // 立即传送到安全位置（世界出生点）防止继续受到伤害
        World world = Bukkit.getWorld(hyperSpleef.getWorldName());
        if (world != null) {
            player.teleport(world.getSpawnLocation());
        }

        // 调用游戏的死亡处理逻辑（记录死亡顺序、检查队伍淘汰等）
        if (hyperSpleef != null) {
            hyperSpleef.onPlayerFallDeath(player);
        }
    }
}
