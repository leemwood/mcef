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
- **后续任务**:
    - 在 `AndroidMCEFBrowser` 中通过 JNI/反射调用 `android.webkit.WebView`。
    - 适配 `MCEFRenderer` 以支持 `SurfaceTexture` 纹理更新。
    - 实现 Minecraft 到 WebView 的事件转换逻辑。

### 构建与环境配置
- **E 盘构建命令**:
    - 使用以下命令确保所有依赖和缓存都在 E 盘，且使用正确的 JDK：
      ```powershell
      $env:JAVA_HOME = "E:\jdk21"; $env:GRADLE_USER_HOME = "E:\.gradle"; $env:PATH = "E:\jdk21\bin;" + $env:PATH; ./gradlew build
      ```
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




