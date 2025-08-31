package mcevent.MCEFramework.games.votingSystem.gameObject;

import mcevent.MCEFramework.generalGameObject.MCEGameBoard;

/**
 * VotingSystemGameBoard: 投票系统的空白游戏板
 * 不显示记分板，只是为了满足框架要求
 */
public class VotingSystemGameBoard extends MCEGameBoard {

    public VotingSystemGameBoard(String gameName, String mapName, int round) {
        super(gameName, mapName, round);
    }

    @Override
    public void globalDisplay() {
        // 投票系统不显示记分板，留空即可
    }

    @Override
    public void setStateTitle(String stateTitle) {
        super.setStateTitle(stateTitle);
        // 不更新记分板
    }
}