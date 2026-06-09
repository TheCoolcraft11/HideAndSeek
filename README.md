![HideAndSeek Banner](assets/images/banner.png)

> A customizable Minecraft Hide and Seek minigame plugin, with custom items, loadouts,
> cosmetics, multiple game modes, a perk system, and extensive stats tracking.

---

## Table of Contents

- [Overview](#overview)
- [Screenshots](#screenshots)
- [Game Modes](#game-modes)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Map Setup](#map-setup)
- [Loadout System](#loadout-system)
- [Perk System](#perk-system)
- [Items Reference](#items-reference)
- [Commands & Permissions](#commands--permissions)
- [NMS Features](#nms-features)
- [Project Structure](#project-structure)
- [Building from Source](#building-from-source)

---

## Overview

HideAndSeek is a Minecraft minigame plugin in which one team hides while the other seeks. Hiders can disguise themselves
as blocks, shrink to a tiny size, wear custom skins, or simply rely on good hiding spots, while seekers use a diverse
arsenal of utility items to track, reveal, and eliminate them. The plugin is built on top of MinigameFramework (also
developed by me), which handles phase management, team assignment, item registration, and player data persistence.

The plugin supports custom maps with per-map setting overrides, a lobby voting system for maps and game modes, a
persistent loadout and cosmetic skin system, a round-based perk shop, detailed per-player statistics, and an extensive
in-game settings GUI provided by MinigameFramework.

---

## Screenshots

https://github.com/user-attachments/assets/5b4c1312-afd0-4cdd-96a1-fd94ada127a8

*Block mode: hiders disguise as blocks and blend into the environment.*

![Loadout GUI Hider](assets/images/LoadoutHider.png) ![Loadout GUI Seeker](assets/images/LoadoutSeeker.png)

*The loadout screen. Hiders and seekers have separate item pools and token budgets.*

https://github.com/user-attachments/assets/d648ddb3-64c7-426b-a90d-e6340d015343

*Block Selector GUI for choosing and customizing your block disguise.*

https://github.com/user-attachments/assets/4ef363c3-2379-4074-812d-bf023cd184ff

https://github.com/user-attachments/assets/5cc24e07-a249-480d-83b5-f1e79d4e51a4

https://github.com/user-attachments/assets/ebb8a135-0015-4d5d-8daa-06d11a6f2318

*The Proximity Sensor, Camera System, and Cage Trap in action.*

---

## Game Modes

| Mode     | Description                                                                                                                                                                       |
|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `NORMAL` | Hiders hide in the map as themselves. Seekers hunt them down using their sword and utility items.                                                                                 |
| `BLOCK`  | Hiders disguise as configurable blocks and blend into the scenery. Seekers must spot inconsistencies.                                                                             |
| `SMALL`  | Hiders are scaled down to a configurable size, making them much harder to spot. Seekers can optionally be resized too.                                                            |
| `SKIN`   | Hiders disguise using custom skins (e.g. block-like or environment-matching skins) to blend into the landscape. They may also be optionally scaled down for increased difficulty. |

Game modes can be changed via the in-game settings GUI or voted on by players in the lobby.

---

## Features

### Core Gameplay

- Four distinct game modes: Normal, Block, Small, Skin
- Lobby with player readiness checks and map/gamemode/role-preference voting
- Configurable hiding and seeking phase durations
- World border support with per-map border configurations
- Automatic map cleanup and lobby return after each round
- Points and scoring system with round-end coin rewards
- Boss bar showing remaining hiders and seekers during the seeking phase
- Anti-cheat visibility filtering: seekers only see hiders within a configured range and line of sight
- Hider camping detection and punishment system
- Environmental damage capping (drowning, fire, lava, suffocation, freezing, contact, hot floor) to prevent hiders escaping through hazards
- Adrenaline Rush comeback mechanic: buffs trigger when the last hider(s) survive or time runs low

### Map System

- Multiple maps with per-map configuration (allowed blocks, skins, timings, seeker count, setting overrides, vending machine locations)
- Random map selection or player lobby voting
- Automatic world copying and deletion per round
- Per-map world borders, spawn points, and block lists
- Map info title shown at round start (configurable display mode)

### Loadout System

- Players customize their item selection in the lobby before each round
- Token budget system with five rarity tiers: Common, Uncommon, Rare, Epic, Legendary
- Separate hider and seeker loadout slots and token pools
- Slot preferences: assign item types to specific inventory slots with configurable fallbacks
- Admin loadout manager GUI: filter items per role (blacklist/whitelist), lock individual player loadouts, define admin role presets, force a preset on all players
- Loadout data persisted across sessions via database or YAML

### Perk System

- Per-round perk shop: a random selection of perks is offered each round
- Two shop modes: inventory row (items placed directly in the player's hotbar) or vending machine (droppers placed at configured map locations)
- Hider perks: Adaptive Speed, Seeker Warning, Extra Life, Shadow Step, Camouflage, Double Jump, Trap Sense
- Seeker perks: Death Zone, Random Swap, Map Teleport, Relocate, Elytra Rush, Proximity Meter, Scent Trail, Auto Aim, Hit Display, Sword Bounce
- Finite perks: only one player per team can buy certain perks per round
- Seeker rebuyable perks: can be purchased multiple times with a configurable cooldown
- All perk costs, cooldowns, thresholds, and behaviours are configurable per-setting

### Cosmetic System

- Item skins (variants) purchasable with coins earned from round points
- Each item has multiple skins with unique visual appearances
- Kill effects: custom effects played when a seeker eliminates a hider
- Win skins: special effects played for the winning team at round end
- Death messages: custom kill/environmental death message formatting
- Coin shop with configurable rarity-based prices
- Cosmetic data persisted across sessions

### Statistics System

- Per-player stats tracked across all rounds: wins, losses, kills, deaths, damage dealt, survival time, playtime, taunts, last-hider-standing rounds, points and coins earned
- Per-item usage tracking (times equipped, times used)
- Per-map and per-game-mode play count tracking
- Perk usage tracking
- Block mode: hidden vs exposed time tracking
- Five-tab stats GUI: Overview, Combat, Items, Maps, Perks
- Admin `/mg stats` command to view any player's stats

### Hider Items

Firecracker, Big Firecracker, Firework Rocket, Cat Sound, Speed Boost, Knockback Stick, Tracker Crossbow, Block Swap,
Random Block, Smoke Bomb, Slowness Ball, Invisibility Cloak, Medkit, Totem of Undying, Ghost Essence *(NMS only,
falls back to limited behaviour without NMS)*, Remote Gateway

### Seeker Items

Seeker's Blade (chargeable + throwable), Grappling Hook, Ink Splash, Lightning Freeze, Glowing Compass, Curse Spell,
Block Randomizer, Chain Pull, Proximity Sensor, Cage Trap, Phantom Viewer, Camera System *(NMS only, disabled without
it)*, Seeker's Assistant *(NMS only, disabled without it)*

---

## Requirements

| Requirement       | Version  |
|-------------------|----------|
| Java              | 21+      |
| Paper (or Purpur) | 1.21.10+ |
| MinigameFramework | 1.0.0    |

### MinigameFramework

> [!IMPORTANT]
> HideAndSeek depends on **MinigameFramework**, a companion library also developed by me. **It is not currently publicly
> available, neither the JAR nor the source code.**

> [!NOTE]
> MinigameFramework will be released publicly in the future. This section will be updated when that happens.

---

## Installation

1. Ensure your server runs Paper 1.21.10/11 with Java 21 or newer.
2. Wait until `MinigameFramework.jar` is released and place it in your `plugins/` folder.
3. Place `HideAndSeek-1.0-SNAPSHOT.jar` in your `plugins/` folder.
4. Start the server once to generate config files.
5. Set up at least two scoreboard teams via MinigameFramework's team configuration.
6. Add at least one map (see [Map Setup](#map-setup)).
7. Restart the server.

### Quick Checklist

- [ ] Java 21+ on the server
- [ ] Paper 1.21.10/11 server jar
- [ ] `MinigameFramework.jar` in `plugins/`
- [ ] `HideAndSeek-1.0-SNAPSHOT.jar` in `plugins/`
- [ ] Two teams configured in MinigameFramework
- [ ] At least one map entry in `plugins/HideAndSeek/maps.yml`
- [ ] Server restarted after first run

---

## Configuration

The plugin generates `config.yml` and `maps.yml` in `plugins/HideAndSeek/` on first run.

### How settings work

All gameplay settings are managed through the **in-game settings GUI**, provided by MinigameFramework. The `config.yml`
file only serves to override the default value of a setting. If a setting is not present in `config.yml`, the plugin's
built-in default is used instead. Changes made in the GUI take effect immediately but do **NOT** persist across restarts.

### Top-level config keys

```yaml
# Hide & Seek Plugin Configuration

# List of available map world names
# These must match the world names in maps.yml
maps:
  - "map1"
  - "map2"

# NOTE: Map-specific configuration (spawn-points, allowed-blocks, world-borders, preferred-modes)
# is saved in maps.yml. See that file for per-map settings.

nms:
   # Master switch: attempt to load NMS adapter. If true, plugin will try to enable NMS features
   # on startup. If adapter missing or fails, behavior depends on fallbacks.
  enabled: true

persistence:
  save-skin-data: true
  save-loadout-data: true

inject-datapack: true

tab:
   enable-header: true
   enable-footer: true
   server-ip: "yourserver.com"

   # Header configuration
   header:
      # Color gradient settings for animated text
      title-colors:
         - "#FF0000" # Red
         - "#FFFF00" # Yellow
      title-speed: 0.5

      ip-colors:
         - "#00FF00" # Green
         - "#00FFFF" # Aqua
         - "#0088FF" # Light Blue
      ip-speed: 0.3

      # Header elements
      elements:
         - text: "Hello "
           enabled: true
           color: "#808080" # Gray
           animated: false
         - text: "{player}"
           enabled: true
           color: "#FFFF00" # Yellow
           animated: false
         - text: "!"
           enabled: true
           color: "#808080" # Gray
           animated: false
         - text: "\nYou are playing "
           enabled: true
           color: "#808080" # Gray
           animated: false
         - text: "HideAndSeek"
           enabled: true
           color-list: "title-colors" # Use color gradient
           speed: "title-speed"
           animated: true
         - text: " on "
           enabled: true
           color: "#808080" # Gray
           animated: false
         - text: "{server-ip}"
           enabled: true
           color-list: "ip-colors" # Use color gradient
           speed: "ip-speed"
           animated: true
         - text: "\n\n"
           enabled: true
           color: "#FFFFFF" # White
           animated: false

   # Footer configuration
   footer:
      # Footer elements
      elements:
         - text: "\nRole: "
           enabled: true
           color: "#A9A9A9" # Dark Gray
         - text: "{role}"
           enabled: true
           color: "{role-color}"
         - text: " | "
           enabled: true
           color: "#A9A9A9" # Dark Gray
         - text: "{mode}"
           enabled: true
           color: "#008000" # Green
         - text: "\nPlayers: "
           enabled: true
           color: "#A9A9A9" # Dark Gray
         - text: "{players-total}"
           enabled: true
           color: "#FFFFFF" # White
         - text: " ("
           enabled: true
           color: "#A9A9A9" # Dark Gray
         - text: "{players-hiders}"
           enabled: true
           color: "#00AA00" # Green
         - text: "|"
           enabled: true
           color: "#A9A9A9" # Dark Gray
         - text: "{players-seekers}"
           enabled: true
           color: "#FF0000" # Red
         - text: ")"
           enabled: true
           color: "#A9A9A9" # Dark Gray
         - text: "\nPoints: "
           enabled: true
           color: "#FFD700" # Gold
         - text: "{points}"
           enabled: true
           color: "#FFD700" # Gold
         - text: "\nCoins: "
           enabled: true
           color: "#FFFF00" # Yellow
         - text: "{coins}"
           enabled: true
           color: "#FFFF00" # Yellow
         - text: "\n"
           enabled: true
           color: "#FFFFFF" # White

scoreboard:
   enabled: true
   title:
      text: "HideAndSeek"
      small-caps: true
      underline: true
      colors:
         gradient:
            - "#FF0000" # Red
            - "#FFFF00" # Yellow

   animation:
      enabled: true
      speed: 0.15

   formatting:
      small-caps: true
      padding: true
      padding-amount: 3

   lines:
      - text: "Role:"
        enabled: true
        color: "#A9A9A9" # Dark Gray
      - text: "{role}"
        enabled: true
        color: "#0000FF" # Blue
      - text: "Mode:"
        enabled: true
        color: "#A9A9A9" # Dark Gray
      - text: "{mode}"
        enabled: true
        color: "#008000" # Green
      - text: "Players: {players-total} ({players-hiders}|{players-seekers})"
        enabled: true
        color: "#A9A9A9,#FFFFFF,#00FF00,#FF0000"
      - text: "Points: {points}"
        enabled: true
        color: "#FFD700" # Gold
      - text: "Coins: {coins}"
        enabled: true
        color: "#FFFF00" # Yellow

settings:
   # Global disallowed blockstate properties for the appearance GUI
  disallowed-blockstates:
    - "waterlogged"
    - "conditional"

  seeker-break-blocks:
    - "SHORT_GRASS"
    - "TALL_GRASS"
    - "SEAGRASS"
    - "TALL_SEAGRASS"

  block-interaction-exceptions:
    - "*_DOOR"
    - "*_FENCE_GATE"
    - "*_TRAPDOOR"
    - "*_BUTTON"
    - "*_LEVER"

  block-physics-exceptions:
    - "*_DOOR"
    - "*_FENCE_GATE"
    - "*_TRAPDOOR"
    - "*_BUTTON"
    - "*_LEVER"
```

### Overriding setting defaults

Any setting shown in the in-game GUI can have its default value overridden in `config.yml` under the `settings:` key.
For example:

```yaml
settings:
  game.mode: BLOCK
  game.hiding-time: 90
  game.seeking-time: 240
  game.hiders.health: 10
  anticheat.seeking.visibility-range: 16.0
  loadout.hider-max-items: 4
  loadout.hider-max-tokens: 14
  hider-items.speed-boost.cooldown: 15
  seeker-items.glowing-compass.cooldown: 30
  points.hider.survival.amount: 10
  perks.enabled: true
  perks.perks-per-round: 3
```

A full list of available setting keys can be found in
[`SettingRegistrar.java`](plugin/src/main/java/de/thecoolcraft11/hideAndSeek/setting/SettingRegistrar.java).

---

## Map Setup

Maps are defined in `plugins/HideAndSeek/maps.yml`. Each key is the internal map name and must match the name of a
world folder present on the server.

```yaml
map1:
  pretty-name: "Starter Valley"
  description: "Default map"
  author: "Server Admin"
  size: "MEDIUM"
  icon: "GRASS_BLOCK"
  spawn-points:
    - "0,65,0,0,0"       # Format: x,y,z,yaw,pitch
  world-borders: []       # Format: centerX,centerZ,size  OR  centerX,centerZ,r:radius
  preferred-modes:
    - "BLOCK"
  seeker-break-blocks:
    - "SHORT_GRASS"
    - "TALL_GRASS"
  block-interaction-exceptions:
    - "*_DOOR"
    - "*_FENCE_GATE"
    - "*_TRAPDOOR"
    - "*_BUTTON"
    - "*_LEVER"
  block-physics-exceptions:
    - "*_DOOR"
    - "*_FENCE_GATE"
    - "*_TRAPDOOR"
    - "*_BUTTON"
    - "*_LEVER"
  setting-overrides:
    hider-items:
      firework-rocket:
        target-y: 64
  players:
    min: 4
    recommended: 8
    max: 16
  seekers:
    min: 1
    per-players: 8
    max: 4
  timings:
    hiding-time: 60
    seeking-time: 300
  vending-machine-locations:    # Dropper blocks placed during seeking for the perk vending machine shop mode
    - "12,64,-8"
    - "-40,71,22"
  allowed-blocks:               # Used in BLOCK mode
    - "STONE"
    - "OAK_LOG[*]"
    - "*CANDLE{RED_CANDLE}"
    - "OAK_TRAPDOOR[half,open,facing]"
  allowed-skins:                # Used in SKIN mode; references keys defined in skins.yml
    - "oak_log"
    - "stone"
```

### World Border Format

If spawn-point count equals world-border count, each spawn is matched to its corresponding border by index. Otherwise,
all spawns use the first border.

### Block Pattern Syntax

The `allowed-blocks` list accepts a pattern DSL for flexible block selection:

| Pattern                        | Meaning                                                   |
|--------------------------------|-----------------------------------------------------------|
| `STONE`                        | Exactly the `STONE` material                              |
| `OAK_SLAB[type=bottom]`        | Oak slab locked to a specific block state                 |
| `OAK_SLAB[*]`                  | Oak slab with any block state allowed                     |
| `*SLAB{OAK_SLAB}`              | All slab variants, defaulting to Oak Slab                 |
| `*SLAB{OAK_SLAB}[type=bottom]` | All slab variants, default Oak Slab, locked block state   |
| `#planks{OAK_PLANKS}`          | All blocks in the `planks` Bukkit tag, default Oak Planks |
| `{STONE,GRANITE,DIORITE}`      | Explicit custom material list                             |

### Skin Setup

Custom skins for `SKIN` mode are defined in `plugins/HideAndSeek/skins.yml`:

```yaml
oak_log:
  name: "Oak Log"
  icon: "minecraft:oak_log"
  value: "<base64 skin texture value>"
  signature: "<base64 skin texture signature>"
```

Skin textures can be generated using services such as [Mineskin](https://mineskin.org). Each skin entry must have a
valid signed `value`/`signature` pair.

---

## Loadout System

Players configure their loadout in the lobby before each round. Each item has a rarity that determines its token cost.

| Rarity    | Default Token Cost |
|-----------|--------------------|
| Common    | 1                  |
| Uncommon  | 2                  |
| Rare      | 4                  |
| Epic      | 6                  |
| Legendary | 10                 |

By default, hiders have 12 tokens and up to 3 item slots. Seekers have 12 tokens and up to 4 item slots. All values are
configurable via the in-game settings GUI or under `settings.loadout.*` in `config.yml`.

### Slot Preferences

Players can configure which item *types* (e.g. Mobility, Offense, Trap) are placed in which inventory slots, with a
fallback type per slot. This is accessible from the loadout GUI via the Slot Preferences button.

### Admin Preset System

Admins can define up to 5 role presets per role (hider/seeker) in the Admin Loadout Manager GUI (`/mg loadout admin`).
Presets can be:

- **Enabled/disabled** per slot
- **Forced**: locks all players of that role to the specified preset
- **Restricted**: players may only choose from enabled admin presets rather than building their own loadout

---

## Perk System

At the start of each seeking phase, a random pool of perks is selected (configurable count). Players spend their
accumulated round points in the perk shop to purchase perks.

### Shop Modes

- **Inventory**: perk items are placed directly in the player's second hotbar row during the seeking phase.
- **Vending Machine**: dropper blocks are placed at positions defined in `maps.yml` under `vending-machine-locations`.
  Players interact with the dropper to open the shop.

### Hider Perks

| Perk           | Description                                                         |
|----------------|---------------------------------------------------------------------|
| Adaptive Speed | Grants Speed when HP drops below a configurable threshold           |
| Seeker Warning | Displays a warning title and border pulse when a seeker approaches  |
| Extra Life     | Earns absorption hearts proportional to points accumulated          |
| Shadow Step    | Auto-teleports to a safe random location when HP drops critically   |
| Camouflage     | Periodically cleanses negative status effects and glow              |
| Double Jump    | Allows a mid-air second jump using the flight key                   |
| Trap Sense     | Highlights nearby traps with particles and a warning title          |

### Seeker Perks

| Perk            | Description                                                                        |
|-----------------|------------------------------------------------------------------------------------|
| Death Zone      | Opens a map picker to draw a danger circle; hiders inside must escape or die       |
| Random Swap     | Teleports the seeker to a random hider's position (and vice versa)                 |
| Map Teleport    | Opens a map picker to select a teleport destination, with a blindness landing      |
| Relocate        | Forces every hider to move away from their current position or die                 |
| Elytra Rush     | Launches the seeker into a brief glide with a configurable launch boost            |
| Proximity Meter | Displays a hot/cold subtitle ping based on distance to the nearest hider           |
| Scent Trail     | Renders a particle trail showing recent hider movement paths                       |
| Auto Aim        | Guides the Seeker's Blade toward nearby hiders on throw (passive)                  |
| Hit Display     | Shows the sword's projected flight curve while charging (passive)                  |
| Sword Bounce    | The thrown Seeker's Blade ricochets to additional hiders after each hit (passive)  |

All perk costs, cooldowns, radii, durations, and other parameters are configurable via the settings GUI under the
`perks.*` sections.

---

## Items Reference

### Hider Items

| Item               | Rarity    | Without NMS | Description                                                              |
|--------------------|-----------|-------------|--------------------------------------------------------------------------|
| Firecracker        | Common    | Full        | Place a small exploding candle taunt for points                          |
| Cat Sound          | Common    | Full        | Play a loud cat sound to all online players                              |
| Random Block       | Common    | Full        | Reroll your disguise block (limited uses)                                |
| Speed Boost        | Common    | Full        | Gain a speed effect or velocity burst                                    |
| Tracker Crossbow   | Common    | Full        | Shoot seekers to earn points and upgrade items                           |
| Knockback Stick    | Common    | Full        | Knock seekers away; upgrades when crossbow hits threshold                |
| Firework Rocket    | Uncommon  | Full        | Launch a high-altitude firework taunt                                    |
| Slowness Ball      | Uncommon  | Full        | Throw a projectile that slows seekers                                    |
| Smoke Bomb         | Uncommon  | Full        | Throw a smoke cloud for visual cover                                     |
| Block Swap         | Rare      | Full        | Swap block disguise with the nearest other hider                         |
| Big Firecracker    | Rare      | Full        | Large explosion with bouncing mini firecrackers                          |
| Medkit             | Rare      | Full        | Hold block to channel-heal; release early to cancel                      |
| Ghost Essence      | Rare      | Fallback    | Phase through walls briefly; falls back to limited behaviour without NMS |
| Remote Gateway     | Epic      | Full        | Place paired gateways that teleport between anchors until the round ends |
| Invisibility Cloak | Epic      | Full        | Turn invisible for a configurable duration                               |
| Totem of Undying   | Legendary | Full        | Activate a one-time revive window on death                               |

### Seeker Items

| Item               | Rarity    | Without NMS | Description                                            |
|--------------------|-----------|-------------|--------------------------------------------------------|
| Grappling Hook     | Common    | Full        | Cast and reel in to launch yourself forward            |
| Curse Spell        | Uncommon  | Full        | Empower your sword to curse hiders on hit              |
| Chain Pull         | Uncommon  | Full        | Pull the hider in front of you to your position        |
| Proximity Sensor   | Rare      | Full        | Place a sensor that glows hiders who walk into range   |
| Ink Splash         | Rare      | Full        | Blind all hiders within a radius                       |
| Cage Trap          | Rare      | Full        | Place a hidden trap that cages and immobilises a hider |
| Phantom Viewer     | Rare      | Full        | Capture a rough map snapshot from a hider perspective  |
| Block Randomizer   | Epic      | Full        | Force all hiders to immediately reroll their block     |
| Glowing Compass    | Epic      | Full        | Reveal the nearest hider with a glow effect            |
| Camera             | Epic      | Disabled    | Place up to 5 cameras and spectate through them        |
| Lightning Freeze   | Legendary | Fallback    | Freeze all hiders in place for a short duration        |
| Seeker's Assistant | Legendary | Disabled    | Summon an AI hunting mob that tracks and shoots hiders |

---

## Commands & Permissions

| Command                  | Permission                          | Description                                                                      |
|--------------------------|-------------------------------------|----------------------------------------------------------------------------------|
| `/mg loadout`            | `hideandseek.command.loadout`       | Open the loadout selection GUI                                                   |
| `/mg loadout admin`      | `hideandseek.command.loadout.admin` | Open the admin loadout management GUI                                            |
| `/mg map <name>`         | `hideandseek.command.map`           | Set or view the current map                                                      |
| `/mg vote`               | `hideandseek.command.vote`          | Open the map/gamemode/role voting GUI                                            |
| `/mg ready`              | `hideandseek.command.ready`         | Toggle your ready state in the lobby                                             |
| `/mg skin`               | `hideandseek.command.skin`          | Open the item skin shop                                                          |
| `/mg unstuck`            | `hideandseek.command.unstuck`       | Teleport to your last safe position                                              |
| `/mg stats [player]`     | `hideandseek.command.stats`         | Open the stats GUI for yourself or another player                                |
| `/mg debug <subcommand>` | *(op)*                              | Debug utilities (points, coins, skins, perks, config validation, YAML migration) |

> **Note:** Full permission nodes and additional admin commands will be documented once MinigameFramework's command API
> is finalized.

### Role Permissions

The following permissions are assigned automatically during a round and can be used for other plugins or command guards:

| Permission                   | Assigned to                                 |
|------------------------------|---------------------------------------------|
| `hideandseek.role.hider`     | Players currently assigned as hiders        |
| `hideandseek.role.seeker`    | Players currently assigned as seekers       |
| `hideandseek.role.spectator` | Players in spectator during an active round |

---

## NMS Features

| Capability                                                       | Behaviour without NMS                                                         |
|------------------------------------------------------------------|-------------------------------------------------------------------------------|
| Client gamemode spoofing (appear in survival while in spectator) | Ghost Essence falls back to limited behaviour                                 |
| Mob pathfinding API                                              | Ghost Essence falls back; Seeker's Assistant is disabled                      |
| Client camera entity spoofing                                    | Camera item is disabled entirely                                              |
| Client entity glowing packets                                    | Camera night-vision mode unavailable; Trap Sense glow disabled                |
| Client entity spawning packets                                   | Camera item is disabled entirely                                              |
| Client entity removal packets                                    | Camera item is disabled entirely                                              |
| Client entity visibility packets                                 | Anti-cheat visibility filter falls back to server-side hide/show              |
| Custom entity AI goals                                           | Seeker's Assistant is disabled entirely                                       |
| Projectile entity raycast for hit detection                      | Seeker's Blade throw falls back to vanilla raycast                            |
| Client lightning packet                                          | Lightning Freeze uses a server-side lightning bolt visible only to the target |
| Client fake world-border warning                                 | Adrenaline Rush and Seeker Warning perk border pulse are not shown            |
| Per-player dialog registry (Netty packet injection)              | In-game quick-action dialog buttons not available                             |
| Spectator inventory Netty interception                           | Spectator teleport GUI non-functional                                         |

The NMS implementation currently targets **Paper 1.21.10** (`nms-v1_21_10`) and **Paper 1.21.11** (`nms-v1_21_11`).
Adding support for another version requires a new Gradle module implementing the `NmsAdapter` interface and providing an
`NmsAdapterMeta` that declares which Minecraft versions it supports.

---

## Project Structure

The repository is a multi-module Gradle project:

```
HideAndSeek/
├── plugin/          # Core plugin logic: items, phases, GUIs, listeners, commands, perks, stats
├── nms/             # NMS API module: NmsAdapter interface, NmsLoader, NoopNmsAdapter fallback, NmsCapabilities enum
├── nms-v1_21_10/    # Version-specific NMS implementation for Paper 1.21.10
├── nms-v1_21_11/    # Version-specific NMS implementation for Paper 1.21.11
├── build.gradle     # Root build file
└── settings.gradle  # Module declarations
```

The `nms` module defines the `NmsAdapter` interface and a `NoopNmsAdapter` that provides safe no-op fallbacks for every
method. Each versioned NMS module contains a live implementation along with an `NmsAdapterMeta` class that declares
which Minecraft versions it supports. `NmsLoader` scans the shadow JAR at runtime and instantiates the first matching
adapter. The `plugin` module depends on all modules and is bundled into a single shadow JAR at build time.

---

## Building from Source

### Prerequisites

- Java 21 JDK
- Git
- `MinigameFramework.jar`: **not publicly available**; will be released soon

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/TheCoolcraft11/HideAndSeek.git
   cd HideAndSeek
   ```

2. Install `MinigameFramework.jar` into your local Maven cache so Gradle can resolve it. Replace the version with
   whatever the author provides:

   Linux/Mac:
   ```bash
   mvn install:install-file \
     -Dfile=/path/to/MinigameFramework.jar \
     -DgroupId=de.thecoolcraft11 \
     -DartifactId=minigameframework \
     -Dversion=1.0-SNAPSHOT \
     -Dpackaging=jar
   ```

   Windows:
   ```cmd
   mvn install:install-file -Dfile=C:\path\to\MinigameFramework.jar -DgroupId=de.thecoolcraft11 -DartifactId=minigameframework -Dversion=1.0-SNAPSHOT -Dpackaging=jar
   ```

3. Build the plugin:
   ```bash
   ./gradlew shadowJar
   ```
   Windows:
   ```bash
   gradlew.bat shadowJar
   ```

4. The output JAR will be at:
   ```
   plugin/build/libs/HideAndSeek-1.0-SNAPSHOT.jar
   ```

5. Copy the JAR to your server's `plugins/` directory alongside `MinigameFramework.jar`.