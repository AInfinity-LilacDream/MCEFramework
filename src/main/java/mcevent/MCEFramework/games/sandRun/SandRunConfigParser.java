package mcevent.MCEFramework.games.sandRun;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;
import org.bukkit.Material;

import java.io.IOException;
import java.nio.file.Files;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;
import static mcevent.MCEFramework.miscellaneous.Constants.sandRun;

/*
SandRunConfigParser: SandRun 配置文件解析器
 */
@Getter @Setter
public class SandRunConfigParser extends MCEConfigParser {
    
    private long sandFallInterval = 10L; // 默认每0.5秒 (10 ticks)

    public void parse() {
        try {
            lines = Files.readAllLines(configPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String currentSection = null;

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) continue;

            if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                currentSection = trimmedLine.substring(1, trimmedLine.length() - 1);
                continue;
            }

            if (currentSection != null) {
                switch (currentSection) {
                    case "map_name":
                        for (String value : trimmedLine.split(",")) {
                            String v = value.trim();
                            sandRun.setWorldName(v);
                        }
                        break;
                    case "sand_fall_interval":
                        for (String matStr : trimmedLine.split(",")) {
                            String matName = matStr.trim();
                            sandFallInterval = Long.parseLong(matName);
                        }
                        break;
                }
            }
        }
        plugin.getLogger().info("配置文件读取完毕");
    }
}