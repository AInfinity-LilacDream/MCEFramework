package mcevent.MCEFramework.games.football;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;
import java.io.IOException;
import java.nio.file.Files;
import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
FootballConfigParser: 读取足球游戏配置文件
*/
@Getter @Setter
public class FootballConfigParser extends MCEConfigParser {
    private String mapName = "";
    private int maxScore = 3;

    public void parse() {
        mapName = "";
        maxScore = 3;

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
                        mapName = trimmedLine;
                        currentSection = null;
                        break;
                    case "max_score":
                        maxScore = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                }
            }
        }
        plugin.getLogger().info("足球游戏配置文件读取完毕");
    }
}