package mcevent.MCEFramework.games.survivalGame.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEBlockRestoreUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
BuildRulesHandler: 饥饿游戏建造规则
- 允许放置方块，记录替换前状态与坐标
- 仅允许破坏玩家放置的方块
- 手持烈焰弹(FIRE_CHARGE)右键生火，消耗一枚
*/
public class BuildRulesHandler extends MCEResumableEventHandler implements Listener {

    public BuildRulesHandler() {
        setSuspended(true);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean isActiveInSGWorld(World world) {
        if (isSuspended())
            return false;
        if (!MCEMainController.isRunningGame())
            return false;
        if (!(MCEMainController.getCurrentRunningGame() instanceof SurvivalGame))
            return false;
        return world != null && survivalGame != null && world.getName().equals(survivalGame.getWorldName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        World world = event.getBlock().getWorld();
        if (!isActiveInSGWorld(world))
            return;

        // 允许放置；记录被替换的状态以便回溯
        try {
            MCEBlockRestoreUtils.recordReplacedState(event.getBlockReplacedState());
        } catch (Throwable ignored) {
        }

        // 记录玩家放置的方块位置
        Location loc = event.getBlockPlaced().getLocation();
        SurvivalGameFuncImpl.registerPlacedBlock(loc);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        World world = event.getBlock().getWorld();
        if (!isActiveInSGWorld(world))
            return;

        // 仅允许破坏玩家放置的方块
        Location loc = event.getBlock().getLocation();
        if (!SurvivalGameFuncImpl.isPlayerPlaced(loc)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUseFireCharge(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getItem() == null || event.getItem().getType() != Material.FIRE_CHARGE)
            return;
        Block clicked = event.getClickedBlock();
        World world = clicked != null ? clicked.getWorld() : null;
        if (!isActiveInSGWorld(world))
            return;

        BlockFace face = event.getBlockFace();
        if (clicked == null || face == null)
            return;
        Block target = clicked.getRelative(face);
        if (target.getType() != Material.AIR)
            return;

        // 记录替换状态并点燃
        try {
            MCEBlockRestoreUtils.recordReplacedState(target.getState());
        } catch (Throwable ignored) {
        }
        target.setType(Material.FIRE, false);
        SurvivalGameFuncImpl.registerPlacedBlock(target.getLocation());

        // 消耗一枚烈焰弹（非创造）
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            var stack = event.getItem();
            stack.setAmount(Math.max(0, stack.getAmount() - 1));
        }

        // 播放原版音效
        event.getPlayer().playSound(event.getPlayer().getLocation(), org.bukkit.Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.ALLOW);
        event.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);
    }
}
