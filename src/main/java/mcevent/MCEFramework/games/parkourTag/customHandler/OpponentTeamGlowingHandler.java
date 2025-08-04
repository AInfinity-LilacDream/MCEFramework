package mcevent.MCEFramework.games.parkourTag.customHandler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import static mcevent.MCEFramework.miscellaneous.Constants.*;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

/*
敌对队伍发光处理器
 */
public class OpponentTeamGlowingHandler extends MCEResumableEventHandler {
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    private void setGlowing(Player target, Player viewer) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId());

        List<WrappedDataValue> dataValues = new ArrayList<>();
        dataValues.add(
                new WrappedDataValue(
                        0,
                        WrappedDataWatcher.Registry.get(Byte.class),
                        (byte) 0x40
                )
        );
        packet.getDataValueCollectionModifier().write(0, dataValues);

        try {
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toggleGlowing() {
        if (isSuspended()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            Team opponentTeam = pkt.getOpponentTeam(team);

            if (player.getScoreboardTags().contains("runner")) {
                for (Player runner : MCETeamUtils.getPlayers(team)) {
                    if (runner.getGameMode() == GameMode.SPECTATOR) continue;
                    if (runner.getScoreboardTags().contains("runner"))
                        setGlowing(runner, player);
                }
            }
            else if (player.getScoreboardTags().contains("chaser")) {
                for (Player runner : MCETeamUtils.getPlayers(opponentTeam)) {
                    if (runner.getGameMode() == GameMode.SPECTATOR) continue;
                    if (runner.getScoreboardTags().contains("runner"))
                        setGlowing(runner, player);
                }
            }
        }
    }
}
