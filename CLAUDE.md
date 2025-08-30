# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build System

This is a Minecraft plugin using Gradle as the build system.

### Essential Commands
- `./gradlew build` - Build the plugin (creates jar in build/libs/)
- `./gradlew shadowJar` - Create fat jar with dependencies included
- `./gradlew runServer` - Run test server with the plugin loaded
- No specific test commands found - manual testing via runServer

### Dependencies
- **Paper API 1.21.4** - Core Minecraft server API
- **ProtocolLib 5.3.0** - Required dependency for packet manipulation
- **Multiverse-Core** - Required dependency for world management
- **Lombok** - For code generation (@Getter, @Setter, @Data)
- **ACF (Aikar's Command Framework)** - Command handling
- **FastBoard** - Scoreboard management

## Architecture Overview

### Game Framework Structure

The framework follows a **timeline-based game architecture** where each game extends `MCEGame` and implements a state-driven timeline system:

```
MCEGame (base class)
├── Timeline system (MCETimeline + MCETimelineNode)
├── GameBoard system (MCEGameBoard + subclasses)
└── Game implementations (DiscoFever, MusicDodge, ParkourTag, SandRun)
```

### Key Components

#### 1. Core Game Flow
- **MCETimeline**: Manages game states/phases (launch → intro → preparation → cycle_start → cycle_end → end)
- **MCETimelineNode**: Individual timeline phases with durations and callbacks
- **MCEGame**: Base class defining game lifecycle hooks (`onLaunch()`, `onCycleStart()`, `onEnd()`, etc.)

#### 2. Configuration System
- **Config files**: Located in `src/main/resources/MCEConfig/*.cfg`
- **ConfigParser classes**: Each game has its own parser (e.g., `DiscoFeverConfigParser`)
- **Dynamic map loading**: Map names read from config files, not hardcoded

#### 3. Team Management
- **Constants.teams**: Array of `TeamWithDetails` defining all available teams
- **MCETeamUtils**: Utilities for team operations, use `getTeam(player)` to get player's team
- **Team colors**: Use `TeamWithDetails.teamColor()` with `NamedTextColor` enum values

#### 4. Function Implementation Pattern
**CRITICAL**: Each game follows the `FuncImpl` pattern where game logic is extracted to separate implementation classes:

```java
// Game class contains state and timeline management
public class GameName extends MCEGame {
    public void onCycleStart() {
        GameNameFuncImpl.someGameLogic(this);
    }
}

// FuncImpl contains static methods with actual game logic
public class GameNameFuncImpl {
    public static void someGameLogic(GameName game) {
        // Implementation here
    }
}
```

### Game Implementations

#### Current Games
1. **DiscoFever** - Platform survival game with colored blocks
2. **MusicDodge** - Dodge particle attacks synchronized to music (has advanced particle optimization)
3. **ParkourTag** - Tag game with parkour elements
4. **SandRun** - Survival game with falling sand

#### Game-Specific Patterns
- **Custom Handlers**: Each game can have custom event handlers in `customHandler/` subdirectories
- **Game Objects**: Game-specific objects in `gameObject/` subdirectories
- **Attack System** (MusicDodge): Complex attack framework with particle optimization via Plugin Messages

## Development Patterns

### Adding New Games
1. Extend `MCEGame` class
2. Create corresponding `GameNameFuncImpl` class for logic
3. Create `GameNameConfigParser` for configuration
4. Create `GameNameGameBoard` for scoreboard
5. Add custom handlers in `customHandler/` if needed
6. Add to Constants.java game instances

### Team Color Handling
```java
// CORRECT: Use team names and Constants.teams
for (TeamWithDetails teamDetails : Constants.teams) {
    Material material = getConcreteByColor(teamDetails.teamColor());
    teamColorMap.put(teamDetails.teamName(), material);
}

// WRONG: Don't call non-existent methods like MCETeamUtils.getRedTeam()
```

### Config File Structure
- Use `.cfg` extension
- Follow existing patterns in `src/main/resources/MCEConfig/`
- Map names should be configurable, not hardcoded

### Particle Optimization (MusicDodge)
- Uses Plugin Message Channel `mce:musicdodge` for client-side particle rendering
- **AttackDataEncoder** encodes attack data as strings
- **DustParticleInterceptor** prevents server-side particle packets
- See `MUSICDODGE_PARTICLE_OPTIMIZATION.md` for full details

### Command System
Commands use ACF framework and are registered in `MCEMainController.onEnable()`. All commands require OP permissions by default.

### Global State Management
- **MCEMainController**: Main plugin controller with static game timeline management
- **Constants.java**: Global static instances and configuration
- **Tool classes**: Utility classes in `tools/` package (MCEPlayerUtils, MCETeamUtils, etc.)

## Important Notes

### Required Dependencies
The plugin requires **ProtocolLib** and **Multiverse-Core** to function. These are declared as hard dependencies in plugin.yml.

### Java Version
- Target Java 21
- Uses modern Java features (switch expressions, records)
- Lombok annotations for boilerplate reduction

### Resource Management
- Game tasks must be properly cleaned up in `stop()` methods
- Use `BukkitRunnable.cancel()` for scheduled tasks
- Event handlers should be unregistered when games end