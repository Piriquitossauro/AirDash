package me.airdash.managers;

import me.airdash.AirDashPlugin;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DashManager {

    private final AirDashPlugin plugin;

    // Mapa de cooldowns: UUID -> timestamp (ms) quando o cooldown termina
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public DashManager(AirDashPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Retorna os segundos de cooldown restantes.
     * Retorna 0 se não houver cooldown ativo.
     */
    public int getCooldownSecondsLeft(Player player) {
        Long end = cooldowns.get(player.getUniqueId());
        if (end == null) return 0;
        long remaining = end - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(player.getUniqueId());
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Aplica o impulso de dash na direção exata em que o player está olhando.
     * A física do servidor cuida do resto (gravidade, colisão, queda etc.)
     */
    public void startDash(Player player) {
        double force         = plugin.getConfig().getDouble("dash-force", 1.5);
        int    cooldownSecs  = plugin.getConfig().getInt("cooldown-seconds", 3);

        // Direção normalizada de onde o player está olhando (inclui Y)
        Vector impulse = player.getLocation().getDirection().normalize().multiply(force);

        // Aplica o impulso — um único setVelocity, física do MC faz o resto
        player.setVelocity(impulse);

        // Registra o cooldown
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldownSecs * 1000L));
    }

    /**
     * Remove dados do player ao deslogar.
     */
    public void removePlayer(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
}
