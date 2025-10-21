package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.crazyMiner.CrazyMiner;
import mcevent.MCEFramework.games.spleef.Spleef;
import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEBlockRestoreUtils;
import org.bukkit.GameMode;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
GlobalBlockInteractionHandler: 全局生存模式方块交互限制
- 默认：生存模式下禁止破坏与放置
- 例外：
  * SurvivalGame：允许放置（记录替换方块以便回溯），禁止破坏
  * CrazyMiner：允许破坏，禁止放置
  * Spleef：允许破坏，禁止放置
*/
public class GlobalBlockInteractionHandler extends MCEResumableEventHandler implements Listener {

    public GlobalBlockInteractionHandler() {
        setSuspended(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean isSurvivalAndInGame(Player player) {
        if (player.getGameMode() != GameMode.SURVIVAL)
            return false;
        if (!MCEMainController.isRunningGame())
            return false;
        return true;
    }

    private boolean isCurrentGame(Class<?> clazz) {
        var game = MCEMainController.getCurrentRunningGame();
        return game != null && clazz.isInstance(game);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (!isSurvivalAndInGame(player))
            return;

        // 例外：CrazyMiner、Spleef 允许破坏；SurvivalGame 禁止
        if (isCurrentGame(CrazyMiner.class) || isCurrentGame(Spleef.class)) {
            return; // 允许破坏
        }

        // 默认禁止：取消并不改变世界
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (!isSurvivalAndInGame(player))
            return;

        // 例外：SurvivalGame 允许放置，并记录被替换的方块用于回溯
        if (isCurrentGame(SurvivalGame.class)) {
            BlockState replaced = event.getBlockReplacedState();
            if (replaced != null) {
                MCEBlockRestoreUtils.recordReplacedState(replaced);
            }
            return; // 允许放置
        }

        // 默认禁止放置
        event.setCancelled(true);
    }
}
