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
- **离屏渲染 (OSR)**: 使用 `CefBrowserOsr` 进行离屏渲染。Chromium 不会创建独立的系统窗口，而是将页面内容渲染到内存缓冲区（ByteBuffer）。
- **OpenGL 纹理映射**:
    - [MCEFRenderer.java](file:///e:/project/mcef/common/src/main/java/com/cinemamod/mcef/MCEFRenderer.java) 创建一个 OpenGL 纹理。
    - 当 Chromium 触发 `onPaint` 回调时，将缓冲区内容通过 `glTexImage2D` 或 `glTexSubImage2D` 更新到该纹理中。
- **动态下载**: [MCEFDownloader.java](file:///e:/project/mcef/common/src/main/java/com/cinemamod/mcef/MCEFDownloader.java) 负责在运行时从远程服务器下载对应平台的 JCEF 原生二进制文件（如 `.dll`, `.so`, `.dylib`）。
- **输入转发**: [MCEFBrowser.java](file:///e:/project/mcef/common/src/main/java/com/cinemamod/mcef/MCEFBrowser.java) 监听 Minecraft 的鼠标和键盘事件，并将其转发给 JCEF 实例，实现页面交互。
- **Mixin 注入**: 通过 Mixin（如 [CefInitMixin.java](file:///e:/project/mcef/common/src/main/java/com/cinemamod/mcef/mixins/CefInitMixin.java)）在 Minecraft 启动流程中自动初始化 CEF。

### 安卓 (ARM64) 环境适配进度
- **已完成架构重构**:
    - **接口化**: 引入 `IMCEFBrowser` 接口，解耦 JCEF 依赖。
    - **兼容性包装**: `MCEFBrowser` 类改为委托模式，内部根据平台持有 `JCEFBrowser` (桌面) 或 `AndroidMCEFBrowser` (安卓) 实例。
    - **初始化优化**: `MCEF.initialize()` 在安卓平台自动跳过 JCEF 下载和初始化流程。
    - **反射桥接**: `AndroidBridge` 实现了跨启动器（Pojav/FCL/ZL）的 ActivityThread 反射方案，动态获取 Context。
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
    - `CefMouseEvent` 中手动补充了 `MOUSE_PRESSED`, `MOUSE_RELEASED`, `MOUSE_WHEEL` 等缺失常量。

- **GitHub Actions CI**:
    - 已添加 `.github/workflows/build.yml` 以实现自动构建验证。
    - 配置为使用 JDK 21，在每次 push 和 pull request 时触发。
    - 支持递归子模块拉取以确保 `java-cef` 源码完整。


