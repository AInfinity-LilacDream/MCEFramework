package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
SummonGhost: 召唤冤魂
usage: /summonGhost sb <玩家名>
 */
@CommandAlias("summonGhost")
@CommandPermission("summonGhost.use")
public class SummonGhost extends BaseCommand {

    @Subcommand("sb")
    public void summonGhostSb(CommandSender sender, String targetPlayerName) {
        if (!(sender instanceof Player commandSender)) {
            sender.sendMessage(Component.text("只有玩家才能使用此命令！").color(NamedTextColor.RED));
            return;
        }

        // 查找目标玩家
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            commandSender.sendMessage(Component.text("找不到玩家: " + targetPlayerName).color(NamedTextColor.RED));
            return;
        }

        // 检查目标玩家是否在线
        if (!targetPlayer.isOnline()) {
            commandSender.sendMessage(Component.text("目标玩家不在线！").color(NamedTextColor.RED));
            return;
        }

        // 在目标玩家位置生成溺尸
        Drowned ghost = targetPlayer.getWorld().spawn(targetPlayer.getLocation(), Drowned.class, drowned -> {
            // 设置名字
            String ghostName = commandSender.getName() + " 的冤魂";
            drowned.customName(Component.text(ghostName));
            drowned.setCustomNameVisible(true);
            
            // 设置为不自然消失
            drowned.setRemoveWhenFarAway(false);
            
            // 设置为不在水中自然生成（避免AI冲突）
            drowned.setShouldBurnInDay(false);
        });

        // 创建下界合金装备
        ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
        ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
        
        // 添加保护2附魔
        ItemMeta helmetMeta = helmet.getItemMeta();
        if (helmetMeta != null) {
            helmetMeta.addEnchant(Enchantment.PROTECTION, 2, true);
            helmet.setItemMeta(helmetMeta);
        }
        
        ItemMeta chestplateMeta = chestplate.getItemMeta();
        if (chestplateMeta != null) {
            chestplateMeta.addEnchant(Enchantment.PROTECTION, 2, true);
            chestplate.setItemMeta(chestplateMeta);
        }
        
        ItemMeta leggingsMeta = leggings.getItemMeta();
        if (leggingsMeta != null) {
            leggingsMeta.addEnchant(Enchantment.PROTECTION, 2, true);
            leggings.setItemMeta(leggingsMeta);
        }
        
        ItemMeta bootsMeta = boots.getItemMeta();
        if (bootsMeta != null) {
            bootsMeta.addEnchant(Enchantment.PROTECTION, 2, true);
            boots.setItemMeta(bootsMeta);
        }
        
        // 创建下界合金剑（锋利3，火焰附加）
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.addEnchant(Enchantment.SHARPNESS, 3, true);
            swordMeta.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
            sword.setItemMeta(swordMeta);
        }
        
        // 装备物品
        ghost.getEquipment().setHelmet(helmet);
        ghost.getEquipment().setChestplate(chestplate);
        ghost.getEquipment().setLeggings(leggings);
        ghost.getEquipment().setBoots(boots);
        ghost.getEquipment().setItemInMainHand(sword);
        
        // 设置装备不掉落
        ghost.getEquipment().setHelmetDropChance(0f);
        ghost.getEquipment().setChestplateDropChance(0f);
        ghost.getEquipment().setLeggingsDropChance(0f);
        ghost.getEquipment().setBootsDropChance(0f);
        ghost.getEquipment().setItemInMainHandDropChance(0f);
        
        // 添加药水效果：速度5（放大等级4）和隐身
        ghost.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 4, false, false));
        ghost.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        
        // 锁定目标玩家
        ghost.setTarget(targetPlayer);
        
        // 创建定时任务，每3秒发送消息
        new BukkitRunnable() {
            @Override
            public void run() {
                // 检查冤魂和目标是否还存在
                if (!ghost.isValid() || !targetPlayer.isOnline()) {
                    this.cancel();
                    return;
                }
                
                // 发送消息到聊天栏
                String message = targetPlayer.getName() + "，555我好菜555";
                Bukkit.broadcast(Component.text(message).color(NamedTextColor.RED));
            }
        }.runTaskTimer(plugin, 60L, 60L); // 60 ticks = 3秒
        
        commandSender.sendMessage(Component.text("已召唤冤魂！").color(NamedTextColor.GREEN));
    }
}

