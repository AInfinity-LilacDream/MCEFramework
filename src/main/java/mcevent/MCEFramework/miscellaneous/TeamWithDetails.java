package mcevent.MCEFramework.miscellaneous;

import net.kyori.adventure.text.format.NamedTextColor;

/*
TeamWithDetails: 记录队伍详细信息的队伍类
 */
public record TeamWithDetails(String teamName, String teamNameNoColor, String alias, NamedTextColor teamColor,
                              String textColorPre, String textColorPost) {}
