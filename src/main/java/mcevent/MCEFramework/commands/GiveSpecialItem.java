package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/*
GiveSpecialItem: 给予特殊物品
usage: /giveSpecialItem sg_chest_selector
 */
@CommandAlias("giveSpecialItem")
@CommandPermission("giveSpecialItem.use")
public class GiveSpecialItem extends BaseCommand {

    @Subcommand("sg_chest_selector")
    public void onSgChestSelector(Player player) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("箱子标注器")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        item.setItemMeta(meta);
        player.getInventory().addItem(item);

        player.sendMessage(Component.text("已获得箱子标注器！").color(NamedTextColor.GREEN));
    }

    @Subcommand("wind_charge_launcher")
    public void onWindChargeLauncher(Player player) {
        ItemStack blazeRod = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = blazeRod.getItemMeta();
        if (meta != null) {
            net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage
                    .miniMessage();
            java.util.List<net.kyori.adventure.text.Component> lore = java.util.Arrays.asList(
                    mm.deserialize("<yellow>右键发射风弹</yellow>"),
                    mm.deserialize("<gray>击中玩家给予发光效果</gray>"),
                    mm.deserialize("<red>冷却时间: 3秒</red>"));
            meta.displayName(mm.deserialize("<red><bold>风弹发射器</bold></red>"));
            meta.lore(lore);
            blazeRod.setItemMeta(meta);
        }
        player.getInventory().addItem(blazeRod);
        player.sendMessage(Component.text("已获得风弹发射器！").color(NamedTextColor.GREEN));
    }
}
