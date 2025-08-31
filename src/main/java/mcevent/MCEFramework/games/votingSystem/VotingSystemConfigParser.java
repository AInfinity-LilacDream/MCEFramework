package mcevent.MCEFramework.games.votingSystem;

import mcevent.MCEFramework.tools.MCEConfigParser;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;

/**
 * VotingSystemConfigParser: 投票系统配置解析器
 */
public class VotingSystemConfigParser extends MCEConfigParser {

    @Override
    public ArrayList<Component> openAndParse(String fileName) {
        ArrayList<Component> introTexts = new ArrayList<>();
        
        // 投票系统的介绍文本
        introTexts.add(Component.text("§6§l游戏投票系统"));
        introTexts.add(Component.text("§e每个游戏结束后，所有玩家将进行投票"));
        introTexts.add(Component.text("§e右键点击投票卡，选择您想要游玩的下一个游戏"));
        introTexts.add(Component.text("§e投票时间为30秒，得票最多的游戏将会开始"));
        introTexts.add(Component.text("§e如果出现平票，将随机选择一个游戏"));
        
        return introTexts;
    }
}