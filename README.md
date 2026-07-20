# 三国 M 助手

面向 BlueStacks Air 中《三国群英传 M 亚服》的本地 Android 自动化助手。应用通过无障碍服务、固定坐标、截图分析和中文 OCR 操作游戏，提供练级、野王、副本、军务、定时奖励、自动出售和故障诊断。

> 本项目只针对固定的本机模拟器环境开发，不是通用游戏自动化框架。运行前请确认游戏分辨率为横屏 `1280 × 720`、DPI 为 `240`，并自行承担使用自动化工具的账号风险。

完整的功能状态、默认定时和已知限制见 [FEATURES.md](FEATURES.md)。

## 运行环境

- macOS + BlueStacks Air
- 游戏包名：`hk.phx.khm.cs`
- 助手包名：`com.local.sgmhelper`
- Java 17
- Android SDK 36
- Android 7.0（API 24）或更高版本
- BlueStacks 已开启 ADB，并允许助手使用无障碍服务

项目目前维护三台模拟器：

| 名称 | BlueStacks 实例 | ADB 地址 |
| --- | --- | --- |
| 地球瘦子 | `Tiramisu64` | `127.0.0.1:5555`；部分环境也显示为 `emulator-5554` |
| 栗威 | `Tiramisu64_14` | `127.0.0.1:5695` |
| 地球 | `Tiramisu64_15` | `127.0.0.1:5705` |

## 构建与验证

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_SDK_ROOT=/opt/homebrew/share/android-commandlinetools
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew testDebugUnitTest lintDebug assembleDebug
```

生成的 APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

只修改文档时不需要重新构建 APK。发布应用更新时，先在 `app/build.gradle` 同时递增 `versionCode` 和 `versionName`，再构建并安装到三台模拟器。

## 安装

可以从 BlueStacks 的 **Install APK** 手动安装，也可以使用自带的 ADB：

```sh
ADB="/Applications/BlueStacks.app/Contents/MacOS/hd-adb"
APK="$PWD/app/build/outputs/apk/debug/app-debug.apk"

"$ADB" devices
"$ADB" -s 127.0.0.1:5555 install -r -t "$APK"
"$ADB" -s 127.0.0.1:5695 install -r -t "$APK"
"$ADB" -s 127.0.0.1:5705 install -r -t "$APK"
```

如果 `127.0.0.1:5555` 在传输 APK 时断开，改用设备列表中的 `emulator-5554`：

```sh
"$ADB" -s emulator-5554 install -r -t "$APK"
```

安装后可核对版本：

```sh
"$ADB" -s 127.0.0.1:5695 shell dumpsys package com.local.sgmhelper \
  | grep -E 'versionCode|versionName'
```

## 首次设置

1. 打开“三国 M 助手”。
2. 如果无障碍开关不可用，进入应用详情，点击右上角 `⋮`，选择“允许受限设置”。
3. 返回助手，打开无障碍设置并启用“三国 M 助手悬浮服务”。
4. 返回助手；游戏打开后，左下角会出现可拖动的“辅”悬浮按钮。
5. 在“辅 → 设置 → 辅助 → 登录账号”中保存游戏账号和密码。凭据只保存在当前模拟器的应用私有目录。
6. 在“练级 / BOSS / 副本 / 定时任务”中完成需要的选项和时间设置。

## 日常使用

点击“辅”可打开主菜单：

- **练级**：启动当前练级配置，并在启用自动军务时检查到期军务。
- **BOSS**：启动野王自动化；世界王尚未实现。
- **副本**：按已勾选等级从低到高扫荡。
- **复活士兵**：回城寻找军营并执行“复活全部”。
- **终止**：停止当前动作链，并把默认主线恢复为练级。
- **设置**：配置练级、BOSS、副本、定时任务、自动出售和登录账号。
- **退出**：退出游戏流程。

任务执行期间，屏幕顶部会显示当前进度。练级、BOSS 和副本属于主线任务；军务、福利、奖励、自动出售等临时任务完成后会恢复原主线。

## 重启三台模拟器

重启全部实例，并等待 Android 启动、检查无障碍服务、拉起助手和游戏：

```sh
./restart_all_simulators.sh
```

只重启一台：

```sh
./restart_bluestacks_instance.sh Tiramisu64_14 127.0.0.1:5695
```

脚本不会替代 APK 安装。应用代码更新后，应先构建并安装新版 APK。

## 日志与故障截图

宿主机日志目录：

```text
/Users/Shared/Library/Application Support/BlueStacks/Engine/UserData/SharedFolder/SGMHelperLogs
```

模拟器内对应目录：

```text
/mnt/macos/BstSharedFolder/SGMHelperLogs
```

每台模拟器有独立日志，例如：

```text
sgmhelper-地球瘦子-077e5a706d186981.log
sgmhelper-栗威-d30b62f2263b6d49.log
sgmhelper-地球-60377771f3d25b63.log
```

- 主日志按时间正序追加，最大约 `2 MB`；超出后保留一个 `.log.1` 备份。
- `*-latest.log` 保留最近 `300` 行并按最新在前排列，通常每 `30s` 更新；发生 `ERROR` 时立即刷新。
- 自动化错误和关键 OCR 连续失败会在同一目录保存带时间戳的 PNG 截图。
- 如果共享目录不可写，应用会回退到自身私有目录；这时可通过 ADB 或 Android Studio 导出。

## 项目结构

```text
app/src/main/java/com/local/sgmhelper/
├── HelperAccessibilityService.java  # 悬浮 UI、任务编排、截图与 OCR 平台能力
├── AutomationHost.java              # 各自动化模块使用的平台接口
├── TrainingAutomation.java          # 练级
├── WildernessNavigator.java         # 荒野营地、传送官、选区与选怪
├── BossAutomation.java              # 野王
├── DungeonSweepAutomation.java      # 副本扫荡
├── TaskAutomation.java              # 自动军务
├── WelfareAutomation.java           # 福利领取
├── RewardAutomation.java            # 膜拜与军团奖励
├── HeavenfallAutomation.java        # 天降 BOSS
├── SoldierRevivalAutomation.java    # 复活士兵
├── AutoSellAutomation.java          # 背包检测与自动出售
├── LoginAutomation.java             # 登录和启动页处理
├── ScreenGuard.java                 # 通用遮挡窗口处理
├── AntiCheatVerification.java       # 换线后的头像方向验证
└── DiagnosticLog.java               # 分模拟器日志与故障截图
```

## 开发约束

- 固定坐标以 `1280 × 720` 游戏画面为基准，不能直接适配其他分辨率或 UI 缩放。
- 新功能优先放入独立自动化类；只有 Android 平台操作放进 `HelperAccessibilityService`，并通过 `AutomationHost` 暴露。
- OCR 可能把一个词拆成相邻片段。任务关键字匹配应优先复用已有的相邻片段合并逻辑，并为连续失败保留 OCR 行和截图。
- 自动化失败应进行有上限的恢复并返回原主线，不应无限登录、重启或退出游戏。
- 每次 APK 更新必须递增应用版本，并默认验证三台模拟器。

## 已知限制

- 世界王未实现。
- “勇讨军团天将”目前只负责承接，不负责执行。
- “野境”暂未形成独立路线，目前与标记点使用相同的标记启动流程。
- 部分流程仍依赖固定坐标、游戏中文文本和当前 UI 布局；游戏更新后需要重新验证。
- 本项目不包含远程调度、反检测或账号安全保证。
