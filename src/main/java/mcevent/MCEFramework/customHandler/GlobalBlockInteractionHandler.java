package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.crazyMiner.CrazyMiner;
import mcevent.MCEFramework.games.spleef.Spleef;
import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl;
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

    private boolean isSurvival(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL;
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
        if (!isSurvival(player))
            return;

        // 在未运行任何游戏（主城）或投票系统阶段：默认禁止破坏
        if (!MCEMainController.isRunningGame()
                || isCurrentGame(mcevent.MCEFramework.games.votingSystem.VotingSystem.class)) {
            event.setCancelled(true);
            return;
        }

        // 运行中游戏的例外
        // SurvivalGame：仅允许破坏玩家放置的方块
        if (isCurrentGame(SurvivalGame.class)) {
            if (SurvivalGameFuncImpl.isPlayerPlaced(event.getBlock().getLocation())) {
                return; // 允许破坏
            }
            event.setCancelled(true);
            return;
        }
        // CrazyMiner 允许破坏
        if (isCurrentGame(CrazyMiner.class))
            return;
        // Spleef 仅在回合正式开始时允许破坏（依据 SnowBreakHandler 是否激活）
        if (isCurrentGame(Spleef.class)) {
            var game = MCEMainController.getCurrentRunningGame();
            if (game instanceof Spleef sp) {
                try {
                    if (!sp.getSnowBreakHandler().isSuspended()) {
                        return; // 回合中，允许破坏
                    }
                } catch (Throwable ignored) {
                }
            }
            event.setCancelled(true);
            return;
        }

        // 默认禁止：取消并不改变世界
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (!isSurvival(player))
            return;

        // 在未运行任何游戏（主城）或投票系统阶段：默认禁止放置
        if (!MCEMainController.isRunningGame()
                || isCurrentGame(mcevent.MCEFramework.games.votingSystem.VotingSystem.class)) {
            event.setCancelled(true);
            return;
        }

        // 运行中游戏的例外：SurvivalGame 允许放置，并记录被替换的方块用于回溯与登记玩家放置
        if (isCurrentGame(SurvivalGame.class)) {
            BlockState replaced = event.getBlockReplacedState();
            if (replaced != null) {
                MCEBlockRestoreUtils.recordReplacedState(replaced);
            }
            // 登记玩家放置的方块坐标
            mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl.registerPlacedBlock(
                    event.getBlockPlaced().getLocation());
            return; // 允许放置
        }

        // CrazyMiner 允许放置
        if (isCurrentGame(mcevent.MCEFramework.games.crazyMiner.CrazyMiner.class)) {
            return;
        }

        // Spleef 仅在回合正式开始时允许放置
        if (isCurrentGame(Spleef.class)) {
            var game = MCEMainController.getCurrentRunningGame();
            if (game instanceof Spleef sp) {
                try {
                    if (!sp.getSnowBreakHandler().isSuspended()) {
                        return; // 回合中允许放置
                    }
                } catch (Throwable ignored) {
                }
            }
            event.setCancelled(true);
            return;
        }

        // 默认禁止放置
        event.setCancelled(true);
    }
}
