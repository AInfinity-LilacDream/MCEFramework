package mcevent.MCEFramework.games.discoFever;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;
import org.bukkit.Material;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
DiscoFeverConfigParser: 读取游戏配置文件
 */
@Getter @Setter
public class DiscoFeverConfigParser extends MCEConfigParser {
    private List<Double> timeList = new ArrayList<>();
    private List<Material> materialList = new ArrayList<>();
    private int maxState = 0;

    public void parse() {
        timeList.clear();
        materialList.clear();
        maxState = 0;

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
                    case "time":
                        for (String value : trimmedLine.split(",")) {
                            String v = value.trim();
                            if (!v.isEmpty()) timeList.add(Double.parseDouble(v));
                        }
                        break;
                    case "material":
                        for (String matStr : trimmedLine.split(",")) {
                            String matName = matStr.trim();
                            materialList.add(Material.getMaterial(matName));
                        }
                        break;
                    case "max_state":
                        maxState = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                }
            }
        }
        plugin.getLogger().info("配置文件读取完毕");
    }
}
