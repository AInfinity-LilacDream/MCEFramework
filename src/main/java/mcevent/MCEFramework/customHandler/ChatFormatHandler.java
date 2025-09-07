package mcevent.MCEFramework.customHandler;

import io.papermc.paper.event.player.AsyncChatEvent;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.miscellaneous.Constants;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * ChatFormatHandler: 自定义聊天格式处理器
 * 将聊天消息格式化为: <带颜色的玩家名称（如果有队伍）>: xxx
 */
public class ChatFormatHandler extends MCEResumableEventHandler implements Listener {
    
    public ChatFormatHandler() {
        setSuspended(false); // 始终启用
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(AsyncChatEvent event) {
        if (isSuspended()) return;
        
        Player player = event.getPlayer();
        Component originalMessage = event.message();
        
        // 获取玩家队伍信息
        Team team = MCETeamUtils.getTeam(player);
        String teamName = team != null ? team.getName() : null;
        
        // 使用Component Builder API构建聊天消息
        Component chatMessage = Component.text()
                .append(createPlayerNameComponent(player, teamName))
                .append(Component.text(": ", NamedTextColor.WHITE))
                .append(originalMessage)
                .build();
        
        // 替换原始消息格式
        event.renderer((source, sourceDisplayName, message, viewer) -> chatMessage);
    }
    
    /**
     * 创建带颜色的玩家名称组件
     */
    private Component createPlayerNameComponent(Player player, String teamName) {
        if (teamName != null && !teamName.isEmpty()) {
            // 玩家有队伍，使用队伍颜色
            return MiniMessage.miniMessage().deserialize(MCEPlayerUtils.getColoredPlayerName(player));
        } else {
            // 玩家没有队伍，使用默认白色
            return Component.text(player.getName(), NamedTextColor.WHITE);
        }
    }
}