# Core Utility Layer Documentation

## Repository Guidelines

### Project Structure & Module Organization
- Source lives under `src/main/java/mcevent/MCEFramework`, with `commands` for Paper executors, `games/*` per minigame, `generalGameObject` for shared abstractions, and `tools` for utilities.
- Resources stay in `src/main/resources` with `plugin.yml` and default configs under `MCEConfig/`.
- Gradle builds emit jars to `build/libs/`.
- The dev Paper server keeps its runtime state in `run/`.

### Build, Test, and Development Commands
- Always use the Gradle wrapper (`./gradlew` on Unix, `.\gradlew.bat` on Windows).
- `./gradlew clean build` compiles against Paper 1.21.4 and runs unit tests.
- `./gradlew shadowJar` produces `build/libs/MCEFramework-1.0-SNAPSHOT-all.jar` with relocated dependencies.
- `./gradlew runServer` launches the local Paper server; copy the shaded jar into `run/plugins/` for manual checks.

### Coding Style & Naming Conventions
- Target Java 21, four-space indentation, same-line braces, and explicit `@Override` on plugin hooks.
- Packages stay lowercase (for example `mcevent.MCEFramework.games.survivalGame`), classes use PascalCase, and shared helpers often start with `MCE`.
- Constants use `UPPER_SNAKE_CASE`, and config keys mirror their YAML paths (such as `spectator.teleportDelay`).
- Lombok is welcome for data holders but should be avoided in Bukkit entry points.

### Testing Guidelines
- Add unit or integration coverage under `src/test/java`, naming files `<ClassUnderTest>Test`.
- Prefer MockBukkit or lightweight stubs over spinning up a server inside tests.
- Run `./gradlew test` before opening a PR and document any manual `./gradlew runServer` steps, commands issued, and expected outputs.

### Commit & Pull Request Guidelines
- Use short Conventional Commit prefixes (`fix:`, `feat:`) and squash fixups.
- PRs must outline player-facing impact, link issues, and attach testing evidence (logs, screenshots, reproduction commands).
- Flag config migrations or data resets so operations teams can prepare.

### Configuration & Security Notes
- Commit only redacted templates under `src/main/resources/MCEConfig`; keep production secrets on the server.
- When adding YAML options, document defaults in the relevant parser (for example `VotingSystemConfigParser`) and describe upgrade steps in the PR.
- Shadow relocates `co.aikar.*` and `fr.mrmicky.fastboard`, so avoid reflection on their original package names.

  ## Class Index

  - MCEResumableEventHandler — Toggleable base for pauseable listeners/controllers.
  - GamePlayerJoinHandler — Contract for mid-game player admission rules.
  - DefaultGamePlayerJoinHandler — Stock implementation that restores participants or spectates newcomers.
  - MCEGame — Abstract minigame template with lifecycle, timeline, and helper utilities.
  - MCEGameBoard — Base scoreboard/bossbar presenter plus participant counters.
  - MCEGameQuitHandler — Shared quit/elimination helper with team wipe detection.
  - MCESpecialItem — Cooldown-aware special-item base class.
  - MCETimeline — Ordered list of timeline nodes with pause/resume support.
  - MCETimelineNode — A single timeline stage with duration/run logic.
  - MCETimeLineNodeFunction — Functional interface for node callbacks.
  - MCEPausableTimer — Per-stage timer that can pause/resume and tick boards.
  - MCEConfigParser — Config loader + intro reader inherited by all games.
  - MCEBlockRestoreUtils — Records original block data and restores worlds.
  - MCEWorldUtils — World/PVP helper (offset math, global PVP toggles).
  - MCETimerUtils — Delayed/framed task helpers and bossbar countdowns.
  - MCETimerFunction — Callback interface used by MCETimerUtils.
  - MCETeleporter — Mass teleport helper with duel-world inventory cleanup.
  - MCETeamUtils — Scoreboard team lookups, formatting, and friendly-fire toggles.
  - MCEPlayerUtils — Bulk player operations (tags, gamemode, inventory, sounds).
  - MCEMessenger — Central MiniMessage/titles/countdown helper.
  - MCEGlowingEffectManager — ProtocolLib-based glowing manager.
  - Constants — Global IDs, map names, team palette, plugin handle.
  - IntroTexts — Prebuilt MiniMessage intro snippets and dividers.
  - TeamWithDetails — Record describing a team’s names/colors.
  - GameSettingsState — In-memory admin settings toggles.
  - GameSettingsGUI — Inventory-based UI backed by GameSettingsState.
  - GameSettingsHandler — Gives admins the settings item and routes GUI events.
  - ChatFormatHandler — Team-colored <name> chat renderer.
  - GlobalPVPHandler — Master PVP gate wired into games/commands.
  - FriendlyFireHandler — Blocks damage between teammates unless enabled.
  - GlobalBlockInteractionHandler — Lobby-safe block edit rules with per-game overrides.
  - GlobalEliminationHandler — Unified PlayerDeath handling, SG chest drops, round end checks.
  - GlobalWorldBorderHandler — Custom half-heart tick damage outside world borders.
  - PlayerJoinHandler — Teleports/cleans players on join based on current game.
  - GamePlayerQuitHandler — Marks quitters dead and informs games.
  - SpecialItemInteractionHandler — Dispatcher for MCESpecialItem interactions.
  - LobbyItemHandler — Issues lobby loadouts and admin settings items.
  - LobbyDoubleJumpHandler — Enables flight-tap double jump in lobby/voting.
  - LobbyBounceHandler — Bounces lobby players above the Y floor.
  - LobbySaturationHandler — Reapplies saturation in the lobby.
  - LobbyTeleportCompassHandler — Handles lobby↔duel teleport compasses & voting cards.
  - WindLauncherHandler — Lobby wind-charge blaster with glow/knockback.
  - WelcomeMessageHandler — Animated welcome action bar & bossbar when idle.
  - WorldDaylightFreezeHandler — Freezes time at noon when players enter worlds.

  ## MCEResumableEventHandler (generalGameObject/MCEResumableEventHandler.java:9)

  - Responsibility: Minimal base that gives every listener/controller an isSuspended flag plus start()/suspend() helpers
    so behaviour can be toggled without unregistering events.
  - Typical usage: All global handlers (e.g., customHandler/ChatFormatHandler.java:24) extend it and guard event bodies
    with if (isSuspended()) return;.
  - Public API:
      - boolean isSuspended() / void setSuspended(boolean) — Lombok accessors.
      - void start() — sets isSuspended to false.
      - void suspend() — sets isSuspended to true.
  - Notes: Does not unregister listeners; callers must ensure event handlers always check isSuspended().

  ## GamePlayerJoinHandler (generalGameObject/GamePlayerJoinHandler.java:9)

  - Responsibility: Defines how each game reintegrates players who join mid-round (teleport, participation eligibility,
    default gamemode).
  - Typical usage: MCEGame.handlePlayerJoinDuringGame delegates to the current handler (generalGameObject/
    MCEGame.java:254); customHandler/PlayerJoinHandler.java:49 invokes it whenever players log in during an active game.
  - Public API:
      - void handlePlayerJoinDuringGame(Player player) — run when someone joins while the game is live.
      - boolean isGameParticipant(Player player) — determines whether other systems treat the player as active.
      - default GameMode getDefaultJoinGameMode() — defaults to spectator; override per game if needed.
  - Notes: Implementations must assume calls happen on main thread; heavy logic should be scheduled.

  ## DefaultGamePlayerJoinHandler (generalGameObject/DefaultGamePlayerJoinHandler.java:15)

  - Responsibility: Default handler used by all games: refreshes active teams, restores Active tags for real
    participants, and spectates observers/dead players.
  - Typical usage: Every MCEGame constructor sets this handler (generalGameObject/MCEGame.java:68). Games override by
    calling setPlayerJoinHandler.
  - Public API:
      - DefaultGamePlayerJoinHandler(MCEGame game) — binds to specific game instance.
      - void handlePlayerJoinDuringGame(Player player) — removes players from teams if necessary, sets tags, schedules
        gamemode fix.
      - boolean isGameParticipant(Player player) — returns true when player has Participant tag and is in the game
        world.
  - Notes: Uses 10-tick delayed tasks to avoid conflicts with teleport; assures scoreboard tags stay consistent.

  ## MCEGame (generalGameObject/MCEGame.java:16)

  - Responsibility: Abstract base for each minigame; manages metadata, configuration, timeline creation, lifecycle
    hooks, participant tags, and delayed tasks.
  - Typical usage: Games like games/survivalGame/SurvivalGame.java:32 extend it and override hooks such as onLaunch() or
    onCycleStart(). MCEMainController tracks the current instance (MCEMainController.java:427).
  - Public API highlights:
      - Constructors set title/world/durations; automatically install DefaultGamePlayerJoinHandler.
      - void init(boolean intro) — builds the timeline (launch → intro → preparation → cycles → end).
      - Hook methods onLaunch, intro, onPreparation, onCyclePreparation, onCycleStart, onCycleEnd, onEnd, initGameBoard.
      - Participant utilities: markParticipantsByWorld(), applyGamemodeByParticipation(), handlePlayerJoinDuringGame,
        isGameParticipant.
      - Task helpers: BukkitRunnable setDelayedTask(double seconds, MCETimerFunction fn), clearDelayedTasks().
      - Quit handling template: handlePlayerQuitDuringGame(Player) plus overridable checkGameEndCondition().
  - Notes: Timelines assume synchronous execution; remember to cancel outstanding tasks when ending a game.

  ## MCEGameBoard (generalGameObject/MCEGameBoard.java:15)

  - Responsibility: Provides titles/state placeholders plus helper counters for alive participants/teams across the
    active game world.
  - Typical usage: Each game implements its own board by extending this class (e.g., games/tntTag/gameObject/
    TNTTagGameBoard.java:18) and overriding globalDisplay().
  - Public API:
      - Constructors for (gameName, mapName[, totalRounds]).
      - void updateRoundTitle(int currentRound).
      - void globalDisplay() — expected to push scoreboard/bossbar updates.
      - Static counters: countRemainingParticipants(), countRemainingParticipantTeams(), countParticipantsTotal(),
        countParticipantTeamsTotal().
  - Notes: Counter methods depend on MCEMainController.getCurrentRunningGame() and scoreboard tags (Participant, dead).

  ## MCEGameQuitHandler (generalGameObject/MCEGameQuitHandler.java:17)

  - Responsibility: Standardizes how quitting players are treated: only counts as death during active cycle stages and
    provides helpers for team elimination ordering.
  - Typical usage: Games call handlePlayerQuit from their quit observers (games/spleef/Spleef.java:93), and
    checkTeamElimination to update elimination lists.
  - Public API:
      - static boolean isInCycleStartPhase(MCEGame game) — determines whether quitting should count as death.
      - static void handlePlayerQuit(MCEGame game, Player player, Runnable onDeathCallback) — marks player dead if
        appropriate and runs custom callbacks.
      - static boolean checkTeamElimination(String playerName, Team team, List<Team> order) — detects team wipes and
        sends global messages.
  - Notes: Uses delayed tasks to ensure post-quit logic executes after the player fully leaves; relies on MCEPlayerUtils
    and MCEMessenger for feedback.

  ## MCESpecialItem (generalGameObject/MCESpecialItem.java:26)

  - Responsibility: Base class for special right-click items with Adventure display names, cooldown logic, and action
    hooks.
  - Typical usage: HyperSpleef registers IceArrow, BlizzardStaff, etc., with its SpecialItemInteractionHandler (games/
    hyperSpleef/HyperSpleef.java:83).
  - Public API:
      - Constructor (String itemName, Material material, long cooldownTicks, Plugin plugin).
      - ItemStack createItem(), boolean handleRightClick(PlayerInteractEvent, Player).
      - Cooldown helpers: isOnCooldown, setCooldown, getRemainingCooldown, clearCooldown, clearAllCooldowns,
        setItemCooldown.
      - Visual helpers: playSound, spawnParticles, sendActionBar.
      - protected abstract boolean executeAction(Player player, PlayerInteractEvent event).
  - Notes: handleRightClick cancels the vanilla event when the item matches; implement executeAction to perform actual
    behaviour and return whether cooldown should apply.

  ## MCETimeline (generalGameObject/MCETimeline.java:11)

  - Responsibility: Stores ordered MCETimelineNodes and manages transitions, suspension, and timers.
  - Typical usage: MCEMainController uses a global event timeline (MCEMainController.java:54), while each MCEGame builds
    its own in init() (generalGameObject/MCEGame.java:103).
  - Public API:
      - void addTimelineNode(MCETimelineNode node), void reset(), void start(), void suspend(), void resume(), void
        nextState().
      - Time queries: int getCounter(), int getCurrentTimelineNodeDuration(), int getRemainingTime().
  - Notes: Expects nodes to have valid timers; nextState() logs transitions for debugging.

  ## MCETimelineNode (generalGameObject/MCETimelineNode.java:12)

  - Responsibility: Encapsulates a timeline stage: duration, suspendability, runnable, timer, and optional sub-timeline
    switch.
  - Typical usage: MCEGame.init builds the entire game flow by chaining nodes (generalGameObject/MCEGame.java:97
    onwards).
  - Public API:
      - Constructor (int maxDurationSeconds, boolean canSuspend, MCETimeLineNodeFunction onRunning, MCETimeline parent,
        MCEGameBoard board).
      - void start(), void stop(), void suspend(), void resume().
  - Notes: Nodes with isSwitchNode true hand off to a sub-timeline via MCEMainController.switchToTimeline.

  ## MCETimeLineNodeFunction (generalGameObject/MCETimeLineNodeFunction.java:7)

  - Responsibility: Single-method functional interface used for timeline node actions.
  - Typical usage: Pass lambdas when constructing MCETimelineNodes (e.g., MCEGame.init lines 97, 109, etc.).
  - Public API: void onRunning().
  - Notes: Runs synchronously on the main thread when node starts.

  ## MCEPausableTimer (tools/MCEPausableTimer.java:13)

  - Responsibility: Repeating Bukkit task that decrements per-second counters, refreshes game boards, and advances
    timelines when a stage ends.
  - Typical usage: Instantiated inside every MCETimelineNode to manage durations.
  - Public API:
      - Constructor (int seconds, MCETimeline parentTimeline, MCEGameBoard board).
      - void start(), void pause(), void resume(), void stop().
      - int getRemainingTime().
  - Notes: Calls gameBoard.globalDisplay() every second; ensure board updates are lightweight.

  ## MCEConfigParser (tools/MCEConfigParser.java:20)

  - Responsibility: Handles copying default configs from resources, reading them, parsing [Intro] sections into
    Adventure components, and exposing parse() for subclasses.
  - Typical usage: All game-specific parsers extend this class and implement parse() (see games/votingSystem/
    VotingSystemConfigParser.java:11).
  - Public API:
      - ArrayList<Component> openAndParse(String configFileName).
      - Protected helpers void open(String configFileName), ArrayList<Component> readIntro(), void parse() (override).
  - Notes: Uses Constants.plugin.getResource for defaults; ensure resource paths include MCEConfig/ when necessary.

  ## MCEBlockRestoreUtils (tools/MCEBlockRestoreUtils.java:15)

  - Responsibility: Stores the original BlockData for modified blocks so matches can restore worlds afterward.
  - Typical usage: Block place handlers call recordReplacedState (customHandler/GlobalBlockInteractionHandler.java:208),
    and games clean up with restoreAllForWorld (games/survivalGame/SurvivalGame.java:248).
  - Public API:
      - static void recordReplacedState(BlockState replacedState).
      - static int restoreAllForWorld(String worldName).
  - Notes: Only the first modification per coordinate is stored; best used in block place events before modifications
    occur.

  ## MCEWorldUtils (tools/MCEWorldUtils.java:9)

  - Responsibility: Utility functions for computing teleport offsets and toggling global PVP via GlobalPVPHandler.
  - Typical usage: commands/PKTSelectChaser.java:80 uses teleportLocation; games call disablePVP() before prepping
    (games/captureCenter/CaptureCenter.java:65).
  - Public API:
      - static Location teleportLocation(Location baseLoc, Location offsetLoc, int offset).
      - static void enablePVP(), static void disablePVP().
  - Notes: Both PVP methods delegate to MCEMainController.getGlobalPVPHandler(); handler must be initialized.

  ## MCETimerUtils (tools/MCETimerUtils.java:13)

  - Responsibility: Convenience scheduling helpers (delayed tasks, per-tick tasks, timed tasks) plus bossbar countdown
    animation.
  - Typical usage: MCEGame.setDelayedTask wraps setDelayedTask, TNT Tag phases call showGlobalDurationOnBossBar (games/
    tntTag/TNTTag.java:235).
  - Public API:
      - static BukkitRunnable setDelayedTask(double seconds, MCETimerFunction function).
      - static BukkitRunnable setFramedTask(MCETimerFunction function).
      - static void setFramedTaskWithDuration(MCETimerFunction function, double duration).
      - static void showGlobalDurationOnBossBar(BossBar bossBar, double duration, boolean isReversed).
  - Notes: All tasks run on the main thread; keep callbacks lightweight.

  ## MCETimerFunction (tools/MCETimerFunction.java:6)

  - Responsibility: Functional interface used by MCETimerUtils.
  - Public API: void run().
  - Notes: Typically implemented via lambdas.

  ## MCETeleporter (tools/MCETeleporter.java:13)

  - Responsibility: Teleports every online player to a target world spawn, clearing duel inventories just before
    switching.
  - Typical usage: Most games call globalSwapWorld(getWorldName()) when launching (games/survivalGame/
    SurvivalGame.java:80).
  - Public API:
      - static void globalSwapWorld(String worldName).
  - Notes: Teleporter assumes the world exists; will throw NullPointerException otherwise.

  ## MCETeamUtils (tools/MCETeamUtils.java:19)

  - Responsibility: Scoreboard team introspection/manipulation (counts, member lists, color formatting) and friendly
    fire toggles via FriendlyFireHandler.
  - Typical usage: Join handlers check getTeam, elimination logic uses getTeamColoredName, and global systems toggle
    friendly fire via enableFriendlyFire/disableFriendlyFire.
  - Public API:
      - Team lookup: int getActiveTeamCount(), ArrayList<Team> getActiveTeams(), ArrayList<Player> getPlayers(Team),
        Team getTeam(Player).
      - Formatting: String[] getTeamColor(Team), String getTeamColoredName(Team), String getUncoloredTeamName(Team).
      - Name tag visibility toggles.
      - Friendly fire: enableFriendlyFire(), disableFriendlyFire(), isFriendlyFireEnabled().
  - Notes: Depends on Constants.teams. Friendly-fire methods call handler start/suspend to flip behaviour.

  ## MCEPlayerUtils (tools/MCEPlayerUtils.java:18)

  - Responsibility: Batch operations across all online players—tags, gamemode, inventory, potion effects, FastBoard
    resets, sounds.
  - Typical usage: MCEGame.onLaunch clears inventories (generalGameObject/MCEGame.java:98); elimination messages use
    getColoredPlayerName; lobby resets use globalClearFastBoard.
  - Public API (selection):
      - void grantGlobalPotionEffect(PotionEffect), clearGlobalTags(), clearTag(Player), globalGrantTag(String).
      - void globalSetGameMode(GameMode), globalSetGameModeDelayed(GameMode, long).
      - void globalClearFastBoard(), globalHideNameTag(), globalShowNameTag(), globalChangeTeamNameTag().
      - void globalPlaySound(String), globalStopAllSounds(), globalStopMusic().
      - void globalClearInventory(), globalClearInventoryAllSlots(), globalClearPotionEffects().
      - String getColoredPlayerName(Player).
  - Notes: Methods iterate over all online players; avoid calling them every tick.

  ## MCEMessenger (tools/MCEMessenger.java:21)

  - Responsibility: Adventure/MiniMessage helper for global/player/team info messages, titles, countdowns, intro
    broadcasts, action bars.
  - Typical usage: MCEGame.intro uses sendIntroText; elimination handler uses sendGlobalInfo; countdowns triggered
    before matches start.
  - Public API:
      - void sendMatchTitleToPlayer, sendGlobalText, sendGlobalInfo, sendIntroText, sendInfoToPlayer, sendInfoToTeam.
      - Title helpers: sendGlobalTitle, sendTitleToPlayer.
      - Countdown/action bar: sendGlobalCountdown, sendGlobalActionBarMessage, sendActionBarMessageToPlayer.
  - Notes: Intro text scheduling sends lines every 10 seconds; ensure no overlapping intros per game to avoid chat spam.

  ## MCEGlowingEffectManager (tools/MCEGlowingEffectManager.java:20)

  - Responsibility: Applies/clears glowing using ProtocolLib so teams can see enemy highlights without affecting others.
  - Typical usage: Startup/lobby cleanup calls clearPlayerGlowingEffect (MCEMainController.java:538); voting system uses
    it to clear glows (games/votingSystem/VotingSystem.java:57).
  - Public API:
      - static void setPlayerGlowing(Player target, Player viewer, boolean glowing).
      - static void clearPlayerGlowingEffect(Player target), static void clearAllGlowingEffects().
      - static void setTeamGlowingEffect(List<Player> target, List<Player> viewer, boolean glowing),
        clearTeamGlowingEffect(...).
      - static boolean hasGlowingTag(Player player).
  - Notes: Requires ProtocolLib; fallback to vanilla glowing by calling Player#setGlowing.

  ## Constants (miscellaneous/Constants.java:26)

  - Responsibility: Holds global IDs, map names, team palette, references to singleton game instances, saturation
    effect, and the plugin handle.
  - Typical usage: Referenced across the codebase for IDs (commands), map names (MCEMainController), and plugin
    reference inside tools.
  - Public API: Static fields only (e.g., public static final int PARKOUR_TAG_ID, public static Plugin plugin, public
    static String[] mapNames, public static TeamWithDetails[] teams).
  - Notes: Ensure plugin.yml name matches "MCEFramework" or plugin lookup will fail.

  ## IntroTexts (miscellaneous/IntroTexts.java:10)

  - Responsibility: Pre-built MiniMessage intro components and dividers used during intro stages.
  - Typical usage: MCEMessenger.sendIntroText imports divider/blankLine and uses introTextList.
  - Public API: Component divider, Component blankLine, ArrayList<ArrayList<Component>> introTextList.
  - Notes: Add additional intro lists per game as needed.

  ## TeamWithDetails (miscellaneous/TeamWithDetails.java:8)

  - Responsibility: Immutable record storing team name, alias, colors, and MiniMessage wrappers.
  - Typical usage: MCETeamUtils uses it for getTeamColor/getTeamColoredName.
  - Public API: Auto-generated record accessors (teamName(), textColorPre(), etc.).
  - Notes: Ensure scoreboard team names align exactly with teamName.

  ## GameSettingsState (games/settings/GameSettingsState.java:6)

  - Responsibility: Holds volatile in-memory settings: manual-start toggle and football team count.
  - Typical usage: GameSettingsGUI reads/updates it and MCEMainController.returnToLobbyOrLaunchVoting reads
    isManualStartEnabled.
  - Public API:
      - static int getFootballTeams() / setFootballTeams(int teams) (clamps to 2 or 4).
      - static boolean isManualStartEnabled() / setManualStartEnabled(boolean enabled).
  - Notes: No persistence; values reset on plugin restart.

  ## GameSettingsGUI (games/settings/GameSettingsGUI.java:16)

  - Responsibility: Inventory-driven UI to manage global/manual-start settings and football team count.
  - Typical usage: GameSettingsHandler opens it when admins right-click the settings item; inventory clicks are handled
    via handleInventoryClick.
  - Public API:
      - static void openMainGUI(Player player), openGlobalGUI(Player player), openFootballGUI(Player player).
      - static void handleInventoryClick(InventoryClickEvent event) — routes clicks and updates state.
  - Notes: Uses MiniMessage for titles and item names. Fills unused slots with gray panes to prevent stray clicks.

  ## GameSettingsHandler (customHandler/GameSettingsHandler.java:23)

  - Responsibility: Provides the “Game Settings” command-block item and routes interactions into GameSettingsGUI.
  - Typical usage: Instantiated in MCEMainController (line 177). LobbyItemHandler gives admins the settings item
    (customHandler/LobbyItemHandler.java:96).
  - Public API:
      - static ItemStack createSettingsItem().
      - Event listeners onRightClick(PlayerInteractEvent) and onInventoryClick(InventoryClickEvent).
  - Notes: Checks for main hand interactions and uses GameSettingsState to render current values in lore.

  ## ChatFormatHandler (customHandler/ChatFormatHandler.java:24)

  - Responsibility: Overrides chat formatting so messages look like <TeamColoredName> message.
  - Typical usage: Always active once instantiated (MCEMainController.java:183).
  - Public API: Event handler onAsyncChat(AsyncChatEvent event).
  - Notes: Uses Paper’s renderer override (event.renderer(...)); ensure no other listener overrides it later in the
    pipeline.

  ## GlobalPVPHandler (customHandler/GlobalPVPHandler.java:18)

  - Responsibility: Cancels PVP damage globally unless handler is “suspended” (meaning PVP disabled).
  - Typical usage: Games call MCEWorldUtils.enablePVP() or disablePVP() to control combat; command /togglepvp also
    toggles it.
  - Public API: void onPlayerAttack(EntityDamageByEntityEvent event).
  - Notes: Allows exceptions for HyperSpleef snowballs and any entity with metadata spleef_snowball_damage.

  ## FriendlyFireHandler (customHandler/FriendlyFireHandler.java:16)

  - Responsibility: Cancels team-on-team damage when friendly fire is off; toggled via MCETeamUtils.
  - Typical usage: Instantiated at enable; games call MCETeamUtils.enableFriendlyFire() before matches needing ally
    damage.
  - Public API: void onTeamMemberAttack(EntityDamageByEntityEvent event) (handles direct hits and projectiles).
  - Notes: Handler uses isSuspended() inverted: setSuspended(false) means friendly fire disabled.

  ## GlobalBlockInteractionHandler (customHandler/GlobalBlockInteractionHandler.java:38)

  - Responsibility: Protects worlds during lobby/prep by cancelling block edits and sign changes unless specific games
    allow them.
  - Typical usage: Always running; checks MCEMainController.isRunningGame() and isCurrentGame(Class<?>) to decide
    allowances.
  - Public API: Event handlers for SignChange, BlockBreak, PlayerInteract (axe stripping), BlockPlace.
  - Notes: Integrates with SurvivalGameFuncImpl to track player-placed blocks for restoration.

  ## GlobalEliminationHandler (customHandler/GlobalEliminationHandler.java:36)

  - Responsibility: Handles all player deaths: marks dead spectators, plays sounds, creates SurvivalGame death chests,
    records eliminations, and advances timelines when win conditions met.
  - Typical usage: Registered once; relies on MCEMainController.getCurrentRunningGame() for context.
  - Public API:
      - void onPlayerDeath(PlayerDeathEvent event).
      - static void eliminateNow(Player victim).
  - Notes: Contains game-specific logic (SurvivalGame, UnderworldGame, TNTTag). Update evaluateRoundEnd when adding new
    elimination rules.

  ## GlobalWorldBorderHandler (customHandler/GlobalWorldBorderHandler.java:18)

  - Responsibility: Cancels vanilla border damage and applies half-heart damage every 0.5 seconds to players outside
    the border.
  - Typical usage: Constructor schedules a repeating task that adjusts world border behaviour globally.
  - Public API: None beyond constructor side-effects.
  - Notes: Uses Bukkit.getWorlds() each tick; ensure world count isn’t enormous.

  ## PlayerJoinHandler (customHandler/PlayerJoinHandler.java:24)

  - Responsibility: Intercepts joins, suppresses default join message, clears glowing, and teleports players to lobby or
    current game spawn. Delegates to the game’s join handler when relevant.
  - Typical usage: Created on enable; MCEMainController references it via getter.
  - Public API: void onPlayerJoin(PlayerJoinEvent event).
  - Notes: For active games, teleports to game world and informs players of the current game title.

  ## GamePlayerQuitHandler (customHandler/GamePlayerQuitHandler.java:20)

  - Responsibility: Marks quitting participants as dead (spectator + tags) and notifies the active game to handle quit
    logic.
  - Typical usage: Running constantly; relies on MCEGame.isGameParticipant.
  - Public API: void onPlayerQuit(PlayerQuitEvent event).
  - Notes: Uses MCEResumableEventHandler but defaults to always running (setSuspended(false)). Clears glowing manually.

  ## SpecialItemInteractionHandler (customHandler/SpecialItemInteractionHandler.java:20)

  - Responsibility: Aggregates all MCESpecialItems in a game and forwards PlayerInteractEvents to them until one handles
    the event.
  - Typical usage: Games instantiate one handler and register items (games/hyperSpleef/HyperSpleef.java:83).
  - Public API:
      - void registerItem(MCESpecialItem item), void unregisterItem(MCESpecialItem item), void clearAllItems().
      - Event handler void onPlayerInteract(PlayerInteractEvent event).
  - Notes: Does not filter by world; suspend/clear items when a game ends.

  ## LobbyItemHandler (customHandler/LobbyItemHandler.java:20)

  - Responsibility: Gives lobby loadouts (wind launcher, compass, admin settings) when players join or enter the lobby
    world; ensures PVP/friendly fire is off there.
  - Typical usage: MCEMainController.getLobbyItemHandler() is used after games end to reissue items
    (MCEMainController.java:449).
  - Public API:
      - Event handlers onPlayerJoin, onPlayerChangedWorld.
      - void giveLobbyItems(Player player).
  - Notes: Skips granting items if a game is running; voting system issues its own items.

  ## LobbyDoubleJumpHandler (customHandler/LobbyDoubleJumpHandler.java:26)

  - Responsibility: Adds a double jump mechanic by giving flight when on ground and intercepting PlayerToggleFlightEvent
    to boost forward/upward.
  - Typical usage: Active in lobby/voting; uses cooldown map lastJumpAt.
  - Public API: void onPlayerMove(PlayerMoveEvent event), void onToggleFlight(PlayerToggleFlightEvent event).
  - Notes: Applies only in survival mode and only when the player is in the lobby world (or when the voting system is
    active).

  ## LobbyBounceHandler (customHandler/LobbyBounceHandler.java:18)

  - Responsibility: Prevents lobby players from falling below Y=190 by applying vertical velocity and sound.
  - Typical usage: Continuously running; logs when players are bounced.
  - Public API: void onPlayerMove(PlayerMoveEvent event).
  - Notes: Checks if velocity Y is already positive to avoid repeated boosts.

  ## LobbySaturationHandler (customHandler/LobbySaturationHandler.java:21)

  - Responsibility: Keeps lobby players saturated, reapplying the effect every 40 ticks and removing it when they leave.
  - Typical usage: Runs via scheduled task plus join/world change events.
  - Public API: void onPlayerJoin(PlayerJoinEvent event), void onPlayerChangedWorld(PlayerChangedWorldEvent event).
  - Notes: Task runs indefinitely; ensure plugin shutdown cancels it if necessary.

  ## LobbyTeleportCompassHandler (customHandler/LobbyTeleportCompassHandler.java:24)

  - Responsibility: Manages compass items for traveling between lobby and duel worlds; during voting, supplies voting
    cards when players return.
  - Typical usage: Compass items created in LobbyItemHandler.giveLobbyItems; handler listens for right-clicks and world
    changes.
  - Public API:
      - static ItemStack createBackToLobbyCompass(), static ItemStack createToDuelCompass().
      - Event handlers onRightClick(PlayerInteractEvent), onWorldChange(PlayerChangedWorldEvent),
        onPlayerRespawn(PlayerRespawnEvent).
  - Notes: Maintains per-player cooldown to prevent rapid toggling; integrates with VotingSystemFuncImpl to clear votes
    when leaving lobby.

  ## WindLauncherHandler (customHandler/WindLauncherHandler.java:30)

  - Responsibility: Implements the lobby “wind bullet” (blaze rod) gadget: shoots wind charges, applies knockback/glow,
    enforces cooldown.
  - Typical usage: Always active in lobby when no game is running (or during voting). Works with LobbyItemHandler which
    gives players the blaze rod.
  - Public API:
      - void onRightClick(PlayerInteractEvent event) — launches projectile if not on cooldown.
      - void onProjectileHit(ProjectileHitEvent event) — applies effects when projectile hits a player.
  - Notes: Uses metadata mce_wind to identify own projectiles; fallback to snowball if WindCharge is unavailable.

  ## WelcomeMessageHandler (customHandler/WelcomeMessageHandler.java:29)

  - Responsibility: Displays animated welcome action bars and a bossbar when no game is running.
  - Typical usage: MCEMainController.startWelcomeMessage() triggers it after games or on startup; stopWelcomeMessage()
    is called before launching a game.
  - Public API:
      - static void startWelcomeMessage(), static void stopWelcomeMessage(), static boolean isWelcomeMessageActive().
      - Event handlers onPlayerJoin, onPlayerChangedWorld to start per-player animations.
  - Notes: Manages per-player BukkitTasks; ensure they’re cancelled when stopping to avoid leaks.

  ## WorldDaylightFreezeHandler (customHandler/WorldDaylightFreezeHandler.java:21)

  - Responsibility: Sets DO_DAYLIGHT_CYCLE=false and locks time to noon whenever players enter a world.
  - Typical usage: Runs globally; ensures consistent lighting in lobby/games.
  - Public API: void onPlayerJoin(PlayerJoinEvent event), void onWorldChange(PlayerChangedWorldEvent event).
  - Notes: Uses a Set<String> to avoid reapplying repeatedly, though it still re-applies on every join for safety.

  ## LobbyTeleportCompassHandler (already documented above; retained for completeness)

  - See earlier section for responsibility and API.

  ———
