package mcevent.MCEFramework.games.discoFever;

import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCETeleporter;

/*
DiscoFever: disco fever的完整实现
 */
public class DiscoFever extends MCEGame {
    public DiscoFever(String title, int id, String mapName, int round, boolean isMultiGame) {
        super(title, id, mapName, round, isMultiGame);
    }

    @Override
    public void onLaunch() {
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEMessenger.sendGlobalInfo("Disco fever start!");
    }

    @Override
    public void intro() {
        MCEMessenger.sendGlobalInfo("Disco fever Intro!");
    }
}
