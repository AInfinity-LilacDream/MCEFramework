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
}
