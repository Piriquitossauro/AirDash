package me.airdash;

import me.airdash.listeners.DashListener;
import me.airdash.managers.DashManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AirDashPlugin extends JavaPlugin {

    private static AirDashPlugin instance;
    private DashManager dashManager;

    public static AirDashPlugin get() {
        return instance;
    }

    public DashManager getDashManager() {
        return dashManager;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Salva o config.yml padrão se não existir
        saveDefaultConfig();

        dashManager = new DashManager(this);

        Bukkit.getPluginManager().registerEvents(new DashListener(this), this);

        getLogger().info("AirDash iniciado! Use SHIFT no ar com botas de ouro para dar um dash.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AirDash desligado.");
    }
}
