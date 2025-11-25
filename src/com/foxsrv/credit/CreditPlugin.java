package com.foxsrv.credit;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreditPlugin extends JavaPlugin {

    public static CreditPlugin INSTANCE;
    private FileConfiguration cfg;

    // cooldown map (player UUID -> last timestamp ms)
    private final Map<UUID, Long> lastTx = new HashMap<>();

    // config values
    public String serverId;
    public BigDecimal worth;
    public int decimals;
    public String commandAfter;
    public String apiBase;
    public long cooldownMs;

    private ApiClient apiClient;

    @Override
    public void onEnable() {
        INSTANCE = this;
        saveDefaultConfig();
        cfg = getConfig();
        loadConfigValues();

        // init http client
        apiClient = new ApiClient(apiBase);

        // register commands (one registration only)
        BuyCommand buy = new BuyCommand(this);
        this.getCommand("credit").setExecutor(buy);
        this.getCommand("credit").setTabCompleter(buy);

        getLogger().info("CoinCredit enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CoinCredit disabled.");
    }

    public void loadConfigValues() {
        serverId = cfg.getString("ServerID", "829198573318747000");
        worth = new BigDecimal(cfg.getString("Worth", "1.00"));
        decimals = cfg.getInt("Decimals", 2);
        commandAfter = cfg.getString("command", "gemseconomy:eco give %player% %amount%");
        apiBase = cfg.getString("API", "http://coin.foxsrv.net:26450/");
        cooldownMs = cfg.getLong("cooldown_ms", 1000L);
    }

    public boolean isOnCooldown(UUID player) {
        Long last = lastTx.get(player);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < cooldownMs;
    }

    public void setLastTx(UUID player) {
        lastTx.put(player, System.currentTimeMillis());
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void reloadAll() {
        reloadConfig();
        cfg = getConfig();
        loadConfigValues();
        apiClient = new ApiClient(apiBase);
    }

    public ConsoleCommandSender getConsole() {
        return Bukkit.getConsoleSender();
    }
}
