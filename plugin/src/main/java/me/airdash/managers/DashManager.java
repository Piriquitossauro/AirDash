package me.airdash.managers;

import me.airdash.AirDashPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
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
        int    cooldownSecs  = plugin.getConfig().getInt("cooldown-seconds", 5);

        // Direção normalizada de onde o player está olhando (inclui Y)
        Vector impulse = player.getLocation().getDirection().normalize().multiply(force);

        // Aplica o impulso — um único setVelocity, física do MC faz o resto
        player.setVelocity(impulse);

        // Reduz a durabilidade das botas de ouro
        int durabilityLoss = plugin.getConfig().getInt("dash-durability-cost", 5);
        ItemStack boots = player.getInventory().getBoots();
        if (boots != null) {
            ItemMeta meta = boots.getItemMeta();
            if (meta instanceof Damageable damageable) {
                int newDamage = damageable.getDamage() + durabilityLoss;
                if (newDamage >= boots.getType().getMaxDurability()) {
                    // Bota destruída — toca o som nativo de item quebrando
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    player.getInventory().setBoots(null);
                } else {
                    damageable.setDamage(newDamage);
                    boots.setItemMeta(meta);
                }
            }
        }

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
