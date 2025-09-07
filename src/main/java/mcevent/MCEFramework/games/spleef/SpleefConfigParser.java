package mcevent.MCEFramework.games.spleef;

import mcevent.MCEFramework.tools.MCEConfigParser;
import net.kyori.adventure.text.Component;
import static mcevent.MCEFramework.miscellaneous.Constants.*;

import java.util.ArrayList;

/*
SpleefConfigParser: 冰雪掘战配置解析器
*/
public class SpleefConfigParser extends MCEConfigParser {

    @Override
    public ArrayList<Component> openAndParse(String configFileName) {
        ArrayList<Component> introTexts = new ArrayList<>();
        
        try {
            // 使用基类的方法读取配置文件
            open(configFileName);
            parse();
            
            // 解析intro文本，如果基类已经处理了intro就直接使用
            introTexts = readIntro();
            
            // 如果没有读到intro文本，使用默认的
            if (introTexts.isEmpty()) {
                introTexts = getDefaultIntroTexts();
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("无法读取冰雪掘战配置文件 " + configFileName + ": " + e.getMessage());
            
            // 使用默认的intro文本
            introTexts = getDefaultIntroTexts();
        }
        
        return introTexts;
    }
    
    
    /**
     * 获取默认的intro文本
     */
    private ArrayList<Component> getDefaultIntroTexts() {
        ArrayList<Component> defaultIntros = new ArrayList<>();
        
        defaultIntros.add(Component.text("§b§l=== 冰雪掘战 ==="));
        defaultIntros.add(Component.text(""));
        defaultIntros.add(Component.text("§f游戏规则:"));
        defaultIntros.add(Component.text("§7• 使用效率5金铲子铲雪"));
        defaultIntros.add(Component.text("§7• 铲雪可获得雪球"));
        defaultIntros.add(Component.text("§7• 手持金铲子右键连发雪球"));
        defaultIntros.add(Component.text("§7• 雪球可以击退敌人"));
        defaultIntros.add(Component.text("§c• 掉落到y < 26会被淘汰"));
        defaultIntros.add(Component.text(""));
        defaultIntros.add(Component.text("§e目标: 成为最后存活的队伍！"));
        defaultIntros.add(Component.text("§b祝你好运！"));
        
        return defaultIntros;
    }
    
}