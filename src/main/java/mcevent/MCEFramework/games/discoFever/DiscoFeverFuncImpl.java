package mcevent.MCEFramework.games.discoFever;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.discoFever.gameObject.DiscoFeverGameBoard;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
DiscoFeverFuncImpl: 封装DiscoFever游戏逻辑函数
 */
public class DiscoFeverFuncImpl {

    private static final DiscoFeverConfigParser discoFeverConfigParser = discoFever.getDiscoFeverConfigParser();

    // 从配置文件加载数据
    protected static void loadConfig() {
        discoFever
                .setIntroTextList(discoFever.getDiscoFeverConfigParser().openAndParse(discoFever.getConfigFileName()));
        discoFever.setCurrentState(0);
        discoFever.setMaxState(discoFeverConfigParser.getMaxState());
        discoFever.setTimeList(discoFeverConfigParser.getTimeList());
        discoFever.setMaterialList(discoFeverConfigParser.getMaterialList());
    }

    // 生成一个新的随机平台
    protected static void generateNewPlatform(Location baseLoc) {
        Random rand = new Random();

        int x = baseLoc.getBlockX();
        int y = baseLoc.getBlockY();
        int z = baseLoc.getBlockZ();
        int blockCount = discoFever.getMaterialList().size();

        for (int i = x; i < x + 20; ++i) {
            for (int j = z; j < z + 20; ++j) {
                Location loc = new Location(Bukkit.getWorld(discoFever.getWorldName()), i, y, j);
                Material type = discoFever.getMaterialList().get(rand.nextInt(blockCount));
                Block block = loc.getBlock();
                block.setType(type);
            }
        }

        for (int i = x - 4; i < x; ++i) {
            for (int j = z; j < z + 20; ++j) {
                Location loc = new Location(Bukkit.getWorld(discoFever.getWorldName()), i, y, j);
                Block block = loc.getBlock();
                block.setType(Material.LIGHT_GRAY_CONCRETE);
            }
        }
    }

    // 更新平台，保留指定方块，后部塌陷
    protected static void updatePlatform(Location baseLoc, Material material, String worldName) {
        int x = baseLoc.getBlockX();
        int y = baseLoc.getBlockY();
        int z = baseLoc.getBlockZ();

        for (int i = x; i < x + 20; ++i) {
            for (int j = z; j < z + 20; ++j) {
                Location loc = new Location(Bukkit.getWorld(worldName), i, y, j);
                Block block = loc.getBlock();
                if (block.getType() != material)
                    block.setType(Material.AIR);
            }
        }

        for (int i = x - 4; i < x; ++i) {
            for (int j = z; j < z + 20; ++j) {
                Location loc = new Location(Bukkit.getWorld(worldName), i, y, j);
                Block block = loc.getBlock();
                block.setType(Material.LIGHT_GRAY_CONCRETE_POWDER);
            }
        }
    }

    // 平台前移
    protected static void updateCurrentPlatformLocation() {
        Location loc = discoFever.getCurrentPlatformLocation();
        discoFever.setCurrentPlatformLocation(new Location(
                loc.getWorld(),
                loc.getX() + 4,
                loc.getY(),
                loc.getZ()));
    }

    protected static void resetPlatform(String worldName) {
        Location baseLoc = new Location(Bukkit.getWorld(worldName), 4, 6, 0); // 使用传入的世界名称

        int x = baseLoc.getBlockX();
        int y = baseLoc.getBlockY();
        int z = baseLoc.getBlockZ();

        for (int i = x; i < x + 300; ++i) {
            for (int j = z; j < z + 20; ++j) {
                Location loc = new Location(Bukkit.getWorld(worldName), i, y, j);
                Block block = loc.getBlock();
                block.setType(Material.AIR);
            }
        }

        for (int i = x - 4; i < x; ++i) {
            for (int j = z; j < z + 20; ++j) {
                Location loc = new Location(Bukkit.getWorld(worldName), i, y, j);
                Block block = loc.getBlock();
                block.setType(Material.LIGHT_GRAY_CONCRETE);
            }
        }
    }

    // 给予玩家指示方块
    protected static void fillPlayerInventoryWithBlock(Material material) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerInventory inventory = player.getInventory();
            ItemStack item = new ItemStack(material, 1);

            for (int i = 0; i < 36; ++i) {
                inventory.setItem(i, item);
            }

            inventory.setItemInOffHand(item);
        }
    }

    // 初始化游戏展示板
    protected static void resetGameBoard() {
        DiscoFeverGameBoard gameBoard = (DiscoFeverGameBoard) discoFever.getGameBoard();
        // 基于 Participant 的初始化统计
        gameBoard.updatePlayerRemainTitle(0);
        java.util.List<Team> teams = discoFever.getActiveTeams();
        int teamSize = teams != null ? teams.size() : 0;
        gameBoard.setTeamRemainCount(
                mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipantTeams());
        for (int i = 0; i < teamSize; ++i)
            gameBoard.getTeamRemain()[i] = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getScoreboardTags().contains("Participant") || player.getGameMode() == GameMode.SPECTATOR)
                continue;
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            int idx = discoFever.getTeamId(team);
            if (idx >= 0 && idx < gameBoard.getTeamRemain().length)
                gameBoard.getTeamRemain()[idx]++;
        }
        gameBoard.updateTeamRemainTitle(null);
    }

    // 发送获胜消息，并退出游戏
    protected static void sendWinningMessage() {
        MCEMessenger.sendGlobalText("<newline><yellow><bold>=== 色盲狂热 结果统计 ===</bold></yellow>");
        List<Player> survivors = Bukkit.getOnlinePlayers().stream()
                .filter(pl -> pl.getGameMode() != GameMode.SPECTATOR)
                .collect(Collectors.toList());
        List<Player> rankList = new ArrayList<>(64);
        Map<UUID, Integer> temp = new HashMap<>(discoFever.getDeathOrder());
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
                if (!discoFever.getDeathOrder().isEmpty()) {
                    var uuid = Collections.max(discoFever.getDeathOrder().entrySet(), Map.Entry.comparingByValue()).getKey();
                    var player = Bukkit.getPlayer(uuid);
                    var id = discoFever.getDeathOrder().remove(uuid);
                    String coloredName = MCEPlayerUtils.getColoredPlayerName(player);
                    String ordinal = number2OrdinalString(rank);
                    MCEMessenger.sendGlobalText("<red>" + ordinal + " </red>" + coloredName + "<red> 淘汰于第 " + id + " 个平台</red>");
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

        MCEMainController.setRunningGame(false);
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
