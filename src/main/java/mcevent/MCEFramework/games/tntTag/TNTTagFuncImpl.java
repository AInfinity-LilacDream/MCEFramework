package mcevent.MCEFramework.games.tntTag;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETimerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
TNTTagFuncImpl: 封装TNTTag游戏逻辑函数
*/
public class TNTTagFuncImpl {

    // 爆炸TNT携带者
    protected static void explodeTNTCarrier(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();

        if (world != null) {
            // 播放爆炸声音
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

            // 生成爆炸粒子效果
            world.spawnParticle(Particle.EXPLOSION, location, 10, 1.0, 1.0, 1.0, 0.1);
            world.spawnParticle(Particle.LAVA, location, 20, 2.0, 2.0, 2.0, 0.1);
            world.spawnParticle(Particle.FLAME, location, 30, 1.5, 1.5, 1.5, 0.1);
        }

        // 清除头盔
        player.getInventory().setHelmet(null);

        MCEMessenger.sendInfoToPlayer("<red>[💥] 你爆炸了！</red>", player);
    }

    // 发送最终结果
    protected static void sendFinalResults() {
        if (tnttag.getAlivePlayers().size() == 1) {
            Player winner = tnttag.getAlivePlayers().get(0);
            MCEMessenger.sendGlobalTitle("<gold><bold>🎉 游戏结束！ 🎉</bold></gold>",
                    "<yellow>胜利者：" + winner.getName() + "</yellow>");
        } else {
            MCEMessenger.sendGlobalTitle("<gold><bold>🎉 游戏结束！ 🎉</bold></gold>",
                    "<yellow>没有胜利者</yellow>");
        }

        // 延迟显示详细统计
        MCETimerUtils.setDelayedTask(5, () -> {
            MCEMessenger.sendGlobalText("<newline><gold><bold>=== 丢锅大战 结果统计 ===</bold></gold>");
            var survivors = tnttag.getAlivePlayers();
            List<Player> rankList = new ArrayList<>(64);
            Map<UUID, Integer> temp = new HashMap<>(tnttag.getDeathOrder());
            while (!temp.isEmpty()) {
                var uuid = Collections.max(temp.entrySet(), Map.Entry.comparingByValue()).getKey();
                temp.remove(uuid);
                rankList.add(Bukkit.getPlayer(uuid));
            }
            if (!survivors.isEmpty()) {
                StringJoiner joiner = new StringJoiner("<green><bold>, </bold></green>");
                survivors.stream()
                        .map(MCEPlayerUtils::getColoredPlayerName)
                        .forEach(joiner::add);
                MCEMessenger.sendGlobalText("<newline><green><bold>🏆 胜利者：" + joiner + "</bold></green>");
            }
            MCEMessenger.sendGlobalText("<newline><red><bold>📊 排行榜：</bold></red><newline>");
            survivors.stream()
                    .map(MCEPlayerUtils::getColoredPlayerName)
                    .forEach(name -> MCEMessenger.sendGlobalText("<red>① </red>" + name + "<green> 存活</green>"));
            var size = survivors.size();
            if (size < 5) {
                int extra = 5 - size;
                for (int i = 0; i < extra; i++) {
                    int rank = size + i + 1;
                    if (!tnttag.getDeathOrder().isEmpty()) {
                        var uuid = Collections.max(tnttag.getDeathOrder().entrySet(), Map.Entry.comparingByValue()).getKey();
                        var player = Bukkit.getPlayer(uuid);
                        var id = tnttag.getDeathOrder().remove(uuid);
                        String coloredName = MCEPlayerUtils.getColoredPlayerName(player);
                        String ordinal = number2OrdinalString(rank);
                        MCEMessenger.sendGlobalText("<red>" + ordinal + " </red>" + coloredName + "<red> 淘汰于第 " + id + " 轮</red>");
                    } else {
                        break;
                    }
                }
            }
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (survivors.contains(player)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<newline><bold><red>🥇 您的名次是：</red><gold>第 1 名</gold></bold><newline>"));
                } else {
                    if (!rankList.contains(player)) return;
                    int rank = survivors.size() + rankList.indexOf(player) + 1;
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<newline><bold><red>🥇 您的名次是：</red><gold>第 " + rank + " 名</gold></bold><newline>"));
                }
            });
        });
    }

    private static String number2OrdinalString(int n) {
        return switch (n) {
            case 1 -> "①";
            case 2 -> "②";
            case 3 -> "③";
            case 4 -> "④";
            case 5 -> "⑤";
            default -> "";
        };
    }
}