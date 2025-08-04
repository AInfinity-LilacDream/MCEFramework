package mcevent.MCEFramework.tools;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static org.bukkit.Bukkit.getLogger;

/*
MCEConfigParser: 读取游戏配置文件的基类
 */
@Getter @Setter
public class MCEConfigParser {
    protected Path dataFolderPath = plugin.getDataPath();
    protected Path configPath;

    protected List<String> lines;

    public ArrayList<Component> openAndParse(String configFileName) {
        open(configFileName);
        parse();
        return readIntro();
    }

    protected void open(String configFileName) {
        configPath = dataFolderPath.resolve(configFileName);

        // 文件不存在，则从资源目录复制默认配置文件
        try {
            Files.createDirectories(configPath.getParent());

            if (!Files.exists(configPath)) {
                try (InputStream defaultConfig = plugin.getResource(configFileName)) {
                    if (defaultConfig != null)
                        Files.copy(defaultConfig, configPath);
                    else
                        Files.createFile(configPath);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected ArrayList<Component> readIntro() {
        ArrayList<Component> introLines = new ArrayList<>();
        boolean inIntroSection = false;
        StringBuilder currentBlock = new StringBuilder(); // 用于累积当前引号内的内容
        boolean inQuote = false; // 标记是否正在处理被引号包围的块

        List<String> allLines;

        try {
            allLines = Files.readAllLines(configPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String line : allLines) {
            String trimmedLine = line.trim();

            // 检查是否进入 [Intro] 部分
            if (trimmedLine.equals("[Intro]")) {
                inIntroSection = true;
                continue;
            }

            if (line.startsWith("[")) {
                inIntroSection = false;
                if (inQuote) {
                    inQuote = false;
                    currentBlock.setLength(0);
                }
                continue;
            }

            // 只在 [Intro] 部分内处理
            if (inIntroSection) {
                if (trimmedLine.startsWith("\"") && !inQuote) {
                    inQuote = true;
                    currentBlock.append(trimmedLine.substring(1));
                    continue;
                }

                if (inQuote && trimmedLine.endsWith("\"")) {
                    String content = trimmedLine.substring(0, trimmedLine.length() - 1);
                    currentBlock.append(content);
                    try {
                        Component component = MiniMessage.miniMessage().deserialize(currentBlock.toString());
                        introLines.add(component);
                    } catch (Exception e) {
                         plugin.getLogger().severe("Error parsing MiniMessage in Intro block: " + e.getMessage());
                    }
                    // 重置状态
                    inQuote = false;
                    currentBlock.setLength(0); // 清空 StringBuilder
                    continue;
                }

                if (inQuote) {
                    currentBlock.append(line);
                    continue;
                }
            }
        }

        // 循环结束后，检查是否还有未关闭的引号块（配置文件末尾未正确关闭）
        if (inQuote) {
             plugin.getLogger().warning("Unclosed quote block at the end of file.");
            // 尝试处理这个未关闭的块
            try {
                Component component = MiniMessage.miniMessage().deserialize(currentBlock.toString());
                introLines.add(component);
            } catch (Exception e) {
                 plugin.getLogger().severe("Error parsing incomplete Intro block: " + e.getMessage());
            }
        }

        return introLines;
    }

    // 接口
    protected void parse() {}
}
