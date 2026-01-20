package gg.brim.plantrestrictions.listener;

import gg.brim.kingdoms.api.KingdomsAPI;
import gg.brim.plantrestrictions.PlantRestrictions;
import gg.brim.plantrestrictions.manager.PlantManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Слушатель событий посадки растений.
 * В Folia события вызываются в регионе игрока, поэтому
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
     * Обработка взаимодействия (посадка семян в землю)
     * Семена сажаются через правый клик по земле, а не через BlockPlaceEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Только правый клик по блоку
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Только главная рука (избегаем двойной обработки)
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
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

        // Проверяем, можно ли посадить на этот блок
        if (!canPlantOn(clickedBlock, material)) {
            return;
        }

        // Проверяем bypass право
        if (player.hasPermission("plantrestrictions.bypass")) {
            plugin.debug("Игрок " + player.getName() + " имеет bypass право (interact)");
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
                    " (королевство: " + kingdomId + ")");
        }
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
     * Проверяет, можно ли посадить данный материал на указанный блок
     */
    private boolean canPlantOn(Block block, Material plantMaterial) {
        Material blockType = block.getType();

        return switch (plantMaterial) {
            // Семена сажаются на вспаханную землю
            case WHEAT_SEEDS, BEETROOT_SEEDS, MELON_SEEDS, PUMPKIN_SEEDS,
                 TORCHFLOWER_SEEDS, PITCHER_POD, POTATO, CARROT -> 
                blockType == Material.FARMLAND;

            // Адский нарост на песок душ
            case NETHER_WART -> 
                blockType == Material.SOUL_SAND || blockType == Material.SOUL_SOIL;

            // Какао на джунглевое дерево
            case COCOA_BEANS -> 
                blockType == Material.JUNGLE_LOG || 
                blockType == Material.JUNGLE_WOOD ||
                blockType == Material.STRIPPED_JUNGLE_LOG ||
                blockType == Material.STRIPPED_JUNGLE_WOOD;

            // Ягоды на землю/траву
            case SWEET_BERRIES, GLOW_BERRIES -> 
                isGrassOrDirt(blockType);

            default -> false;
        };
    }

    /**
     * Проверяет, является ли материал землёй или травой
     */
    private boolean isGrassOrDirt(Material material) {
        return switch (material) {
            case GRASS_BLOCK, DIRT, COARSE_DIRT, ROOTED_DIRT, PODZOL, 
                 MYCELIUM, MOSS_BLOCK, MUD, MUDDY_MANGROVE_ROOTS -> true;
            default -> false;
        };
    }
}
