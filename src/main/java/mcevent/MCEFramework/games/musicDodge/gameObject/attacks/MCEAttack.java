package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import java.util.HashSet;

@Getter @Setter
public abstract class MCEAttack {
    private double alertDurationBeats, attackDurationBeats;
    private int bpm;
    private final HashSet<Player> damagedPlayers = new HashSet<>();

    public MCEAttack(double alertDurationBeats, double attackDurationBeats, int bpm) {
        this.alertDurationBeats = alertDurationBeats;
        this.attackDurationBeats = attackDurationBeats;
        this.bpm = bpm;
    }
    
    // Convert measures (小节) to ticks (20 ticks = 1 second)
    // 1 measure = 4 beats, so: measures * 4 beats/measure * 60 seconds/minute / bpm beats/minute * 20 ticks/second
    protected int getAlertDurationTicks() {
        return (int) (alertDurationBeats * 4 * 60.0 / bpm * 20);
    }
    
    protected int getAttackDurationTicks() {
        return (int) (attackDurationBeats * 4 * 60.0 / bpm * 20);
    }
    
    // Legacy methods for backward compatibility
    @Deprecated
    protected int getAlertDuration() {
        return (int) (alertDurationBeats * 4 * 60.0 / bpm);
    }
    
    @Deprecated
    protected int getAttackDuration() {
        return (int) (attackDurationBeats * 4 * 60.0 / bpm);
    }

    public void toggle() {}

    protected void playAttackSound(String soundName) {
        MCEPlayerUtils.globalPlaySound(soundName);
    }

    protected void damagePlayer(Player player, double damage) {
        if (!damagedPlayers.contains(player)) {
            player.damage(damage);
            damagedPlayers.add(player);
        }
    }

    protected void checkPlayerDamage(Location attackLocation, double damage) {
        for (Player player : attackLocation.getWorld().getPlayers()) {
            if (isPlayerInAttackRange(player)) {
                damagePlayer(player, damage);
            }
        }
    }

    protected abstract boolean isPlayerInAttackRange(Player player);
}
