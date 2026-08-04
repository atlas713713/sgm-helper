# 武当 `data` 模板分析记录

本文用于持续记录武当 APK `assets/data` 目录的模板知识，方便后续继续分析、选择 OCR
替代目标和复查历史结论。本文不表示所有模板都已验证可用于当前游戏画面。

最后更新：2026-08-04。

## 一、证据标记

- **已确认**：可以由反编译常量、模板图像内容、当前源码或实际日志直接证明。
- **合理推断**：文件内容和调用位置支持该解释，但尚未完成真实画面验证。
- **待验证**：目前只有文件名、图像外观或不完整反编译线索，不能直接接入生产流程。

后续更新本文时应继续保留这三种标记，避免把推断写成事实。

## 二、下次从哪里开始

主要目录：

```text
jadx-output/wudang/resources/assets/data/
```

反编译语义入口：

```text
jadx-output/wudang/sources/com/findsdk/wudang/data/GameConstants.java
jadx-output/wudang/sources/com/findsdk/library/netty/p092f5/C2187w.java
jadx-output/wudang/sources/com/findsdk/library/netty/p092f5/C2183u1.java
jadx-output/wudang-fallback/sources/com/findsdk/wudang/util/AiHelper.java
jadx-output/wudang-fallback/sources/com/findsdk/wudang/util/core/OpenCVUtil.java
```

当前项目模板实现：

```text
app/src/main/java/com/local/sgmhelper/WudangTemplateMatcher.java
app/src/main/java/com/local/sgmhelper/AutomationHost.java
```

实际日志目录：

```text
/Users/Shared/Library/Application Support/BlueStacks/Engine/UserData/SharedFolder/SGMHelperLogs
```

本批次已完成 `t1`–`t21` 的资源接入和固定 ROI 目录化；后续应优先用真实截图逐项补正负样本，
不要把合成测试当成真实画面准确率。

## 三、目录规模与文件格式

### 3.1 已确认的统计

截至 2026-08-03：

| 项目 | 数量 |
|---|---:|
| `.wd` 文件总数 | 329 |
| 实际内容为 WebP | 244 |
| 实际内容为 PNG | 85 |
| 同时存在普通版与 `_a` 版的模板组 | 91 |
| 只有 `_a`、没有普通版的模板 | 0 |
| 没有 `_a` 配对的普通模板 | 147 |
| `chi_traineddata/` 内文件 | 3958 |

`.wd` 只是伪装扩展名。读取时应根据文件内容解码，不要按扩展名假设格式。

目录中另外存在：

```text
chi_sim.traineddata
chi_traineddata/
```

它们属于武当旧 OCR/中文字库数据，不属于 OpenCV 图片模板。分析模板时应将两者分开。

### 3.2 `_a` 版本的已确认选择条件

`C2187w` 为大量模板返回普通版或 `_a` 版。`C2183u1.m1720a()` 的条件是：

```text
gameServerType == 3 && Android SDK >= 30
```

所以 `_a` 不能简单解释为“高清版”或“亚服版”。它对应武当代码中的特定运行分支。
当前项目在无法确定分支时同时尝试普通版和 `_a` 版，这是兼容策略，不代表两张图都适合当前画面。

## 四、模板分类总览

| 分类 | 主要前缀/文件 | 已确认用途 |
|---|---|---|
| 主菜单文字 | `t1.wd`–`t21.wd` | 固定菜单或功能名称 |
| HUD 和导航锚点 | `m.wd`、`n.wd`、`g01/g02/g04`、`mjt.wd` 等 | 地图、对话、上下箭头、自动寻路和菜单图标 |
| 功能按钮 | `bf1`–`bf5`、`d.wd`、`eda.wd` | 领取、前往领取、验证、对话箭头等 |
| 背包与货币 | `i*`、`p*`、`ppi.wd` | 锁、银币、金币、收藏、药品和道具图标 |
| 商店与物品列表 | `s*`、`sl*`、`zzi*` | 商店物品、列表物品和自动购买目标 |
| 技能与角色 | `sk*`、`pmj/phj/pjs/pfs` | 回城、复活、治疗、士兵、武器和职业图标 |
| 队伍 | `tm*`、`tc*` | 队友颜色、添加队友、队长和副本营地状态 |
| 地图与界面状态 | `bmc*`、`bmf.wd`、`ls.wd`、`gg*` | 大地图城市/楼层、线路下拉、网页关闭按钮 |
| 活动专用 | `jjc*`、`jys*`、`lsi*`、`ql*`、`wv*` 等 | 竞技场、交易所、军团商店、活动限制和按钮 |
| 登录与服务器 | `l*`、`a*`、`sat*`、`sn01.wd` | 登录方式、账号中心、选服页面 |
| 战斗判断 | `f.wd`、`g.wd` 及部分小色块/状态模板 | 假 BOSS、状态或颜色特征；不能仅按图像外观解释 |

## 五、`t1`–`t21` 主菜单文字模板

以下文字由模板图像直接确认：

| 模板 | 内容 | 原始尺寸 | 当前项目已复制 |
|---|---|---:|---|
| `t1.wd` | 元宝回收 | 131×33 | 是 |
| `t2.wd` | 士兵和阵形 | 174×34 | 是 |
| `t3.wd` | 商店 | 64×33 | 是 |
| `t4.wd` | 军营 | 63×32 | 是 |
| `t5.wd` | 技能 | 66×33 | 是 |
| `t6.wd` | 客栈 | 66×33 | 是 |
| `t7.wd` | 分流信息 | 83×21 | 是 |
| `t8.wd` | 军团 | 64×31 | 是 |
| `t9.wd` | 好友 | 65×33 | 是 |
| `t10.wd` | 宫殿 | 63×33 | 是 |
| `t11.wd` | 副本 | 63×32 | 是 |
| `t12.wd` | 炼造 | 64×33 | 是 |
| `t13.wd` | 武魂擂台 | 128×32 | 是 |
| `t14.wd` | 自动功能 | 127×32 | 是 |
| `t15.wd` | 奖励找回 | 137×35 | 是 |
| `t16.wd` | 钱庄 | 65×31 | 是 |
| `t17.wd` | 战利品 | 63×20 | 是 |
| `t18.wd` | 战役 | 63×31 | 是 |
| `t19.wd` | 名将挑战 | 131×33 | 是 |
| `t20.wd` | 历史战场 | 129×30 | 是 |
| `t21.wd` | 千里单骑 | 129×29 | 是 |

注意：

- `t11.wd` 只代表主菜单中的“副本”，不能直接代替“副本宫殿（一般）”对话选项 OCR。
- `t14.wd` 是完整的“自动功能”，当前实际日志中的 OCR 经常只返回“自动”。两者是否为同一视觉元素必须用截图确认。
- 普通版和 `_a` 版的尺寸、颜色或边缘可能不同。不能仅凭文件名决定使用哪一张。

## 六、`GameConstants.AssetsData` 已确认语义

以下映射直接来自反编译常量，不是根据文件名猜测。

### 6.1 地图、菜单和导航

| 常量语义 | 文件 |
|---|---|
| 大地图城市标记 1 | `bmc1.wd` |
| 大地图城市标记 2 | `bmc2.wd` |
| 大地图楼层分隔 | `bmf.wd` |
| 自动寻路按钮 | `g04.wd` |
| 引导标签向下 | `g01.wd` |
| 引导标签向上 | `g02.wd` |
| 线路下拉标记 | `ls.wd` |
| 地图 HUD 标签 | `m.wd` |
| 对话 HUD 标签 | `n.wd` |

菜单图标：

| 常量语义 | 文件 |
|---|---|
| 副本菜单图标 | `mfb.wd` |
| 军团任务菜单图标 | `mjt.wd` |
| 交易所菜单图标 | `mjys.wd` |
| 元宝回收菜单图标 | `mbr.wd` |
| 商店菜单图标 | `msd.wd` |
| 状态菜单图标 | `mbs.wd` |
| 信件菜单图标 | `mxj.wd`（常量存在，但当前导出的资源目录缺少该文件） |
| 自动菜单图标 | `mzd.wd` |

`mjt.wd` 是 37×20 的 RGB WebP 图标，不是文字模板。它适合测试固定图标定位，不能单独证明文字 OCR 可以被模板替代。

### 6.2 背包、货币和道具

| 常量语义 | 文件 |
|---|---|
| 银币 | `i2.wd` |
| 金币 | `i3.wd` |
| 收藏/喜爱 | `i5.wd` |
| 背包位置索引 | `ppi.wd` |
| 补给酒 | `p01.wd` |
| 补给酒特殊状态 | `p01g.wd` |
| 火神玉 | `p02.wd` |
| 复活卷 | `p03.wd` |
| 神武卷轴 | `p04.wd` |
| 高级道具类图标 1 | `p05.wd` |
| 高级道具类图标 2 | `p06.wd` |
| 高级道具类图标 3 | `p07.wd` |

`p05`–`p07` 的缩写含义来自常量名，但中文物品全名尚未由实际界面核对。

### 6.3 商店和自动购买

| 常量语义 | 文件 |
|---|---|
| 商店火神玉 | `s301.wd` |
| 商店韧金 | `s302.wd` |
| 列表火神玉 | `sl301.wd` |
| 列表韧金 | `sl302.wd` |
| 自动购买生命类物品 | `zzi1.wd` |
| 自动购买法力类物品 | `zzi2.wd` |
| 自动购买火神玉 | `zzi3.wd` |
| 自动购买士兵复活物品 | `zzi4.wd` |
| 自动购买其他补给 | `zzi5.wd` |

### 6.4 技能与职业

| 常量语义 | 文件 |
|---|---|
| 技能菜单文字 | `t5.wd` |
| 回城技能 | `sk01.wd` |
| 复活技能 | `sk02.wd` |
| 治疗技能 | `sk03.wd` |
| 士兵开启状态 | `sk041.wd` |
| 士兵关闭状态 | `sk042.wd` |
| 武器快捷位 1 | `sk051.wd` |
| 武器快捷位 2 | `sk052.wd` |
| 猛将职业图标 | `pmj.wd` |
| 豪杰职业图标 | `phj.wd` |
| 军师职业图标 | `pjs.wd` |
| 方士职业图标 | `pfs.wd` |

### 6.5 队伍

| 常量语义 | 文件 |
|---|---|
| 队长本人标记 | `tc01.wd` |
| 队长/队友标记 | `tc02.wd` |
| 队友对话标记 | `tc03.wd` |
| 副本营地状态标记 | `tc04.wd` |
| 队友颜色状态 1–5 | `tm1.wd`–`tm5.wd` |
| 添加队友 | `tm6.wd` |

### 6.6 登录、服务器和网页

| 常量语义 | 文件 |
|---|---|
| 快捷登录 | `l1.wd` |
| 账号登录 | `l2.wd` |
| 账号中心 | `a1.wd` |
| 另一账号中心样式 | `a2.wd` |
| 选服页面标题 | `sn01.wd` |
| 台湾账号中心访客/密码/管理/切换状态 | `sata/satb/sate/satf.wd` |
| 中国网页关闭按钮 | `gg1.wd` |
| 其他网页关闭样式 | `gg2.wd`–`gg4.wd` |

5745（`gameServerType=2`）对应的固定检测区域已从原实现确认：

| 模板 | 原始尺寸 | 固定 ROI（1280×720） | 当前状态 |
|---|---:|---|---|
| `ls.wd` 线路下拉标记 | 12×11 | `1249,698,14×13` | 已在 5745 HUD 正样本验证，彩色原尺寸相关系数 `0.9994` |
| `gg1.wd` 中国网页关闭 | 25×34 | `1140,92,40×40` | 已接入固定 ROI，待登录/公告正样本 |
| `gg2.wd` 网页关闭样式 1 | 28×28 | `1048,45,30×30` | 已接入固定 ROI，待对应地区正样本 |
| `gg3.wd` 网页关闭样式 2 | 38×38 | `970,60,40×40` | 已接入固定 ROI，待对应地区正样本 |
| `gg4.wd` 网页关闭样式 3 | 32×32 | `1017,51,34×34` | 已接入固定 ROI，待对应地区正样本 |

`ls` 的实机截图中模板最佳落点为 `(1250,699)`，落在上述 14×13 原始检测框内；没有点击箭头，避免改变分流状态。当前代码已让 BOSS 换线和换线测试先用 `ls.wd` 命中箭头并点击模板中心，失败时才回退原来的 `(1215,705)` 固定点击。`gg` 暂只接入模板目录和 matcher，不把未经正样本验证的关闭动作绑定到登录流程。

### 6.7 其他已确认常量

| 常量语义 | 文件 |
|---|---|
| 元宝回收文字 | `t1.wd` |
| 交易所对话箭头 | `eda.wd` |
| 军团商店状态 | `lsib.wd` |
| 选择服务器页面标题 | `sn01.wd` |
| 战役/活动入口图标 | `wyy.wd` |

## 七、其他已目视确认的模板族

### 7.1 福利领取按钮

| 模板 | 目视内容 |
|---|---|
| `bf1.wd` | 红色小状态块，具体语义待验证 |
| `bf2.wd` | 一键领取 |
| `bf3.wd` | 领取 |
| `bf4.wd` | 前往领取 |
| `bf5.wd` | 领取的另一种裁剪/状态 |

### 7.2 对话与物品状态

- `d.wd`：按钮文字“验证”。
- `d101`–`d108`、`d201`–`d204`、`d301`–`d306`：不同道具或物品图标。
- 带 `t` 后缀的个别 `d*` 模板是同一物品的另一视觉状态，具体含义待调用位置确认。
- `dit.wd`：文字“人数”。

### 7.3 活动和交易界面

已目视看到的内容包括：

- `jjc.wd`：竞技场。
- `jjc2.wd` / `jjc3.wd`：6v6 / 3v3。
- `jys3.wd`：贩卖设定。
- `jyst.wd`：交易所。
- `jyst1.wd`：上架贩卖。
- `ql01.wd` / `ql02.wd`：选择化身。
- `ql03.wd`：右箭头按钮。
- `phbt.wd`：排行榜。
- `rytt.wd`：荣耀厅。
- `gqddj.wd`：强度等级。
- `gqhcs.wd`：剩余强化次数。
- `gzb.wd`：装备。
- `gzyxz.wd`：职业限制。
- `h.wd`：学习点数。

这些图像内容已经可以辨认，但在没有追到对应 `ItemView` 和动作类前，不应直接决定搜索区域和点击坐标。

## 八、武当原始匹配方法

以下是反编译结果支持的已确认行为：

1. 武当通过 `ItemView` 提供精确的截图裁剪区域。
2. `AiHelper.findMenuBoxItemPoint` 先裁剪该区域，再加载指定 `.wd` 模板。
3. `OpenCVUtil` 默认阈值为 `0.85`。
4. OpenCV 方法编号为 `5`，对应 `TM_CCOEFF_NORMED`。
5. 默认 `isGray=false`，即一般情况下不先转灰度。
6. 原始方法按模板原生尺寸匹配，没有看到当前项目这种按截图尺寸统一缩放模板的步骤。
7. 找到模板后，武当把匹配坐标加回 `ItemView` 原点，再加模板宽高的一半得到点击中心。

这说明“使用同一张模板”不等于“复制了武当的匹配行为”。裁剪区域、颜色通道、模板尺寸和客户端分支都可能决定结果。

## 九、当前项目的模板实现

`WudangTemplateMatcher` 当前行为：

1. 以 `1280×720` 为参考分辨率。
2. 截图先转成灰度。
3. 根据截图相对 `1280×720` 的比例缩放模板和 ROI。
4. 使用 `TM_CCOEFF_NORMED`。
5. 固定阈值为 `0.85`。
6. 有 `_a` 版时同时尝试普通版和 `_a` 版，选择最高分。
7. 模板未命中时保留 OCR fallback。
8. 模板缓存在线程内复用，并记录 `score`、`hit`、ROI 和 `elapsedMs`。

当前已接入的模板：

```text
地图、对话、自动寻路、元宝回收、商店、技能、客栈、军团、宫殿、副本、
自动功能、奖励找回、一键领取、领取、前往领取
```

当前项目资源目录中已经存在：

```text
m.wd n.wd g04.wd mjt.wd
t1.wd
t3/t3_a.wd
t5/t5_a.wd
t6/t6_a.wd
t8/t8_a.wd
t10/t10_a.wd
t11/t11_a.wd
t14/t14_a.wd
t15/t15_a.wd
bf2/bf2_a.wd
bf3/bf3_a.wd
bf4/bf4_a.wd
bf5/bf5_a.wd
```

## 十、截至 2026-08-03 的实际日志结果

在当前宿主机可检索日志中，共找到 102 条 `TEMPLATE_MATCH`：

```text
hit=true:   0
hit=false: 102
```

按模板汇总：

| 模板 | 次数 | 命中 | 平均分 | 最低分 | 最高分 | 平均计算时间 | 时间范围 |
|---|---:|---:|---:|---:|---:|---:|---:|
| 元宝回收 | 5 | 0 | 0.412 | 0.228 | 0.535 | 11.2ms | 5–16ms |
| 军团 | 4 | 0 | 0.286 | 0.283 | 0.289 | 15.8ms | 12–22ms |
| 地图 | 13 | 0 | 0.231 | 0.203 | 0.270 | 3.8ms | 1–8ms |
| 客栈 | 17 | 0 | 0.359 | 0.323 | 0.408 | 7.3ms | 2–17ms |
| 对话 | 13 | 0 | 0.285 | 0.243 | 0.317 | 4.6ms | 2–9ms |
| 自动功能 | 50 | 0 | 0.214 | 0.173 | 0.229 | 6.1ms | 1–27ms |

结论边界：

- 模板计算本身明显比 Paddle OCR 快。
- 这些日志同时证明当前实现尚未成功匹配任何真实画面。
- 不能根据低耗时直接宣布模板已经可以替代 OCR。
- 目前日志没有保存每次模板匹配所用的原始截图，因此还不能区分错误 ROI、错误客户端模板、缩放、灰度化或画面状态不对。
- 该统计只代表当时宿主机中存在的日志，不是所有模拟器和所有版本的永久结论。

抽样日志中，“自动功能”模板匹配通常只需 1–22ms；fallback 后的局部 Paddle OCR 大约一秒左右返回。全屏 HUD OCR 常在数秒后才完成。后续应在代码内统一记录端到端耗时，避免仅靠相邻日志时间估算。

## 十一、第一个受控实验：`t8.wd`“军团”

选择 `t8.wd`，而不是先接更多模板，原因是：

1. “军团”是固定菜单文字，视觉状态比地图名、BOSS 名和数字稳定。
2. 同时存在 `t8.wd` 与 `t8_a.wd`。
3. 当前项目已经有 `LEGION` 调用和 OCR fallback，不需要先扩展业务流程。
4. 已经有 4 次真实失败记录，分数集中在 `0.283–0.289`，便于建立基线。
5. 一旦找出失败原因，同一方法可验证其他 `t*.wd` 菜单文字。

### 11.1 实验输入

至少保存：

- 20 张“军团”完整可见的正样本。
- 20 张菜单打开但“军团”不可见的负样本。
- 当前 Debug 模拟器固定使用 `127.0.0.1:5745` / `Tiramisu64_19`。
- 每张图保留原始 `1280×720` 截图，不要只保存裁剪图。

### 11.2 同图对照方案

在同一批图片上分别运行：

1. 当前实现：灰度、按截图缩放、当前 ROI。
2. 武当接近实现：彩色、原生尺寸、精确 ROI、`TM_CCOEFF_NORMED`。
3. 普通版 `t8.wd` 与 `_a` 版 `t8_a.wd` 分开记录结果。
4. 若原生尺寸失败，再做小范围尺度扫描；不要一开始就无限多尺度搜索。

每次记录：

```text
截图文件、模板文件、ROI、颜色模式、尺度、最高分、坐标、耗时、是否应命中、是否实际命中
```

### 11.3 建议成功标准

第一轮建议目标：

- 正样本命中率至少 95%。
- 负样本误报为 0。
- 热缓存后的单次匹配低于 50ms。
- 点击中心落在“军团”真实按钮范围内。
- 模板失败时仍进入已有 OCR fallback，不退出自动化。

阈值应由正负样本分数分布决定。不要为了让当前 `0.28` 分通过而直接把阈值降到 `0.25`；没有负样本时，这样无法判断误报风险。

## 十二、哪些 OCR 可以被模板替代

适合模板优先：

- 固定菜单文字，如“军团”“副本”“客栈”。
- 固定按钮，如“领取”“一键领取”。
- 固定图标，如自动寻路、职业、队长标记。
- 固定页面标题或状态锚点。

不适合只靠单模板：

- 动态地图名和城市名。
- BOSS、NPC 或玩家名字。
- 背包 `已用/总量` 数字。
- 倒计时、等级、数量和其他变化文本。
- 会滚动、截断或由服务器动态下发的列表内容。
- “副本宫殿（一般）”这类目前没有对应完整模板的对话选项。

因此目标应是“模板优先，OCR 兜底，并逐步减少固定 UI OCR”，而不是一次性删除 OCR。

## 十三、已知风险和未解决问题

1. 当前 102 次真实匹配全部失败，尚未有成功样本。
2. 当前实现先灰度化，而武当默认是彩色匹配；这是一项待验证差异，不是已确认根因。
3. 当前实现会按截图比例缩放模板，武当原逻辑更接近原生尺寸匹配；这同样只是待验证差异。
4. 现有 ROI 是否与武当 `ItemView` 完全一致尚未核对。
5. `_a` 条件已经找到，但当前游戏的 `gameServerType` 对应值尚未在真实运行中确认。
6. 模板来自某一版武当 APK；游戏 UI 更新后，原始模板可能已经过期。
7. `m.wd` / `n.wd` 的 HUD 识别目前分数很低，不能把它们当作可靠的登录状态判据。
8. `f.wd` / `g.wd` 与假 BOSS 流程有关，但该流程还结合攻击反馈、血量、怒气等状态，不能简化为一次模板命中。
9. 大量活动模板只有目视语义，尚未追到精确 `ItemView`、阈值和动作调用链。
10. `GameConstants.AssetsData` 共引用 78 个唯一 `.wd` 文件；其中 `mxj.wd` 在常量中存在，但当前 `jadx-output/wudang/resources/assets/data` 中缺失，需要回查 APK 解包完整性或资源版本。

## 十四、可复跑的只读命令

统计 `.wd` 文件：

```sh
find jadx-output/wudang/resources/assets/data -maxdepth 1 \
  -type f -name '*.wd' | wc -l
```

检查真实文件格式：

```sh
find jadx-output/wudang/resources/assets/data -maxdepth 1 \
  -type f -name '*.wd' -exec file {} +
```

寻找反编译代码中的模板引用：

```sh
rg -n 'data/[A-Za-z0-9_]+\.wd' jadx-output/wudang/sources
```

寻找当前项目已接入的模板：

```sh
rg -n 'Template\.|data/[A-Za-z0-9_]+\.wd' \
  app/src/main/java app/src/test app/src/androidTest
```

汇总实际模板命中/失败：

```sh
LOG_DIR='/Users/Shared/Library/Application Support/BlueStacks/Engine/UserData/SharedFolder/SGMHelperLogs'
rg 'TEMPLATE_MATCH' "$LOG_DIR"/*.log
```

只看成功记录：

```sh
rg 'TEMPLATE_MATCH.*hit=true' "$LOG_DIR"/*.log
```

## 十五、后续记录格式

每完成一次新分析，在本文末尾追加一条：

```text
日期：
目标模板：
业务文字/图标：
模板文件：
调用类与方法：
原始 ItemView/ROI：
武当阈值与预处理：
测试设备与版本：
正样本数/命中率：
负样本数/误报率：
耗时：
结论：
遗留问题：
```

## 十六、更新记录

### 2026-08-03

- 建立首版持续分析文档。
- 统计 329 个 `.wd` 模板及真实格式。
- 整理 `t1`–`t21` 主菜单文字。
- 整理 `GameConstants.AssetsData` 中可确认的主要语义。
- 记录 `_a` 版本选择条件。
- 对比武当原始匹配方法与当前项目实现。
- 汇总当前日志 `0/102` 命中结果。
- 确定 `t8.wd`“军团”为第一个受控实验对象。

## 十七、`t8.wd` 实际复刻记录

### 17.1 实验目标和语义边界

`t8.wd` 在武当原实现中对应的是 `LegionDialog.titleItem` 的“军团”标题，不是右侧菜单里的军团入口。因此本次复刻只验证“军团窗口已经打开后，标题是否可被模板命中”，不把标题命中误当成菜单导航。

目标流程是：

`固定入口/菜单图标打开军团 → 截图 → 精确 title ROI → t8.wd 彩色匹配 → 命中后继续；未命中保留 OCR/既有回退路径`

### 17.2 分支和参数

- `hk.phx.khm.cs` 对应 `gameServerType=2`。虽然模拟器 API 为 33，但武当 `_a` 分支要求 `serverType=3 && API>=30`，所以本分支使用 `data/t8.wd`，不使用 `t8_a.wd`。
- `t8.wd` 原始尺寸为 `64×31`。
- 5745 上的 `LegionDialog` 标题区域为 `x=570, y=27, width=150, height=34`，即横向 `[570,720)`、纵向 `[27,61)`；截图必须是 `1280×720`。
- 匹配使用 RGBA 彩色图，不转灰度、不缩放，算法为 `TM_CCOEFF_NORMED`；阈值采用严格 `score > 0.85`。截图尺寸或 ROI 不符合预期时直接拒绝，不能自动 resize 伪造命中。

### 17.3 5745 采样步骤

测试设备为 `127.0.0.1:5745`，API 33，物理分辨率 `1280×720`，`sys.boot_completed=1`。

1. 初始画面是“元宝回收 → 快速贩卖装备”弹窗，记录为负样本。
2. 点击 `(820,113)` 关闭弹窗，露出“军团”窗口，记录为正样本。
3. 点击军团窗口关闭按钮 `(1105,48)`，回到主 HUD，记录为负样本。
4. 点击菜单按钮 `(1215,58)`，打开右侧菜单，记录为负样本。

四张截图使用同一套原尺寸模板和同一个精确 ROI，避免用不同裁剪口径比较结果。

### 17.4 离线分数证据

主机 Python 没有 `cv2`，所以先用 NumPy 对 RGBA 像素计算与 `TM_CCOEFF_NORMED` 等价的归一化相关系数。以下是截图证据，不是 APK 端到端日志：

| 样本 | 分数 | 最佳位置（绝对坐标） | 结果 |
|---|---:|---|---|
| 军团窗口正样本 | `0.999410927` | `(616,29)` | 命中 |
| 主 HUD 负样本 | `0.234927580` | `(582,27)` | 不命中 |
| 右侧菜单负样本 | `0.226320028` | `(630,28)` | 不命中 |

正负样本间隔为 `0.764483347`，说明在该固定 ROI 下，“彩色＋原尺寸＋精确 ROI”能够把军团标题与当前 HUD/菜单区分开。

### 17.5 已落地的代码改动

- `app/src/main/java/com/local/sgmhelper/WudangTemplateMatcher.java`：为 `LEGION` 增加固定 1280×720 title ROI；走彩色 Mat、原尺寸模板和 `TM_CCOEFF_NORMED`；保留其他模板原有灰度/缩放逻辑；`LEGION` 采用严格阈值。
- `app/src/androidTest/java/com/local/sgmhelper/WudangTemplateMatcherInstrumentedTest.java`：增加原尺寸彩色正/负样本测试，并验证严格阈值。
- `app/build.gradle`：版本从 `292/0.1.292` 递增到 `299/0.1.299`。
- `WildernessNavigator` 的正常业务流程已在进入军团菜单后使用 `t8.wd` 确认军团窗口；模板失败
  时保留 OCR fallback，确认失败则停止后续固定点击，避免误操作其他页面。

### 17.6 构建和安装结果

- `git diff --check` 通过。
- `./gradlew testDebugUnitTest assembleDebug` 成功。
- 向 `127.0.0.1:5745` 执行 `adb install -r` 时两次遇到 `Broken pipe (32)` / `Can't find service: package`；期间 `adb reconnect offline` 曾恢复设备一次，但顺序重试后再次离线。
- 没有执行卸载、清数据或重建模拟器；Instrumentation 和 APK 端到端验证尚未完成。

### 17.7 后续模板复刻清单

1. 先确认模板在武当原实现中的语义对象：标题、按钮、菜单项还是地图/对话锚点。
2. 确认游戏包、`gameServerType`、API 与 `_a` 分支，选择正确的 `.wd` 文件。
3. 读取模板真实格式、颜色通道和原始尺寸；不要根据文件名猜测。
4. 从原布局（`ItemView`、`MenuBox`、`GuideLabel` 等）计算截图坐标和 ROI。
5. 决定该模板使用彩色原尺寸匹配，还是现有灰度/缩放匹配；不要全局替换。
6. 在 5745 采集至少一个正样本和两个负样本，记录分数、位置、阈值和耗时。
7. 先做 matcher 单元/Instrumentation 测试，再安装 APK；安装失败时保留错误证据，停止并行重试，不先卸载或清数据。
8. 只有验证通过后，才把该模板接入具体业务流程，并保留 OCR 或固定坐标回退。

## 十八、`g04.wd` HUD 自动寻路锚点记录

### 18.1 原始语义和资源

- `g04.wd` 是 `GameConstants.AssetsData.GUIDE_LABEL_BTN`，语义为 `GuideLabel.guildLabelBtn`，即 HUD 上打开“寻路”面板的指南针按钮。
- 文件格式为 WebP，解码颜色为 RGB，原始尺寸 `25×25`；不是文字模板，不应交给 OCR 识别。
- 武当 `GuideLabel` 的非 Asia 分支（5745 实际画面对应此分支）把按钮点击区域定义为 `x=1114, y=479, width=32, height=32`；Asia 分支是 `x=1124, y=479`，暂不混用。

### 18.2 5745 实际采样

设备：`127.0.0.1:5745`，API 33，`1280×720`，`sys.boot_completed=1`。

1. 关闭当前福利窗口，得到主 HUD 正样本。
2. 在 HUD 上确认 g04 图标位于按钮容器内，模板实际最佳落点为 `(1118,483)`，即容器左上角内缩 `4px`。
3. 点击武当 ItemView 中心 `(1130,495)`，右侧成功打开“寻路”面板；这验证了语义和点击坐标是一致的。
4. 以打开后的寻路面板截图作为负样本：面板遮住原 g04 图标，但保持同一个 32×32 ROI。

### 18.3 彩色原尺寸分数

使用 RGBA/原尺寸模板、同一个固定 ROI，主机 NumPy 计算与 OpenCV `TM_CCOEFF_NORMED` 等价的分数：

| 样本 | 分数 | 最佳位置 | 结果 |
|---|---:|---|---|
| HUD 正样本 | `0.998204231` | `(1118,483)` | 命中 |
| 寻路面板负样本 | `0.063979316` | `(1116,483)` | 不命中 |

正负间隔为 `0.934224915`，当前 `0.85` 阈值有明显余量。

### 18.4 已落地代码

- `WudangTemplateMatcher` 为 `AUTO_PATH` 增加固定 `1280×720`、`(1114,479)-(1146,511)` 彩色原尺寸分支，使用 `TM_CCOEFF_NORMED`，不灰度、不缩放；模板命中使用严格 `score > 0.85`。
- `AutomationHost.clickTemplateOrText` 对 `AUTO_PATH` 只把模板匹配 ROI 改为上述精确区域，OCR fallback 仍沿用调用方原来的大范围 ROI；这样不会因为 g04 是图标而破坏原有回退路径。
- Instrumentation 增加 `25×25` g04 正样本/空白负样本检查，同时修正测试辅助方法按传入模板取回结果。
- Debug 版本递增到 `versionCode=301`、`versionName=0.1.301`。
- `TaskAutomation` 的点击动作和自动寻路业务状态机没有重写；本次只替换识别入口，待设备端测试通过后再评估是否删除 OCR 或调整回退。

### 18.5 构建和安装结果

- `git diff --check` 通过。
- `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest` 成功。
- 5745 安装先后两次遇到 `Broken pipe (32)` / `Can't find service: package`；`adb reconnect offline` 后曾恢复连接，但再次安装仍失败，随后 shell 出现 `error: closed`。
- 没有卸载、清数据、重建实例，也没有运行 Instrumentation；APK 端到端命中率和耗时仍待设备恢复后验证。

### 18.6 下次 pickup

1. 先恢复 5745 的 package service，确认 `dumpsys package com.local.sgmhelper` 版本是否仍为旧版本。
2. 只对包含 g04/mjt 的 `versionCode=302` APK 做一次顺序安装；成功后运行 `WudangTemplateMatcherInstrumentedTest`。
3. 在主 HUD 和打开寻路面板两种状态各采集一次 `TEMPLATE_MATCH`，核对 ROI、score、hit、elapsedMs。
4. 只有设备端通过后，才继续检查 `g04` 是否能替代自动寻路相关 OCR；不要先删除 OCR fallback。

### 18.7 g04 快速路径优化

- `HelperAccessibilityService.openAutoPathPanel()` 现在先在固定 `(1114,479)-(1146,511)` ROI 查找 `g04.wd`，命中后点击模板中心；匹配失败才回退旧的 `(1150,500)` 点击和 OCR 确认。
- 打开后的确认先检查 `g04` 是否从 ROI 消失；通常一次彩色模板截图即可继续，连续未消失才回退原来的自动寻路面板 OCR。
- `closeAutoPathPanel()` 在面板已关闭时也先用 `g04` 命中直接通过，避免每次先做整块 OCR。
- `TaskAutomation` 的两个可选自动寻路点击改用快速点击回调（约 200ms），普通模板路径和 OCR fallback 不变。
- 本次只修改识别/等待路径，未 build、未 bump、未安装；设备端耗时和误判仍需在 5745 上补测。

## 十九、`mjt.wd` 军团菜单入口记录

### 19.1 原始语义和布局

- `mjt.wd` 是 `GameConstants.AssetsData.MENU_BOX_JT`，语义是菜单里的“军团”入口图标；它与 `t8.wd` 不同：`t8.wd` 是打开军团窗口后的标题文字。
- 文件格式为 WebP，解码颜色为 RGB，原始尺寸 `37×20`。
- 5745 使用武当 `MenuBox` 的非 Asia 分支：菜单布局为 `x=846, y=101, width=392, height=465`，4 列×5 行，每个 `ItemView` 为 `98×93`。
- “军团”是 `menuList[5]`（第 2 行第 2 列），因此精确 ItemView ROI 是 `(944,194)-(1042,287)`；点击中心是 `(993,240)`。

### 19.2 5745 实际采样

1. 关闭福利、奖励提示和奖励找回窗口，回到主 HUD。
2. 点击右上角菜单 `(1215,58)`，打开右侧 MenuBox。
3. 红框标记第 2 行第 2 列军团 ItemView，绿框标记 `mjt.wd` 实际命中片段；最佳位置为 `(972,225)`。
4. 主 HUD 截图保持同一 `(944,194)-(1042,287)` ROI，作为负样本。

### 19.3 彩色原尺寸分数

使用同一 98×93 ROI、RGB 原尺寸模板，主机 NumPy 计算与 OpenCV `TM_CCOEFF_NORMED` 等价的分数：

| 样本 | 分数 | 最佳位置 | 结果 |
|---|---:|---|---|
| MenuBox 军团入口正样本 | `0.992362050` | `(972,225)` | 命中 |
| 主 HUD 负样本 | `0.349931029` | `(967,244)` | 不命中 |

正负间隔为 `0.642431021`，当前 `0.85` 阈值仍有余量。

### 19.4 已落地代码

- `WudangTemplateMatcher` 新增独立 `Template.LEGION_MENU`（`data/mjt.wd`），不再复用 `Template.LEGION`；后者继续专用于 `t8.wd` 军团窗口标题。
- `LEGION_MENU` 使用固定 1280×720、98×93 MenuBox ItemView ROI，走彩色 Mat、原尺寸模板和 `TM_CCOEFF_NORMED`，严格 `score > 0.85`。
- `AutomationHost.clickTemplateOrText` 对 `LEGION_MENU` 只把模板匹配 ROI 改为精确 ItemView，OCR fallback 仍使用调用方传入的整个菜单区域。
- `WildernessNavigator` 的军团入口改为 `LEGION_MENU`；模板未命中时仍保留原来的 OCR/固定点击回退。
- Instrumentation 增加 `37×20` mjt 正样本/空白负样本检查。
- Debug 版本递增到 `versionCode=302`、`versionName=0.1.302`。

### 19.5 构建和安装结果

- `git diff --check` 通过。
- `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest` 成功。
- 向 `127.0.0.1:5745` 安装 `0.1.302` 时再次遇到 `Broken pipe (32)` / `Can't find service: package`；没有卸载、清数据或并行重试。
- Instrumentation 尚未运行；当前 mjt 结论是“离线正负样本通过，设备端 APK 验证待模拟器 package service 恢复”。

### 19.6 下次 pickup

1. 先恢复 5745 package service，再只安装 `versionCode=302`。
2. 运行 `WudangTemplateMatcherInstrumentedTest`，确认 g04、mjt、t8 三组彩色原尺寸测试一起通过。
3. 在 MenuBox 打开状态检查 `TEMPLATE_MATCH name=军团入口` 的 ROI、score、hit、elapsedMs。
4. 通过后再验证点击模板中心进入军团窗口；不要把 `mjt` 的菜单命中当作 `t8` 标题命中。

## 二十、`t1.wd` 元宝回收标题记录

### 20.1 原始语义：不是主菜单格文字

- `t1.wd` 是 `GameConstants.AssetsData.GEAR_RECYCLE`，武当原实现把它传给 `GearRecycle2Action.checkOnRecycle()`，匹配对象是 `RecycleDialog.titleItem`。
- `RecycleDialog.titleItem` 的非 Asia（5745）布局来自 `ScreenAdapterHelper.RecycleDialogSize`：`x=540, y=27, width=200, height=38`。
- 因此，虽然主菜单第二页也有一个小字号“元宝回收”入口，但 `t1.wd` 本身是打开后的功能页标题模板；不能把它当作主菜单格模板直接使用，否则会把 32px 标题字和菜单格小字混用。
- 文件格式为 WebP，解码颜色为 RGB，原始尺寸 `131×33`。

### 20.2 5745 实际采样

1. 在 5745 启动游戏并进入主 HUD；没有安装新 APK。
2. 打开主菜单并点击第二页“元宝回收”入口，进入元宝回收页面。
3. 页面顶部标题位于原实现 ROI `(540,27)-(740,65)`；模板在该 ROI 内的最佳落点是 `(575,30)`。
4. 真实截图保存为 `/tmp/sgmhelper-t1-recycle-open.png`，可用于之后复核；主菜单第二页截图为 `/tmp/sgmhelper-t1-menu-page2.png`。

### 20.3 彩色原尺寸分数

对真实标题截图的固定 ROI 使用 RGB 原尺寸模板、`TM_CCOEFF_NORMED` 等价计算：

| 样本 | 分数 | 最佳位置 | 结果 |
|---|---:|---|---|
| 元宝回收页面标题正样本 | `0.999238789` | `(575,30)` | 命中 |

模板尺寸 `131×33` 小于原始标题 ItemView `200×38`，只在该 200×38 ROI 内搜索，不缩放、不改色；这与武当原实现传入 `titleItem` 的边界一致。

### 20.4 已落地代码

- `WudangTemplateMatcher` 为 `Template.YUANBAO_RECYCLE` 增加固定 `(540,27)-(740,65)` 的彩色原尺寸分支，使用严格 `score > 0.85`。
- `AutomationHost.waitTemplateOrText` 对 t1 的模板匹配改用上述精确标题 ROI；OCR fallback 仍沿用调用方区域。
- 主菜单入口仍保留原有 OCR/菜单区域逻辑：因为菜单里的“元宝回收”是另一种字号，不能用 `t1.wd` 冒充入口模板。
- Instrumentation 增加 `131×33` t1 正样本/空白负样本检查。
- Debug 版本递增到 `versionCode=303`、`versionName=0.1.303`。

### 20.5 构建和安装结果

- `git diff --check` 通过后执行本地 `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest`；本次仅构建和测试编译，不安装 APK。
- 按当前要求没有执行 `adb install`，因此 t1 的 APK 端到端 Instrumentation 尚未运行；真实 5745 截图的离线正样本已验证。

### 20.6 下次 pickup

1. 继续处理 `t2.wd`，先确认它是主菜单入口还是功能页标题，再决定是否使用 `MenuBox` 单元 ROI。
2. 设备 package service 恢复后，再统一运行 t1、t8、g04、mjt 的 Instrumentation；不要为此重试安装 pipe 错误。

## 二十一、`t2.wd` 士兵和阵形标题记录

### 21.1 原始语义：Soldier.titleItem

- `t2.wd` 由武当资源选择器 `C2187w.m1862z0()` 返回；原实现的匹配对象是 `console.entity.Soldier.titleItem`，标题文字为“士兵和阵形”。
- 5745 使用非 Asia `Soldier` 布局：`titleX=540, titleY=32, titleWidth=200, titleHeight=35`，精确 ROI 为 `(540,32)-(740,67)`。
- 这仍是功能页标题，不是主菜单格的小字号“士兵”入口；不能把 `t2.wd` 直接当作 MenuBox 单元模板。
- `t2.wd` 为 PNG/RGB、原始尺寸 `174×34`；同时保留武当 Asia 资源 `t2_a.wd`（WebP/RGB、`155×29`）。两份资源均从原始 JADX assets 原样复制，未重新编码。

### 21.2 5745 实际采样

1. 从 5745 主 HUD 打开菜单；先把菜单从第二页滑回第一页。
2. 点击第一行第二列“士兵”入口，打开“士兵和阵形”页面。
3. 标题截图保存为 `/tmp/sgmhelper-t2-soldier-open2.png`；标题模板在固定 ROI 内最佳落点为 `(558,33)`。

### 21.3 彩色原尺寸分数

真实标题截图使用固定 200×35 ROI、RGB 原尺寸模板和 `TM_CCOEFF_NORMED` 等价计算：

| 样本 | 分数 | 最佳位置 | 结果 |
|---|---:|---|---|
| 士兵和阵形页面标题正样本 | `0.997911453` | `(558,33)` | 命中 |
| 主 HUD 负样本 | `-0.015862977` | `(545,32)` | 不命中 |

### 21.4 已落地代码

- `WudangTemplateMatcher` 新增 `Template.SOLDIER`（`data/t2.wd`、`data/t2_a.wd`），固定使用 `(540,32)-(740,67)` 彩色原尺寸分支，严格 `score > 0.85`。
- `AutomationHost.clickTemplateOrText` 和 `waitTemplateOrText` 对 t2 使用上述精确标题 ROI；OCR fallback 仍保留调用方原区域。
- Instrumentation 增加 `174×34` t2 正样本/空白负样本检查。
- Debug 版本递增到 `versionCode=304`、`versionName=0.1.304`。

### 21.5 构建和安装结果

- `git diff --check` 通过。
- `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest` 成功。
- 按要求没有执行 APK 安装；Instrumentation 只完成编译，真实 5745 截图完成离线正样本验证。

### 21.6 下次 pickup

1. 下一项处理 `t6.wd`，先查原实现对应的 `ItemView` 和非 Asia ROI。
2. 不要把 t2 标题模板误接到主菜单小字号“士兵”入口；如果要替换入口 OCR，应另做菜单字号样本或使用 MenuBox 坐标。

## 二十二、`t3.wd` 商店标题记录

### 22.1 原始语义和精确 ROI

- `t3.wd` 由武当资源选择器 `C2187w.m1774A0()` 返回，原实现对应
  `console.entity.Store.titleItem`，不是 HUD 顶部“商城”按钮，也不是主菜单“炼造房”。
- `Store` 的两个布局分支都把标题框固定为 `titleX=580, titleY=27,
  titleWidth=120, titleHeight=38`，因此 1280×720 的精确 ROI 是
  `(580,27)-(700,65)`。
- 普通版 `t3.wd` 是 PNG/RGB、原始尺寸 `64×33`；`t3_a.wd` 是 WebP/RGB、
  原始尺寸 `53×29`。两份文件均与 JADX 原始资源 SHA-256 一致：
  `37ff9fed93d64d8e6cc5f55bbdc7ccbf94d4e31144ab09df74c7c32bbaddf359`、
  `7c43c13387b007c3884db9ccf393508b0258619b8e2cc0af07c9513b9f24af0f`。

### 22.2 5745 实际正负样本

1. 关闭 HUD 顶部“商城”页后，在成都城内走到商店入口；第二次点击入口后打开真实商店面板。
2. 真实正样本保存为 `/tmp/sgmhelper-t3-store-approach3.png`，顶部标题确实是“商店”。
3. `/tmp/sgmhelper-t3-store-open.png` 的“商城”页和 `/tmp/sgmhelper-t3-shop-open2.png` 的“炼造”页均作为错误页面排除，不能拿来验证 `t3.wd`。

使用固定 ROI、RGB 原尺寸模板和 `TM_CCOEFF_NORMED` 等价计算：

| 样本 | 分数 | 最佳位置 | 结果 |
|---|---:|---|---|
| 成都商店面板标题正样本 | `0.999334931` | `(608,30)` | 命中 |
| 成都 HUD 负样本 | `0.010040371` | `(635,27)` | 不命中 |

### 22.3 已落地代码

- `WudangTemplateMatcher` 为 `Template.SHOP` 增加固定 `(580,27)-(700,65)` 的彩色原尺寸分支，使用 `TM_CCOEFF_NORMED`，不灰度、不缩放；命中使用严格 `score > 0.85`。
- `AutomationHost.clickTemplateOrText` / `waitTemplateOrText` 对 t3 统一改用精确标题 ROI；OCR fallback 仍沿用调用方区域。
- `CityTravelAutomation` 在进入城市商店面板时优先等待 t3 模板，失败仍走 OCR 和原有退开重试逻辑；客栈流程不受影响。
- Instrumentation 增加 `64×33` t3 正样本/空白负样本检查，并继续检查普通版与 `_a` 资源可解码。
- Debug 版本递增到 `versionCode=305`、`versionName=0.1.305`。

### 22.4 构建和安装结果

- `git diff --check` 通过。
- `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest` 成功。
- 按当前批次要求没有安装 APK，也没有重试 `5745` 的 Broken pipe/package service；真实 5745 截图已完成离线正负样本验证，Instrumentation 只完成编译。

### 22.5 下次 pickup

1. 下一项处理 `t6.wd`，先确认它对应的原实现 `ItemView` 和精确 ROI。
2. 继续区分“功能页标题”和“主菜单格文字”；不要用 t3 的“商店”标题模板去匹配 HUD“商城”。

## 二十三、`t4.wd` 军营标题记录

### 23.1 原始语义和精确 ROI

- `t4.wd` 由 `C2187w.m1826h0()` 返回，原实现对应
  `console.entity.MilitaryCamp.titleItem`，标题文字为“军营”。
- 5745 使用非 Asia `MilitaryCamp` 分支：`titleX=580, titleY=25,
  titleWidth=120, titleHeight=40`，精确 ROI 为 `(580,25)-(700,65)`。
- 普通版 `t4.wd` 是 PNG/RGB、原始尺寸 `63×32`；`t4_a.wd` 是 WebP/RGB、原始尺寸 `56×28`。
  两份资源均从 JADX 原始 assets 原样复制，SHA-256 分别为：
  `57a1fbda0e18ebc1e70735d9a740131e43a39096188ae0341d5361f5a9dda108`、
  `d65654d50bd425676795c8409c3d7f87576920ad694e8807a47bf45644c19e78`。

### 23.2 5745 实际正负样本

1. 在成都城内打开自动寻路栏，点击“军营”入口；到达军营后打开真实军营面板。
2. 正样本保存为 `/tmp/sgmhelper-t4-camp-open-real.png`，顶部标题为“军营”。
3. HUD 截图 `/tmp/sgmhelper-t4-hud-start2.png` 作为负样本。

使用固定 ROI、RGB 原尺寸模板和 `TM_CCOEFF_NORMED` 等价计算：

| 样本 | 分数 | 最佳位置 | 结果 |
|---|---:|---|---|
| 军营面板标题正样本 | `0.999085903` | `(609,29)` | 命中 |
| HUD 负样本 | `0.170277789` | `(611,30)` | 不命中 |

### 23.3 已落地代码

- `WudangTemplateMatcher` 新增 `Template.MILITARY_CAMP`，固定使用
  `(580,25)-(700,65)` 彩色原尺寸分支，严格 `score > 0.85`。
- `AutomationHost` 为 t4 提供精确标题 ROI；OCR fallback 仍保留。
- `SoldierRevivalAutomation` 等待军营到达时优先识别 t4，未命中继续原有重试/fallback。
- Instrumentation 增加 `63×32` t4 正样本/空白负样本检查。
- Debug 版本递增到 `versionCode=306`、`versionName=0.1.306`。

### 23.4 构建和安装结果

- `git diff --check` 通过。
- `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest` 成功。
- 按要求没有安装 APK；真实 5745 截图完成离线正负样本验证，Instrumentation 只完成编译。

### 23.5 下次 pickup

1. 下一项处理 `t6.wd`（客栈），先确认原实现对应的标题对象和精确 ROI。
2. 不要把自动寻路栏里的小字号“军营”入口，误当成 `t4.wd` 的功能页标题。

## 二十四、`t5.wd` 技能标题记录

### 24.1 原始语义和精确 ROI

- `t5.wd` 由 `C2187w.m1839o()` 返回，原实现对应
  `console.entity.SkillDialog.titleItem`，标题文字为“技能”。
- 5745 使用非 Asia `SkillDialog` 分支：`titleX=560, titleY=30,
  titleWidth=160, titleHeight=33`，精确 ROI 为 `(560,30)-(720,63)`。
- 普通版 `t5.wd` 是 PNG/RGB、原始尺寸 `66×33`；`t5_a.wd` 是 WebP/RGB、
  原始尺寸 `56×29`。两份资源已与 JADX 原始 assets 原样校验，SHA-256 分别为：
  `84787a2f89eb1cca16a00e94e29d1139b93835c54120c44a5e0c03c93b61e891`、
  `0d56fdca5d8e9917378c34d4bb37e8e2160adc88f2819c4a2f291d5e49c1517e`。

### 24.2 5745 实际正负样本

1. 从主菜单第一页点击“技能”，打开真实技能面板。
2. 正样本保存为 `/tmp/sgmhelper-t5-skill-open.png`，顶部标题为“技能”。
3. `/tmp/sgmhelper-t5-hud.png` 作为 HUD 负样本。

使用固定 ROI、RGB 原尺寸模板和 `TM_CCOEFF_NORMED` 等价计算：

| 样本 | 分数 | 最佳位置 | 结果 |
|---|---:|---|---|
| 技能面板标题正样本 | `0.999194324` | `(608,30)` | 命中 |
| HUD 负样本 | `0.233979911` | `(644,30)` | 不命中 |

### 24.3 已落地代码

- `WudangTemplateMatcher` 为已有 `Template.SKILL` 增加固定 `(560,30)-(720,63)` 的彩色原尺寸分支，严格 `score > 0.85`。
- `AutomationHost.clickTemplateOrText` / `waitTemplateOrText` 对 t5 使用精确标题 ROI，OCR fallback 保留。
- Instrumentation 增加 `66×33` t5 正样本/空白负样本检查。
- 本轮不 build、不 bump version；`app/build.gradle` 保持 `versionCode=306`、`versionName=0.1.306`。

### 24.4 本轮状态

- `git diff --check` 已通过。
- 没有执行 Gradle build，也没有安装 APK；t5 结论来自原实现 ROI、5745 真实截图和离线 NCC 分数。

### 24.5 下次 pickup

1. 下一项处理 `t6.wd`（客栈）。
2. 不要用 t5 技能标题模板去匹配主菜单“技能”入口的小字号文字。

## 二十五、`t6`–`t21` 批量接入记录

### 25.1 原实现 ROI 目录

本批次将原实现中能直接追到 `ItemView` 的 1280×720 非 Asia 布局坐标写入
`WudangTemplateMatcher.Template.fixedRegion`。匹配器对这些模板统一使用武当原实现的
关键性质：截图必须是 1280×720、调用 ROI 必须与固定框完全相等、模板保持原始尺寸和
RGB 通道、使用 `TM_CCOEFF_NORMED`；不做灰度化、不做缩放。模板尺寸小于 `ItemView`
时，只在该精确框内寻找原尺寸模板，返回模板实际落点。

| 模板 | 原实现对象/语义 | 精确 ROI `(left,top)-(right,bottom)` | 证据 |
|---|---|---|---|
| `t6.wd` | `RytDialog.titleArea` 客栈 | `(580,28)-(700,63)` | 已确认：反编译字段 |
| `t7.wd` | `LineDialog.titleArea` 分流信息 | `(590,67)-(690,90)` | 已确认：非 Asia 分支字段 |
| `t8.wd` | Legion 标题 军团 | `(570,27)-(720,61)` | 已确认：已有 5745 正负样本 |
| `t9.wd` | `Friend.title` 好友 | `(601,19)-(721,52)` | 已确认：反编译字段 |
| `t10.wd` | `Palace.titleItem` 宫殿 | `(590,30)-(690,63)` | 已确认：反编译字段 |
| `t11.wd` | `DungeonDialog.titleItem` 副本 | `(600,27)-(680,65)` | 已确认：非 Asia 构造分支 |
| `t12.wd` | `RefineDialog.titleItem` 炼造 | `(600,28)-(680,64)` | 已确认：反编译字段 |
| `t13.wd` | `WarSoulDialog.titleItem` 武魂擂台 | `(570,16)-(710,50)` | 已确认：反编译字段 |
| `t14.wd` | `AutoFunction.titleItem` 自动功能 | `(570,6)-(1280,44)` | 已确认：非 Asia 构造分支 |
| `t15.wd` | `RewardRecovery.titleItem` 奖励找回 | `(540,27)-(740,63)` | 已确认：反编译字段 |
| `t16.wd` | `Bank.titleItem` 钱庄 | `(580,16)-(700,56)` | 已确认：反编译字段 |
| `t17.wd` | 战斗结果/状态文字 战利品 | `(600,114)-(680,134)` | 合理推断：`GameWar.statueText` 与尺寸吻合；尚无真实截图 |
| `t18.wd` | `WarDialog.titleAreaItem` 战役 | `(560,27)-(720,67)` | 已确认：非 Asia 构造分支 |
| `t19.wd` | `LegionDialog.challengeDialogTitleArea` 名将挑战标题 | `(570,25)-(730,62)` | 已确认：5745 非 Asia 构造分支，原实现字段 |
| `t20.wd` | `HistoricWar.lszcTitleArea` 历史战场 | `(560,27)-(720,67)` | 已确认：反编译字段 |
| `t21.wd` | `QldqDialog.titleItem` 千里单骑 | `(560,26)-(720,66)` | 已确认：非 Asia 构造分支 |

`t17` 是这一批唯一没有找到明确“标题 ItemView”调用者的资源。它的原始尺寸
`63×20` 与 `GameWar.statueText` 的 `80×20` 区域一致，因此先按战斗结果状态文字接入，
并在真实战斗截图验证前保留“合理推断”标记；如果后续截图显示它属于副本结算按钮，
只需替换该条 `fixedRegion`，不会影响其他模板。

### 25.2 代码和测试

- `app/src/main/assets/data/` 已原样复制 `t6`–`t21` 的普通版和 `_a` 版；不重新编码，
  通过 `BitmapFactory` 解码校验。
- `Template` 现在以一个 `FixedRegion` 元数据表驱动全部固定模板，删除了调用方按模板
  名称维护的重复 ROI 分支；`AutomationHost` 仍保留 OCR fallback，模板失败不会终止任务。
- Instrumentation 新增固定目录的合成正样本检查：逐个模板按原尺寸放入固定 ROI，要求
  `score > 0.95`；同时保留既有 t1/t2/t3/t4/t5/t8/g04/mjt 检查和全部资源解码检查。
- 本批次尚未宣称 t6–t21 的真实画面准确率；真实截图正负样本应在后续逐项补齐。

### 25.3 pickup

下一次若要继续降低 OCR 时间，优先在 5745 上采集 t6、t9、t10、t11、t14、t15、t18、
t19、t20、t21 的真实正负截图；其中 t14 的宽 ROI 和 t17 的语义最值得优先复核。

## 二十六、t6–t21 实机验收（2026-08-03）

### 26.1 设备和前置状态

- 目标严格限定为 `127.0.0.1:5745` / `Tiramisu64_19`，没有使用其他模拟器。
- 重启实例后 `sys.boot_completed=1`，`service check package` 返回 `found`；重启前后已安装
  的辅助程序仍为 `versionCode=292`、`versionName=0.1.292`。
- 本轮待验收 APK 是 `versionCode=307`、`versionName=0.1.307`。使用 `adb install -r`
  保留数据，不执行卸载、清数据或重建实例。

### 26.2 安装通道故障证据

以下三条路径均按顺序单独执行，结果相同：

1. `adb install -r app-debug.apk`：`Failure calling service package: Broken pipe (32)`；
2. `adb install --no-streaming -r app-debug.apk`：先 push 成功，随后仍为同一错误；
3. 先 `adb push` 到 `/data/local/tmp`，再执行设备内 `pm install -r`：仍为同一错误。

每次失败后 5745 会变为 `offline/closed`，需要 `adb reconnect offline`；日志中可见
`system_server` 重启和 package service 重建。空间充足（`/data` 仍有约 114G 可用），新旧 APK
签名证书 SHA-256 相同，因此当前证据指向 BlueStacks package-manager 安装通道，不指向模板
资源、版本号或签名不兼容。安装失败后版本核验仍为 `0.1.292`。

### 26.3 游戏前置也未形成可验收 HUD

- 游戏包为 `hk.phx.khm.cs`（`0.3.3`）。实例恢复后可启动到登录页，截图保存为
  `/tmp/game-login-final.png`。
- 点击“开始游戏”后停留在“请稍候”，日志出现 `LOGIN_DISCONNECT`；没有进入主 HUD 或
  任一 t6–t21 对应功能页。因此不能把登录页、黑屏或网络断线误记为模板负样本/正样本。

### 26.4 t6–t21 逐项结论

本轮 16 项全部为“未验收（前置阻塞）”，不是“匹配失败”：

`t6 客栈`、`t7 分流信息`、`t8 军团`、`t9 好友`、`t10 宫殿`、`t11 副本`、
`t12 炼造`、`t13 武魂擂台`、`t14 自动功能`、`t15 奖励找回`、`t16 钱庄`、
`t17 战利品`、`t18 战役`、`t19 名将挑战`、`t20 历史战场`、`t21 千里单骑`。

没有生成任何一项“真实目标页正样本命中”结论；因此 25.1 中 t17、t19 的推断标记仍然
保留，其他条目的 ROI 也只能算原实现/代码证据，不能升级为实机准确率。

### 26.5 下次 pickup

先恢复 5745 的 package-manager 安装能力并把辅助程序核验到 `0.1.307`，再让游戏成功进入
HUD；之后按 t6 → t21 顺序，每项保存一张目标页正样本和一张 HUD/相邻页面负样本，使用
1280×720、彩色、原尺寸、精确 ROI 的 `TM_CCOEFF_NORMED` 记录分数。安装或登录前置没有恢复
前，不再修改任何 t6–t21 ROI。

## 二十七、t6–t21 第二轮实机验收（2026-08-04）

### 27.1 验收边界

- 设备仍严格限定为 `127.0.0.1:5745` / `Tiramisu64_19`；未安装新 APK、未 build、未 bump
  版本，当前辅助程序仍是 `0.1.292`。本轮只使用已经运行的游戏 `hk.phx.khm.cs 0.3.3`
  画面和宿主端同算法复核。
- 所有分数均为彩色 RGB、原始模板尺寸、`TM_CCOEFF_NORMED`；固定 ROI 内不缩放、不灰度化。
  `t19` 普通版 `t19.wd` 是当前画面的命中资源，`t19_a.wd` 不是本画面的分支资源。

### 27.2 已完成正负样本

| 模板 | 5745 真实页面 | 正样本分数 | 结论 |
|---|---|---:|---|
| t6 | 客栈 | 0.999216 | 通过 |
| t7 | 分流信息 | 0.998760 | 通过 |
| t8 | 军团窗口 | 0.998983 | 通过 |
| t9 | 好友 | 0.999026 | 通过 |
| t10 | 城中间的宫殿 | 0.999202 | 通过 |
| t11 | 副本 | 0.998787 | 通过 |
| t12 | 炼造 | 0.999237 | 通过 |
| t13 | 武魂擂台 | 0.986333 | 通过 |
| t14 | 自动功能 | 0.894440 | 通过但余量较小 |
| t15 | 奖励找回 | 0.999014 | 通过 |
| t16 | 钱庄 | 0.998946 | 通过 |
| t18 | 战役 | 0.998539 | 通过 |
| t19 | 军团内“名将挑战” | 0.9984 | 通过 |
| t21 | 千里单骑 | 0.998290 | 通过 |

公共 HUD 负样本 `/tmp/sgmhelper-hud-real-base3.png` 中，上述模板均低于 `0.37`，没有把
HUD 误判成主菜单文字。t10 使用的是城内宫殿页面，不是副本大厅；t16 使用的是钱庄页面。

### 27.3 t19 ROI 根因和修正

用户打开的真实页面标题确认为“名将挑战”，截图为
`/tmp/sgmhelper-t19-jys-now.png`。原代码先写成 `(570,27)-(710,61)`，在该框内只有
`0.277349`，看起来像失败；但整图能在 `(584,27)` 取得约 `0.9984`，说明模板和画面本身
完全一致，只是固定框右侧截断了标题的 5 个像素。

反编译的 `LegionDialog` 对 5745 的非 Asia 分支给出：
`challengeDialogTitleAreaX=570`、`Y=25`、`Width=160`、`Height=37`。因此已将
`WudangTemplateMatcher` 的 t19 改为精确 ROI `(570,25)-(730,62)`；模板 `131×33` 在框内
的实际落点是 `(584,27)`，保持 1:1 裁剪和原始彩色匹配。

### 27.4 暂缓项目

- `t17 战利品`：用户尚未定位到真实页面，继续保留“语义待确认”。
- `t20 历史战场`：按用户要求暂跳过，不把未测试写成失败。

## 二十八、t7/t8 窗口状态接入（2026-08-04）

### 28.1 t7 分流信息

`BossAutomation` 点击 HUD 分流按钮后，先使用 `Template.LINE_INFO` 在武当原实现的固定 ROI
`(590,67)-(690,90)` 检查“分流信息”标题；命中后才继续 OCR 大分流窗口中的当前分流数字。
这样 t7 负责窗口状态，OCR 只负责动态数字，不把标题模板当作分流号识别。

`ChannelSwitchTest` 也使用相同的 t7 检查；如果模板和标题 OCR fallback 都失败，则停止换线测试，
不再盲点分流列表。

### 28.2 t8 军团

`WildernessNavigator` 点击 `mjt.wd` 军团入口后，先用 `Template.LEGION` 在
`(570,27)-(720,61)` 检查军团窗口标题；命中后才点击荒野相关按钮。定时军团奖励流程在打开
军团入口后也使用同一个 t8 检查。

`mjt.wd` 仍然只代表菜单里的军团入口，`t8.wd` 代表已经打开的军团窗口，两者不能互相替代。
模板失败时允许一次标题 OCR fallback；仍失败就停止该子流程，避免在 HUD 或其他窗口上继续执行
固定坐标点击。

## 二十九、登录方式模板 `l1.wd` / `l2.wd`（2026-08-04）

本轮按实际用途先只接入两个登录方式文字模板；`a*`、`sat*`、`sn*` 暂不进入运行链路。

### 29.1 原实现语义和尺寸

| 模板 | 原实现语义 | 原始尺寸 | 证据状态 |
|---|---|---:|---|
| `l1.wd` | `AsiaLoginPage.switchLogin` 区域中的“快捷登录” | 85×20 | 已确认：`LoginGameAction` 调用 `AiHelper` 检查 `data/l1.wd` |
| `l2.wd` | `AsiaLoginPage.switchLogin` 区域中的“账号登录” | 85×20 | 已确认：`LoginGameAction` 调用 `AiHelper` 检查 `data/l2.wd` |

`AsiaLoginPage` 的 1280×720 构造分支把 `switchLogin` 定义为
`(550,482)`、尺寸 `180×50`。模板本身只有 85×20，因此检测框使用这个完整
`ItemView`，在框内搜索模板的原始尺寸；不把 180×50 缩放成模板，也不把模板放大到
整块区域。

### 29.2 当前项目接入

- 两个 `.wd` 已按原始字节复制到 `app/src/main/assets/data/`，没有重新编码。
- `WudangTemplateMatcher.Template.LOGIN_QUICK` / `LOGIN_ACCOUNT` 使用固定彩色 ROI
  `(550,482)-(730,532)`，保持 1280×720、RGB、原尺寸、`TM_CCOEFF_NORMED`。
- `LoginAutomation` 每次检查登录画面时，先在该 ROI 同时匹配 `l1` 和 `l2`；命中后分别
  直接进入账号登录选择或账号密码输入。两者都未命中时，才执行原有整屏 OCR，用于公告、
  开始游戏、福利弹窗和已登录 HUD 等其它状态。
- 账号输入坐标、登录提交坐标和 OCR fallback 均未改动；模板只替代“登录方式判别”这一步。

### 29.3 验证边界

固定 ROI 的合成匹配会随现有 `WudangTemplateMatcherInstrumentedTest` 的模板目录检查
覆盖两个新模板，资源解码和 1:1 彩色匹配可在设备端复核。本轮未 build、未 bump、未安装；
5745 当前截图是“账号登录错误”弹窗，不是 `l1`/`l2` 正样本，因此暂记为“原实现和资源已确认，
真实登录页正样本待补”，不能把离线/合成通过写成实机准确率。

下一次可在 5745 出现登录方式选择页时保存一张正样本和一张非登录页负样本，记录两个模板
在 `(550,482)-(730,532)` 内的分数，再决定是否把 OCR fallback 次数继续下调。

## 三十、`p` 月卡领取与 `v2` 秘境（2026-08-04）

### 30.1 `p.png` 的原实现调用链

用户提供的 `jadx-output/wudang/resources/assets/data-pictures/p.png` 是 `45×26` 的
“领取”字形；它与武当运行时使用的 `data/p.wd` 像素内容一致，`p.png` 属于资源图片，
真正被选择的是 `.wd` 文件。`C2187w.m1812a0()` 在非 Asia 分支返回 `data/p.wd`，Asia
分支返回 `data/p_a.wd`。

当前能追到的调用者是 `ReceiveYkAction` 的商城/月卡流程：它把模板交给
`AiHelper`，搜索 `MallPage.Czbj.itemLqBtnArea`，然后点击找到的一个或多个“领取”点。
5745 使用非 Asia 的原实现布局，区域为 `(320,592)-(1200,637)`，即宽 `880`、高 `45`。
因此 `p` 不是“福利”页所有领取按钮的公共模板，也不能直接加入通用 `Template.CLAIM`，
否则会扩大每轮匹配并增加误命中可能。

当前项目的 `Template.MONTH_CARD_CLAIM` 已登记 `p.wd` / `p_a.wd` 和上述精确区域，
但还没有凭空新增完整的商城/月卡业务流程；以后接入该流程时，应在这个固定 ItemView
内做彩色、原尺寸、`TM_CCOEFF_NORMED` 匹配。

### 30.2 当前福利领取变慢的根因与处理

当前 `WelfareAutomation` 的通用按钮集合仍是 `bf2`–`bf5`。每次普通 `tap` 在
`HelperAccessibilityService` 中默认等待 `CLICK_DELAY_MS=2000`，所以连续领取和每日奖励
槽位会把每一次点击都放大成约两秒；模板计算本身通常只有毫秒级耗时。已将福利里的：

- `领取` 模板命中点击；
- `一键领取` 模板命中点击；
- 在线奖励固定槽位点击；
- 每日挑战奖励槽位点击；

改为已有的 `tapFast`（点击后约 `200ms` 继续）。模板未命中时仍保留原 OCR fallback，
没有把 OCR 直接删除，也没有改变分类进入和页面滑动等待。

这是一项点击节奏优化，不等于已经在 5745 实机完成验收；下一次实机观察应重点确认奖励
动画是否在 `200ms` 后已完成。如果某一类出现漏领，再只给该类增加等待，不要把所有领取
按钮重新改回两秒。

### 30.3 `v2` 秘境模板

`C2187w.m1796L0()` 在非 Asia 分支返回 `data/v2.wd`，Asia 分支返回 `data/v2_a.wd`。
调用者是 `TeleportWildVipAction`，匹配对象为 `WildVipDialog.titleItem`；`v2` 的语义是
“秘境”窗口标题，不是普通地图标签。

5745 的非 Asia 构造字段为 `titleX=580`、`titleY=28`、`titleWidth=120`、
`titleHeight=38`。当前项目已登记 `Template.WILD_VIP`，复制两种原始 `.wd` 资源，并按
固定 ROI `(580,28)-(700,66)` 走彩色原尺寸匹配。当前项目尚无现成的秘境传送业务状态机，
因此这一步只完成可复用的标题状态检测，不虚构一条未验证的秘境自动寻路流程。

### 30.4 pickup

1. 若要继续优化福利，先在 5745 保存一张实际福利页截图，确认 `bf` 与 `p` 的视觉分支，
   再决定是否为某个具体 ItemView 加模板；不要把 `p` 并入全局 `CLAIM`。
2. 若要接秘境，先用 `Template.WILD_VIP` 判断窗口已打开，再补 `WildVipDialog` 的选项、
   页码和传送动作；模板命中只负责窗口状态，不负责动态文本。
