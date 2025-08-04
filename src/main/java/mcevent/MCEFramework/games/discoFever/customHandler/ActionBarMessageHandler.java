package mcevent.MCEFramework.games.discoFever.customHandler;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Team;

import java.util.Objects;

/*
ActionBarMessageHandler: 动作栏消息事件处理器
 */
public class ActionBarMessageHandler extends MCEResumableEventHandler {

    @Setter
    private String actionBarMessage;

    public void showMessage() {
        if (isSuspended()) return;

        MCEMessenger.sendGlobalActionBarMessage(actionBarMessage);
    }
}
