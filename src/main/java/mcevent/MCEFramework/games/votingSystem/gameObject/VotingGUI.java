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
    
    private static final Component GUI_TITLE = MiniMessage.miniMessage().deserialize("<gold><bold>选择下一个游戏</bold></gold>");
    
    // GUI布局配置
    private static final int GUI_SIZE = 27; // 3行
    private static final int[] GAME_SLOTS = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12}; // 游戏ID 0-9 对应的槽位，不包括投票系统
    private static final int SKIP_INTRO_SLOT = 16; // 跳过Intro选项的槽位
    
    // 游戏对应的物品（按照Constants中的游戏ID顺序）
    private static final Material[] GAME_MATERIALS = {
        Material.NETHERITE_BOOTS,    // ID 0: 瓮中捉鳖 - 下界合金靴子
        Material.MUSIC_DISC_PIGSTEP, // ID 1: 色盲狂热 - pigstep唱片
        Material.NOTE_BLOCK,         // ID 2: 跃动音律 - 音符盒
        Material.RED_CONCRETE_POWDER,// ID 3: 落沙漫步 - 红色混凝土粉末
        Material.STICK,              // ID 4: 占山为王 - 木棍
        Material.GRASS_BLOCK,        // ID 5: 少林足球 - 草方块
        Material.IRON_PICKAXE,       // ID 6: 惊天矿工团 - 铁镐
        Material.CROSSBOW,           // ID 7: 暗矢狂潮 - 弩
        Material.TNT,                // ID 8: 丢锅大战 - TNT
        Material.SNOWBALL            // ID 9: 冰雪掘战 - 雪球
    };
    
    // 游戏名称（按照Constants中的游戏ID顺序）
    private static final String[] GAME_NAMES = {
        "瓮中捉鳖", "色盲狂热", "跃动音律", "落沙漫步", "占山为王", "少林足球", "惊天矿工团", "暗矢狂潮", "丢锅大战", "冰雪掘战"
    };
    
    // 游戏描述
    private static final String[] GAME_DESCRIPTIONS = {
        "跑酷追逐游戏", "彩色平台生存", "音乐节奏躲避", "沙子下落生存", "占点竞技", "足球竞技", "挖掘生存大逃杀", "弩箭战斗竞技", "TNT传递生存", "雪球战斗竞技"
    };

    /**
     * 为玩家打开投票GUI
     */
    public static void openVotingGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        
        // 添加所有游戏选项
        for (int gameId = 0; gameId < GAME_SLOTS.length; gameId++) {
            ItemStack gameItem = createGameItem(gameId);
            gui.setItem(GAME_SLOTS[gameId], gameItem);
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
    private static ItemStack createGameItem(int gameId) {
        ItemStack item = new ItemStack(GAME_MATERIALS[gameId]);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // 设置显示名称
            Component displayName = MiniMessage.miniMessage().deserialize("<gold><bold>" + GAME_NAMES[gameId] + "</bold></gold>");
            meta.displayName(displayName);
            
            // 获取当前票数
            int votes = VotingSystemFuncImpl.getVotes(gameId);
            
            // 设置描述 - 使用MiniMessage
            List<Component> lore = Arrays.asList(
                MiniMessage.miniMessage().deserialize("<gray>" + GAME_DESCRIPTIONS[gameId] + "</gray>"),
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<yellow>当前票数: <white>" + votes + " 票</white></yellow>"),
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<green>▶ 点击投票</green>")
            );
            meta.lore(lore);
            
            item.setItemMeta(meta);
        }
        
        return item;
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
                Component displayName = MiniMessage.miniMessage().deserialize("<red><bold>跳过介绍 <green>[已启用]</green></bold></red>");
                meta.displayName(displayName);
                
                List<Component> lore = Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<gray>下一个游戏将跳过介绍阶段</gray>"),
                    MiniMessage.miniMessage().deserialize("<gray>直接开始游戏</gray>"),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize("<red>▶ 点击禁用</red>")
                );
                meta.lore(lore);
            } else {
                Component displayName = MiniMessage.miniMessage().deserialize("<gold><bold>跳过介绍 <red>[已禁用]</red></bold></gold>");
                meta.displayName(displayName);
                
                List<Component> lore = Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<gray>下一个游戏将播放完整介绍</gray>"),
                    MiniMessage.miniMessage().deserialize("<gray>然后开始游戏</gray>"),
                    Component.empty(),
                    MiniMessage.miniMessage().deserialize("<green>▶ 点击启用</green>")
                );
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
        for (int gameId = 0; gameId < GAME_SLOTS.length; gameId++) {
            if (GAME_SLOTS[gameId] == slot) {
                return gameId;
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
        if (player.getOpenInventory() == null) return false;
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
        for (int gameId = 0; gameId < GAME_SLOTS.length; gameId++) {
            ItemStack gameItem = createGameItem(gameId);
            inventory.setItem(GAME_SLOTS[gameId], gameItem);
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