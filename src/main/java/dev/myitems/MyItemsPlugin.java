package dev.myitems;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class MyItemsPlugin extends JavaPlugin implements TabExecutor {

    private ItemRegistry registry;

    @Override
    public void onEnable() {
        registry = new ItemRegistry(this);
        registry.load();
        var cmd = getCommand("myitems");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Использование: /myitems <give|reload|list> [id] [игрок]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                registry.load();
                sender.sendMessage("Предметы перезагружены.");
            }
            case "list" -> sender.sendMessage("Предметы: " + String.join(", ", registry.ids()));
            case "give" -> give(sender, args);
            default -> sender.sendMessage("Неизвестная подкоманда.");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Использование: /myitems give <id> [игрок]");
            return;
        }
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("Укажи игрока.");
            return;
        }
        if (target == null) {
            sender.sendMessage("Игрок не найден.");
            return;
        }
        ItemStack stack = registry.create(args[1], 1);
        if (stack == null) {
            sender.sendMessage("Нет такого предмета: " + args[1]);
            return;
        }
        target.getInventory().addItem(stack);
        sender.sendMessage("Выдан предмет " + args[1] + " игроку " + target.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("give", "reload", "list"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            registry.ids().forEach(out::add);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        String prefix = args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(prefix));
        return out;
    }
}
