package gg.brim.plantrestrictions;

import gg.brim.kingdoms.api.KingdomsAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlantRestrictionsCommand implements CommandExecutor, TabCompleter {

    private final PlantRestrictions plugin;

    public PlantRestrictionsCommand(PlantRestrictions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender, args);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(Component.text("Неизвестная команда. Используйте /pr help", NamedTextColor.RED));
            }
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("plantrestrictions.reload")) {
            sender.sendMessage(plugin.getConfigManager().getNoPermissionCmdMessage());
            return;
        }

        plugin.reload();
        sender.sendMessage(plugin.getConfigManager().getReloadSuccessMessage());
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("plantrestrictions.info")) {
            sender.sendMessage(plugin.getConfigManager().getNoPermissionCmdMessage());
            return;
        }

        String kingdomId;

        if (args.length > 1) {
            // Указано королевство в аргументе
            kingdomId = args[1].toLowerCase();
        } else if (sender instanceof Player player) {
            // Получаем королевство игрока
            kingdomId = plugin.getKingdomsAPI().getPlayerKingdom(player.getUniqueId());
            if (kingdomId == null) {
                sender.sendMessage(plugin.getConfigManager().getNoKingdomMessage());
                return;
            }
        } else {
            sender.sendMessage(Component.text("Укажите королевство: /pr list <kingdom>", NamedTextColor.RED));
            return;
        }

        // Получаем отображаемое имя королевства
        KingdomsAPI api = plugin.getKingdomsAPI();
        String displayName = api.getKingdomDisplayName(kingdomId);
        if (displayName == null) {
            displayName = kingdomId;
        }

        // Получаем разрешённые растения
        Set<Material> allowedPlants = plugin.getPlantManager().getAllowedPlants(kingdomId);

        sender.sendMessage(plugin.getConfigManager().getListHeader(displayName));

        if (allowedPlants.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getListEmptyMessage());
        } else {
            // Сортируем для удобства
            List<Material> sorted = allowedPlants.stream()
                    .sorted((a, b) -> a.name().compareTo(b.name()))
                    .toList();

            for (Material material : sorted) {
                sender.sendMessage(plugin.getConfigManager().getListItem(formatMaterialName(material)));
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== PlantRestrictions ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/pr help", NamedTextColor.YELLOW)
                .append(Component.text(" - Показать эту справку", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/pr list [kingdom]", NamedTextColor.YELLOW)
                .append(Component.text(" - Список разрешённых растений", NamedTextColor.GRAY)));

        if (sender.hasPermission("plantrestrictions.reload")) {
            sender.sendMessage(Component.text("/pr reload", NamedTextColor.YELLOW)
                    .append(Component.text(" - Перезагрузить конфигурацию", NamedTextColor.GRAY)));
        }
    }

    /**
     * Форматирует имя материала в читаемый вид
     */
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        // Capitalize first letter of each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("help");
            subCommands.add("list");

            if (sender.hasPermission("plantrestrictions.reload")) {
                subCommands.add("reload");
            }

            String input = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());

        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            // Подсказываем королевства
            String input = args[1].toLowerCase();
            List<String> kingdoms = plugin.getKingdomsAPI().getAllKingdoms();

            completions = kingdoms.stream()
                    .filter(k -> k.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
