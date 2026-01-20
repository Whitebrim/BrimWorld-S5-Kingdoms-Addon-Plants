# PlantRestrictions

Плагин для Minecraft-сервера Folia 1.21+, ограничивающий посадку растений по королевствам.

## Описание

PlantRestrictions работает совместно с KingdomsAddon и позволяет настроить, какие растения могут сажать игроки каждого королевства. Например, снежное королевство может сажать только ели и ягоды, тропическое - бамбук и какао.

## Особенности

- **Folia-совместимость**: Полная поддержка многопоточной архитектуры Folia
- **Гибкая настройка**: Отдельный список растений для каждого королевства
- **Глобальные разрешения**: Растения, доступные всем королевствам
- **Bypass система**: Администраторы могут обходить ограничения
- **Поддержка всех растений**: Саженцы, семена, цветы, грибы и другие

## Требования

- Folia 1.21+ или Paper 1.21+
- Java 21+
- KingdomsAddon 1.7.0+

## Установка

1. Скачайте `PlantRestrictions-1.0.0.jar`
2. Поместите JAR в папку `plugins/`
3. Убедитесь, что KingdomsAddon установлен
4. Запустите сервер
5. Настройте `config.yml`

## Сборка

```bash
mvn clean package
```

Результат: `target/PlantRestrictions-1.0.0.jar`

## Конфигурация

### config.yml

```yaml
# Режим отладки
debug: false

# Сообщения
messages:
  no-permission: "&cУ вашего королевства нет права сажать это растение!"
  no-kingdom: "&cВы не принадлежите ни к одному королевству!"
  reload-success: "&aКонфигурация PlantRestrictions перезагружена!"

# Разрешённые растения для каждого королевства
kingdoms:
  snow_kingdom:
    allowed-plants:
      - SPRUCE_SAPLING      # Ель
      - BIRCH_SAPLING       # Берёза
      - SWEET_BERRIES       # Сладкие ягоды
      - POTATO
      - CARROT

  forest_kingdom:
    allowed-plants:
      - OAK_SAPLING         # Дуб
      - BIRCH_SAPLING       # Берёза
      - DARK_OAK_SAPLING    # Тёмный дуб
      - WHEAT_SEEDS
      - PUMPKIN_SEEDS

  tropical_kingdom:
    allowed-plants:
      - JUNGLE_SAPLING      # Джунгли
      - BAMBOO_SAPLING              # Бамбук
      - COCOA_BEANS         # Какао
      - SUGAR_CANE
      - MELON_SEEDS

# Глобально разрешённые растения (для всех)
global-allowed:
  - WHEAT_SEEDS
  - NETHER_WART

# Ограничивать игроков без королевства?
restrict-teamless: true
```

## Команды

| Команда | Описание | Право |
|---------|----------|-------|
| `/pr help` | Показать справку | - |
| `/pr list [kingdom]` | Список разрешённых растений | `plantrestrictions.info` |
| `/pr reload` | Перезагрузить конфигурацию | `plantrestrictions.reload` |

Альтернативы: `/plantrestrictions`, `/plants`

## Права

| Право | Описание | По умолчанию |
|-------|----------|--------------|
| `plantrestrictions.admin` | Все права | op |
| `plantrestrictions.reload` | Перезагрузка конфигурации | op |
| `plantrestrictions.bypass` | Обход ограничений | op |
| `plantrestrictions.info` | Просмотр информации | true |

## Поддерживаемые растения

### Саженцы
- OAK_SAPLING, SPRUCE_SAPLING, BIRCH_SAPLING
- JUNGLE_SAPLING, ACACIA_SAPLING, DARK_OAK_SAPLING
- CHERRY_SAPLING, MANGROVE_PROPAGULE, PALE_OAK_SAPLING

### Семена
- WHEAT_SEEDS, BEETROOT_SEEDS, MELON_SEEDS, PUMPKIN_SEEDS

### Овощи
- POTATO, CARROT

### Другие растения
- COCOA_BEANS, SUGAR_CANE, CACTUS, BAMBOO_SAPLING
- SWEET_BERRIES, GLOW_BERRIES, NETHER_WART
- CHORUS_FLOWER

### Грибы
- BROWN_MUSHROOM, RED_MUSHROOM
- CRIMSON_FUNGUS, WARPED_FUNGUS

### Цветы
- SUNFLOWER, LILAC, ROSE_BUSH, PEONY

## Совместимость с Folia

Плагин полностью совместим с Folia:
- Использует потокобезопасные коллекции (`ConcurrentHashMap`)
- События обрабатываются в контексте региона игрока
- Не требует главного потока
- Флаг `folia-supported: true` в plugin.yml

## Лицензия

MIT License
