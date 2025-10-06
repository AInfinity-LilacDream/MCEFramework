package mcevent.MCEFramework.games.survivalGame.customHandler;

import mcevent.MCEFramework.tools.MCEMessenger;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/**
 * ChestSelectorHandler: 箱子标注器处理器
 * 用于在 SurvivalGame 配置文件中添加或移除箱子位置
 */
public class ChestSelectorHandler implements Listener {

    private static final String CHEST_SELECTOR_NAME = "箱子标注器";
    private static final String CONFIG_FILE = "MCEConfig/SurvivalGame.cfg";

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 检查是否手持箱子标注器
        if (item == null || item.getType() != Material.BLAZE_ROD) {
            return;
        }

        if (!item.hasItemMeta()) {
            return;
        }

        Component itemName = item.getItemMeta().displayName();
        if (itemName == null) {
            return;
        }

        // 检查物品名称是否包含"箱子标注器"
        String plainText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(itemName);
        if (!plainText.contains(CHEST_SELECTOR_NAME)) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        event.setCancelled(true);

        // 检查是否为箱子
        if (clickedBlock.getType() != Material.CHEST) {
            MCEMessenger.sendActionBarMessageToPlayer(player, "<red>只能标注箱子！</red>");
            return;
        }

        Location chestLoc = clickedBlock.getLocation();
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_BLOCK) {
            // 左键：检查箱子是否已记录
            handleLeftClick(player, chestLoc);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            // 右键：切换箱子标注状态
            handleRightClick(player, chestLoc);
        }
    }

    private void handleLeftClick(Player player, Location chestLoc) {
        List<Location> chestLocations = loadChestLocations();
        boolean isRecorded = chestLocations.stream()
                .anyMatch(loc -> isSameLocation(loc, chestLoc));

        if (isRecorded) {
            MCEMessenger.sendActionBarMessageToPlayer(player, "<green>✓ 此箱子会生成随机物品！</green>");
        } else {
            MCEMessenger.sendActionBarMessageToPlayer(player, "<red>✗ 此箱子不会生成随机物品！</red>");
        }
    }

    private void handleRightClick(Player player, Location chestLoc) {
        List<Location> chestLocations = loadChestLocations();
        boolean isRecorded = chestLocations.stream()
                .anyMatch(loc -> isSameLocation(loc, chestLoc));

        if (isRecorded) {
            // 从配置文件中移除
            removeChestFromConfig(chestLoc);
            MCEMessenger.sendActionBarMessageToPlayer(player, "<yellow>已从配置文件中移除此箱子</yellow>");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        } else {
            // 添加到配置文件
            addChestToConfig(chestLoc);
            MCEMessenger.sendActionBarMessageToPlayer(player, "<green>已添加此箱子到配置文件</green>");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    private List<Location> loadChestLocations() {
        List<Location> locations = new ArrayList<>();
        Path configPath = plugin.getDataPath().resolve(CONFIG_FILE);

        try {
            if (!Files.exists(configPath)) {
                // 配置文件不存在，从资源目录复制
                Files.createDirectories(configPath.getParent());
                try (var input = plugin.getClass().getResourceAsStream("/" + CONFIG_FILE)) {
                    if (input != null) {
                        Files.copy(input, configPath);
                    }
                }
            }

            List<String> lines = Files.readAllLines(configPath);
            boolean inChestSection = false;
            String mapName = getMapNameFromConfig(lines);

            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.equals("[chest_loc]")) {
                    inChestSection = true;
                    continue;
                }

                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    inChestSection = false;
                    continue;
                }

                if (inChestSection && !trimmed.isEmpty()) {
                    Location loc = parseLocation(trimmed, mapName);
                    if (loc != null) {
                        locations.add(loc);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return locations;
    }

    private String getMapNameFromConfig(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals("[map_name]")) {
                if (i + 1 < lines.size()) {
                    return lines.get(i + 1).trim();
                }
            }
        }
        return "";
    }

    private Location parseLocation(String line, String worldName) {
        String[] parts = line.split(",");
        if (parts.length >= 3) {
            try {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                return new Location(org.bukkit.Bukkit.getWorld(worldName), x, y, z);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void addChestToConfig(Location chestLoc) {
        Path configPath = plugin.getDataPath().resolve(CONFIG_FILE);

        try {
            if (!Files.exists(configPath)) {
                // 配置文件不存在，从资源目录复制
                Files.createDirectories(configPath.getParent());
                try (var input = plugin.getClass().getResourceAsStream("/" + CONFIG_FILE)) {
                    if (input != null) {
                        Files.copy(input, configPath);
                    }
                }
            }

            List<String> lines = Files.readAllLines(configPath);
            int chestSectionIndex = -1;

            // 找到 [chest_loc] 部分
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().equals("[chest_loc]")) {
                    chestSectionIndex = i;
                    break;
                }
            }

            if (chestSectionIndex == -1) {
                return;
            }

            // 找到下一个 section 的位置（如果有）
            int nextSectionIndex = lines.size();
            for (int i = chestSectionIndex + 1; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    nextSectionIndex = i;
                    break;
                }
            }

            // 删除 [chest_loc] 标题之后到下一个 section 之前的所有空行（避免标题与首条之间空一行）
            for (int j = nextSectionIndex - 1; j >= chestSectionIndex + 1; j--) {
                if (lines.get(j).trim().isEmpty()) {
                    lines.remove(j);
                }
            }

            // 重新定位下一个 section（因为上面可能删除了行）
            nextSectionIndex = lines.size();
            for (int i = chestSectionIndex + 1; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    nextSectionIndex = i;
                    break;
                }
            }

            // 在 chest_loc 段末尾（下一个 section 之前）插入新坐标
            String chestLine = String.format("%.0f, %.0f, %.0f",
                    chestLoc.getBlockX() + 0.0,
                    chestLoc.getBlockY() + 0.0,
                    chestLoc.getBlockZ() + 0.0);
            lines.add(nextSectionIndex, chestLine);

            // 若存在后续 section，则确保段末与下一个 section 之间恰好留一空行
            if (nextSectionIndex + 1 < lines.size()) {
                // 此时 nextSectionIndex+1 应该是下一个 section 标题的位置
                String maybeSection = lines.get(nextSectionIndex + 1).trim();
                if (maybeSection.startsWith("[") && maybeSection.endsWith("]")) {
                    // 插入一个空行作为分隔
                    lines.add(nextSectionIndex + 1, "");
                }
            }

            Files.write(configPath, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeChestFromConfig(Location chestLoc) {
        Path configPath = plugin.getDataPath().resolve(CONFIG_FILE);

        try {
            if (!Files.exists(configPath)) {
                return;
            }

            List<String> lines = Files.readAllLines(configPath);
            List<String> newLines = new ArrayList<>();
            boolean inChestSection = false;
            String mapName = getMapNameFromConfig(lines);

            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.equals("[chest_loc]")) {
                    inChestSection = true;
                    newLines.add(line);
                    continue;
                }

                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    inChestSection = false;
                }

                if (inChestSection && !trimmed.isEmpty()) {
                    Location loc = parseLocation(trimmed, mapName);
                    if (loc != null && isSameLocation(loc, chestLoc)) {
                        // 跳过这一行（删除）
                        continue;
                    }
                }

                newLines.add(line);
            }

            Files.write(configPath, newLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isSameLocation(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }
}
