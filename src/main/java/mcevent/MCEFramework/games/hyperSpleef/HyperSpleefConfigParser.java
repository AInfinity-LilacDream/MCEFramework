package mcevent.MCEFramework.games.hyperSpleef;

import mcevent.MCEFramework.tools.MCEConfigParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import static mcevent.MCEFramework.miscellaneous.Constants.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/*
HyperSpleefConfigParser: 冰雪乱斗配置解析器
*/
public class HyperSpleefConfigParser extends MCEConfigParser {

    private String mapName = null;

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
            plugin.getLogger().warning("无法读取冰雪乱斗配置文件 " + configFileName + ": " + e.getMessage());

            // 使用默认的intro文本
            introTexts = getDefaultIntroTexts();
        }

        return introTexts;
    }

    @Override
    protected void parse() {
        mapName = null;

        plugin.getLogger().info("HyperSpleef: parse() - configPath = " + configPath);

        try {
            lines = Files.readAllLines(configPath);
            plugin.getLogger().info("HyperSpleef: parse() - 读取了 " + lines.size() + " 行");

            // 打印前几行和最后几行
            for (int i = 0; i < Math.min(5, lines.size()); i++) {
                plugin.getLogger().info("HyperSpleef: parse() - 行 " + i + ": [" + lines.get(i) + "]");
            }
            if (lines.size() > 5) {
                for (int i = Math.max(5, lines.size() - 3); i < lines.size(); i++) {
                    plugin.getLogger().info("HyperSpleef: parse() - 行 " + i + ": [" + lines.get(i) + "]");
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("无法读取配置文件: " + e.getMessage());
            return;
        }

        String currentSection = null;

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty())
                continue;

            if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                currentSection = trimmedLine.substring(1, trimmedLine.length() - 1);
                plugin.getLogger().info("HyperSpleef: parse() - 进入节: " + currentSection);
                continue;
            }

            if (currentSection != null) {
                if ("map_name".equals(currentSection)) {
                    mapName = trimmedLine;
                    plugin.getLogger().info("HyperSpleef: 从配置文件读取到地图名称: " + mapName);
                    currentSection = null;
                    break;
                }
            }
        }

        plugin.getLogger().info("HyperSpleef: parse() - 最终 mapName = " + mapName);
    }

    /**
     * 获取地图名称
     */
    public String getMapName() {
        return mapName;
    }

    /**
     * 获取默认的intro文本
     */
    private ArrayList<Component> getDefaultIntroTexts() {
        ArrayList<Component> defaultIntros = new ArrayList<>();

        defaultIntros.add(MiniMessage.miniMessage().deserialize("<b><aqua>=== 冰雪乱斗 ===</aqua></b>"));
        defaultIntros.add(MiniMessage.miniMessage().deserialize(" "));
        defaultIntros.add(MiniMessage.miniMessage().deserialize("<yellow>游戏规则:</yellow>"));
        defaultIntros.add(MiniMessage.miniMessage().deserialize("<gray>• 使用工具挖掘方块, 丢弃雪球击退他人</gray>"));
        defaultIntros.add(MiniMessage.miniMessage().deserialize("<gray>• 右键金铲可以直接丢弃雪球</gray>"));
        defaultIntros.add(MiniMessage.miniMessage().deserialize("<gray>• 雪球触碰雪块后, 雪块变成浮冰, 1s后浮冰变成冰</gray>"));
        defaultIntros.add(MiniMessage.miniMessage().deserialize("<gray>• 右键 TNT 会抛出一个已激活的 TNT</gray>"));
        defaultIntros.add(MiniMessage.miniMessage().deserialize("<gray>• 游戏过程中会有随机事件发生</gray>"));
        defaultIntros.add(MiniMessage.miniMessage().deserialize("<c>• 掉入虚空的玩家将会被淘汰</c>"));
        defaultIntros.add(MiniMessage.miniMessage().deserialize(" "));
        defaultIntros.add(MiniMessage.miniMessage().deserialize("<gold>目标: 成为最后存活的队伍！</gold>"));
        defaultIntros.add(MiniMessage.miniMessage().deserialize("<aqua>祝你好运！</aqua>"));

        return defaultIntros;
    }

}
