# 后羿之弓操作记录

> 本文只记录构建、测试、打包、上传、远端切换、哈希和回滚状态。
>
> - 按版本开发路程：[PROGRESS.md](PROGRESS.md)
> - 当前功能与机制说明：[PROGRESS_FEATURES.md](PROGRESS_FEATURES.md)
> - 崩溃与失败尝试：[BugGress.md](BugGress.md)

## 1. 操作边界

- 本地项目目录：`/Users/apple/Desktop/MyProject/HouYisBow`。
- 构建系统：Gradle + NeoForge ModDev。
- 本地构建 JDK：Java 21。
- Windows SSH/SCP 只在用户当次明确要求上传或替换时使用。
- 目标 Windows OpenSSH 使用兼容的旧式 SCP 通道，传输 JAR 时使用 `scp -O`。
- Windows 只传输构建后的后羿之弓 JAR，不传输源码。
- 不在 Minecraft 运行期间切换活动 JAR。
- 不强制终止用户的 Minecraft 或 Java 进程。
- 不修改其它 Mod、配置、资源包、数据包或存档。
- 不修改地图、Xaero 地图数据和路径点。
- 活动版本切换使用可恢复方式：旧 JAR 改名为 `.jar.disabled`，新包先以非 JAR 临时文件上传并校验，再启用。
- 凭据、SSH 私钥、主机地址和密码不得写入仓库文档。

## 2. 路径说明

### 2.1 本地构建产物

```text
/Users/apple/Desktop/MyProject/HouYisBow/build/libs/houyis-bow-<版本>.jar
```

### 2.2 用户指定的 Windows 项目交付目录

```text
D:\桌面\A_little_Project\后羿之弓
```

该目录用于用户要求的项目 JAR 交付。是否上传由用户当次指令决定。

### 2.3 Windows 实际游戏实例 Mod 目录

```text
E:\我的世界\.minecraft\versions\林双的重工乐事\mods
```

该目录是历次“替换实例旧版本”操作的实际目标。路径只用于后羿之弓 JAR 切换。

## 3. 标准本地构建流程

### 3.1 构建命令

```bash
./gradlew --no-daemon clean test jar
```

该命令按顺序完成：

1. 清理旧构建目录；
2. 解析 NeoForge/Minecraft 开发依赖；
3. 编译主源码；
4. 生成 Mod 元数据；
5. 处理资源；
6. 编译并运行测试；
7. 生成最终 JAR。

### 3.2 每次发布必须核对

- Gradle 任务整体成功。
- 测试数量、失败数和跳过数。
- 最终 JAR 文件名与目标版本一致。
- `META-INF/neoforge.mods.toml` 中版本一致。
- Manifest 的 `Specification-Version` 与 `Implementation-Version` 一致。
- JAR 不包含不应被打包的可选依赖类。
- 新增资源真实存在于 JAR。
- Mixin JSON 和 Mixin 类结构符合运行时规则。
- `git diff --check` 没有空白错误。
- 计算并记录 SHA-256。

## 4. Windows 标准切换流程

1. 只读确认目标 `mods` 目录存在。
2. 列出所有 `houyis-bow-*` 文件，确认当前唯一活动 JAR。
3. 检查 `java.exe` / `javaw.exe` 是否运行。
4. 如果 Minecraft 正在运行：
   - 只把新包上传为 `.jar.uploading`；
   - 校验远端哈希；
   - 不切换、不结束进程；
   - 等用户正常退出游戏。
5. Minecraft 退出后再次确认 Java 进程数为 0。
6. 验证临时文件 SHA-256 与本地一致。
7. 将旧活动 JAR 改名为 `.jar.disabled`。
8. 将 `.jar.uploading` 改名为正式 `.jar`。
9. 再次验证正式活动文件 SHA-256。
10. 确认活动后羿之弓 JAR 数量为 1。
11. 确认临时上传文件数量为 0。
12. 记录回滚文件和完整实例待验证项目。

文件传输使用 `scp -O`，只传到已经核对过的明确目标文件名，不使用宽泛目录通配或递归覆盖。

如果新包启用失败，应优先把新包恢复为非活动文件，再把上一版 `.jar.disabled` 改回 `.jar`，而不是删除备份。

## 5. 构建与测试历史

| 版本 | 构建结果 | 测试 | SHA-256 | 备注 |
| --- | --- | --- | --- | --- |
| 1.0.1 | 已构建 | 当时未在现有记录中保留最终数量 | 未在现有记录中保留 | 首个独立物品基线 |
| 1.0.2 | 已构建 | 本地测试未发现 Mixin 运行时结构错误 | 未在现有记录中保留 | 完整实例出现 BG-003 |
| 1.0.3 | 成功 | 6 项通过 | `87719eadf99cd9636f1b24517054915015460540cb6a3fe1bbc899bb27fa1715` | 配方同步修复成功，但出现 BG-004 |
| 1.0.4 | 成功 | 5 项通过 | `11172e5652f22d7e04a862e128f91d58d87a3a463296f4befd4bc5587a380395` | 完整实例成功进入世界 |
| 1.0.5 | 成功 | 6 项通过 | `5e9c46ace949bc983dd3bade9b60ca21ee396a19e87d260983baf58ef1e31e32` | 已部署，功能界面和粒子待用户验收 |

## 6. 1.0.3 操作记录

### 6.1 本地

- 命令：`./gradlew --no-daemon clean test jar`。
- 结果：成功。
- 测试：6 项全部通过。
- 产物：`build/libs/houyis-bow-1.0.3.jar`。
- SHA-256：`87719eadf99cd9636f1b24517054915015460540cb6a3fe1bbc899bb27fa1715`。
- JAR 文件名、Mod 元数据和 Manifest 版本一致。

### 6.2 Windows

- 上传前检查实例 Mod 目录。
- 将 1.0.3 上传并核验远端 SHA-256。
- 原 1.0.2 改名为 `.jar.disabled`。
- 1.0.1 也保留为 `.jar.disabled`。
- 远端只保留一个活动后羿之弓 JAR。
- 没有传输源码或修改其它 Mod。

### 6.3 运行反馈

- 完整实例越过 `update_recipes` 配方同步阶段，证明配方 codec 修复有效。
- 之后发生 BG-004，因此 1.0.3 不可作为稳定回滚首选。

## 7. 1.0.4 操作记录

### 7.1 本地

- 命令：`./gradlew --no-daemon clean test jar`。
- 结果：成功。
- 测试：5 项全部通过。
- 产物：`build/libs/houyis-bow-1.0.4.jar`。
- SHA-256：`11172e5652f22d7e04a862e128f91d58d87a3a463296f4befd4bc5587a380395`。
- 反编译确认 Northstar 注入处理器为私有静态方法。
- JAR 内没有 `NorthstarStartupGuard`。
- Mixin JSON 只包含真实 Mixin。

### 7.2 Windows

- 上传前确认目标目录存在。
- 确认 1.0.3 是唯一活动包。
- 确认 Minecraft/Java 没有运行。
- 新包先使用非 JAR 临时文件名上传。
- 远端哈希通过后才执行切换。
- 1.0.3 改名为 `.jar.disabled`。
- 1.0.4 成为唯一活动 JAR。
- 远端 SHA-256：`11172E5652F22D7E04A862E128F91D58D87A3A463296F4BEFD4BC5587A380395`。
- 临时文件数量为 0。
- 1.0.1～1.0.3 保留为禁用备份。

### 7.3 运行反馈

- 用户确认 1.0.4 能成功加载并进入世界。
- 未再反馈 BG-001～BG-004 的保存世界崩溃。
- 后续发现创造栏、JEI 与粒子颜色问题，但不影响物品注册和实际锻造。

## 8. 1.0.5 操作记录

### 8.1 本地构建

- 命令：`./gradlew --no-daemon clean test jar`。
- 最终结果：成功。
- 测试：6 项通过，0 跳过，0 失败，0 错误。
- 产物：`build/libs/houyis-bow-1.0.5.jar`。
- 本地 SHA-256：`5e9c46ace949bc983dd3bade9b60ca21ee396a19e87d260983baf58ef1e31e32`。

### 8.2 资源验证

- 中性声波蒙版：16 张。
- 每张尺寸：32×32。
- 原版帧与蒙版 alpha 差异：0。
- 可见非白像素：0。
- 可见像素总数：1513。
- 四个粒子 JSON 的蒙版引用：64 个。
- `minecraft:sonic_boom_*` 引用：0 个。

### 8.3 JEI 验证

- `HouYisBowJeiPlugin` 主类和匿名扩展类存在于 JAR。
- JAR 内 `mezz/jei` 类数量：0。
- JEI 依赖配置为 `compileOnly`。
- 编译使用版本：`19.39.0.372`。
- 未安装 JEI 时，后羿弓主入口不直接引用插件类。

### 8.4 版本与字节码验证

- `neoforge.mods.toml`：`1.0.5`。
- Manifest `Specification-Version`：`1.0.5`。
- Manifest `Implementation-Version`：`1.0.5`。
- 战斗页监听器字节码调用 `CreativeModeTabs.COMBAT` 和 `event.accept`。
- `git diff --check` 通过。

### 8.5 第一次 Windows 上传

- 用户明确要求替换实例旧版本。
- 只读检查确认目标 `mods` 目录存在。
- 当时唯一活动包是 `houyis-bow-1.0.4.jar`。
- 检测到一个 `javaw.exe`，命令行属于 Minecraft 客户端。
- 没有强制结束进程，也没有在运行时改名旧 JAR。
- 先将新包上传为 `houyis-bow-1.0.5.jar.uploading`。
- 远端临时文件长度：50656 字节。
- 远端 SHA-256：`5E9C46ACE949BC983DD3BADE9B60CA21EE396A19E87D260983BAF58EF1E31E32`。
- 哈希与本地一致。

### 8.6 用户退出后的切换

- 用户确认已正常退出游戏。
- 再次检测 Java 进程数：0。
- 切换前状态：1.0.4 活动文件存在、1.0.5 临时文件存在、1.0.5 正式文件不存在、1.0.4 禁用备份不存在。
- 执行可回滚切换：
  - `houyis-bow-1.0.4.jar` → `houyis-bow-1.0.4.jar.disabled`；
  - `houyis-bow-1.0.5.jar.uploading` → `houyis-bow-1.0.5.jar`。
- 切换脚本在新包改名或哈希验证失败时会恢复旧包，本次未触发回滚。

### 8.7 切换后结果

| 检查项 | 结果 |
| --- | --- |
| 活动后羿之弓 JAR 数量 | 1 |
| 活动文件 | `houyis-bow-1.0.5.jar` |
| 1.0.4 备份 | `houyis-bow-1.0.4.jar.disabled` |
| 临时上传文件 | 0 |
| 远端 SHA-256 | `5E9C46ACE949BC983DD3BADE9B60CA21EE396A19E87D260983BAF58EF1E31E32` |
| 其它 Mod | 未修改 |
| 配置与存档 | 未修改 |
| 是否远程启动游戏 | 否 |

## 9. 当前回滚点

- 当前活动版本：1.0.5。
- 最近可恢复备份：1.0.4 `.jar.disabled`。
- 1.0.4 已有完整实例成功进入世界的运行反馈，是当前首选回滚点。
- 1.0.3 曾触发 BG-004，不应作为首选稳定版本。
- 回滚前仍必须确认 Minecraft 已完全退出，并再次核对活动 JAR 数量。

## 10. 当前待操作事项

用户启动完整实例后，需要反馈：

1. 战斗标签页是否出现后羿之弓；
2. 创造搜索是否可以找到后羿之弓；
3. JEI 是否显示锻造配方；
4. JEI 是否显示模板 ×3、弓 ×1、下界合金锭 ×12；
5. 实际锻造是否仍正常扣料；
6. 满蓄力声波是否为浅金、金、琥珀、深金交替；
7. 射箭、多箭、耐久、拖尾、附魔是否有回归；
8. 如有异常，提供完整 `latest.log`，不要只提供末端 crash report。
