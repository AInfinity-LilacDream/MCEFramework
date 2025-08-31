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
    
    // 游戏对应的物品（按照Constants中的游戏ID顺序）
    private static final Material[] GAME_MATERIALS = {
        Material.NETHERITE_BOOTS,    // ID 0: 瓮中捉鳖 - 下界合金靴子
        Material.MUSIC_DISC_PIGSTEP, // ID 1: 色盲狂热 - pigstep唱片
        Material.NOTE_BLOCK,         // ID 2: 跃动音律 - 音符盒
        Material.RED_CONCRETE_POWDER,// ID 3: 落沙漫步 - 红色混凝土粉末
        Material.STICK,              // ID 4: 占山为王 - 木棍
        Material.GRASS_BLOCK         // ID 5: 少林足球 - 草方块
    };
    
    // 游戏名称（按照Constants中的游戏ID顺序）
    private static final String[] GAME_NAMES = {
        "瓮中捉鳖", "色盲狂热", "跃动音律", "落沙漫步", "占山为王", "少林足球"
    };
    
    // 游戏描述
    private static final String[] GAME_DESCRIPTIONS = {
        "跑酷追逐游戏", "彩色平台生存", "音乐节奏躲避", "沙子下落生存", "占点竞技", "足球竞技"
    };

    /**
     * 为玩家打开投票GUI
     */
    public static void openVotingGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, GUI_TITLE);
        
        // 添加游戏选项（前3个游戏在槽位1-3，后3个游戏在槽位5-7）
        for (int i = 0; i < 3; i++) {
            ItemStack gameItem = createGameItem(i);
            gui.setItem(i + 1, gameItem); // 槽位1-3
        }
        for (int i = 3; i < 6; i++) {
            ItemStack gameItem = createGameItem(i);
            gui.setItem(i + 2, gameItem); // 槽位5-7（跳过中间槽位4）
        }
        
        // 添加跳过Intro选项（中间槽位）
        ItemStack skipIntroItem = createSkipIntroItem(player);
        gui.setItem(4, skipIntroItem); // 第5个槽位（中间）
        
        // 添加装饰性物品
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.text(" "));
            glass.setItemMeta(glassMeta);
        }
        gui.setItem(0, glass); // 第1个槽位
        gui.setItem(8, glass); // 第9个槽位
        
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
        // 槽位1-3对应游戏0-2，槽位5-7对应游戏3-5
        if (slot >= 1 && slot <= 3) {
            return slot - 1;
        } else if (slot >= 5 && slot <= 7) {
            return slot - 2;
        }
        return -1; // 无效槽位
    }
    
    /**
     * 检查点击的槽位是否为跳过Intro选项
     */
    public static boolean isSkipIntroSlot(int slot) {
        return slot == 4; // 中间槽位
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
        
        // 更新游戏选项物品（槽位1-3和5-7）
        for (int i = 0; i < 3; i++) {
            ItemStack gameItem = createGameItem(i);
            inventory.setItem(i + 1, gameItem); // 槽位1-3
        }
        for (int i = 3; i < 6; i++) {
            ItemStack gameItem = createGameItem(i);
            inventory.setItem(i + 2, gameItem); // 槽位5-7
        }
        
        // 更新跳过Intro选项物品（槽位4）
        ItemStack skipIntroItem = createSkipIntroItem(player);
        inventory.setItem(4, skipIntroItem);
        
        player.updateInventory();
    }
}