package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.crazyMiner.CrazyMiner;
import mcevent.MCEFramework.games.spleef.Spleef;
import mcevent.MCEFramework.games.hyperSpleef.HyperSpleef;
import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl;
import mcevent.MCEFramework.games.underworldGame.UnderworldGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEBlockRestoreUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.WorldBorder;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
GlobalBlockInteractionHandler: 全局生存模式方块交互限制
- 默认：生存模式下禁止破坏与放置
- 例外：
  * SurvivalGame：允许放置（记录替换方块以便回溯），禁止破坏
  * CrazyMiner：允许破坏，禁止放置
  * Spleef：允许破坏，禁止放置
  * HyperSpleef：允许破坏，禁止放置
  * UnderworldGame：允许破坏和放置（正常生存模式行为）
*/
public class GlobalBlockInteractionHandler extends MCEResumableEventHandler implements Listener {

    public GlobalBlockInteractionHandler() {
        setSuspended(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // 禁止编辑告示牌（生存模式下）
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (!isSurvival(player))
            return;
        // 统一禁止编辑（无论是否在边界外）
        event.setCancelled(true);
    }

    private boolean isSurvival(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL;
    }

    private boolean isOutsideBorder(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null)
            return false;
        try {
            WorldBorder wb = loc.getWorld().getWorldBorder();
            return wb != null && !wb.isInside(loc);
        } catch (Throwable ignored) {
            return false;
        }
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

        // 边界外一律允许破坏
        if (isOutsideBorder(event.getBlock().getLocation()))
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
        // HyperSpleef 仅在回合正式开始时允许破坏（依据 SnowBreakHandler 是否激活）
        if (isCurrentGame(HyperSpleef.class)) {
            var game = MCEMainController.getCurrentRunningGame();
            if (game instanceof HyperSpleef hs) {
                try {
                    if (!hs.getSnowBreakHandler().isSuspended()) {
                        return; // 回合中，允许破坏
                    }
                } catch (Throwable ignored) {
                }
            }
            event.setCancelled(true);
            return;
        }
        // UnderworldGame 允许破坏和放置（正常生存模式行为）
        if (isCurrentGame(UnderworldGame.class)) {
            return; // 允许破坏
        }

        // 默认禁止：取消并不改变世界
        event.setCancelled(true);
    }

    // 禁止“劈树皮”等使用斧头对木类方块的右键改性（生存模式下）
    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (isSuspended())
            return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Player player = event.getPlayer();
        if (!isSurvival(player))
            return;
        Block clicked = event.getClickedBlock();
        if (clicked == null)
            return;
        // 边界外一律允许右键交互（包括劈树皮）
        if (isOutsideBorder(clicked.getLocation()))
            return;
        if (event.getItem() == null)
            return;
        Material tool = event.getItem().getType();
        if (!isAxe(tool))
            return;
        if (!isStrippable(clicked.getType()))
            return;

        // 默认全面禁止生存模式下的“劈树皮”改性
        event.setCancelled(true);
    }

    private boolean isAxe(Material tool) {
        String n = tool.name();
        return n.endsWith("_AXE");
    }

    private boolean isStrippable(Material type) {
        String n = type.name();
        // 包含所有可被斧头右键改性的木本/菌类方块族
        return n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_STEM") || n.endsWith("_HYPHAE")
                || n.equals("BAMBOO_BLOCK") || n.equals("PALE_OAK_LOG") || n.equals("PALE_OAK_WOOD");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (!isSurvival(player))
            return;

        // 边界外一律允许放置
        if (isOutsideBorder(event.getBlockPlaced().getLocation()))
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

        // HyperSpleef 仅在回合正式开始时允许放置
        if (isCurrentGame(HyperSpleef.class)) {
            var game = MCEMainController.getCurrentRunningGame();
            if (game instanceof HyperSpleef hs) {
                try {
                    if (!hs.getSnowBreakHandler().isSuspended()) {
                        return; // 回合中允许放置
                    }
                } catch (Throwable ignored) {
                }
            }
            event.setCancelled(true);
            return;
        }
        // UnderworldGame 允许破坏和放置（正常生存模式行为）
        if (isCurrentGame(UnderworldGame.class)) {
            return; // 允许放置
        }

        // 默认禁止放置
        event.setCancelled(true);
    }
}
