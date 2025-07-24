package mcevent.MCEFramework.miscellaneous;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.ArrayList;

/*
IntroTexts: 游戏介绍文本
 */
public class IntroTexts {
    public static final Component divider = MiniMessage.miniMessage().deserialize("<green>══════════════════</green>");
    public static final Component blankLine = MiniMessage.miniMessage().deserialize(" ");

    private static final ArrayList<Component> pktIntroTextList = new ArrayList<>() {{
        add(MiniMessage.miniMessage().deserialize(
                """
                        <b><yellow> 欢迎来到瓮中捉鳖！</yellow></b>

                        <yellow>  - 比喻想要捕捉的对象已在掌握之中</yellow>
                          - 形容手到擒来，轻易而有把握
                        """
        ));
        add(MiniMessage.miniMessage().deserialize(
                """
                         <b><yellow> 如何游玩：</yellow></b>

                          - 玩家们在一个竞技场（瓮）中跑酷
                          - 队伍中的一名玩家作为猎人
                          - 尝试尽快抓住对方队伍的成员
                        """
        ));
        add(MiniMessage.miniMessage().deserialize(
                """
                         <b><yellow> 如何得分：</yellow></b>

                          - 猎人通过抓捕猎物得分
                          - 猎物则通过存活时间得分
                        """
        ));
    }};

    public static ArrayList<ArrayList<Component>> introTextList = new ArrayList<>() {{
        add(pktIntroTextList);
    }};
}
