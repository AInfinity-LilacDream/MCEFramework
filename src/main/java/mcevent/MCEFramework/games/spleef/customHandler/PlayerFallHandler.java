package mcevent.MCEFramework.games.spleef.customHandler;

import mcevent.MCEFramework.games.spleef.Spleef;
import mcevent.MCEFramework.games.spleef.SpleefFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
PlayerFallHandler: 冰雪掘战玩家掉落处理器
*/
public class PlayerFallHandler extends MCEResumableEventHandler implements Listener {

    private Spleef spleef;

    public void register(Spleef game) {
        this.spleef = game;
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

        Player player = event.getPlayer();

        // 仅参与者且未死亡
        if (!player.getScoreboardTags().contains("Participant") ||
                player.getScoreboardTags().contains("dead")) {
            return;
        }

        // 检查Y坐标是否低于阈值
        if (player.getLocation().getY() < Spleef.getFallYThreshold()) {
            SpleefFuncImpl.handlePlayerFall(player);
        }
    }
}