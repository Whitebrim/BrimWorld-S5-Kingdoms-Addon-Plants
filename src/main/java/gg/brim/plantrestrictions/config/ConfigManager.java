package gg.brim.plantrestrictions.config;

import gg.brim.plantrestrictions.PlantRestrictions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final PlantRestrictions plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    private boolean debug;
    private boolean restrictTeamless;

    // Сообщения
    private String msgNoPermission;
    private String msgNoKingdom;
    private String msgReloadSuccess;
    private String msgListHeader;
    private String msgListItem;
    private String msgListEmpty;
    private String msgNoPermissionCmd;

    public ConfigManager(PlantRestrictions plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        this.debug = config.getBoolean("debug", false);
        this.restrictTeamless = config.getBoolean("restrict-teamless", true);

        // Загрузка сообщений
        this.msgNoPermission = config.getString("messages.no-permission", 
                "&cУ вашего королевства нет права сажать это растение!");
        this.msgNoKingdom = config.getString("messages.no-kingdom", 
                "&cВы не принадлежите ни к одному королевству!");
        this.msgReloadSuccess = config.getString("messages.reload-success", 
                "&aКонфигурация PlantRestrictions перезагружена!");
        this.msgListHeader = config.getString("messages.list-header", 
                "&6=== Разрешённые растения для %kingdom% ===");
        this.msgListItem = config.getString("messages.list-item", 
                "&7- &a%plant%");
        this.msgListEmpty = config.getString("messages.list-empty", 
                "&7Нет разрешённых растений");
        this.msgNoPermissionCmd = config.getString("messages.no-permission-cmd", 
                "&cУ вас нет прав на эту команду!");

        plugin.debug("Конфигурация загружена. Debug: " + debug + ", RestrictTeamless: " + restrictTeamless);
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isRestrictTeamless() {
        return restrictTeamless;
    }

    // Методы для получения сообщений как Component (Adventure API)
    public Component getNoPermissionMessage() {
        return legacySerializer.deserialize(msgNoPermission);
    }

    public Component getNoKingdomMessage() {
        return legacySerializer.deserialize(msgNoKingdom);
    }

    public Component getReloadSuccessMessage() {
        return legacySerializer.deserialize(msgReloadSuccess);
    }

    public Component getListHeader(String kingdom) {
        return legacySerializer.deserialize(msgListHeader.replace("%kingdom%", kingdom));
    }

    public Component getListItem(String plant) {
        return legacySerializer.deserialize(msgListItem.replace("%plant%", plant));
    }

    public Component getListEmptyMessage() {
        return legacySerializer.deserialize(msgListEmpty);
    }

    public Component getNoPermissionCmdMessage() {
        return legacySerializer.deserialize(msgNoPermissionCmd);
    }
}
