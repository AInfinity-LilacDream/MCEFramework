package mcevent.MCEFramework.games.musicDodge;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.musicDodge.gameObject.MCEMusicBPMPerformer;
import mcevent.MCEFramework.games.musicDodge.gameObject.MusicDodgeGameBoard;
import mcevent.MCEFramework.games.musicDodge.gameObject.attacks.*;
import mcevent.MCEFramework.games.musicDodge.gameObject.items.TeleportOrb;
import mcevent.MCEFramework.games.musicDodge.AttackConfig;
import mcevent.MCEFramework.games.musicDodge.ParticleOptimizationIntegration;
import mcevent.MCEFramework.customHandler.SpecialItemInteractionHandler;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.checkerframework.checker.units.qual.C;

import java.util.List;
import java.util.Objects;

import static mcevent.MCEFramework.games.musicDodge.MusicDodgeFuncImpl.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;
import static mcevent.MCEFramework.miscellaneous.Constants.*;

@Getter
@Setter
public class MusicDodge extends MCEGame {

    private MusicDodgeConfigParser musicDodgeConfigParser = new MusicDodgeConfigParser();
    private MCEMusicBPMPerformer musicBPMPerformer = new MCEMusicBPMPerformer("minecraft:music_dodge_why_do_i", 200);
    private TeleportOrb teleportOrb;
    private SpecialItemInteractionHandler itemHandler;

    // 粒子优化系统
    private AttackDataManager attackDataManager;
    private DustParticleInterceptor particleInterceptor;

    public MusicDodge(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration,
                cycleEndDuration, endDuration);

        // 初始化传送宝珠和监听器
        this.teleportOrb = new TeleportOrb(plugin);
        this.itemHandler = new SpecialItemInteractionHandler();
        this.itemHandler.registerItem(teleportOrb);

        // 初始化粒子优化系统
        this.attackDataManager = AttackDataManager.getInstance(plugin);
        this.particleInterceptor = DustParticleInterceptor.create(plugin);
    }

    @Override
    public void onLaunch() {
        loadConfig();
        MCEPlayerUtils.globalClearPotionEffects();
        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SURVIVAL, 5L);
        // 设置玩家血量为10颗心（20.0血量）
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
        }

        grantGlobalPotionEffect(saturation);
        MCEPlayerUtils.clearGlobalTags();

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
    }

    @Override
    public void onCycleStart() {
        resetGameBoard();
        this.getGameBoard().setStateTitle("<red><bold> 剩余时间：</bold></red>");

        MCEMessenger.sendGlobalTitle("<aqua><bold>Why Do I (Hatsune Miku Version)</bold></aqua>",
                "<gold>Set It Off,初音ミク</gold>");

        // 启用粒子优化系统
        ParticleOptimizationIntegration.setOptimizationEnabled(true);
        particleInterceptor.enable();
        attackDataManager.start();

        // 给每个玩家分发传送宝珠
        distributeTeleportOrbs();

        // 启动特殊物品监听器
        itemHandler.start();

        // 从配置文件加载攻击序列（包含同步信息）
        List<AttackConfig> attackConfigs = musicDodgeConfigParser.parseAttackConfigs(this.getWorldName(),
                musicBPMPerformer.getBPM());

        // 开始按顺序播放所有攻击（支持同步）
        musicBPMPerformer.playWithSync(attackConfigs);
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new MusicDodgeGameBoard(getTitle(), getWorldName()));
    }

    @Override
    public void onEnd() {
        // 不在结束阶段修改玩家游戏模式

        // onEnd结束后立即清理展示板和资源，然后启动投票系统
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop(); // 停止所有游戏资源
            MCEMainController.returnToLobbyOrLaunchVoting();
        });
    }

    @Override
    public void stop() {
        super.stop();
        musicBPMPerformer.stop();

        // 停止粒子优化系统
        ParticleOptimizationIntegration.setOptimizationEnabled(false);
        particleInterceptor.disable();
        attackDataManager.stop();

        itemHandler.suspend();
        clearTeleportOrbs();
    }

    /**
     * 给所有玩家分发传送宝珠
     */
    private void distributeTeleportOrbs() {
        for (Team team : getActiveTeams()) {
            for (Player player : MCETeamUtils.getPlayers(team)) {
                player.getInventory().addItem(teleportOrb.createItem());
            }
        }
    }

    /**
     * 清理所有玩家的传送宝珠
     */
    private void clearTeleportOrbs() {
        for (Team team : getActiveTeams()) {
            for (Player player : MCETeamUtils.getPlayers(team)) {
                player.getInventory().clear();
                teleportOrb.clearCooldown(player);
            }
        }
    }

}
