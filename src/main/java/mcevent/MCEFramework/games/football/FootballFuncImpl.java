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
    public static void loadConfig() {
        football.setIntroTextList(football.getFootballConfigParser().openAndParse(football.getConfigFileName()));
        // 设置最大分数
        football.setMaxScore(footballConfigParser.getMaxScore());
    }

    // 确保只有红蓝两队，如果不是则重新分队
    public static void ensureTwoTeamsSplit() {
        // 若设置为4队，交由ensureFourTeamsSplit处理
        if (mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams() == 4) {
            ensureFourTeamsSplit();
            return;
        }
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
    public static void teleportPlayersToSpawns() {
        int teams = mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams();
        if (teams == 4) {
            teleportPlayersToSpawnsFourTeams();
            return;
        }
        List<Player> redPlayers = new ArrayList<>();
        List<Player> bluePlayers = new ArrayList<>();

        // 分类玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team != null) {
                if (team.getName().equals(mcevent.MCEFramework.miscellaneous.Constants.teams[0].teamName())) { // 红队
                    redPlayers.add(player);
                } else if (team.getName().equals(mcevent.MCEFramework.miscellaneous.Constants.teams[7].teamName())) { // 蓝队
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

    // ==== 四队逻辑开始 ====
    private static void ensureFourTeamsSplit() {
        Scoreboard teamBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        List<Team> activeTeams = new ArrayList<>();

        for (Team team : teamBoard.getTeams()) {
            if (!team.getEntries().isEmpty())
                activeTeams.add(team);
        }

        String redName = teams[0].teamName();
        String blueName = teams[7].teamName();
        String cyanName = teams[5].teamName();
        String yellowName = teams[2].teamName();

        boolean hasRed = hasTeamByExactName(activeTeams, redName);
        boolean hasBlue = hasTeamByExactName(activeTeams, blueName);
        boolean hasCyan = hasTeamByExactName(activeTeams, cyanName);
        boolean hasYellow = hasTeamByExactName(activeTeams, yellowName);

        if (activeTeams.size() == 4 && hasRed && hasBlue && hasCyan && hasYellow)
            return;

        // 重新按红/蓝/青/黄平均分配
        divideFourTeams();
    }

    private static boolean hasTeamByExactName(List<Team> teamList, String exactName) {
        for (Team t : teamList) {
            if (t.getName() != null && t.getName().equals(exactName))
                return true;
        }
        return false;
    }

    private static void divideFourTeams() {
        Scoreboard teamBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        // 清除现有队伍
        teamBoard.getTeams().forEach(Team::unregister);

        // 使用全局定义的队伍名与颜色
        Team red = teamBoard.registerNewTeam(teams[0].teamName());
        red.color(teams[0].teamColor());
        Team blue = teamBoard.registerNewTeam(teams[7].teamName());
        blue.color(teams[7].teamColor());
        Team cyan = teamBoard.registerNewTeam(teams[5].teamName());
        cyan.color(teams[5].teamColor());
        Team yellow = teamBoard.registerNewTeam(teams[2].teamName());
        yellow.color(teams[2].teamColor());

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        java.util.Collections.shuffle(players);
        int i = 0;
        for (Player p : players) {
            String name = p.getName();
            switch (i % 4) {
                case 0 -> red.addEntry(name);
                case 1 -> blue.addEntry(name);
                case 2 -> cyan.addEntry(name);
                case 3 -> yellow.addEntry(name);
            }
            i++;
        }

        MCEMessenger.sendGlobalInfo("<green>四队重新分队完成！红:" + red.getEntries().size() +
                " 蓝:" + blue.getEntries().size() + " 青:" + cyan.getEntries().size() +
                " 黄:" + yellow.getEntries().size() + "</green>");
    }

    private static void teleportPlayersToSpawnsFourTeams() {
        List<Player> redPlayers = new ArrayList<>();
        List<Player> bluePlayers = new ArrayList<>();
        List<Player> cyanPlayers = new ArrayList<>();
        List<Player> yellowPlayers = new ArrayList<>();

        String redName = teams[0].teamName();
        String blueName = teams[7].teamName();
        String cyanName = teams[5].teamName();
        String yellowName = teams[2].teamName();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            String name = team.getName();
            if (redName.equals(name))
                redPlayers.add(player);
            else if (blueName.equals(name))
                bluePlayers.add(player);
            else if (cyanName.equals(name))
                cyanPlayers.add(player);
            else if (yellowName.equals(name))
                yellowPlayers.add(player);
        }

        // 传送各队，红/蓝用现有点，青/黄使用第二球场点位
        mcevent.MCEFramework.games.football.Football game = football;
        // 红蓝
        Location[] red = game.getRedSpawns();
        for (int i2 = 0; i2 < redPlayers.size(); i2++)
            redPlayers.get(i2).teleport(red[i2 % red.length]);
        Location[] blue = game.getBlueSpawns();
        for (int i2 = 0; i2 < bluePlayers.size(); i2++)
            bluePlayers.get(i2).teleport(blue[i2 % blue.length]);

        // 青黄（第二球场）
        Location[] cyanSpawns = game.getCyanSpawns();
        Location[] yellowSpawns = game.getYellowSpawns();
        for (int i2 = 0; i2 < cyanPlayers.size(); i2++)
            cyanPlayers.get(i2).teleport(cyanSpawns[i2 % cyanSpawns.length]);
        for (int i2 = 0; i2 < yellowPlayers.size(); i2++)
            yellowPlayers.get(i2).teleport(yellowSpawns[i2 % yellowSpawns.length]);
    }
    // ==== 四队逻辑结束 ====

    // 生成犰狳作为球
    public static void spawnBall() {
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

        // 使用PersistentDataContainer设置犰狳状态为蜷缩与标记球类型
        PersistentDataContainer container = ball.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, "state"), PersistentDataType.STRING, "scared");
        container.set(new NamespacedKey(plugin, "football_ball"), PersistentDataType.STRING, "rb");

        // 添加发光效果
        ball.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

        football.setBall(ball);

        // 重置球的反弹处理器速度状态
        football.getBallBounceHandler().resetVelocity();
    }

    // 生成第二球场犰狳
    public static void spawnBall2() {
        if (football.getBall2() != null && !football.getBall2().isDead()) {
            football.getBall2().remove();
        }
        Location spawn = football.getBallSpawn2();
        Armadillo ball = (Armadillo) spawn.getWorld().spawnEntity(spawn, EntityType.ARMADILLO);
        ball.setAI(true);
        ball.setMaxHealth(1000.0);
        ball.setHealth(1000.0);
        ball.setCustomNameVisible(false);
        ball.setSilent(true);
        ball.setRemoveWhenFarAway(false);
        Objects.requireNonNull(ball.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.0);
        ball.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        // 标记第二球场球
        ball.getPersistentDataContainer().set(new NamespacedKey(plugin, "football_ball"), PersistentDataType.STRING,
                "cy");
        football.setBall2(ball);
        football.getBallBounceHandler2().resetVelocity();
    }

    // 移除场内残留的足球（两块球场）
    public static void removeExistingBalls() {
        org.bukkit.World w = Bukkit.getWorld(football.getWorldName());
        if (w == null)
            return;
        int removed = 0;
        for (org.bukkit.entity.Entity e : w.getEntities()) {
            if (e instanceof Armadillo arm) {
                arm.remove();
                removed++;
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("[Football] 清理残留足球数量: " + removed);
        }
    }

    // 仅重置红蓝场玩家到出生点
    public static void teleportRBPlayersToSpawns() {
        java.util.List<Player> redPlayers = new java.util.ArrayList<>();
        java.util.List<Player> bluePlayers = new java.util.ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            if (team.getName().equals(teams[0].teamName()))
                redPlayers.add(player);
            else if (team.getName().equals(teams[7].teamName()))
                bluePlayers.add(player);
        }
        Location[] red = football.getRedSpawns();
        for (int i = 0; i < redPlayers.size(); i++)
            redPlayers.get(i).teleport(red[i % red.length]);
        Location[] blue = football.getBlueSpawns();
        for (int i = 0; i < bluePlayers.size(); i++)
            bluePlayers.get(i).teleport(blue[i % blue.length]);
    }

    // 仅重置青黄场玩家到出生点
    public static void teleportCYPlayersToSpawns() {
        java.util.List<Player> cyanPlayers = new java.util.ArrayList<>();
        java.util.List<Player> yellowPlayers = new java.util.ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            if (team.getName().equals(teams[5].teamName()))
                cyanPlayers.add(player);
            else if (team.getName().equals(teams[2].teamName()))
                yellowPlayers.add(player);
        }
        Location[] cyan = football.getCyanSpawns();
        for (int i = 0; i < cyanPlayers.size(); i++)
            cyanPlayers.get(i).teleport(cyan[i % cyan.length]);
        Location[] yellow = football.getYellowSpawns();
        for (int i = 0; i < yellowPlayers.size(); i++)
            yellowPlayers.get(i).teleport(yellow[i % yellow.length]);
    }

    // 给所有玩家添加效果
    public static void applyPlayerEffects() {
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

    // 仅对红蓝队施加准备阶段效果
    public static void applyPlayerEffectsRB() {
        String redName = teams[0].teamName();
        String blueName = teams[7].teamName();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            String name = team.getName();
            if (redName.equals(name) || blueName.equals(name)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.0);
                Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.0);
            }
        }
        giveWeaponsSubset(redName, blueName);
    }

    // 仅对青黄队施加准备阶段效果
    public static void applyPlayerEffectsCY() {
        String cyanName = teams[5].teamName();
        String yellowName = teams[2].teamName();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            String name = team.getName();
            if (cyanName.equals(name) || yellowName.equals(name)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.0);
                Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.0);
            }
        }
        giveWeaponsSubset(cyanName, yellowName);
    }

    private static void giveWeaponsSubset(String teamA, String teamB) {
        // 给指定两支队伍的玩家发放武器
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            String name = team.getName();
            if (!teamA.equals(name) && !teamB.equals(name))
                continue;
            if (player.getGameMode() == GameMode.SURVIVAL) {
                // 先清空背包，避免叠加
                player.getInventory().clear();
                // 创建与发放三把武器（与全体版本一致）
                ItemStack woodStick = new ItemStack(Material.STICK);
                ItemMeta woodMeta = woodStick.getItemMeta();
                if (woodMeta != null) {
                    woodMeta.displayName(MiniMessage.miniMessage().deserialize("<gray><bold>普通木棍（击退1）</bold></gray>"));
                    woodStick.setItemMeta(woodMeta);
                    woodStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
                }
                ItemStack knockbackStick = new ItemStack(Material.BLAZE_ROD);
                ItemMeta knockbackMeta = knockbackStick.getItemMeta();
                if (knockbackMeta != null) {
                    knockbackMeta
                            .displayName(MiniMessage.miniMessage().deserialize("<red><bold>强力击退棒（击退3）</bold></red>"));
                    knockbackStick.setItemMeta(knockbackMeta);
                    knockbackStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 3);
                }
                ItemStack superKnockbackStick = new ItemStack(Material.BREEZE_ROD);
                ItemMeta superMeta = superKnockbackStick.getItemMeta();
                if (superMeta != null) {
                    superMeta
                            .displayName(MiniMessage.miniMessage().deserialize("<gold><bold>超级旋风棒（击退7）</bold></gold>"));
                    superKnockbackStick.setItemMeta(superMeta);
                    superKnockbackStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 7);
                }
                player.getInventory().addItem(woodStick, knockbackStick, superKnockbackStick);
            }
        }
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
            if (player.getGameMode() == GameMode.SURVIVAL) {
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
            if (player.getGameMode() == GameMode.SURVIVAL) {
                player.getInventory().addItem(woodStick.clone());
                player.getInventory().addItem(knockbackStick.clone());
                player.getInventory().addItem(superKnockbackStick.clone());
            }
        }

        plugin.getLogger().info("已给予所有玩家足球装备");
    }

    // 移除移动限制
    public static void removeMovementRestrictions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 恢复正常移速和跳跃
            Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
            Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.42);
        }
    }

    // 仅对红蓝队解除移动限制
    public static void removeMovementRestrictionsRB() {
        String redName = teams[0].teamName();
        String blueName = teams[7].teamName();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            String name = team.getName();
            if (redName.equals(name) || blueName.equals(name)) {
                Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
                Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.42);
            }
        }
    }

    // 仅对青黄队解除移动限制
    public static void removeMovementRestrictionsCY() {
        String cyanName = teams[5].teamName();
        String yellowName = teams[2].teamName();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            String name = team.getName();
            if (cyanName.equals(name) || yellowName.equals(name)) {
                Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
                Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.42);
            }
        }
    }

    // 初始化游戏展示板
    public static void resetGameBoard() {
        updateScoreboard();
    }

    // 更新计分板
    public static void updateScoreboard() {
        FootballGameBoard gameBoard = (FootballGameBoard) football.getGameBoard();
        gameBoard.updateScores(football.getRedScore(), football.getBlueScore());
        gameBoard.updateScoresSecond(football.getCyanScore(), football.getYellowScore());

        // 统计队伍人数：红蓝与青黄分别统计
        int redCount = 0, blueCount = 0, cyanCount = 0, yellowCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team != null) {
                if (team.getName().equals(teams[0].teamName())) {
                    redCount++;
                } else if (team.getName().equals(teams[7].teamName())) {
                    blueCount++;
                } else if (team.getName().equals(teams[5].teamName())) {
                    cyanCount++;
                } else if (team.getName().equals(teams[2].teamName())) {
                    yellowCount++;
                }
            }
        }
        gameBoard.updateTeamCounts(redCount, blueCount);
        gameBoard.updateTeamCountsSecond(cyanCount, yellowCount);
        // 同步两块场地的回合标题（仅在四队模式时使用，避免两队模式误用）
        if (mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams() == 4) {
            gameBoard.updateRoundTitleRB(football.getRoundRB());
            gameBoard.updateRoundTitleCY(football.getRoundCY());
        }
    }

    // 发送获胜消息
    public static void sendWinningMessage() {
        // 两队模式：沿用原逻辑
        if (mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams() != 4) {
            String message;
            if (football.getRedScore() > football.getBlueScore()) {
                message = "<red><bold>红队获胜！</bold></red> 最终比分：红队 " + football.getRedScore() + " : "
                        + football.getBlueScore() + " 蓝队";
            } else {
                message = "<blue><bold>蓝队获胜！</bold></blue> 最终比分：红队 " + football.getRedScore() + " : "
                        + football.getBlueScore() + " 蓝队";
            }
            MCEMessenger.sendGlobalInfo(message);
            MCEMainController.setRunningGame(false);
            return;
        }

        // 四队模式：分别播报两场最终结果（若尚未播报）
        if (!football.isRbAnnounced()) {
            String rbMsg = (football.getRedScore() > football.getBlueScore())
                    ? ("<red><bold>红队获胜！</bold></red> 最终比分：红队 " + football.getRedScore() + " : "
                            + football.getBlueScore() + " 蓝队")
                    : ("<blue><bold>蓝队获胜！</bold></blue> 最终比分：红队 " + football.getRedScore() + " : "
                            + football.getBlueScore() + " 蓝队");
            MCEMessenger.sendGlobalInfo(rbMsg);
        }
        if (!football.isCyAnnounced()) {
            String cyMsg = (football.getCyanScore() > football.getYellowScore())
                    ? ("<dark_aqua><bold>青队获胜！</bold></dark_aqua> 最终比分：青 " + football.getCyanScore() + " : "
                            + football.getYellowScore() + " 黄")
                    : ("<yellow><bold>黄队获胜！</bold></yellow> 最终比分：青 " + football.getCyanScore() + " : "
                            + football.getYellowScore() + " 黄");
            MCEMessenger.sendGlobalInfo(cyMsg);
        }
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
            plugin.getLogger()
                    .info(String.format("[进球检测] 球位置(%.2f,%.2f,%.2f) 球门范围X[%.1f-%.1f] Y[%.1f-%.1f] Z[%.1f-%.1f] 结果:%s",
                            x, y, z, minX, maxX, minY, maxY, minZ, maxZ, isInGoal));
        }

        return isInGoal;
    }
}