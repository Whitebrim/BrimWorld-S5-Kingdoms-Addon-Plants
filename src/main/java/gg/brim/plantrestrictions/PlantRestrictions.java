package gg.brim.plantrestrictions;

import gg.brim.kingdoms.api.KingdomsAPI;
import gg.brim.plantrestrictions.config.ConfigManager;
import gg.brim.plantrestrictions.listener.PlantListener;
import gg.brim.plantrestrictions.manager.PlantManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlantRestrictions extends JavaPlugin {

    private static PlantRestrictions instance;
    private ConfigManager configManager;
    private PlantManager plantManager;
    private KingdomsAPI kingdomsAPI;

    @Override
    public void onEnable() {
        instance = this;

        // Проверяем наличие KingdomsAddon
        kingdomsAPI = KingdomsAPI.getInstance();
        if (kingdomsAPI == null) {
            getLogger().severe("KingdomsAddon не найден! Отключение плагина...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Инициализация
        this.configManager = new ConfigManager(this);
        this.plantManager = new PlantManager(this);

        // Загрузка конфигурации
        configManager.loadConfig();
        plantManager.loadAllowedPlants();

        // Регистрация слушателей
        getServer().getPluginManager().registerEvents(new PlantListener(this), this);

        // Регистрация команд
        PlantRestrictionsCommand command = new PlantRestrictionsCommand(this);
        getCommand("plantrestrictions").setExecutor(command);
        getCommand("plantrestrictions").setTabCompleter(command);

        getLogger().info("PlantRestrictions успешно включён!");
        getLogger().info("Загружено " + plantManager.getTotalPlantsCount() + " настроек растений");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlantRestrictions отключён!");
        instance = null;
    }

    public static PlantRestrictions getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlantManager getPlantManager() {
        return plantManager;
    }

    public KingdomsAPI getKingdomsAPI() {
        return kingdomsAPI;
    }

    public void reload() {
        configManager.loadConfig();
        plantManager.loadAllowedPlants();
    }

    public void debug(String message) {
        if (configManager.isDebug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
