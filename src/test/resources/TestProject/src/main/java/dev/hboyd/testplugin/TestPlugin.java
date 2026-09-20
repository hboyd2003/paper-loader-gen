package dev.hboyd.testplugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class TestPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        this.getSLF4JLogger().info("Test Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        this.getSLF4JLogger().info("Test Plugin Disabled!");
    }
}
