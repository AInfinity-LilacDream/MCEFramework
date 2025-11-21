# 核心工具层文档

## 仓库指南

### 项目结构与模块划分
- 源代码位于 `src/main/java/mcevent/MCEFramework`，`commands` 保存 Paper 执行器，`games/*` 以小游戏划分，`generalGameObject` 提供共享抽象，`tools` 则放置通用工具。
- 资源放在 `src/main/resources`，其中包含 `plugin.yml` 以及默认配置目录 `MCEConfig/`。
- Gradle 构建产物输出到 `build/libs/`。
- 开发用 Paper 服务器的运行时状态保存在 `run/`。

### 构建、测试与开发命令
- 始终使用 Gradle Wrapper（Unix 下 `./gradlew`，Windows 下 `.\gradlew.bat`）。
- `./gradlew clean build`：针对 Paper 1.21.4 编译并运行单元测试。
- `./gradlew shadowJar`：生成 `build/libs/MCEFramework-1.0-SNAPSHOT-all.jar`，并完成依赖 relocate。
- `./gradlew runServer`：启动本地 Paper 服务器；手动验证时将阴影包放入 `run/plugins/`。

### 代码风格与命名约定
- 目标 Java 21，使用 4 空格缩进、同一行大括号，并在插件钩子上显式添加 `@Override`。
- 包名保持小写（例如 `mcevent.MCEFramework.games.survivalGame`），类名使用 PascalCase，共享工具类常以 `MCE` 开头。
- 常量采用 `UPPER_SNAKE_CASE`，配置键与 YAML 路径一致（如 `spectator.teleportDelay`）。
- 数据类可以使用 Lombok，但 Bukkit 入口类尽量避免。

### 测试指南
- 单测或集成测试放在 `src/test/java`，命名为 `<ClassUnderTest>Test`。
- 更偏好 MockBukkit 或轻量桩，而不是在测试中启动真实服务器。
- 提交前运行 `./gradlew test`，若进行 `./gradlew runServer` 手动验证，请记录输入命令与期望输出。

### 提交与 PR 规范
- 使用简短的 Conventional Commits 前缀（如 `fix:`、`feat:`），并在合并前 squash fixup。
- PR 需说明对玩家的影响、关联 issue，并附上测试证据（日志、截图、复现命令）。
- 若存在配置迁移或数据清理，需提前告知运维以便准备。

### 配置与安全注意事项
- 仅提交 `src/main/resources/MCEConfig` 下的脱敏模板，生产密钥保留在服务器。
- 新增 YAML 选项时，在相应解析器（例如 `VotingSystemConfigParser`）中记录默认值，并在 PR 描述升级步骤。
- Shadow 会重定位 `co.aikar.*` 与 `fr.mrmicky.fastboard`，因此避免对原始包名使用反射。

## 类索引

- MCEResumableEventHandler - 具备暂停/恢复开关的监听器与控制器基类。
- GamePlayerJoinHandler - 定义玩家在对局中途加入时的处理契约。
- DefaultGamePlayerJoinHandler - 默认实现，负责恢复原参赛者或将新玩家置为观众。
- MCEGame - 抽象小游戏模板，封装生命周期、时间轴与辅助方法。
- MCEGameBoard - 负责记分板/首领条呈现与参赛者计数的基类。
- MCEGameQuitHandler - 共享的退出/淘汰助手，带队伍清空检测。
- MCESpecialItem - 内建冷却控制的特殊物品基类。
- MCETimeline - 带暂停/恢复能力的时间轴节点有序集合。
- MCETimelineNode - 单个时间轴阶段，包含时长与逻辑。
- MCETimeLineNodeFunction - 时间轴节点回调用的函数式接口。
- MCEPausableTimer - 每个阶段使用的可暂停计时器并驱动记分板刷新。
- MCEConfigParser - 所有游戏继承的配置加载与开场文本解析器。
- MCEBlockRestoreUtils - 记录方块原始状态并在赛后恢复。
- MCEWorldUtils - 世界与 PVP 辅助工具（偏移计算、全局 PVP 开关）。
- MCETimerUtils - 延迟/逐帧任务工具与首领条倒计时动画。
- MCETimerFunction - MCETimerUtils 使用的回调接口。
- MCETeleporter - 批量传送并清理对战世界背包的工具。
- MCETeamUtils - 计分板队伍查询、格式化与友伤开关。
- MCEPlayerUtils - 玩家批量操作（标签、模式、背包、音效）。
- MCEMessenger - 统一的 MiniMessage/标题/倒计时消息中心。
- MCEGlowingEffectManager - 基于 ProtocolLib 的发光效果管理器。
- Constants - 存放全局 ID、地图名、队伍调色板与插件句柄。
- IntroTexts - 预置的 MiniMessage 开场文本片段与分隔线。
- TeamWithDetails - 描述队伍名称与颜色的记录类型。
- GameSettingsState - 仅存储于内存的管理员设置状态。
- GameSettingsGUI - 以 GameSettingsState 为后端的物品栏 GUI。
- GameSettingsHandler - 向管理员发放设置物品并路由 GUI 事件。
- ChatFormatHandler - 按队伍颜色渲染 `<name>` 的聊天样式。
- GlobalPVPHandler - 游戏/指令共用的总 PVP 开关。
- FriendlyFireHandler - 未启用前阻止队友互伤。
- GlobalBlockInteractionHandler - 大厅安全的方块交互规则，可被不同游戏覆写。
- GlobalEliminationHandler - 统一的 PlayerDeath 处理、饥饿游戏掉落与局末检测。
- GlobalWorldBorderHandler - 自定义世界边界外每 tick 扣半颗心。
- PlayerJoinHandler - 根据当前游戏进行传送/清理。
- GamePlayerQuitHandler - 标记退出者为阵亡并通知游戏。
- SpecialItemInteractionHandler - MCESpecialItem 交互的分发器。
- LobbyItemHandler - 下发大厅物品与管理员设置物品。
- LobbyDoubleJumpHandler - 在大厅/投票区启用飞行键双跳。
- LobbyBounceHandler - 将掉落到地板以下的玩家弹回高度。
- LobbySaturationHandler - 在大厅持续补满饱食度。
- LobbyTeleportCompassHandler - 处理大厅与对战世界间的指南针与投票卡。
- WindLauncherHandler - 大厅的风之冲击枪，附带发光与击退。
- WelcomeMessageHandler - 空闲时的欢迎动作条与 BossBar 动画。
- WorldDaylightFreezeHandler - 玩家进入世界时把时间锁定在中午。

## 新建小游戏的基本流程

### 必备构件与常用工具
- `MCEGame`：所有游戏的抽象基类，负责时间轴、生命周期钩子、参赛者标签和延迟任务。
- `MCEGameBoard`：自定义记分板/BossBar 展示逻辑，跟踪剩余玩家或回合。
- `MCEConfigParser`：读取 `src/main/resources/MCEConfig/<Game>.yml` 并产生开场文本或参数。
- `MCEResumableEventHandler`：自定义事件处理器的基类，可在游戏切换时 `start()/suspend()`。
- 工具类：`MCETeleporter`（切换世界）、`MCEPlayerUtils`（清背包/模式/音效）、`MCETeamUtils`（队伍与友伤）、`MCEMessenger`（消息与倒计时）、`MCETimerUtils`（延迟与阶段计时）。
- 资源：在 `MCEConfig/` 下放置默认配置与 intro 文本，并在 PR 中说明新增配置。

### 示例目录结构
```text
src/main/java/mcevent/MCEFramework/games/skyRunner/
├── customHandler/
│   └── SkyRunnerDamageHandler.java      # 可选：继承 MCEResumableEventHandler
├── gameObject/
│   └── SkyRunnerGameBoard.java          # 继承 MCEGameBoard
├── SkyRunner.java                       # 继承 MCEGame 的主体
├── SkyRunnerConfigParser.java           # 继承 MCEConfigParser
└── SkyRunnerFuncImpl.java               # 纯静态工具/业务拆分（按需）

src/main/resources/MCEConfig/SkyRunner.yml
```

### 生命周期搭建步骤
1. **继承 `MCEGame`**：在构造器中调用 `super(...)` 设置标题、ID（需在 `Constants` 中登记）、地图名、配置文件名以及各阶段时长。
2. **覆盖关键钩子**：
   - `onLaunch()`：解析配置、`MCETeleporter.globalSwapWorld` 进入游戏世界，初始化玩家状态、PVP/友伤开关、Intro 文本。
   - `onPreparation()` / `onCyclePreparation()`：发放装备、重置计时或注册事件监听。
   - `onCycleStart()`：真正的对局逻辑，通常启动自定义 handler、设置 BossBar、用 `MCEMessenger` 发布状态。
   - `onEnd()` / `stop()`：收尾、清理计分板、暂停 handler，并在需要时调用 `MCEMainController.returnToLobbyOrLaunchVoting()`。
   - `handlePlayerJoinDuringGame()` / `handlePlayerQuitDuringGame()`：维护存活列表、补发物品。
3. **实现 `initGameBoard()`**：构造自定义 `MCEGameBoard`，设置初始回合/标题。
4. **注册事件处理**：新 handler 继承 `MCEResumableEventHandler`，在 `onCycleStart()` 中 `start()`，在 `stop()` 中 `suspend()`。
5. **配置与资源**：`MCEConfigParser` 负责把 `SkyRunner.yml` 的 `[Intro]`、坐标、开关解析为字段；把默认文件放到 `src/main/resources/MCEConfig/`。

### 示例代码片段

```java
public class SkyRunner extends MCEGame {
    private final SkyRunnerConfigParser config = new SkyRunnerConfigParser();
    private final SkyRunnerDamageHandler damageHandler = new SkyRunnerDamageHandler();

    public SkyRunner() {
        super(
            "Sky Runner",
            Constants.SKY_RUNNER_ID,
            "sky_runner_world",
            false,
            "SkyRunner.yml",
            5, 10, 15, 5, 180, 10, 8
        );
    }

    @Override
    public void onLaunch() {
        setIntroTextList(config.openAndParse(getConfigFileName()));
        MCETeleporter.globalSwapWorld(getWorldName());
        MCEPlayerUtils.globalClearInventory();
        MCEWorldUtils.disablePVP();
        MCEMessenger.sendGlobalInfo("<aqua><bold>Sky Runner 即将开始！");
    }

    @Override
    public void onCycleStart() {
        damageHandler.start();
        MCEPlayerUtils.globalSetGameMode(GameMode.ADVENTURE);
        MCEMessenger.sendGlobalCountdown(30, "<yellow>倒计时：<countdown>s");
    }

    @Override
    public void onEnd() {
        damageHandler.suspend();
        MCETeamUtils.disableFriendlyFire();
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            MCEMainController.returnToLobbyOrLaunchVoting();
        });
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new SkyRunnerGameBoard(getTitle(), getWorldName(), getRound()));
    }
}
```

```java
public class SkyRunnerGameBoard extends MCEGameBoard {
    public SkyRunnerGameBoard(String gameName, String mapName, int totalRounds) {
        super(gameName, mapName, totalRounds);
    }

    @Override
    public void globalDisplay() {
        setStateTitle("<green>空岛赛道进行中");
        showRemainingPlayers();
    }
}
```

```java
public class SkyRunnerConfigParser extends MCEConfigParser {
    private List<Component> intro;

    @Override
    protected void parse() {
        this.intro = readIntro();
    }

    public List<Component> openAndParse(String file) {
        return super.openAndParse(file);
    }
}
```

> 将 `SkyRunner` 添加到 `Constants`、在 `MCEMainController` 的注册流程中实例化，并在 `MCEConfig/SkyRunner.yml` 写入默认配置，即可完成一个最小可运行的小游戏。

## MCEResumableEventHandler (generalGameObject/MCEResumableEventHandler.java:9)

- 职责：为所有监听器/控制器提供 `isSuspended` 标记与 start()/suspend() 方法，以便在无需反注册事件的情况下切换逻辑。
- 常见用途：所有全局处理器（如 `customHandler/ChatFormatHandler.java:24`）都会继承它，并在事件体开头判断 `if (isSuspended()) return;`。
- 公共 API：
    - `boolean isSuspended()` / `void setSuspended(boolean)`（Lombok 访问器）。
    - `void start()`：将 `isSuspended` 设为 false。
    - `void suspend()`：将 `isSuspended` 设为 true。
- 备注：不会自动反注册监听器；调用方必须自己在事件里检查 `isSuspended()`。

## GamePlayerJoinHandler (generalGameObject/GamePlayerJoinHandler.java:9)

- 职责：规定每个游戏在对局中途如何接纳新加入的玩家（传送、参赛资格、默认游戏模式等）。
- 常见用途：`MCEGame.handlePlayerJoinDuringGame` 会转发到当前处理器（参见 `generalGameObject/MCEGame.java:254`）；`customHandler/PlayerJoinHandler.java:49` 在游戏运行中玩家登录时调用它。
- 公共 API：
    - `void handlePlayerJoinDuringGame(Player player)`：在对局进行期间有人加入时执行。
    - `boolean isGameParticipant(Player player)`：决定其他系统是否把该玩家视作参赛者。
    - `default GameMode getDefaultJoinGameMode()`：默认返回观战模式，如有需要可在具体游戏中覆写。
- 备注：调用发生在主线程，若有重逻辑需自行调度到异步或延迟任务。

## DefaultGamePlayerJoinHandler (generalGameObject/DefaultGamePlayerJoinHandler.java:15)

- 职责：全游戏共享的默认处理器：刷新活跃队伍、为旧参赛者恢复 Active 标签，并将观察者/死亡玩家置为观众。
- 常见用途：每个 `MCEGame` 构造器都会设置此处理器（`generalGameObject/MCEGame.java:68`），若需自定义则调用 `setPlayerJoinHandler`。
- 公共 API：
    - `DefaultGamePlayerJoinHandler(MCEGame game)`：绑定到特定游戏实例。
    - `void handlePlayerJoinDuringGame(Player player)`：必要时把玩家移出队伍、刷新标签并调度修正游戏模式。
    - `boolean isGameParticipant(Player player)`：当玩家带有 Participant 标签并位于游戏世界时返回 true。
- 备注：通过 10 tick 延迟任务避免与传送冲突，确保计分板标签保持一致。

## MCEGame (generalGameObject/MCEGame.java:16)

- 职责：每个小游戏的抽象基类，负责元数据、配置、时间轴构建、生命周期钩子、参赛标签以及延迟任务管理。
- 常见用途：如 `games/survivalGame/SurvivalGame.java:32` 等游戏都会继承它，并覆写 `onLaunch()`、`onCycleStart()` 等钩子；`MCEMainController` 通过 `MCEMainController.java:427` 持有当前实例。
- 公共 API 重点：
    - 构造函数会设置标题/世界/阶段时长，并自动安装 `DefaultGamePlayerJoinHandler`。
    - `void init(boolean intro)`：搭建完整时间轴（启动 → 开场 → 准备 → 多个周期 → 结束）。
    - 钩子：`onLaunch`、`intro`、`onPreparation`、`onCyclePreparation`、`onCycleStart`、`onCycleEnd`、`onEnd`、`initGameBoard`。
    - 参赛相关：`markParticipantsByWorld()`、`applyGamemodeByParticipation()`、`handlePlayerJoinDuringGame()`、`isGameParticipant()`。
    - 任务工具：`BukkitRunnable setDelayedTask(double seconds, MCETimerFunction fn)`、`clearDelayedTasks()`。
    - 退出模板：`handlePlayerQuitDuringGame(Player)` 及可覆写的 `checkGameEndCondition()`。
- 备注：时间轴假定同步执行；结束游戏时记得取消仍在运行的任务。

## MCEGameBoard (generalGameObject/MCEGameBoard.java:15)

- 职责：提供标题/状态占位符，并在当前游戏世界中统计存活的玩家与队伍数量。
- 常见用途：每个游戏都继承此类实现自己的计分板（如 `games/tntTag/gameObject/TNTTagGameBoard.java:18`），并覆写 `globalDisplay()`。
- 公共 API：
    - 构造函数：`(gameName, mapName[, totalRounds])`。
    - `void updateRoundTitle(int currentRound)`。
    - `void globalDisplay()`：负责推送计分板/BossBar 更新。
    - 静态计数器：`countRemainingParticipants()`、`countRemainingParticipantTeams()`、`countParticipantsTotal()`、`countParticipantTeamsTotal()`。
- 备注：计数方法依赖 `MCEMainController.getCurrentRunningGame()` 以及计分板标签（Participant、dead）。

## MCEGameQuitHandler (generalGameObject/MCEGameQuitHandler.java:17)

- 职责：统一处理玩家退赛：只有在活跃回合阶段才视为死亡，并提供队伍淘汰顺序的辅助方法。
- 常见用途：各游戏在退出监听中调用 `handlePlayerQuit`（如 `games/spleef/Spleef.java:93`），并用 `checkTeamElimination` 更新淘汰列表。
- 公共 API：
    - `static boolean isInCycleStartPhase(MCEGame game)`：判定退出是否应算作死亡。
    - `static void handlePlayerQuit(MCEGame game, Player player, Runnable onDeathCallback)`：在需要时将玩家标记为死亡，并执行自定义回调。
    - `static boolean checkTeamElimination(String playerName, Team team, List<Team> order)`：检测队伍是否全灭，并发送全局消息。
- 备注：通过延迟任务确保在玩家完全离线后再执行业务逻辑，依赖 `MCEPlayerUtils` 与 `MCEMessenger` 进行反馈。

## MCESpecialItem (generalGameObject/MCESpecialItem.java:26)

- 职责：可右键触发的特殊物品基类，内建 Adventure 展示名、冷却控制与动作钩子。
- 常见用途：HyperSpleef 将 IceArrow、BlizzardStaff 等物品注册到 `SpecialItemInteractionHandler`（`games/hyperSpleef/HyperSpleef.java:83`）。
- 公共 API：
    - 构造函数 `(String itemName, Material material, long cooldownTicks, Plugin plugin)`。
    - `ItemStack createItem()`、`boolean handleRightClick(PlayerInteractEvent, Player)`。
    - 冷却辅助：`isOnCooldown`、`setCooldown`、`getRemainingCooldown`、`clearCooldown`、`clearAllCooldowns`、`setItemCooldown`。
    - 可视化辅助：`playSound`、`spawnParticles`、`sendActionBar`。
    - `protected abstract boolean executeAction(Player player, PlayerInteractEvent event)`。
- 备注：`handleRightClick` 在物品匹配时会取消原事件；在 `executeAction` 中实现真正逻辑，并返回是否应用冷却。

## MCETimeline (generalGameObject/MCETimeline.java:11)

- 职责：保存按序排列的 `MCETimelineNode`，并负责状态切换、暂停/恢复与计时器管理。
- 常见用途：`MCEMainController` 使用全局事件时间轴（`MCEMainController.java:54`），每个 `MCEGame` 在 `init()`（`generalGameObject/MCEGame.java:103`）里也会构建自己的时间轴。
- 公共 API：
    - `void addTimelineNode(MCETimelineNode node)`、`void reset()`、`void start()`、`void suspend()`、`void resume()`、`void nextState()`。
    - 时间查询：`int getCounter()`、`int getCurrentTimelineNodeDuration()`、`int getRemainingTime()`。
- 备注：默认假定节点都配置了有效计时器；`nextState()` 会打印日志便于调试。

## MCETimelineNode (generalGameObject/MCETimelineNode.java:12)

- 职责：封装时间轴中的单个阶段：包括持续时间、是否可暂停、执行逻辑、计时器以及可选的子时间轴切换。
- 常见用途：`MCEGame.init` 通过串联多个节点来构建完整流程（参见 `generalGameObject/MCEGame.java:97` 及之后行）。
- 公共 API：
    - 构造函数 `(int maxDurationSeconds, boolean canSuspend, MCETimeLineNodeFunction onRunning, MCETimeline parent, MCEGameBoard board)`。
    - `void start()`、`void stop()`、`void suspend()`、`void resume()`。
- 备注：`isSwitchNode` 为 true 的节点会通过 `MCEMainController.switchToTimeline` 切换到子时间轴。

## MCETimeLineNodeFunction (generalGameObject/MCETimeLineNodeFunction.java:7)

- 职责：供时间轴节点执行逻辑使用的单方法函数式接口。
- 常见用途：创建 `MCETimelineNode` 时传入 Lambda（如 `MCEGame.init` 第 97、109 行等）。
- 公共 API：`void onRunning()`。
- 备注：节点开始时在主线程同步执行。

## MCEPausableTimer (tools/MCEPausableTimer.java:13)

- 职责：循环执行的 Bukkit 任务，用于每秒递减计时、刷新游戏面板，并在阶段结束时推进时间轴。
- 常见用途：每个 `MCETimelineNode` 内都会实例化一个来管理时长。
- 公共 API：
    - 构造函数 `(int seconds, MCETimeline parentTimeline, MCEGameBoard board)`。
    - `void start()`、`void pause()`、`void resume()`、`void stop()`。
    - `int getRemainingTime()`。
- 备注：每秒都会调用 `gameBoard.globalDisplay()`，要保证面板更新足够轻量。

## MCEConfigParser (tools/MCEConfigParser.java:20)

- 职责：从资源复制默认配置、读取内容、将 `[Intro]` 段落解析为 Adventure 组件，并向子类暴露 `parse()`。
- 常见用途：所有游戏的专用解析器都会继承该类并实现 `parse()`（见 `games/votingSystem/VotingSystemConfigParser.java:11`）。
- 公共 API：
    - `ArrayList<Component> openAndParse(String configFileName)`。
    - 受保护辅助：`void open(String configFileName)`、`ArrayList<Component> readIntro()`、`void parse()`（覆写）。
- 备注：默认文件通过 `Constants.plugin.getResource` 读取，需要时请确保资源路径包含 `MCEConfig/`。

## MCEBlockRestoreUtils (tools/MCEBlockRestoreUtils.java:15)

- 职责：记录被修改方块的原始 `BlockData`，以便赛后还原世界。
- 常见用途：方块放置事件中调用 `recordReplacedState`（`customHandler/GlobalBlockInteractionHandler.java:208`），赛后通过 `restoreAllForWorld` 清理（`games/survivalGame/SurvivalGame.java:248`）。
- 公共 API：
    - `static void recordReplacedState(BlockState replacedState)`。
    - `static int restoreAllForWorld(String worldName)`。
- 备注：同一坐标只记录第一次修改，最好在方块放置事件中、修改发生前调用。

## MCEWorldUtils (tools/MCEWorldUtils.java:9)

- 职责：提供传送偏移计算与通过 `GlobalPVPHandler` 切换全局 PVP 的实用函数。
- 常见用途：`commands/PKTSelectChaser.java:80` 使用 `teleportLocation`；各游戏在准备前调用 `disablePVP()`（如 `games/captureCenter/CaptureCenter.java:65`）。
- 公共 API：
    - `static Location teleportLocation(Location baseLoc, Location offsetLoc, int offset)`。
    - `static void enablePVP()`、`static void disablePVP()`。
- 备注：两种 PVP 方法都委托 `MCEMainController.getGlobalPVPHandler()`，调用前需确保处理器已初始化。

## MCETimerUtils (tools/MCETimerUtils.java:13)

- 职责：提供延迟任务、逐帧任务、有时长任务等调度器，并封装 BossBar 倒计时动画。
- 常见用途：`MCEGame.setDelayedTask` 内部调用 `setDelayedTask`；TNT Tag 阶段调用 `showGlobalDurationOnBossBar`（`games/tntTag/TNTTag.java:235`）。
- 公共 API：
    - `static BukkitRunnable setDelayedTask(double seconds, MCETimerFunction function)`。
    - `static BukkitRunnable setFramedTask(MCETimerFunction function)`。
    - `static void setFramedTaskWithDuration(MCETimerFunction function, double duration)`。
    - `static void showGlobalDurationOnBossBar(BossBar bossBar, double duration, boolean isReversed)`。
- 备注：所有任务都在主线程执行，需保持回调逻辑轻量。

## MCETimerFunction (tools/MCETimerFunction.java:6)

- 职责：`MCETimerUtils` 所用的函数式接口。
- 公共 API：`void run()`。
- 备注：通常通过 Lambda 实现。

## MCETeleporter (tools/MCETeleporter.java:13)

- 职责：将所有在线玩家传送到目标世界的出生点，并在切换前清理对战世界背包。
- 常见用途：大多数游戏在启动时调用 `globalSwapWorld(getWorldName())`（`games/survivalGame/SurvivalGame.java:80`）。
- 公共 API：
    - `static void globalSwapWorld(String worldName)`。
- 备注：假定目标世界已存在，否则会抛出 `NullPointerException`。

## MCETeamUtils (tools/MCETeamUtils.java:19)

- 职责：用于计分板队伍的查询/操作（数量、成员列表、颜色格式化），并通过 `FriendlyFireHandler` 控制友伤。
- 常见用途：加入处理器使用 `getTeam`，淘汰逻辑用 `getTeamColoredName`，全局系统通过 `enableFriendlyFire`/`disableFriendlyFire` 切换友伤。
- 公共 API：
    - 队伍查询：`int getActiveTeamCount()`、`ArrayList<Team> getActiveTeams()`、`ArrayList<Player> getPlayers(Team)`、`Team getTeam(Player)`。
    - 格式化：`String[] getTeamColor(Team)`、`String getTeamColoredName(Team)`、`String getUncoloredTeamName(Team)`。
    - 名牌显示开关。
    - 友伤控制：`enableFriendlyFire()`、`disableFriendlyFire()`、`isFriendlyFireEnabled()`。
- 备注：依赖 `Constants.teams`；友伤方法通过调用处理器的 `start`/`suspend` 来切换行为。

## MCEPlayerUtils (tools/MCEPlayerUtils.java:18)

- 职责：针对所有在线玩家执行批量操作，包括标签、游戏模式、背包、药水效果、FastBoard 重置与音效。
- 常见用途：`MCEGame.onLaunch` 会清理背包（`generalGameObject/MCEGame.java:98`）；淘汰消息使用 `getColoredPlayerName`；大厅重置依赖 `globalClearFastBoard`。
- 公共 API（节选）：
    - `void grantGlobalPotionEffect(PotionEffect)`、`clearGlobalTags()`、`clearTag(Player)`、`globalGrantTag(String)`。
    - `void globalSetGameMode(GameMode)`、`globalSetGameModeDelayed(GameMode, long)`。
    - `void globalClearFastBoard()`、`globalHideNameTag()`、`globalShowNameTag()`、`globalChangeTeamNameTag()`。
    - `void globalPlaySound(String)`、`globalStopAllSounds()`、`globalStopMusic()`。
    - `void globalClearInventory()`、`globalClearInventoryAllSlots()`、`globalClearPotionEffects()`。
    - `String getColoredPlayerName(Player)`。
- 备注：所有方法都会遍历在线玩家，请勿在每 tick 调用。

## MCEMessenger (tools/MCEMessenger.java:21)

- 职责：基于 Adventure/MiniMessage 的消息助手，可发送全局/玩家/队伍信息、标题、倒计时、开场广播与动作条。
- 常见用途：`MCEGame.intro` 调用 `sendIntroText`；淘汰处理器使用 `sendGlobalInfo`；比赛开始前触发倒计时。
- 公共 API：
    - `sendMatchTitleToPlayer`、`sendGlobalText`、`sendGlobalInfo`、`sendIntroText`、`sendInfoToPlayer`、`sendInfoToTeam`。
    - 标题辅助：`sendGlobalTitle`、`sendTitleToPlayer`。
    - 倒计时/动作条：`sendGlobalCountdown`、`sendGlobalActionBarMessage`、`sendActionBarMessageToPlayer`。
- 备注：开场文本默认每 10 秒发送一行；同一时间避免多个游戏重叠发 intro，以免刷屏。

## MCEGlowingEffectManager (tools/MCEGlowingEffectManager.java:20)

- 职责：通过 ProtocolLib 单独为玩家施加/清除发光，使队伍能看到敌方高亮而不影响其他人。
- 常见用途：启动或回到大厅时调用 `clearPlayerGlowingEffect`（`MCEMainController.java:538`）；投票系统也会清除发光（`games/votingSystem/VotingSystem.java:57`）。
- 公共 API：
    - `static void setPlayerGlowing(Player target, Player viewer, boolean glowing)`。
    - `static void clearPlayerGlowingEffect(Player target)`、`static void clearAllGlowingEffects()`。
    - `static void setTeamGlowingEffect(List<Player> target, List<Player> viewer, boolean glowing)`、`clearTeamGlowingEffect(...)`。
    - `static boolean hasGlowingTag(Player player)`。
- 备注：依赖 ProtocolLib；若缺失，可退化为调用 `Player#setGlowing` 的原版方案。

## Constants (miscellaneous/Constants.java:26)

- 职责：存放全局 ID、地图名称、队伍调色板、各游戏单例引用、饱和度效果与插件句柄。
- 常见用途：命令获取 ID、`MCEMainController` 读取地图名、工具类获取插件实例等。
- 公共 API：仅包含静态字段，例如 `public static final int PARKOUR_TAG_ID`、`public static Plugin plugin`、`public static String[] mapNames`、`public static TeamWithDetails[] teams`。
- 备注：`plugin.yml` 的名称必须是 “MCEFramework”，否则无法正确查找插件。

## IntroTexts (miscellaneous/IntroTexts.java:10)

- 职责：预构建的 MiniMessage 开场文本组件与分隔符，供开场阶段使用。
- 常见用途：`MCEMessenger.sendIntroText` 引用 divider/blankLine，并使用 `introTextList`。
- 公共 API：`Component divider`、`Component blankLine`、`ArrayList<ArrayList<Component>> introTextList`。
- 备注：需要时可为每个游戏新增对应的 intro 列表。

## TeamWithDetails (miscellaneous/TeamWithDetails.java:8)

- 职责：不可变记录类型，存储队伍名称、别称、颜色与 MiniMessage 包裹。
- 常见用途：`MCETeamUtils` 通过它实现 `getTeamColor` / `getTeamColoredName`。
    - 公共 API：记录自动生成的访问器（`teamName()`、`textColorPre()` 等）。
- 备注：计分板中的队伍名必须与 `teamName` 完全一致。

## GameSettingsState (games/settings/GameSettingsState.java:6)

- 职责：保存易失的内存状态，包括手动启动开关与足球队伍数量。
- 常见用途：`GameSettingsGUI` 读取/更新它，`MCEMainController.returnToLobbyOrLaunchVoting` 查看 `isManualStartEnabled`。
- 公共 API：
    - `static int getFootballTeams()` / `setFootballTeams(int teams)`（会限制为 2 或 4）。
    - `static boolean isManualStartEnabled()` / `setManualStartEnabled(boolean enabled)`。
- 备注：无持久化，插件重启后会重置。

## GameSettingsGUI (games/settings/GameSettingsGUI.java:16)

- 职责：基于物品栏的 UI，用于调整全局设置/手动启动开关以及足球队伍数量。
- 常见用途：管理员右键设置物品时由 `GameSettingsHandler` 打开；物品栏点击事件交由 `handleInventoryClick` 处理。
- 公共 API：
    - `static void openMainGUI(Player player)`、`openGlobalGUI(Player player)`、`openFootballGUI(Player player)`。
    - `static void handleInventoryClick(InventoryClickEvent event)`：路由点击并更新状态。
- 备注：标题与物品名称使用 MiniMessage；未使用槽位填充灰玻璃以避免误触。

## GameSettingsHandler (customHandler/GameSettingsHandler.java:23)

- 职责：提供“Game Settings”命令方块物品，并将交互路由到 `GameSettingsGUI`。
- 常见用途：在 `MCEMainController`（第 177 行）中实例化；`LobbyItemHandler` 向管理员发放该物品（`customHandler/LobbyItemHandler.java:96`）。
- 公共 API：
    - `static ItemStack createSettingsItem()`。
    - 事件监听：`onRightClick(PlayerInteractEvent)`、`onInventoryClick(InventoryClickEvent)`。
- 备注：仅处理主手交互，并使用 `GameSettingsState` 将当前值写入 lore。

## ChatFormatHandler (customHandler/ChatFormatHandler.java:24)

- 职责：重写聊天格式，使消息显示为 `<队伍染色名称> 内容`。
- 常见用途：实例化后始终启用（`MCEMainController.java:183`）。
- 公共 API：`onAsyncChat(AsyncChatEvent event)`。
- 备注：使用 Paper 的渲染器覆盖（`event.renderer(...)`）；确保后续监听器不要再覆盖渲染器。

## GlobalPVPHandler (customHandler/GlobalPVPHandler.java:18)

- 职责：只要处理器未被“暂停”就会全局取消 PVP 伤害（暂停即表示禁用 PVP）。
- 常见用途：游戏通过 `MCEWorldUtils.enablePVP()` / `disablePVP()` 控制战斗；`/togglepvp` 命令也会切换。
- 公共 API：`void onPlayerAttack(EntityDamageByEntityEvent event)`。
- 备注：对 HyperSpleef 的雪球和带 `spleef_snowball_damage` 元数据的实体放行。

## FriendlyFireHandler (customHandler/FriendlyFireHandler.java:16)

- 职责：在关闭友伤时阻止队友之间的伤害；通过 `MCETeamUtils` 进行切换。
- 常见用途：插件启用时实例化；需要友伤的比赛在开局前调用 `MCETeamUtils.enableFriendlyFire()`。
    - 公共 API：`void onTeamMemberAttack(EntityDamageByEntityEvent event)`（同时处理近战与投射物）。
- 备注：处理器对 `isSuspended()` 取反：`setSuspended(false)` 表示关闭友伤。

## GlobalBlockInteractionHandler (customHandler/GlobalBlockInteractionHandler.java:38)

- 职责：在大厅/准备阶段保护世界，取消方块编辑与告示牌修改，除非特定游戏允许。
- 常见用途：常驻运行；根据 `MCEMainController.isRunningGame()` 与 `isCurrentGame(Class<?>)` 判断例外。
- 公共 API：`SignChange`、`BlockBreak`、`PlayerInteract`（斧子去皮）、`BlockPlace` 等事件。
- 备注：与 `SurvivalGameFuncImpl` 集成，用于记录玩家放置的方块以便恢复。

## GlobalEliminationHandler (customHandler/GlobalEliminationHandler.java:36)

- 职责：统一处理玩家死亡：设置观战状态、播放音效、为 SurvivalGame 创建死亡宝箱、记录淘汰并在满足胜利条件时推进时间轴。
- 常见用途：全局注册一次，通过 `MCEMainController.getCurrentRunningGame()` 获取上下文。
- 公共 API：
    - `void onPlayerDeath(PlayerDeathEvent event)`。
    - `static void eliminateNow(Player victim)`。
- 备注：内部包含针对 SurvivalGame、UnderworldGame、TNTTag 的特殊逻辑；新增淘汰规则时需更新 `evaluateRoundEnd`。

## GlobalWorldBorderHandler (customHandler/GlobalWorldBorderHandler.java:18)

- 职责：阻止原版世界边界伤害，并对跨出边界的玩家每 0.5 秒造成半颗心的伤害。
- 常见用途：构造函数会调度一个全局重复任务来调整边界行为。
- 公共 API：无（效果由构造函数触发）。
- 备注：每 tick 遍历 `Bukkit.getWorlds()`；请确保世界数量不要太大。

## PlayerJoinHandler (customHandler/PlayerJoinHandler.java:24)

- 职责：拦截玩家加入，屏蔽默认加入消息，清除发光，并将玩家传送到大厅或当前游戏出生点；必要时委托给游戏自带的 join handler。
- 常见用途：插件启用时创建，`MCEMainController` 通过 getter 访问。
    - 公共 API：`void onPlayerJoin(PlayerJoinEvent event)`。
- 备注：若游戏正在进行，会传送到该游戏世界并向玩家展示当前游戏标题。

## GamePlayerQuitHandler (customHandler/GamePlayerQuitHandler.java:20)

- 职责：当参赛者退出时将其标记为死亡（观战 + 标签），并通知当前游戏处理退出逻辑。
- 常见用途：常驻运行，依赖 `MCEGame.isGameParticipant`。
    - 公共 API：`void onPlayerQuit(PlayerQuitEvent event)`。
- 备注：继承自 `MCEResumableEventHandler` 但默认始终运行（`setSuspended(false)`），并手动清除发光效果。

## SpecialItemInteractionHandler (customHandler/SpecialItemInteractionHandler.java:20)

- 职责：汇总一个游戏内的所有 `MCESpecialItem`，并转发 `PlayerInteractEvent`，直到有物品成功处理。
- 常见用途：游戏会实例化一个处理器并注册物品（如 `games/hyperSpleef/HyperSpleef.java:83`）。
- 公共 API：
    - `void registerItem(MCESpecialItem item)`、`void unregisterItem(MCESpecialItem item)`、`void clearAllItems()`。
    - 事件：`void onPlayerInteract(PlayerInteractEvent event)`。
- 备注：不按世界筛选，游戏结束时需要暂停/清空物品。

## LobbyItemHandler (customHandler/LobbyItemHandler.java:20)

- 职责：玩家加入或进入大厅世界时发放大厅套装（风之武器、指南针、管理员设置物品），并确保该区域关闭 PVP/友伤。
- 常见用途：游戏结束后通过 `MCEMainController.getLobbyItemHandler()` 重新发放（`MCEMainController.java:449`）。
- 公共 API：
    - 事件：`onPlayerJoin`、`onPlayerChangedWorld`。
    - `void giveLobbyItems(Player player)`。
- 备注：若游戏正在运行则不会发放；投票系统会自行提供物品。

## LobbyDoubleJumpHandler (customHandler/LobbyDoubleJumpHandler.java:26)

- 职责：为大厅/投票区提供双跳机制：在落地时给予飞行，拦截 `PlayerToggleFlightEvent` 以向前/向上推进。
- 常见用途：在大厅/投票阶段启用，并使用 `lastJumpAt` 映射做冷却。
- 公共 API：`void onPlayerMove(PlayerMoveEvent event)`、`void onToggleFlight(PlayerToggleFlightEvent event)`。
- 备注：仅对生存模式且位于大厅世界（或投票系统活跃）时生效。

## LobbyBounceHandler (customHandler/LobbyBounceHandler.java:18)

- 职责：防止大厅玩家跌到 Y=190 以下，通过施加竖直速度与音效将其弹回。
- 常见用途：持续运行，并在弹回时记录日志。
- 公共 API：`void onPlayerMove(PlayerMoveEvent event)`。
- 备注：若玩家纵向速度已为正则不再施加强制弹跳。

## LobbySaturationHandler (customHandler/LobbySaturationHandler.java:21)

- 职责：让大厅玩家持续保持饱和，每 40 tick 重新施加效果，并在离开时移除。
- 常见用途：通过定时任务及加入/世界切换事件共同驱动。
- 公共 API：`void onPlayerJoin(PlayerJoinEvent event)`、`void onPlayerChangedWorld(PlayerChangedWorldEvent event)`。
- 备注：任务无限运行，必要时需在插件关闭时手动取消。

## LobbyTeleportCompassHandler (customHandler/LobbyTeleportCompassHandler.java:24)

- 职责：管理大厅与对战世界之间的指南针物品；在投票阶段，玩家返回大厅时发放投票卡。
- 常见用途：指南针由 `LobbyItemHandler.giveLobbyItems` 创建；处理器监听右键与世界切换事件。
- 公共 API：
    - `static ItemStack createBackToLobbyCompass()`、`static ItemStack createToDuelCompass()`。
    - 事件：`onRightClick(PlayerInteractEvent)`、`onWorldChange(PlayerChangedWorldEvent)`、`onPlayerRespawn(PlayerRespawnEvent)`。
- 备注：维护玩家级冷却以避免快速切换，并与 `VotingSystemFuncImpl` 集成，在离开大厅时清除投票。

## WindLauncherHandler (customHandler/WindLauncherHandler.java:30)

- 职责：实现大厅“风弹”（烈焰棒）小玩具：发射风之充能，附加击退/发光并强制冷却。
- 常见用途：当无游戏运行或处于投票阶段时始终启用，与发放烈焰棒的 `LobbyItemHandler` 配合。
- 公共 API：
    - `void onRightClick(PlayerInteractEvent event)`：在不冷却时发射弹体。
    - `void onProjectileHit(ProjectileHitEvent event)`：弹体命中玩家时施加效果。
- 备注：通过 `mce_wind` 元数据标记自家弹体；若 WindCharge 不可用则回退到雪球。

## WelcomeMessageHandler (customHandler/WelcomeMessageHandler.java:29)

- 职责：在没有游戏运行时显示欢迎动作条与 BossBar 动画。
- 常见用途：`MCEMainController.startWelcomeMessage()` 在开服或赛后启动它，`stopWelcomeMessage()` 在开启新游戏前停止。
- 公共 API：
    - `static void startWelcomeMessage()`、`static void stopWelcomeMessage()`、`static boolean isWelcomeMessageActive()`。
    - 事件：`onPlayerJoin`、`onPlayerChangedWorld`，用于启动玩家级动画。
- 备注：为每位玩家维护 BukkitTask，停止时务必取消以免泄漏。

## WorldDaylightFreezeHandler (customHandler/WorldDaylightFreezeHandler.java:21)

- 职责：当玩家进入世界时，将 `DO_DAYLIGHT_CYCLE` 设为 false 并把时间锁定在正午。
- 常见用途：全局运行，确保大厅与游戏的光照一致。
- 公共 API：`void onPlayerJoin(PlayerJoinEvent event)`、`void onWorldChange(PlayerChangedWorldEvent event)`。
- 备注：使用 `Set<String>` 来避免重复设置，但仍会在每次加入时重置以确保安全。

## LobbyTeleportCompassHandler（已在上文介绍，此处仅作索引）

- 说明：参见前文的 `LobbyTeleportCompassHandler` 小节了解职责与 API。
