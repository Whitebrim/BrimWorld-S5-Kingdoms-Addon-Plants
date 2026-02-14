package su.brim.plantrestrictions.listener;

import su.brim.kingdoms.api.KingdomsAPI;
import su.brim.plantrestrictions.PlantRestrictions;
import su.brim.plantrestrictions.manager.PlantManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Слушатель событий посадки растений.
 * В Folia события вызываются в регионе игрока/сущности, поэтому
 * дополнительная синхронизация не требуется.
 */
public class PlantListener implements Listener {

    private final PlantRestrictions plugin;
    private final PlantManager plantManager;
    private final KingdomsAPI kingdomsAPI;

    public PlantListener(PlantRestrictions plugin) {
        this.plugin = plugin;
        this.plantManager = plugin.getPlantManager();
        this.kingdomsAPI = plugin.getKingdomsAPI();
    }

    /**
     * Обработка размещения блоков (саженцы, кактусы, грибы и т.д.)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlockPlaced().getType();

        // Проверяем, является ли это растением
        if (!plantManager.isPlantable(material)) {
            return;
        }

        // Проверяем bypass право
        if (player.hasPermission("plantrestrictions.bypass")) {
            plugin.debug("Игрок " + player.getName() + " имеет bypass право");
            return;
        }

        // Проверяем админа KingdomsAddon
        if (kingdomsAPI.isAdmin(player)) {
            plugin.debug("Игрок " + player.getName() + " является админом KingdomsAddon");
            return;
        }

        // Получаем королевство игрока
        String kingdomId = kingdomsAPI.getPlayerKingdom(player.getUniqueId());

        // Проверяем разрешение
        if (!plantManager.canPlant(kingdomId, material)) {
            event.setCancelled(true);

            if (kingdomId == null) {
                player.sendMessage(plugin.getConfigManager().getNoKingdomMessage());
            } else {
                player.sendMessage(plugin.getConfigManager().getNoPermissionMessage());
            }

            plugin.debug("Заблокирована посадка " + material.name() + 
                    " игроком " + player.getName() + 
                    " (королевство: " + kingdomId + ")");
        }
    }

    /**
     * Обработка взаимодействия (посадка семян в землю).
     * Семена сажаются через правый клик по земле, а не через BlockPlaceEvent.
     * 
     * Обрабатываем ОБЕ руки — ранее проверялась только главная рука,
     * что позволяло обойти ограничение через вторую руку (off-hand).
     * 
     * Не используем проверку canPlantOn — ранее при клике по соседнему
     * блоку/растению проверка не срабатывала, и посадка проходила.
     * Достаточно проверять, что игрок держит семена/растение: если ему
     * запрещено сажать, блокируем взаимодействие, а Minecraft сам не даст
     * посадить в невалидное место.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Только правый клик по блоку
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }

        // Получаем предмет из той руки, которой произведён клик
        ItemStack item = (hand == EquipmentSlot.HAND) 
                ? player.getInventory().getItemInMainHand() 
                : player.getInventory().getItemInOffHand();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        Material material = item.getType();

        // Проверяем, является ли это семенами/растением для посадки
        if (!isSeedOrPlantable(material)) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        // Проверяем bypass право
        if (player.hasPermission("plantrestrictions.bypass")) {
            plugin.debug("Игрок " + player.getName() + " имеет bypass право (interact, hand=" + hand + ")");
            return;
        }

        // Проверяем админа KingdomsAddon
        if (kingdomsAPI.isAdmin(player)) {
            plugin.debug("Игрок " + player.getName() + " является админом KingdomsAddon (interact)");
            return;
        }

        // Получаем королевство игрока
        String kingdomId = kingdomsAPI.getPlayerKingdom(player.getUniqueId());

        // Проверяем разрешение
        if (!plantManager.canPlant(kingdomId, material)) {
            event.setCancelled(true);

            if (kingdomId == null) {
                player.sendMessage(plugin.getConfigManager().getNoKingdomMessage());
            } else {
                player.sendMessage(plugin.getConfigManager().getNoPermissionMessage());
            }

            plugin.debug("Заблокирована посадка семян " + material.name() + 
                    " игроком " + player.getName() + 
                    " (рука: " + hand + ", королевство: " + kingdomId + ")");
        }
    }

    /**
     * Блокируем передачу запрещённых семян жителям.
     * Жители (Farmer) могут сажать полученные семена/овощи самостоятельно,
     * обходя ограничения посадки для игрока.
     * 
     * В Folia событие вызывается в регионе целевой сущности (жителя).
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity rightClicked = event.getRightClicked();
        if (!(rightClicked instanceof Villager)) {
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();

        // Получаем предмет из руки, которой кликнули по жителю
        ItemStack item = (hand == EquipmentSlot.HAND)
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        Material material = item.getType();

        // Проверяем только семена/овощи, которые житель может посадить
        if (!isVillagerPlantable(material)) {
            return;
        }

        // Проверяем bypass право
        if (player.hasPermission("plantrestrictions.bypass")) {
            plugin.debug("Игрок " + player.getName() + " имеет bypass право (villager give)");
            return;
        }

        // Проверяем админа KingdomsAddon
        if (kingdomsAPI.isAdmin(player)) {
            plugin.debug("Игрок " + player.getName() + " является админом KingdomsAddon (villager give)");
            return;
        }

        // Получаем королевство игрока
        String kingdomId = kingdomsAPI.getPlayerKingdom(player.getUniqueId());

        // Проверяем разрешение
        if (!plantManager.canPlant(kingdomId, material)) {
            event.setCancelled(true);

            if (kingdomId == null) {
                player.sendMessage(plugin.getConfigManager().getNoKingdomMessage());
            } else {
                player.sendMessage(plugin.getConfigManager().getNoPermissionMessage());
            }

            plugin.debug("Заблокирована передача " + material.name() + 
                    " жителю игроком " + player.getName() + 
                    " (королевство: " + kingdomId + ")");
        }
    }

    /**
     * Дополнительная защита: блокируем посадку растений жителями.
     * Это страхующий обработчик на случай, если житель уже имеет семена
     * в инвентаре (например, подобрал с земли или получил при торговле).
     * 
     * В Folia EntityChangeBlockEvent вызывается в регионе блока.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Villager)) {
            return;
        }

        Material toMaterial = event.getBlockData().getMaterial();

        // Проверяем, является ли новый блок растением
        // Жители сажают: пшеницу, картофель, морковь, свёклу
        Material seedMaterial = cropBlockToSeed(toMaterial);
        if (seedMaterial == null) {
            return;
        }

        // Проверяем, разрешено ли это растение глобально
        if (plantManager.canPlant(null, seedMaterial) && 
                !plugin.getConfigManager().isRestrictTeamless()) {
            // Если для бескоролевственных разрешено и растение глобально доступно — пропускаем
            return;
        }

        // Блокируем посадку жителем — мы не можем определить,
        // к какому королевству принадлежит житель
        event.setCancelled(true);
        plugin.debug("Заблокирована посадка " + toMaterial.name() + 
                " жителем на " + event.getBlock().getLocation());
    }

    /**
     * Проверяет, является ли материал семенами или растением для посадки через взаимодействие
     */
    private boolean isSeedOrPlantable(Material material) {
        return switch (material) {
            case WHEAT_SEEDS,
                 BEETROOT_SEEDS,
                 MELON_SEEDS,
                 PUMPKIN_SEEDS,
                 POTATO,
                 CARROT,
                 NETHER_WART,
                 COCOA_BEANS,
                 SWEET_BERRIES,
                 GLOW_BERRIES -> true;
            default -> false;
        };
    }

    /**
     * Материалы, которые житель может получить и посадить
     */
    private boolean isVillagerPlantable(Material material) {
        return switch (material) {
            case WHEAT_SEEDS,
                 BEETROOT_SEEDS,
                 POTATO,
                 CARROT -> true;
            default -> false;
        };
    }

    /**
     * Преобразует тип блока посева в соответствующий материал семян.
     * Жители сажают блоки посевов (WHEAT, BEETROOTS, POTATOES, CARROTS),
     * а в конфиге ограничения настроены на материалы семян/предметов.
     */
    private Material cropBlockToSeed(Material cropBlock) {
        return switch (cropBlock) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case POTATOES -> Material.POTATO;
            case CARROTS -> Material.CARROT;
            default -> null;
        };
    }
}
