package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import mcevent.MCEFramework.tools.MCEMessenger;
import org.bukkit.command.CommandSender;

import mcevent.MCEFramework.miscellaneous.Constants;

/*
EnabldTeamSelection: 允许 / 禁止玩家自行选择队伍
usage: enabldTeamSelection

 ** 修改的是
 */
@CommandAlias("enableTeamSelection")
@CommandPermission("enableTeamSelection.use")
public class EnableTeamSelection extends BaseCommand {
	@Syntax("<true|false>")
	@CommandCompletion("true|false")
	public void enableTeamSelection(CommandSender sender, @Single String value) {
		value = value.toLowerCase();

		if (value.equals("true")) {
			Constants.enableTeamSelection = true;
			MCEMessenger.sendGlobalInfo("<green>玩家选择队伍已启用！</green>");
		} else if (value.equals("false")) {
			Constants.enableTeamSelection = false;
			MCEMessenger.sendGlobalInfo("<red>玩家选择队伍已禁用！</red>");
		} else {

		}
	}
}
