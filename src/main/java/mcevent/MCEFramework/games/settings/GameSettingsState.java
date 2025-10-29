package mcevent.MCEFramework.games.settings;

/**
 * GameSettingsState: 简易内存态设置存储（可后续扩展为持久化）
 */
public class GameSettingsState {
    private static volatile int footballTeams = 2; // 2 或 4，默认2
    private static volatile boolean manualStartEnabled = false; // 全局：手动启动游戏（默认关）

    public static int getFootballTeams() {
        return footballTeams;
    }

    public static void setFootballTeams(int teams) {
        if (teams != 2 && teams != 4)
            teams = 2;
        footballTeams = teams;
    }

    public static boolean isManualStartEnabled() {
        return manualStartEnabled;
    }

    public static void setManualStartEnabled(boolean enabled) {
        manualStartEnabled = enabled;
    }
}
