package me.airdash.listeners;

import me.airdash.AirDashPlugin;
import me.airdash.managers.DashManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

public class DashListener implements Listener {

    private final AirDashPlugin plugin;
    private final DashManager dashManager;

    public DashListener(AirDashPlugin plugin) {
        this.plugin = plugin;
        this.dashManager = plugin.getDashManager();
    }

    /**
     * Gatilho do dash: jogador pressiona SHIFT enquanto está no ar.
     */
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        // Só ativa quando o jogador COMEÇA a agachar (não ao soltar)
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();

        // Deve estar no ar
        if (player.isOnGround()) return;

        // Verifica bota de ouro
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || boots.getType() != Material.GOLDEN_BOOTS) return;

        // Verifica cooldown
        int secondsLeft = dashManager.getCooldownSecondsLeft(player);
        if (secondsLeft > 0) {
            String msg = plugin.getConfig().getString(
                    "cooldown-message",
                    "§cHabilidade em cooldown em §e{segundos} §csegundo(s)!"
            );
            player.sendMessage(msg.replace("{segundos}", String.valueOf(secondsLeft)));
            return;
        }

        // Executa o impulso!
        dashManager.startDash(player);

        String activatedMsg = plugin.getConfig().getString("dash-activated-message", "§aDash!");
        player.sendMessage(activatedMsg);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dashManager.removePlayer(event.getPlayer());
    }
}
