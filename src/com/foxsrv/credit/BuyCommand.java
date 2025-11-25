package com.foxsrv.credit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BuyCommand implements org.bukkit.command.CommandExecutor, org.bukkit.command.TabCompleter {

    private final CreditPlugin plugin;

    public BuyCommand(CreditPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /credit reload
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("credit.reload")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            plugin.reloadAll();
            sender.sendMessage(ChatColor.GREEN + "CoinCredit config reloaded.");
            return true;
        }

        // Usage: /credit buy <amount> <cardId>
        if (args.length < 3 || !args[0].equalsIgnoreCase("buy")) {
            sender.sendMessage(ChatColor.RED + "Usage: /credit buy <amount> <cardId>");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player p = (Player) sender;
        UUID uuid = p.getUniqueId();

        // cooldown
        if (plugin.isOnCooldown(uuid)) {
            p.sendMessage(ChatColor.RED + "Please wait 1 second each try!");
            return true;
        }
        plugin.setLastTx(uuid);

        String amountStr = args[1];
        String cardId = args[2];

        BigDecimal coins;
        try {
            coins = new BigDecimal(amountStr);
            if (coins.compareTo(BigDecimal.ZERO) <= 0) {
                p.sendMessage(ChatColor.RED + "Invalid amount.");
                return true;
            }
        } catch (Exception ex) {
            p.sendMessage(ChatColor.RED + "Invalid amount format.");
            return true;
        }

        // prepare amount for API: truncate to 8 decimals
        BigDecimal coinsForApi = coins.setScale(8, RoundingMode.DOWN);

        // compute credits: credits = coins * worth
        BigDecimal creditsRaw = coinsForApi.multiply(plugin.worth);

        // minimal unit based on decimals
        BigDecimal minimal;
        if (plugin.decimals <= 0) {
            minimal = BigDecimal.ONE;
        } else {
            minimal = BigDecimal.ONE.divide(BigDecimal.TEN.pow(plugin.decimals), plugin.decimals, RoundingMode.HALF_UP);
        }

        // round credits to configured decimals (HALF_UP)
        BigDecimal creditsRounded = creditsRaw.setScale(Math.max(0, plugin.decimals), RoundingMode.HALF_UP);

        // check too low
        if (creditsRounded.compareTo(minimal) < 0) {
            p.sendMessage(ChatColor.GOLD + "Too low value!");
            return true;
        }

        // call API (synchronous small request) - send { cardCode, toId, amount }
        ApiClient client = plugin.getApiClient();
        try {
            ApiClient.ApiResponse resp = client.transferByCard(cardId, plugin.serverId, coinsForApi);
            if (resp == null) {
                p.sendMessage(ChatColor.WHITE + "API error");
                return true;
            }
            if (!resp.success) {
                p.sendMessage(ChatColor.RED + "Transaction Failed!");
                return true;
            }

            // success: execute configured command replacing %player% %amount% %ID%
            String amountToUse;
            if (plugin.decimals <= 0) {
                amountToUse = creditsRounded.toBigInteger().toString();
            } else {
                // ensure decimal point (uses dot)
                amountToUse = creditsRounded.toPlainString();
            }

            String cmd = plugin.commandAfter
                    .replace("%player%", p.getName())
                    .replace("%amount%", amountToUse)
                    .replace("%ID%", resp.txId != null ? resp.txId : "");

            // execute command as console (use Bukkit.dispatchCommand)
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            });

            p.sendMessage(ChatColor.GREEN + "You bought " + amountToUse + " of credits for " + coinsForApi.stripTrailingZeros().toPlainString() + " of coins! Transaction ID: " + (resp.txId != null ? resp.txId : ""));
            return true;

        } catch (HttpTimeoutException t) {
            p.sendMessage(ChatColor.WHITE + "API error");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            p.sendMessage(ChatColor.WHITE + "API error");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("buy");
            if (sender.hasPermission("credit.reload")) out.add("reload");
        } else if (args.length == 2) {
            out.add("1");
            out.add("0.01");
            out.add("0.00000001");
        }
        return out;
    }
}
