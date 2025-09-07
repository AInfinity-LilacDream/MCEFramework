package mcevent.MCEFramework.games.extractOwn;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
ExtractOwnConfigParser: 暗矢狂潮游戏配置解析器
*/
@Getter @Setter
public class ExtractOwnConfigParser extends MCEConfigParser {
    
    private String mapName = "extractown";
    
    @Override
    protected void parse() {
        mapName = "extractown"; // 默认值
        
        try {
            lines = Files.readAllLines(configPath);
        } catch (IOException e) {
            plugin.getLogger().severe("读取配置文件失败");
            e.printStackTrace();
            return;
        }
        
        String currentSection = "";
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 跳过空行和注释
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }
            
            // 检查是否是节标题
            if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                currentSection = trimmedLine.substring(1, trimmedLine.length() - 1);
                continue;
            }
            
            // 根据当前节解析配置
            switch (currentSection) {
                case "map_name":
                    mapName = trimmedLine;
                    currentSection = null;
                    break;
                // 可以添加更多配置节
                default:
                    break;
            }
        }
    }
}