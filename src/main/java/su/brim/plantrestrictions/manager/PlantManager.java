package su.brim.plantrestrictions.manager;

import su.brim.plantrestrictions.PlantRestrictions;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер разрешённых растений.
 * Использует ConcurrentHashMap для потокобезопасности в Folia.
 */
public class PlantManager {

    private final PlantRestrictions plugin;

    // Потокобезопасные коллекции для Folia
    private final Map<String, Set<Material>> kingdomPlants = new ConcurrentHashMap<>();
    private final Set<Material> globalAllowed = ConcurrentHashMap.newKeySet();

    // Множество всех материалов, которые считаются "растениями"
    private static final Set<Material> PLANTABLE_MATERIALS = createPlantableMaterials();

    public PlantManager(PlantRestrictions plugin) {
        this.plugin = plugin;
    }

    /**
     * Загружает разрешённые растения из конфигурации
     */
    public void loadAllowedPlants() {
        kingdomPlants.clear();
        globalAllowed.clear();

        FileConfiguration config = plugin.getConfig();

        // Загрузка глобально разрешённых растений
        List<String> globalList = config.getStringList("global-allowed");
        for (String materialName : globalList) {
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                globalAllowed.add(material);
                plugin.debug("Глобально разрешено: " + material.name());
            } else {
                plugin.getLogger().warning("Неизвестный материал в global-allowed: " + materialName);
            }
        }

        // Загрузка растений для каждого королевства
        ConfigurationSection kingdomsSection = config.getConfigurationSection("kingdoms");
        if (kingdomsSection != null) {
            for (String kingdomId : kingdomsSection.getKeys(false)) {
                Set<Material> allowedSet = ConcurrentHashMap.newKeySet();

                List<String> plants = config.getStringList("kingdoms." + kingdomId + ".allowed-plants");
                for (String materialName : plants) {
                    Material material = Material.matchMaterial(materialName);
                    if (material != null) {
                        allowedSet.add(material);
                        plugin.debug("Королевство " + kingdomId + ": разрешено " + material.name());
                    } else {
                        plugin.getLogger().warning("Неизвестный материал для " + kingdomId + ": " + materialName);
                    }
                }

                kingdomPlants.put(kingdomId, allowedSet);
                plugin.getLogger().info("Загружено " + allowedSet.size() + " растений для " + kingdomId);
            }
        }
    }

    /**
     * Проверяет, может ли королевство сажать данное растение
     *
     * @param kingdomId ID королевства (null если игрок без королевства)
     * @param material  Материал для проверки
     * @return true если посадка разрешена
     */
    public boolean canPlant(String kingdomId, Material material) {
        // Проверяем, является ли это растением вообще
        if (!isPlantable(material)) {
            return true; // Не растение - разрешаем
        }

        // Глобально разрешённые растения
        if (globalAllowed.contains(material)) {
            plugin.debug("Материал " + material.name() + " глобально разрешён");
            return true;
        }

        // Игрок без королевства
        if (kingdomId == null) {
            boolean allowed = !plugin.getConfigManager().isRestrictTeamless();
            plugin.debug("Игрок без королевства, restrict-teamless: " + !allowed + ", результат: " + allowed);
            return allowed;
        }

        // Проверяем разрешение для королевства
        Set<Material> allowed = kingdomPlants.get(kingdomId);
        if (allowed == null) {
            plugin.debug("Королевство " + kingdomId + " не найдено в конфигурации");
            return false;
        }

        boolean result = allowed.contains(material);
        plugin.debug("Проверка " + material.name() + " для " + kingdomId + ": " + result);
        return result;
    }

    /**
     * Проверяет, является ли материал растением, которое можно посадить
     */
    public boolean isPlantable(Material material) {
        return PLANTABLE_MATERIALS.contains(material);
    }

    /**
     * Получает список разрешённых растений для королевства
     */
    public Set<Material> getAllowedPlants(String kingdomId) {
        Set<Material> result = new HashSet<>(globalAllowed);
        Set<Material> kingdomSet = kingdomPlants.get(kingdomId);
        if (kingdomSet != null) {
            result.addAll(kingdomSet);
        }
        return result;
    }

    /**
     * Получает общее количество настроек растений
     */
    public int getTotalPlantsCount() {
        int count = globalAllowed.size();
        for (Set<Material> plants : kingdomPlants.values()) {
            count += plants.size();
        }
        return count;
    }

    /**
     * Получает список всех королевств с настройками
     */
    public Set<String> getConfiguredKingdoms() {
        return Collections.unmodifiableSet(kingdomPlants.keySet());
    }

    /**
     * Создаёт множество всех материалов, которые можно посадить
     */
    private static Set<Material> createPlantableMaterials() {
        Set<Material> materials = EnumSet.noneOf(Material.class);

        // Саженцы деревьев
        materials.add(Material.OAK_SAPLING);
        materials.add(Material.SPRUCE_SAPLING);
        materials.add(Material.BIRCH_SAPLING);
        materials.add(Material.JUNGLE_SAPLING);
        materials.add(Material.ACACIA_SAPLING);
        materials.add(Material.DARK_OAK_SAPLING);
        materials.add(Material.CHERRY_SAPLING);
        materials.add(Material.MANGROVE_PROPAGULE);
        materials.add(Material.PALE_OAK_SAPLING);

        // Семена и посевы
        materials.add(Material.WHEAT_SEEDS);
        materials.add(Material.BEETROOT_SEEDS);
        materials.add(Material.MELON_SEEDS);
        materials.add(Material.PUMPKIN_SEEDS);

        // Овощи, которые сажаются напрямую
        materials.add(Material.POTATO);
        materials.add(Material.CARROT);

        // Другие растения
        materials.add(Material.COCOA_BEANS);
        materials.add(Material.SUGAR_CANE);
        materials.add(Material.CACTUS);
        materials.add(Material.BAMBOO_SAPLING);
        materials.add(Material.SWEET_BERRIES);
        materials.add(Material.GLOW_BERRIES);
        materials.add(Material.NETHER_WART);
        materials.add(Material.CHORUS_FLOWER);

        // Грибы
        materials.add(Material.BROWN_MUSHROOM);
        materials.add(Material.RED_MUSHROOM);
        materials.add(Material.CRIMSON_FUNGUS);
        materials.add(Material.WARPED_FUNGUS);

        // Высокие растения
        materials.add(Material.SUNFLOWER);
        materials.add(Material.LILAC);
        materials.add(Material.ROSE_BUSH);
        materials.add(Material.PEONY);

        // Азалии
        materials.add(Material.AZALEA);
        materials.add(Material.FLOWERING_AZALEA);

        return Collections.unmodifiableSet(materials);
    }
}
