package mcevent.MCEFramework.games.votingSystem.gameObject;

import mcevent.MCEFramework.games.votingSystem.VotingSystemFuncImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VotingGUI: 投票界面GUI
 */
public class VotingGUI {

    private static final Component GUI_TITLE = MiniMessage.miniMessage()
            .deserialize("<gold><bold>选择下一个游戏</bold></gold>");

    // GUI布局配置
    private static final int GUI_SIZE = 27; // 3行
    private static final int[] GAME_SLOTS = { 1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 19 }; // 游戏ID 0-10、12、13、14 对应的槽位
    private static final int SKIP_INTRO_SLOT = 16; // 跳过Intro选项的槽位

    // 游戏对应的物品（按照Constants中的游戏ID顺序，ID 12和13在最后）
    private static final Material[] GAME_MATERIALS = {
            Material.NETHERITE_BOOTS, // ID 0: 瓮中捉鳖 - 下界合金靴子
            Material.MUSIC_DISC_PIGSTEP, // ID 1: 色盲狂热 - pigstep唱片
            Material.WHITE_STAINED_GLASS_PANE, // ID 2: 跃动音律 - Coming soon
            Material.RED_CONCRETE_POWDER, // ID 3: 落沙漫步 - 红色混凝土粉末
            Material.STICK, // ID 4: 占山为王 - 木棍
            Material.GRASS_BLOCK, // ID 5: 少林足球 - 草方块
            Material.IRON_PICKAXE, // ID 6: 惊天矿工团 - 铁镐
            Material.CROSSBOW, // ID 7: 暗矢狂潮 - 弩
            Material.TNT, // ID 8: 丢锅大战 - TNT
            Material.SNOWBALL, // ID 9: 冰雪掘战 - 雪球
            Material.COOKED_BEEF, // ID 10: 饥饿游戏 - 牛排
            Material.SNOW_BLOCK, // ID 12: 冰雪乱斗 - 雪块
            Material.ENDER_PEARL, // ID 13: 阴间游戏 - 末影珍珠
            Material.BRICKS // ID 14: 墙洞洞墙 - 砖块
    };

    // 游戏名称（按照Constants中的游戏ID顺序，ID 12和13在最后）
    private static final String[] GAME_NAMES = {
            "瓮中捉鳖", "色盲狂热", "Coming soon...", "落沙漫步", "占山为王", "少林足球", "惊天矿工团", "暗矢狂潮", "丢锅大战", "冰雪掘战", "饥饿游戏", "冰雪乱斗", "阴间游戏", "墙洞洞墙"
    };

    // 游戏描述
    private static final String[] GAME_DESCRIPTIONS = {
            "跑酷追逐游戏", "彩色平台生存", "暂不可用", "沙子下落生存", "占点竞技", "足球竞技", "挖掘生存大逃杀", "弩箭战斗竞技", "TNT传递生存", "雪球战斗竞技", "生存大逃杀",
            "超级掘一死战", "随机交换位置生存", "推墙闯关生存"
    };

    // 游戏ID到数组索引的映射（因为ID 12和13不在连续序列中）
    private static int getGameIndex(int gameId) {
        if (gameId == 12) {
            return 11; // ID 12映射到数组索引11
        }
        if (gameId == 13) {
            return 12; // ID 13映射到数组索引12
        }
        if (gameId == 14) {
            return 13; // ID 14映射到数组索引13
        }
        if (gameId >= 0 && gameId <= 10) {
            return gameId; // ID 0-10直接映射
        }
        return -1; // 无效ID
    }

    /**
     * 为玩家打开投票GUI
     */
    public static void openVotingGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        // 添加所有游戏选项
        for (int slotIndex = 0; slotIndex < GAME_SLOTS.length; slotIndex++) {
            ItemStack gameItem = createGameItem(slotIndex);
            if (gameItem != null) {
                gui.setItem(GAME_SLOTS[slotIndex], gameItem);
            }
        }

        // 添加跳过Intro选项
        ItemStack skipIntroItem = createSkipIntroItem(player);
        gui.setItem(SKIP_INTRO_SLOT, skipIntroItem);

        // 添加装饰性物品到空槽位
        fillEmptySlots(gui);

        player.openInventory(gui);
    }

    /**
     * 创建游戏选项物品
     */
    private static ItemStack createGameItem(int slotIndex) {
        // 根据槽位索引获取对应的游戏ID
        int gameId = getGameIdFromSlotIndex(slotIndex);
        if (gameId == -1) {
            return null; // 无效槽位
        }

        int gameIndex = getGameIndex(gameId);
        if (gameIndex == -1) {
            return null; // 无效游戏ID
        }

        // 用物品数量展示票数（最少为1以避免空堆显示异常）
        int votes = VotingSystemFuncImpl.getVotes(gameId);
        int amount = Math.min(64, Math.max(1, votes));
        ItemStack item = new ItemStack(GAME_MATERIALS[gameIndex], amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // 设置显示名称
            Component displayName = MiniMessage.miniMessage()
                    .deserialize("<gold><bold>" + GAME_NAMES[gameIndex] + "</bold></gold>");
            meta.displayName(displayName);

            // 设置描述 - 使用MiniMessage
            List<Component> lore = Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<gray>" + GAME_DESCRIPTIONS[gameIndex] + "</gray>"),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize("<yellow>当前票数: <white>" + votes + " 票</white></yellow>"),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize(gameId == 2 ? "<red>不可投票</red>" : "<green>▶ 点击投票</green>"));
            meta.lore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 根据槽位索引获取对应的游戏ID
     */
    private static int getGameIdFromSlotIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GAME_SLOTS.length) {
            return -1;
        }
        // 前11个槽位对应ID 0-10，第12个槽位对应ID 12，第13个槽位对应ID 13，第14个槽位对应ID 14
        if (slotIndex < 11) {
            return slotIndex;
        } else if (slotIndex == 11) {
            return 12; // hyperSpleef
        } else if (slotIndex == 12) {
            return 13; // underworldGame
        } else if (slotIndex == 13) {
            return 14; // 墙洞洞墙
        }
        return -1;
    }

    /**
     * 创建跳过Intro选项物品
     */
    private static ItemStack createSkipIntroItem(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // 根据当前状态设置显示名称和描述
            boolean currentSkipIntro = VotingSystemFuncImpl.isSkipIntro();

            if (currentSkipIntro) {
                Component displayName = MiniMessage.miniMessage()
                        .deserialize("<red><bold>跳过介绍 <green>[已启用]</green></bold></red>");
                meta.displayName(displayName);

                List<Component> lore = Arrays.asList(
                        MiniMessage.miniMessage().deserialize("<gray>下一个游戏将跳过介绍阶段</gray>"),
                        MiniMessage.miniMessage().deserialize("<gray>直接开始游戏</gray>"),
                        Component.empty(),
                        MiniMessage.miniMessage().deserialize("<red>▶ 点击禁用</red>"));
                meta.lore(lore);
            } else {
                Component displayName = MiniMessage.miniMessage()
                        .deserialize("<gold><bold>跳过介绍 <red>[已禁用]</red></bold></gold>");
                meta.displayName(displayName);

                List<Component> lore = Arrays.asList(
                        MiniMessage.miniMessage().deserialize("<gray>下一个游戏将播放完整介绍</gray>"),
                        MiniMessage.miniMessage().deserialize("<gray>然后开始游戏</gray>"),
                        Component.empty(),
                        MiniMessage.miniMessage().deserialize("<green>▶ 点击启用</green>"));
                meta.lore(lore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 检查点击的物品是否为游戏选项
     */
    public static int getGameIdFromSlot(int slot) {
        for (int slotIndex = 0; slotIndex < GAME_SLOTS.length; slotIndex++) {
            if (GAME_SLOTS[slotIndex] == slot) {
                return getGameIdFromSlotIndex(slotIndex);
            }
        }
        return -1; // 无效槽位
    }

    /**
     * 检查点击的槽位是否为跳过Intro选项
     */
    public static boolean isSkipIntroSlot(int slot) {
        return slot == SKIP_INTRO_SLOT;
    }

    /**
     * 检查是否为投票GUI (通过String标题)
     */
    public static boolean isVotingGUI(String title) {
        // 将传入的String标题转换为plaintext与我们的Component标题比较
        String plainTitle = MiniMessage.miniMessage().stripTags(MiniMessage.miniMessage().serialize(GUI_TITLE));
        return plainTitle.equals(title) || MiniMessage.miniMessage().serialize(GUI_TITLE).equals(title);
    }

    /**
     * 检查玩家是否打开了投票GUI (通过Player对象)
     */
    public static boolean isVotingGUI(Player player) {
        if (player.getOpenInventory() == null)
            return false;
        Component inventoryTitle = player.getOpenInventory().title();
        return GUI_TITLE.equals(inventoryTitle);
    }

    /**
     * 刷新所有玩家的投票GUI
     */
    public static void refreshAllVotingGUIs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() != null) {
                // 检查是否为我们的投票GUI，通过比较title Component
                Component inventoryTitle = player.getOpenInventory().title();
                if (GUI_TITLE.equals(inventoryTitle)) {
                    refreshVotingGUI(player);
                }
            }
        }
    }

    /**
     * 刷新单个玩家的投票GUI
     */
    public static void refreshVotingGUI(Player player) {
        Inventory inventory = player.getOpenInventory().getTopInventory();

        // 更新所有游戏选项
        for (int slotIndex = 0; slotIndex < GAME_SLOTS.length; slotIndex++) {
            ItemStack gameItem = createGameItem(slotIndex);
            if (gameItem != null) {
                inventory.setItem(GAME_SLOTS[slotIndex], gameItem);
            }
        }

        // 更新跳过Intro选项
        ItemStack skipIntroItem = createSkipIntroItem(player);
        inventory.setItem(SKIP_INTRO_SLOT, skipIntroItem);

        player.updateInventory();
    }

    /**
     * 用装饰性物品填充空槽位
     */
    private static void fillEmptySlots(Inventory gui) {
        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glassPane.getItemMeta();

        if (meta != null) {
            Component displayName = MiniMessage.miniMessage().deserialize("<gray> </gray>");
            meta.displayName(displayName);
            glassPane.setItemMeta(meta);
        }

        // 填充所有空槽位
        for (int slot = 0; slot < GUI_SIZE; slot++) {
            // 检查是否为游戏槽位
            boolean isGameSlot = false;
            for (int gameSlot : GAME_SLOTS) {
                if (slot == gameSlot) {
                    isGameSlot = true;
                    break;
                }
            }

            // 检查是否为跳过Intro槽位
            if (slot == SKIP_INTRO_SLOT) {
                isGameSlot = true;
            }

            // 如果不是游戏槽位或跳过Intro槽位，则填充装饰性物品
            if (!isGameSlot) {
                gui.setItem(slot, glassPane);
            }
        }
    }
}
