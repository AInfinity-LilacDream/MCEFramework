package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCETeleporter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
Launch: 立即启动游戏
usage: launch <game>
<game>: 游戏名称
 */
@CommandAlias("launch")
@CommandPermission("launch.use")
public class Launch extends BaseCommand {

    @Subcommand("pkt")
    public void launchParkourTag(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(pkt.getId(), true);
    }

    @Subcommand("pktNoIntro")
    public void launchParkourTagNoIntro(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(pkt.getId(), false);
    }

    @Subcommand("discoFever")
    public void launchDiscoFever(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(discoFever.getId(), true);
    }

    @Subcommand("discoFeverNoIntro")
    public void launchDiscoFeverNoIntro(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(discoFever.getId(), false);
    }

    @Subcommand("musicDodge")
    public void launchMusicDodge(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(musicDodge.getId(), true);
    }

    @Subcommand("musicDodgeNoIntro")
    public void launchMusicDodgeNoIntro(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(musicDodge.getId(), false);
    }

    @Subcommand("sandRun")
    public void launchSandRun(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(sandRun.getId(), true);
    }

    @Subcommand("sandRunNoIntro")
    public void launchSandRunNoIntro(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(sandRun.getId(), false);
    }

    @Subcommand("captureCenter")
    public void launchCaptureCenter(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(captureCenter.getId(), true);
    }

    @Subcommand("captureCenterNoIntro")
    public void launchCaptureCenterNoIntro(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(captureCenter.getId(), false);
    }

    @Subcommand("football")
    public void launchFootball(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(football.getId(), true);
    }

    @Subcommand("footballNoIntro")
    public void launchFootballNoIntro(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(football.getId(), false);
    }

    @Subcommand("vote")
    public void launchVoting(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.launchVotingSystem();
    }

    private boolean checkGameRunning() {
        return MCEMainController.checkGameRunning();
    }
}
