# 项目规则

## 用户要求
- 系统为 Windows Server 2022，终端命令执行使用。
- 不要使用 emoji。
- 默认简体中文。
- 每次用户提要求必须记录到 `.trae/rules/` 下的项目规则文件中。
- 执行任务前考虑是否需要先指定方案，如果需要指定方案，请写出方案让用户确认执行或者修改，这是强制要求。
- 每次调用工具都要简单说明理由。
- Maven 路径：`E:\apache-maven-3.9.12-bin\apache-maven-3.9.12\bin`，执行命令前要设置环境变量。
- JAVA 21 和 25 都在 E 盘。
- **C 盘空间不足，强制要求所有构建相关的依赖和缓存（Gradle/Maven）必须重定向到 E 盘。**
- 新的知识点也需要在项目规则目录下写下。
- **新增：支持安卓 (ARM64) 加载浏览器计划，采用原生 WebView + 反射桥接方案。**

## 知识点
### MCEF (Minecraft Chromium Embedded Framework) 加载机制
- **JCEF 基础**: 该项目基于 JCEF (Java Chromium Embedded Framework) 实现，将 Chromium 浏览器嵌入 Java 应用中。
- **离屏渲染 (OSR)**: 使用 `CefBrowserOsr` 进行离屏渲染（仅限桌面平台 JCEF）。安卓平台采用原生 WebView。
- **OpenGL 纹理映射**:
    - [MCEFRenderer.java](file:///e:/project/mcef/common/src/main/java/com/cinemamod/mcef/MCEFRenderer.java) 创建一个 OpenGL 纹理。
    - 当 Chromium 触发 `onPaint` 回调时，将缓冲区内容通过 `glTexImage2D` 或 `glTexSubImage2D` 更新到该纹理中。
- **动态下载 (已移除)**: 原有的 `MCEFDownloader.java` 及相关下载逻辑已被移除。项目现在仅支持安卓平台，不再支持桌面平台的自动下载和初始化。
- **输入转发**: [MCEFBrowser.java](file:///e:/project/mcef/common/src/main/java/com/cinemamod/mcef/MCEFBrowser.java) 监听 Minecraft 的鼠标和键盘事件，并将其转发给浏览器实例。
- **显式初始化**: 不再使用 Mixin 拦截启动流程，而是通过 [FabricMCEFClientMod.java](file:///e:/project/mcef/fabric/src/main/java/com/cinemamod/mcef/FabricMCEFClientMod.java) 和 [NeoForgeMCEFMod.java](file:///e:/project/mcef/neoforge/src/main/java/com/cinemamod/mcef/NeoForgeMCEFMod.java) 的入口显式调用 `MCEF.initialize()`。

### 安卓 (ARM64) 环境适配进度
- **彻底移除 JCEF**: 删除了所有 JCEF 相关的类（`JCEFBrowser`, `ModScheme`, `MCEFDownloader` 等）和 `build.gradle` 中的相关依赖/任务。
- **纯净 Android 适配**: `MCEF.initialize()` 现在仅在 Android 平台返回成功，其余平台将打印警告并禁用浏览器功能。
- **接口化解耦**: 通过 `IMCEFBrowser` 接口彻底解耦了对 `java-cef` 的编译依赖。
- **CI 优化**: 修复了 CI 中的代理冲突，并增加了构建产物上传和失败报告分析功能。
- **编译修复**: 移除了 `MCEF.java` 中对 `CefCursorType` 的残余引用，确保脱离 JCEF 也能编译。
- **初始化修复**:
    - `MCEFSettings` 的配置文件路径改为延迟加载，避免在模组初始化阶段 `Minecraft.getInstance()` 返回 `null` 导致崩溃。
    - `MCEFAddon` 的方块和物品注册采用了标准的 `Registry.register` 链式调用，并显式调用了 `.setId()` 方法设置 `ResourceKey`。在 Minecraft 1.21.4 (Mojang Mappings) 环境下，方块和物品的 `Properties` 必须通过 `.setId(ResourceKey)` 设置 ID，否则在实例化时会触发 "Block id not set" 导致的 `NullPointerException`。
    - 注册了自定义的创造模式物品栏分类（`CreativeModeTab`），并将浏览器屏幕物品加入其中。
    - **初始化安全性增强**: 移除了 `MCEF.createBrowser` 中的强制断言（`assertInitialized`），改为在未初始化或非支持平台（如 Windows）时返回 `null`。同时在 `BrowserBlockEntity` 中增加了对 `MCEF.isInitialized()` 的检查，确保在不支持浏览器功能的平台上不会触发 `RuntimeException` 导致游戏崩溃。
- **注意**: 在 Fabric 1.21.4 的 Mojang 映射中，该方法名为 `setId` 而非 `id`。此外，创造模式物品栏需要通过 `FabricItemGroup` 显式注册并绑定物品。
- **已知问题**:
    - **OSHI 权限错误**: 在 Android 15+ 环境下，Minecraft 使用的 OSHI 库在读取 CPU 频率信息时会触发 `AccessDeniedException` (SELinux 限制)。这属于系统级权限限制，不影响模组核心功能，可忽略。
- **安卓 (ARM64) 渲染与交互**:
    - **渲染逻辑**: `AndroidMCEFBrowser` 通过反射调用 `android.webkit.WebView` 的 `draw` 方法，将其绘制到 `Bitmap` 上，转换并同步到 OpenGL 纹理。
    - **GLES 适配**: Android 平台使用 `GL_RGBA` 和 `GL_UNSIGNED_BYTE` 格式更新纹理，而桌面平台使用 `GL_BGRA` 和 `GL_UNSIGNED_INT_8_8_8_8_REV`。
    - **交互转发**: 鼠标和键盘事件通过反射构造 `MotionEvent` 和 `KeyEvent` 并分发给 `WebView`。
    - **性能优化**: 渲染循环限制在约 30fps，以平衡性能和功耗。
- **多方块拼接系统 (4x5+)**:
    - **扫描逻辑**: 在 `onPlace` 和 `neighborChanged` 时自动扫描相同 `FACING` 的方块，识别矩形区域并确定左下角为 `masterPos`。
    - **分辨率策略**: 每个方块对应 `512x384` 像素，整个大屏幕的分辨率为 `(width * 512) x (height * 384)`。
    - **UV 映射**: `BrowserBlockEntityRenderer` 根据方块在网格中的位置 `(gridX, gridY)` 计算 UV 偏移，实现无缝显示。

### 新增模组开发计划 (MCEF-Addon)
- **目标版本**: Fabric 1.21.4
- **定位**: 基于 MCEF 核心库的功能扩展模组。
- **核心功能**:
    - **屏幕方块**: 实现可渲染浏览器的方块，支持多方块拼接（最小 4x5）。
    - **交互系统**: 实现射线检测驱动的鼠标左键/右键点击模拟。
    - **输入系统**: 通过 GUI 输入框向浏览器发送键盘字符。
- **工程结构**: 位于 MCEF 项目下的子文件夹 `e:\project\mcef\mcef-addon`，通过 Maven/项目引用依赖 MCEF。

### MCEF-Addon 优化与修复
- **材质更新**: 使用了 WebDisplays 的 `screen0.png` 作为 `browser_screen.png` 的基础材质，解决了原有材质缺失或不美观的问题。
- **分辨率优化**: 将屏幕方块的默认浏览器分辨率从 `1024x768` 降低至 `512x384`。
    - **原因**: 降低安卓 (ARM64) 平台上的显存占用，修复因分辨率过高导致的纹理创建失败和游戏崩溃问题。
    - **同步修改**: 同时更新了 [BrowserScreenBlock.java](file:///e:/project/mcef/mcef-addon/src/main/java/com/cinemamod/mcef/addon/BrowserScreenBlock.java) 中的鼠标点击坐标映射逻辑，确保点击位置依然准确。
- **渲染修复**:
    - **手持模型修复**: 将 [browser_screen.json](file:///e:/project/mcef/mcef-addon/src/main/resources/assets/mcef-addon/models/item/browser_screen.json) 物品模型的父类修改为方块模型 `mcef-addon:block/browser_screen`，修复了手持时材质不显示（紫黑格子或透明）的问题。
- **交互逻辑增强**:
    - **Shift+右键优先级**: 在 [BrowserScreenBlock.java](file:///e:/project/mcef/mcef-addon/src/main/java/com/cinemamod/mcef/addon/BrowserScreenBlock.java) 中，将 Shift+右键打开 URL 输入 GUI 的逻辑移到了浏览器检查之前。这确保了在不支持浏览器的平台（如 Windows）上，玩家依然可以通过 Shift+右键打开 GUI 进行配置。
    - **日志调试**: 增加了在普通右键且浏览器未初始化时的日志输出，方便定位跨平台适配问题。
    - **服务端同步**: 确保 `useWithoutItem` 在服务端返回 `InteractionResult.CONSUME`，以符合 Minecraft 1.21.4 的交互规范。
- **多方块拼接系统 (4x5+)**:
    - **自动检测**: 在 [BrowserScreenBlock.java](file:///e:/project/mcef/mcef-addon/src/main/java/com/cinemamod/mcef/addon/BrowserScreenBlock.java) 中通过 `updateMultiblock` 方法自动扫描并识别矩形区域。
    - **主从委派**: [BrowserBlockEntity.java](file:///e:/project/mcef/mcef-addon/src/main/java/com/cinemamod/mcef/addon/BrowserBlockEntity.java) 采用主从架构，非左下角的方块将所有操作委派给 `masterPos` 处的方块，确保一个大屏幕只运行一个浏览器实例。
    - **主机控制**: [BrowserComputerBlock.java](file:///e:/project/mcef/mcef-addon/src/main/java/com/cinemamod/mcef/addon/BrowserComputerBlock.java) 可以作为控制终端，连接到附近的浏览器屏幕。屏幕的 URL 优先受连接的主机控制，实现集中管理。
    - **点击器交互**: [BrowserClickerItem.java](file:///e:/project/mcef/mcef-addon/src/main/java/com/cinemamod/mcef/addon/BrowserClickerItem.java) 支持左键点击、长按拖拽（发送 `MOUSE_MOVE`）和右键点击（Shift+右键），并伴有音效反馈。
    - **UV 映射渲染**: [BrowserBlockEntityRenderer.java](file:///e:/project/mcef/mcef-addon/src/main/java/com/cinemamod/mcef/addon/BrowserBlockEntityRenderer.java) 根据方块在网格中的 `(gridX, gridY)` 计算 UV 坐标，实现无缝拼接。
    - **版本适配 (1.21.4)**: `neighborChanged` 签名已更新为使用 `Orientation` 参数，并导入了 `net.minecraft.world.level.redstone.Orientation`。

### 构建与环境配置
- **E 盘构建命令**:
    - **构建主项目 (MCEF Core)**: 在项目根目录执行：
      ```powershell
      $env:JAVA_HOME = "E:\jdk21"; $env:GRADLE_USER_HOME = "E:\.gradle"; $env:PATH = "E:\jdk21\bin;" + $env:PATH; ./gradlew build
      ```
    - **构建扩展模组 (MCEF-Addon)**: 在 `e:\project\mcef\mcef-addon` 目录下执行：
      ```powershell
      $env:JAVA_HOME = "E:\jdk21"; $env:GRADLE_USER_HOME = "E:\.gradle"; $env:PATH = "E:\jdk21\bin;" + $env:PATH; ./gradlew build
      ```
    - **注意**: 由于 `mcef-addon` 是通过 `includeBuild` 包含主项目的独立构建，在 `mcef-addon` 目录下执行时不需要带 `:mcef-addon:` 前缀。
- **编译修复**:
    - `IMCEFBrowser` 增加了默认方法 `sendMouseMove(int x, int y)` 以支持旧版调用。
    - `JCEFBrowser` 的 `isTransparent()` 修复为调用 `renderer.isTransparent()` 以避免递归或父类缺失方法错误。
    - 在 `IMCEFBrowser` 中定义了 `MOUSE_PRESSED`, `MOUSE_RELEASED`, `MOUSE_WHEEL` 常量，以解耦对 `java-cef` 子模块源码的非标准修改，确保 CI 构建时子模块可以正常拉取官方仓库。


- **GitHub Actions CI**:
    - 已添加 `.github/workflows/build.yml` 以实现自动构建验证。
    - 配置为使用 JDK 21，在每次 push 和 pull request 时触发。
    - 支持递归子模块拉取以确保 `java-cef` 源码完整（虽然运行时已移除下载，但编译仍需其中的接口定义）。
    - **修复代理冲突**: 注释了 `gradle.properties` 中的本地代理设置（127.0.0.1:7890），这些设置会导致 CI 环境因找不到代理而构建失败。建议本地开发环境的代理配置放在用户目录的 `.gradle/gradle.properties` 中。
    - **自动上传产物**: 增加了 `upload-artifact` 步骤，构建成功后会自动上传各平台的 `.jar` 产物（过滤了 dev/sources/javadoc 包）。
    - **CI 调试优化**: 在 `build.yml` 中添加了 `--scan` 和 `--warning-mode all` 标志，并增加了构建失败时自动上传 Gradle 报告（`**/build/reports/`）的步骤，以便在 GitHub Actions 界面直接分析失败原因。




