package mcevent.MCEFramework.games.tntTag;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;
import java.io.IOException;
import java.nio.file.Files;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
TNTTagConfigParser: 读取TNTTag游戏配置文件
*/
@Getter @Setter
public class TNTTagConfigParser extends MCEConfigParser {
    private String mapName = "";
    private int phaseTimeSeconds = 30;
    private int transitionTimeSeconds = 5;
    private int minPlayersForTwoCarriers = 5;

    public void parse() {
        mapName = "";
        phaseTimeSeconds = 30;
        transitionTimeSeconds = 5;
        minPlayersForTwoCarriers = 5;

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
                    case "phase_time_seconds":
                        phaseTimeSeconds = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "transition_time_seconds":
                        transitionTimeSeconds = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "min_players_for_two_carriers":
                        minPlayersForTwoCarriers = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                }
            }
        }
        plugin.getLogger().info("TNTTag配置文件读取完毕");
    }
}