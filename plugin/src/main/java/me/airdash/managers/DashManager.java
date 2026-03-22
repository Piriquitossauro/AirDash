package me.airdash.managers;

import me.airdash.AirDashPlugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DashManager {

    private final AirDashPlugin plugin;

    // Mapa de cooldowns: UUID -> timestamp (ms) quando o cooldown termina
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Mapa de jogadores em dash ativo
    private final Map<UUID, BukkitRunnable> activeDashes = new HashMap<>();

    public DashManager(AirDashPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Verifica se o jogador está em cooldown.
     * @return segundos restantes, ou 0 se não estiver em cooldown
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
     * Verifica se o jogador está atualmente num dash ativo.
     */
    public boolean isInDash(Player player) {
        return activeDashes.containsKey(player.getUniqueId());
    }

    /**
     * Inicia o dash para o jogador dado.
     */
    public void startDash(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancela qualquer dash anterior (segurança)
        cancelDash(player);

        // Pega configurações
        double initialSpeed = plugin.getConfig().getDouble("dash-initial-speed", 0.7);
        double friction = plugin.getConfig().getDouble("dash-friction", 0.85);
        int durationTicks = plugin.getConfig().getInt("dash-duration-ticks", 20);
        int cooldownSeconds = plugin.getConfig().getInt("cooldown-seconds", 5);

        // Direção horizontal do player (sem componente Y)
        Vector direction = player.getLocation().getDirection();
        direction.setY(0).normalize();

        // Y fixo no momento do início do dash
        final double fixedY = player.getLocation().getY();

        BukkitRunnable dashTask = new BukkitRunnable() {
            int ticks = 0;
            double speed = initialSpeed;

            @Override
            public void run() {
                // Segurança: cancela se o player saiu do servidor
                if (!player.isOnline()) {
                    cancelDash(player);
                    cancel();
                    return;
                }

                // Se chegou ao fim da duração, encerra o dash
                if (ticks >= durationTicks || speed < 0.01) {
                    cancelDash(player);
                    cancel();
                    return;
                }

                // Aplica velocidade horizontal + mantém Y fixo
                Vector velocity = direction.clone().multiply(speed);

                // Mantém o Y fixo: calcula o delta Y necessário para compensar gravidade
                double currentY = player.getLocation().getY();
                double deltaY = fixedY - currentY;
                // Limita o delta para não ser extremo
                deltaY = Math.max(-0.5, Math.min(0.5, deltaY));

                velocity.setY(deltaY);
                player.setVelocity(velocity);

                // Descacelera para o próximo tick
                speed *= friction;
                ticks++;
            }
        };

        dashTask.runTaskTimer(plugin, 0L, 1L);
        activeDashes.put(uuid, dashTask);

        // Aplica cooldown
        cooldowns.put(uuid, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    /**
     * Cancela o dash ativo do jogador, se houver.
     */
    public void cancelDash(Player player) {
        BukkitRunnable task = activeDashes.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * Remove todos os dados do jogador (usado ao deslogar, por exemplo).
     */
    public void removePlayer(Player player) {
        cancelDash(player);
        cooldowns.remove(player.getUniqueId());
    }
}
