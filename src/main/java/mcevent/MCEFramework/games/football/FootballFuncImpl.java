package mcevent.MCEFramework.games.football;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.football.gameObject.FootballGameBoard;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import net.kyori.adventure.util.TriState;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Armadillo;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
FootballFuncImpl: 封装Football游戏逻辑函数
*/
public class FootballFuncImpl {

    private static final FootballConfigParser footballConfigParser = football.getFootballConfigParser();

    // 从配置文件加载数据
    protected static void loadConfig() {
        football.setIntroTextList(football.getFootballConfigParser().openAndParse(football.getConfigFileName()));
        // 设置最大分数
        football.setMaxScore(footballConfigParser.getMaxScore());
    }

    // 确保只有红蓝两队，如果不是则重新分队
    protected static void ensureTwoTeamsSplit() {
        Scoreboard teamBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        List<Team> activeTeams = new ArrayList<>();
        
        // 获取有玩家的队伍
        for (Team team : teamBoard.getTeams()) {
            if (!team.getEntries().isEmpty()) {
                activeTeams.add(team);
            }
        }
        
        // 检查是否只有红蓝两队
        boolean hasRedTeam = false;
        boolean hasBlueTeam = false;
        
        for (Team team : activeTeams) {
            if (team.getName().equals(teams[0].teamName())) { // 红队
                hasRedTeam = true;
            } else if (team.getName().equals(teams[7].teamName())) { // 蓝队
                hasBlueTeam = true;
            }
        }
        
        // 如果队伍不符合要求，重新分队
        if (activeTeams.size() != 2 || !hasRedTeam || !hasBlueTeam) {
            MCEMessenger.sendGlobalInfo("<yellow>检测到队伍分配不正确，正在重新分队...</yellow>");
            divideTwoTeams();
        }
    }
    
    // 将玩家分为红蓝两队（类似DivideTeams的逻辑）
    private static void divideTwoTeams() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(onlinePlayers);
        
        Scoreboard teamBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // 清除现有队伍
        teamBoard.getTeams().forEach(Team::unregister);
        
        // 创建红队和蓝队
        Team redTeam = teamBoard.registerNewTeam(teams[0].teamName()); // 红色山楂
        redTeam.color(teams[0].teamColor());
        
        Team blueTeam = teamBoard.registerNewTeam(teams[7].teamName()); // 蓝色葡萄
        blueTeam.color(teams[7].teamColor());
        
        // 将玩家平均分配到两队
        for (int i = 0; i < onlinePlayers.size(); i++) {
            Player player = onlinePlayers.get(i);
            if (i % 2 == 0) {
                redTeam.addEntry(player.getName());
            } else {
                blueTeam.addEntry(player.getName());
            }
        }
        
        MCEMessenger.sendGlobalInfo("<green>重新分队完成！红队：" + redTeam.getEntries().size() + 
                                  "人，蓝队：" + blueTeam.getEntries().size() + "人</green>");
    }

    // 将玩家传送到对应队伍的出生点
    protected static void teleportPlayersToSpawns() {
        List<Player> redPlayers = new ArrayList<>();
        List<Player> bluePlayers = new ArrayList<>();
        
        // 分类玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team != null) {
                if (team.getName().equals(teams[0].teamName())) { // 红队
                    redPlayers.add(player);
                } else if (team.getName().equals(teams[7].teamName())) { // 蓝队
                    bluePlayers.add(player);
                }
            }
        }
        
        // 传送红队玩家
        Location[] redSpawns = football.getRedSpawns();
        for (int i = 0; i < redPlayers.size(); i++) {
            Location spawn = redSpawns[i % redSpawns.length];
            redPlayers.get(i).teleport(spawn);
        }
        
        // 传送蓝队玩家
        Location[] blueSpawns = football.getBlueSpawns();
        for (int i = 0; i < bluePlayers.size(); i++) {
            Location spawn = blueSpawns[i % blueSpawns.length];
            bluePlayers.get(i).teleport(spawn);
        }
    }

    // 生成犰狳作为球
    protected static void spawnBall() {
        // 移除已存在的球
        if (football.getBall() != null && !football.getBall().isDead()) {
            football.getBall().remove();
        }
        
        Location ballSpawn = football.getBallSpawn();
        Armadillo ball = (Armadillo) ballSpawn.getWorld().spawnEntity(ballSpawn, EntityType.ARMADILLO);

        // 设置犰狳属性
        ball.setAI(true); // 保持AI开启，这样可以被推动
        ball.setMaxHealth(1000.0); // 设置高血量而不是无敌
        ball.setHealth(1000.0);
        ball.setCustomNameVisible(false);
        ball.setSilent(true); // 禁止发出声音
        ball.setRemoveWhenFarAway(false); // 不会因为距离远而消失
        
        // 设置移动速度为0，防止自主移动
        Objects.requireNonNull(ball.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.0);
        
        // 使用PersistentDataContainer设置犰狳状态为蜷缩
        PersistentDataContainer container = ball.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, "state"), PersistentDataType.STRING, "scared");
        
        // 添加发光效果
        ball.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        
        football.setBall(ball);
        
        // 重置球的反弹处理器速度状态
        football.getBallBounceHandler().resetVelocity();
    }

    // 给所有玩家添加效果
    protected static void applyPlayerEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 添加发光效果
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
            
            // 禁止移动：设置移速和跳跃强度为0
            Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.0);
            Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.0);
        }
        
        // 给所有玩家发放武器
        giveWeaponsToAllPlayers();
    }
    
    // 给玩家发放武器
    private static void giveWeapons(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear(); // 清空背包
        
        // 创建击退一的木棍
        ItemStack woodStick = new ItemStack(Material.STICK);
        ItemMeta woodMeta = woodStick.getItemMeta();
        if (woodMeta != null) {
            woodMeta.displayName(MiniMessage.miniMessage().deserialize("<gray><bold>普通木棍（击退1）</bold></gray>"));
            woodStick.setItemMeta(woodMeta);
            woodStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
        }
        
        // 创建击退三的烈焰棒
        ItemStack knockbackStick = new ItemStack(Material.BLAZE_ROD);
        ItemMeta knockbackMeta = knockbackStick.getItemMeta();
        if (knockbackMeta != null) {
            knockbackMeta.displayName(MiniMessage.miniMessage().deserialize("<red><bold>强力击退棒（击退3）</bold></red>"));
            knockbackStick.setItemMeta(knockbackMeta);
            knockbackStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 3);
        }
        
        // 创建击退七的旋风棒
        ItemStack superKnockbackStick = new ItemStack(Material.BREEZE_ROD);
        ItemMeta superMeta = superKnockbackStick.getItemMeta();
        if (superMeta != null) {
            superMeta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>超级旋风棒（击退7）</bold></gold>"));
            superKnockbackStick.setItemMeta(superMeta);
            superKnockbackStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 7);
        }
        
        // 分发武器
        inventory.setItem(0, woodStick); // 第一个槽位
        inventory.setItem(1, knockbackStick); // 第二个槽位  
        inventory.setItem(2, superKnockbackStick); // 第三个槽位
    }
    
    // 给所有玩家发放武器 - 参考占山为王的逻辑
    private static void giveWeaponsToAllPlayers() {
        // 先清空所有玩家背包
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.ADVENTURE) {
                player.getInventory().clear();
            }
        }
        
        // 创建击退一的木棍
        ItemStack woodStick = new ItemStack(Material.STICK);
        ItemMeta woodMeta = woodStick.getItemMeta();
        if (woodMeta != null) {
            woodMeta.displayName(MiniMessage.miniMessage().deserialize("<gray><bold>普通木棍（击退1）</bold></gray>"));
            woodStick.setItemMeta(woodMeta);
            woodStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
        }
        
        // 创建击退三的烈焰棒
        ItemStack knockbackStick = new ItemStack(Material.BLAZE_ROD);
        ItemMeta knockbackMeta = knockbackStick.getItemMeta();
        if (knockbackMeta != null) {
            knockbackMeta.displayName(MiniMessage.miniMessage().deserialize("<red><bold>强力击退棒（击退3）</bold></red>"));
            knockbackStick.setItemMeta(knockbackMeta);
            knockbackStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 3);
        }
        
        // 创建击退七的旋风棒
        ItemStack superKnockbackStick = new ItemStack(Material.BREEZE_ROD);
        ItemMeta superMeta = superKnockbackStick.getItemMeta();
        if (superMeta != null) {
            superMeta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>超级旋风棒（击退7）</bold></gold>"));
            superKnockbackStick.setItemMeta(superMeta);
            superKnockbackStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 7);
        }
        
        // 给所有玩家分发武器
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.ADVENTURE) {
                player.getInventory().addItem(woodStick.clone());
                player.getInventory().addItem(knockbackStick.clone());
                player.getInventory().addItem(superKnockbackStick.clone());
            }
        }
        
        plugin.getLogger().info("已给予所有玩家足球装备");
    }
    
    // 移除移动限制
    protected static void removeMovementRestrictions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 恢复正常移速和跳跃
            Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
            Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.42);
        }
    }

    // 初始化游戏展示板
    protected static void resetGameBoard() {
        updateScoreboard();
    }
    
    // 更新计分板
    protected static void updateScoreboard() {
        FootballGameBoard gameBoard = (FootballGameBoard) football.getGameBoard();
        gameBoard.updateScores(football.getRedScore(), football.getBlueScore());
        
        // 统计队伍人数
        int redCount = 0, blueCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team != null) {
                if (team.getName().equals(teams[0].teamName())) {
                    redCount++;
                } else if (team.getName().equals(teams[7].teamName())) {
                    blueCount++;
                }
            }
        }
        gameBoard.updateTeamCounts(redCount, blueCount);
    }

    // 发送获胜消息
    protected static void sendWinningMessage() {
        String message;
        if (football.getRedScore() > football.getBlueScore()) {
            message = "<red><bold>红队获胜！</bold></red> 最终比分：红队 " + 
                     football.getRedScore() + " : " + football.getBlueScore() + " 蓝队";
        } else {
            message = "<blue><bold>蓝队获胜！</bold></blue> 最终比分：红队 " + 
                     football.getRedScore() + " : " + football.getBlueScore() + " 蓝队";
        }
        
        MCEMessenger.sendGlobalInfo(message);
        MCEMainController.setRunningGame(false);
    }
    
    // 检查位置是否在球门内
    public static boolean isInGoal(Location location, Location goalMin, Location goalMax) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        
        double minX = Math.min(goalMin.getX(), goalMax.getX());
        double maxX = Math.max(goalMin.getX(), goalMax.getX());
        double minY = Math.min(goalMin.getY(), goalMax.getY());
        double maxY = Math.max(goalMin.getY(), goalMax.getY());
        double minZ = Math.min(goalMin.getZ(), goalMax.getZ());
        double maxZ = Math.max(goalMin.getZ(), goalMax.getZ());
        
        boolean isInGoal = x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        
        // 添加调试信息
        if (Math.abs(x - minX) < 2 || Math.abs(x - maxX) < 2) { // 当球接近球门时输出调试信息
            plugin.getLogger().info(String.format("[进球检测] 球位置(%.2f,%.2f,%.2f) 球门范围X[%.1f-%.1f] Y[%.1f-%.1f] Z[%.1f-%.1f] 结果:%s", 
                x, y, z, minX, maxX, minY, maxY, minZ, maxZ, isInGoal));
        }
        
        return isInGoal;
    }
}