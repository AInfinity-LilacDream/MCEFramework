package mcevent.MCEFramework.games.underworldGame;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;

import java.io.IOException;
import java.nio.file.Files;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

@Getter
@Setter
public class UnderworldGameConfigParser extends MCEConfigParser {
    
    @Override
    protected void parse() {
        try {
            lines = Files.readAllLines(configPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        plugin.getLogger().info("UnderworldGame配置文件读取完毕");
    }
}

