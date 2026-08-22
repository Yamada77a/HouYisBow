# 后羿之弓 BugGress

这是本项目唯一的故障处理档案。后续收到新的闪退、断开、进入世界异常或内容缺失时，先阅读本文件，再追加新的记录；不要把已经排除的原因当作新问题重复修复。

## 当前状态

- 当前客户端与 Rainplay 服务端部署包均为 `houyis-bow-1.0.6.jar`；客户端 1.0.5 已删除，服务端 1.0.5 保留在独立回滚目录。
- 1.0.5 已上传并成为目标实例唯一活动的后羿之弓 JAR；1.0.4 保留为 `.jar.disabled` 回滚备份。
- 当前 1.0.5 本地构建与 6 项单元测试已经通过；历史 1.0.3 曾在完整目标整合包中确认发生 BG-004，**不得再把 1.0.3 标记为可用版本**。
- 1.0.3 已成功越过配方同步、玩家加入和世界建立阶段；随后在首批天空渲染帧中因后羿之弓自身的 Mixin 包类加载错误退出。
- 1.0.4 已在完整目标实例中成功加载并进入游戏，未再反馈 BG-001～BG-004 的进入世界崩溃；但用户确认游戏内完全看不到新内容，现归档为 BG-005。
- BG-005 已由“锻造合成正常、战斗页无物品”确认是创造标签页入口遗漏。
- 同次反馈新增 BG-006（JEI 不显示自定义锻造配方）和 BG-007（金色声波实际呈绿色）；三项已合并到 1.0.5、完成构建并部署，尚待完整实例功能验收。
- 后续连续快速射击出现明显卡顿，归档为 BG-008：根因是每支高速箭独立发送火箭粒子，加上原版约 60 秒落地箭存活时间，粒子与实体数量被射速和多箭共同放大。1.0.6 采用单拖尾与 5 秒落地清理的最小修复。

## 固定边界与排查原则

- 用户要求完整保留当前实例的所有其它 Mod：不得通过删除、禁用、降级或修改 Northstar、Create、BetterStats、Connector、Sable、ModernFix 或 Force Close Loading Screen 解决问题。
- 存档、地图、路径点不在修复范围内。Windows 部署只传输构建后的后羿之弓 JAR。
- 先按日志时间找**第一个**异常，再区分直接根因、断开/停服后的连锁异常，以及最终令客户端退出的异常；不能只看最后一段堆栈。
- 兼容补丁必须放在后羿之弓内，并且尽量是可选、客户端侧、针对精确目标方法的窄保护。
- 每个新结论都要写明：证据、已尝试方案、结果、未验证项。没有完整目标实例运行证据时，只能标为“候选修复”，不能标为“已确认解决”。

## 历史总览

| 编号 | 玩家现象 | 首发原因 | 处理版本 | 当前结论 |
| --- | --- | --- | --- | --- |
| BG-001 | 创建或进入世界时出现 Northstar 配置读取崩溃 | 初始报告只保留末端异常；后续完整日志表明它很可能发生在 BG-002 断开、停服和卸载配置之后 | 1.0.2/1.0.3 曾加入兼容守卫 | 1.0.4 已进入游戏，无同类崩溃反馈 |
| BG-002 | 进入世界后出现“保存世界”并闪退 | 后羿之弓锻造配方同步 `update_recipes` 编码失败 | 1.0.3 | 1.0.3 已越过配方同步，运行证据支持已修复 |
| BG-003 | 1.0.2 兼容补丁本身导致 Mixin 闪退 | Mixin 中存在非私有静态辅助方法 | 1.0.3 | 原错误已消失；替代结构引入 BG-004 |
| BG-004 | 世界加载到 100% 后短暂显示反色场景，再“保存世界”并闪退 | 普通 Guard 类仍位于 Mixin 配置保留包，被注入后的 Northstar 类直接引用 | 1.0.4 | 用户已成功加载并进入游戏，运行反馈支持已修复 |
| BG-005 | Mod 已加载，但创造栏/搜索中完全没有新物品 | 后羿之弓已注册，却从未加入任何创造模式标签页 | 1.0.5 | 已构建并部署，待完整实例验证 |
| BG-006 | 实际锻造正常，但 JEI 无法查看配方 | JEI 只认识已登记分类扩展的锻造配方类；自定义配方未登记 | 1.0.5 | 已构建并部署，待完整实例验证 |
| BG-007 | 设定为金色的监守者声波显示为绿色 | 金色顶点色与原版青色声波贴图相乘，红通道无法产生 | 1.0.5 | 已构建并部署，待完整实例验证 |
| BG-008 | 多次快速射击后明显卡顿 | 每支高速箭独立发送拖尾粒子，落地箭又按原版约 60 秒持续堆积 | 1.0.6 | 已构建并部署，待用户重启后性能验证 |

---

## BG-001：Northstar 配置读取异常（后续证据修正了初始归因）

### 报告与证据

- 报告时间：2026-08-11 12:40:28。
- 直接异常：`IllegalStateException: Cannot get config value before config is loaded.`
- 精确调用点：`Northstar PlanetRenderer.render(PlanetRenderer.java:175)`，读取 `NorthstarConfigs.server().atmosphereBaseHeight`。
- 崩溃时世界仍处于首帧附近：集成服务器尚未运行完成，故 SERVER 配置尚未载入。

### 当时的初始推理

1. 堆栈直接落在 Northstar 的天空渲染，不在后羿之弓的箭实体、粒子、网络载荷或锻造代码中。
2. 当时世界时间为起始状态，只有本地玩家；箭、粒子、命中处理均尚未可能运行，因此不能把问题归因于射箭机制。
3. Force Close Loading Screen（经 Connector）会提前移除加载屏；ModernFix、Sable、Iris、Sodium、Veil 等会影响客户端/服务端首帧时序。这些是放大时序窗口的组合条件，而不是本次日志里的直接读取者。
4. 禁用后羿之弓后用户可进入世界，说明后羿之弓会稳定改变触发时序；但“必要触发条件”不等于直接堆栈根因。

### 首次方案：1.0.2

- 在后羿之弓内加入可选客户端 Mixin：
  `@Pseudo @Mixin(targets = "com.lightning.northstar.planet.PlanetRenderer", remap = false)`。
- 仅注入 Northstar 的六参数 `render(ClientLevel, PoseStack, Camera, float, float, boolean)`，在 `HEAD` 判断单人集成服务器是否已 `isReady()` 且仍 `isRunning()`；未就绪/停止时仅跳过当帧 Northstar 行星天空渲染。
- 不改变 Northstar 或任何其它 Mod；远程多人不取消渲染。

### 该方案的实际结果

- 本地构建当时通过，但完整实例运行暴露了 BG-003：Mixin 本身不能应用，因此该保护在 1.0.2 中**从未实际生效**。
- 该方案没有被否定；其目标方法、注入描述符与保护范围均已核对。1.0.3 修复的是 Mixin 实现方式。

### 收到完整日志后的复盘修正

最初附件只有最终 crash report，没有同一轮进入世界的完整 `latest.log`，所以它能证明 Northstar 是**最终崩溃点**，不能证明它是该轮的**第一个异常**。随后收到的 1.0.2 完整日志补齐了此前缺失的顺序：

1. 后羿之弓先在 `update_recipes` 编码失败；
2. 玩家被断开，集成服务器停止并保存世界；
3. SERVER 配置卸载；
4. Create/BetterStats 和渲染侧才读取到已卸载配置；
5. 最终再由当时错误的 Northstar Mixin 使客户端退出。

BG-001 出现时的版本也包含同一错误配方 serializer，而且那份 crash report 显示 `Server Running: false`、`Player Count: 0`，与“配方断开后、配置已卸载”的状态一致。1.0.3 修好配方后，新报告反而显示 `Server Running: true`、玩家已加入、世界时间 939，并且完全没有 `Cannot get config value before config is loaded`。因此当前更强的解释是：BG-001 很可能本来就是 BG-002 的下游连锁，而不是一个需要长期保留补丁的独立首发问题。

联网核对 [Northstar 0.6.4 的 `PlanetRenderer`](https://github.com/Astronauts-of-Create/Northstar-Redux/blob/v0.6.4%2B1.21.1/src/main/java/com/lightning/northstar/planet/PlanetRenderer.java) 后确认，它确实会在客户端天空渲染中直接读取 SERVER 配置；当前开发分支也尚未增加就绪保护。这说明 Northstar 仍具有潜在的时序敏感面。健康流程中服务器配置会在渲染前就绪，而当前完整时间线把未就绪状态追溯到后羿之弓自己的配方断开；但 Force Close Loading Screen 确实可能暴露启动/停服边界，因此保留一个只依赖原版状态、没有外部辅助类的单行保护，风险低于完全撤回后再次暴露 BG-001。

这次修正体现的排查规则是：只有 crash report 时不能把最末端堆栈自动当成整轮首发根因；应优先找同一轮完整 `latest.log` 的最早异常。同时，已经通过联网确认仍存在的上游不安全读取，可以保留最小防线，但不能再为三项布尔判断增加跨类、跨包或跨 Mod 的结构。

---

## BG-002：锻造配方同步导致“保存世界”

### 报告与时间线

用户安装 1.0.2 后仍在进入世界时看到“保存世界”并闪退。新日志显示的真实顺序如下：

1. `21:03:03.062`：服务端编码 `clientbound/minecraft:update_recipes` 失败。
2. 根因：`Can't encode 'HouyisBowSmithingRecipe@…', expected 'HouyisBowSmithingRecipe@…'`。
3. `21:03:03.205`：玩家因该 `EncoderException` 断开。
4. `21:03:03.232`：单人服务器因玩家退出开始停止。
5. `21:03:03.279`：服务器执行 `Saving worlds`。这就是画面上“保存世界”的原因，不是存档损坏。
6. `21:03:03.481`：SERVER 配置开始卸载。
7. `21:03:03.828`：BetterStats/TCDcommons 的延迟加入世界回调重建创造栏；Create 的 `BacktankUtil.maxAirWithoutEnchants` 读取已卸载的 SERVER 配置，才出现第二个 `Cannot get config value before config is loaded`。

### 根因

`HouyisBowSmithingRecipe.Serializer` 在 1.0.2 使用了两个不同对象：

- JSON 配方 codec：`MapCodec.unit(HouyisBowSmithingRecipe::new)`，每次解析会创建新对象；
- 网络 codec：`StreamCodec.unit(new HouyisBowSmithingRecipe())`，只接受另一个固定对象。

该配方没有覆写 `equals`，而 `StreamCodec.unit` 会用 `equals` 验证待编码对象。因此服务器向本地客户端同步配方时必然拒绝 JSON 解析得到的另一个实例。

### 1.0.3 修复

- 新增唯一 `HouyisBowSmithingRecipe.INSTANCE`。
- JSON 和网络 codec 均引用同一个 `INSTANCE`：
  `MapCodec.unit(INSTANCE)` 与 `StreamCodec.unit(INSTANCE)`。
- 通过反编译构建产物确认两个 codec 的静态初始化均从同一个 `INSTANCE` 取值。

### 已排除的错误方向

- 不给 Create / BetterStats 做宽泛补丁：它们的异常发生在前述断开后、配置已卸载之后，是下游连锁而不是首发故障。
- 不把“保存世界”当成世界文件损坏：日志明确显示它紧随配方同步断开而发生。

---

## BG-003：1.0.2 的 Mixin 实现错误

### 证据

- `21:03:03.942`：`InvalidMixinException`。
- 错误文本：`NorthstarPlanetRendererMixin ... contains non-private static method shouldSkipPlanetRendering(ZZZ)Z`。
- Mixin 在应用阶段失败，随后抛出 `MixinApplyError` 并终止客户端。

### 根因与思考

Mixin 会把注入方法合并到目标类。Mixin 0.8.7 不允许普通的非私有静态辅助方法存在于 Mixin 类中；因此 1.0.2 的包可见 `shouldSkipPlanetRendering` 使转换失败。

只把辅助方法改为 `private` 虽可满足可见性规则，却不能继续保留直接单元测试；而把包可见外部方法直接交给合并后的 Northstar 目标类调用，又会有跨包访问风险。因此采用最小且可测试的结构：

- Mixin 内只保留 `private static` 注入处理器；
- 纯判断移到公开普通类 `NorthstarStartupGuard`；
- 该公开类方法是 `public static`，使被合并后的 Northstar 目标类可安全调用；
- 单元测试只测试这条纯布尔判断。

### 1.0.3 验证

- 构建后的 `NorthstarPlanetRendererMixin` 只含一个私有静态注入方法。
- `NorthstarStartupGuard` 为公开类/公开静态方法。
- Mixin JSON 仍只在 `client` 段登记该兼容 Mixin，原有 Smithing Mixin 保持不变。

### 完整实例结果

- 1.0.3 的新报告不再出现 `contains non-private static method`、`InvalidMixinException` 或 `MixinApplyError`。
- 堆栈中的合成处理器 `PlanetRenderer.handler$...houyisBow$deferSkyUntilServerConfigLoads` 证明注入方法已成功合并进 Northstar。
- 因此 BG-003 的原始可见性错误确已消失；但把普通辅助类放在同一 Mixin 保留包的做法产生了 BG-004。不能把“本地测试通过”误写成整项兼容已完成。

---

## BG-004：1.0.3 直接引用 Mixin 保留包中的普通类

### 报告与首发异常

- 报告时间：2026-08-11 21:21:25。
- 运行包：`houyis-bow-1.0.3.jar`。
- 本报告的第一个、也是最终致命异常：
  `IllegalClassLoadError: dev.houyisbow.mixin.NorthstarStartupGuard is in a defined mixin package dev.houyisbow.mixin.* owned by houyis_bow.mixins.json and cannot be referenced directly`。
- 调用链：后羿之弓注入到 `PlanetRenderer` 的处理器 → `PlanetRenderer.render` → `SpaceEffects.renderPlanetsAndStars` → `LevelRenderer.renderSky` / `renderLevel` → `GameRenderer`。

### 代码与官方规则的交叉证据

- `houyis_bow.mixins.json` 声明了 `"package": "dev.houyisbow.mixin"`。
- `NorthstarStartupGuard.java` 仍声明在 `dev.houyisbow.mixin` 中。
- `NorthstarPlanetRendererMixin` 注入处理器会直接调用这个普通 Guard 类。
- [SpongePowered Mixin 官方环境说明](https://github.com/SpongePowered/Mixin/wiki/Introduction-to-Mixins---The-Mixin-Environment#L151-L168)明确写明：Mixin 配置声明的包及其所有子包会从普通运行时类加载中排除，Mixin 类必须位于不包含其它普通类的专用包中。
- [MixinProcessor 0.8.7 官方源码](https://github.com/SpongePowered/Mixin/blob/releases/0.8.7/src/main/java/org/spongepowered/asm/mixin/transformer/MixinProcessor.java#L399-L415)在类名命中已声明 Mixin 包时会抛出 `IllegalClassLoadError`；错误文本与本报告逐字对应。

结论：这不是 Northstar、Connector、村庄生成或存档先出错，而是 1.0.3 把后羿之弓自己的普通运行时类放进了 Mixin 专用包。其它 Mod 只是构成该兼容代码的目标环境，不需要也不允许修改。

### 为什么本地构建与单元测试没有发现

1. Java 编译器允许同一 Java 包内的普通类互相调用，所以 `compileJava` 不会报错。
2. 纯 JUnit 只调用 Guard 的布尔逻辑，没有启动 ModLauncher/Mixin 的专用类加载器。
3. 本地开发运行环境没有完整目标整合包中的 Northstar，因此 `@Pseudo` 会跳过可选目标，无法执行被合并后的真实处理器。
4. 只有完整实例第一次渲染 Northstar 天空、合成处理器尝试加载 Guard 时，Mixin 的包排除规则才会生效。

以后不能再用“Gradle 构建 + 纯单元测试通过”证明第三方 Mixin 兼容成功；必须保留完整实例运行验证这一层。

### “加载 100%”“保存世界”与反色村庄的解释

- 报告显示 `ClientLevel` 和本地玩家已存在、区块统计为 361、世界时间为 939、资源重载完成、集成服务器仍在运行。世界实际上已经越过生成、配方同步和玩家加入，崩在首批天空渲染帧，而不是卡在 100% 的世界文件加载中。
- `Minecraft.emergencySaveAndCrash` 位于异常后的退出链上；玩家看到的“保存世界”是客户端渲染崩溃后让集成服务器安全保存并关闭，不是首发原因，也没有存档损坏证据。
- 堆栈没有村庄、结构或区块生成调用，且报告显示 Shaderpack 关闭。短暂的“村庄反色影子”与渲染帧未完成相符，不能作为村庄 Mod 出错的证据。
- 实例中的 Force Close Loading Screen 会主动提前移除世界加载屏；其[官方说明](https://github.com/kennytv/kennytvs-epic-force-close-loading-screen-mod-for-fabric)也提醒，完全移除加载屏可能短暂暴露世界尚未稳定的视觉画面。这可以解释为什么玩家看到了瞬时场景，但**它不是本次致命异常**；真正异常仍是后羿之弓的 `IllegalClassLoadError`。
- 仅凭 crash report 无法证明反色来自哪一个具体颜色/深度缓冲步骤，因此记录为“渲染中断时的视觉残帧推断”，不把推断写成已证实根因。

### 与 BG-001～BG-003 的状态对照

- BG-001 的 `Cannot get config value before config is loaded` 本报告中没有出现；但是本次在 Guard 的布尔判断执行前就类加载失败，所以 BG-001 仍属于“被 BG-004 提前遮挡，待 1.0.4 完整实例验证”，不能宣称已修复。
- BG-002 的 `update_recipes`、`Can't encode`、`EncoderException` 均未出现；玩家已加入且世界时间达到 939。这是配方 codec 修复成功的强运行证据。
- BG-003 的非私有静态方法/Mixin 应用错误消失，注入处理器已经合并到 Northstar；其原错误已修复，但替代结构的包位置错误成为 BG-004。

### 1.0.4 最小修复方案与实施结果

三路调查曾形成两个候选：完整删除这层推测性兼容 Mixin，或把 Guard 移出 Mixin 包继续保留。联网确认 Northstar 当前仍无保护读取 SERVER 配置后，最终选择第三条、更窄的路径：保留已证明能命中目标方法的 Mixin，但删除所有外部辅助结构。

1. 保留 `@Pseudo` 的 Northstar 六参数 `render` 精确注入和 JSON 客户端登记。
2. 把判断直接写在现有 `private static` 注入处理器中：仅当本地集成服务器为空、未 `isReady()` 或未 `isRunning()` 时取消当帧 Northstar 行星天空渲染。
3. 删除 `NorthstarStartupGuard` 及其纯布尔单元测试，使变换后的 Northstar 类不再加载任何后羿之弓普通辅助类。
4. 保留 `SmithingMenuMixin`，不改 Northstar、Force Close Loading Screen、Connector 或实例中的任何其它 Mod，也不动配置和存档。
5. 版本按规则升为 `1.0.4`，运行 `clean test jar`；检查 JAR 中没有 `NorthstarStartupGuard`，Mixin JSON 仍只列出两个真实 Mixin，反编译确认注入处理器只有私有静态方法且判断已内联。
6. 在完整 Windows 实例中验证新世界、旧世界、退出重进、锻造和射箭，保留完整 `latest.log`。

“把 Guard 移到 `dev.houyisbow.compat` 并保持公开”在技术上可以直接消除本次 `IllegalClassLoadError`，但会让被合并后的 Northstar 类继续硬引用后羿之弓辅助类；为三个布尔值保留这种耦合没有收益，所以不选。

“完整删除 Northstar Mixin”也认真评估过：它最少代码，而且 BG-001 很可能是配方断开后的下游异常；但 Northstar 当前源码仍存在直接读取 SERVER 配置的边界，实例又保留会提前移除加载屏的 Mod。内联判断只增加一个本地条件、远程多人和服务器就绪后的正常帧完全不受影响，因此选择保留这条最小保险。如果 1.0.4 的完整日志证明该 Mixin 仍产生新问题，再删除它，而不是继续增加抽象层。

实际实施与上述方案一致：

- 注入处理器直接判断 `minecraft.isLocalServer()`，并在 `server == null || !server.isReady() || !server.isRunning()` 时取消当帧渲染。
- 已删除 `NorthstarStartupGuard.java` 与原纯布尔测试；源码和成品均不再引用该类。
- Mixin JSON 保持 `SmithingMenuMixin` 与 `NorthstarPlanetRendererMixin` 两个真实 Mixin，没有普通类登记。
- `./gradlew --no-daemon clean test jar` 成功，5 项测试、0 失败、0 错误。
- `javap -p -c` 确认 Northstar Mixin 只有一个 `private static` 注入处理器，字节码直接调用原版 `Minecraft` / `IntegratedServer` 状态，不再调用后羿之弓辅助类。
- 生成文件：`build/libs/houyis-bow-1.0.4.jar`。
- SHA-256：`11172e5652f22d7e04a862e128f91d58d87a3a463296f4befd4bc5587a380395`。
- JAR 内 `neoforge.mods.toml`、Manifest 与文件名均为 `1.0.4`。
- 部署当时尚未在完整目标实例运行，因此当时不能标为 BG-004 已确认解决；后续运行反馈见下一节。

### 完整实例反馈

- 2026-08-15 用户确认 1.0.4 已加载并进入游戏，没有再报告“保存世界”崩溃。
- 因此 BG-004 获得运行时修复证据；新的“没有任何内容”是独立的 BG-005，不应回退 Northstar 或配方修复。

---

## BG-005：注册内容存在，但游戏界面完全不可见

### 现象与证据

- 报告时间：2026-08-15。
- 用户确认 1.0.4 已被游戏加载，但创造模式物品栏、搜索等界面看不到任何新物品。
- 构建 JAR 内已核实存在：`ModItems`、`HouyisBowItem`、物品模型、四张拉弓贴图、语言文件、锻造配方、箭实体与粒子资源。
- `HouYisBow` 构造器已把物品、实体、粒子和配方 serializer 的 `DeferredRegister` 注册到 Mod 事件总线；若该共同入口完全没有运行，游戏加载阶段还会连带出现实体/客户端注册错误，而不是单纯“界面为空”。
- 全项目没有 `BuildCreativeModeTabContentsEvent`、`CreativeModeTabs`、`event.accept(ModItems.HOUYIS_BOW)` 或自定义创造标签页。

### 根因

NeoForge 的 `DeferredRegister.Items` 只负责把物品加入注册表，不会自动把它放进创造模式物品栏。创造标签页及其搜索内容由 `BuildCreativeModeTabContentsEvent` 单独构建；当前后羿之弓没有参加该事件，所以注册物品在创造模式和依赖创造内容构建可见列表的界面中表现为“完全不存在”。

锻造配方 JSON、serializer 和结果物品都在 JAR 中，因此这不是资源漏打包。配方要求同时放入 3 个下界合金升级模板、1 把普通弓和 12 个下界合金锭；界面不可见也不等于注册表不存在。

### 无修改验证分界

- 在启用作弊的世界执行：`/give @s houyis_bow:houyis_bow`。
- 如果命令成功：直接证明注册表正常，BG-005 就是创造标签页遗漏。
- 如果提示未知物品：说明实际运行 JAR 的公共 `@Mod` 入口没有完成注册，需要获取该次 `latest.log`，不能只修创造标签页。

后续用户已确认锻造能够正常产出后羿之弓。这比 `/give` 更强地证明了物品注册、配方 serializer 和结果物品均正常；BG-005 可以正式锁定为创造标签页遗漏，无需再怀疑公共注册入口。

### 1.0.5 修复实现

1. 已在现有 `HouYisBow(IEventBus modEventBus)` 中监听 `BuildCreativeModeTabContentsEvent`。
2. 当 `event.getTabKey() == CreativeModeTabs.COMBAT` 时执行 `event.accept(ModItems.HOUYIS_BOW)`，同时进入战斗标签页和搜索标签页。
3. 不创建只有一个物品的自定义标签页，不新增配置或兼容层。
4. 保留 1.0.4 的注册、配方和崩溃修复，不修改任何其它 Mod 或存档。
5. 版本已升为 1.0.5；本地构建与 JAR 核验结果记录在本文件末尾，完整实例表现仍待用户运行验证。

---

## BG-006：JEI 不显示可正常使用的自定义锻造配方

### 证据与根因

- 用户确认 3 个下界合金升级模板、普通弓和 12 个下界合金锭可以正常锻造成后羿之弓，说明 Minecraft/NeoForge 配方加载与 SmithingMenu 扣料逻辑正常。
- JEI 19.39 的原版锻造分类会先为配方寻找 `ISmithingCategoryExtension`；找不到扩展时 `isHandled` 返回 `false`，该配方会从展示列表中过滤。
- JEI 默认只为 `SmithingTransformRecipe` 与 `SmithingTrimRecipe` 注册扩展；其 NeoForge `RecipeHelper` 对其它 `SmithingRecipe` 返回 `Ingredient.EMPTY`。
- 后羿之弓使用独立的 `HouyisBowSmithingRecipe`，项目当前没有 `@JeiPlugin` 或 `registerVanillaCategoryExtensions`，所以“能合成但 JEI 不显示”是确定行为，不是 JEI 缓存或配方 JSON 损坏。
- 上游依据：[JEI 19.39 SmithingRecipeCategory](https://github.com/mezz/JustEnoughItems/blob/v19.39.0/Library/src/main/java/mezz/jei/library/plugins/vanilla/anvil/SmithingRecipeCategory.java)、[NeoForge RecipeHelper](https://github.com/mezz/JustEnoughItems/blob/v19.39.0/NeoForge/src/main/java/mezz/jei/neoforge/platform/RecipeHelper.java)、[官方锻造扩展 API](https://github.com/mezz/JustEnoughItems/blob/v19.39.0/CommonApi/src/main/java/mezz/jei/api/recipe/category/extensions/vanilla/smithing/IExtendableSmithingRecipeCategory.java)。

### 1.0.5 修复实现

1. 已通过 JEI 官方 Maven 仅以 `compileOnly` 引入与实例一致的 `jei-1.21.1-neoforge-api:19.39.0.372`，不把 JEI 打进后羿之弓，也不把它变成服务端必需依赖。
2. 已新增 `HouYisBowJeiPlugin`，在 `registerVanillaCategoryExtensions` 中只为 `HouyisBowSmithingRecipe` 注册原版锻造分类扩展。
3. 四个槽位明确展示：模板 ×3、普通弓 ×1、下界合金锭 ×12、后羿之弓 ×1；继续复用 JEI 原版锻造页面，没有创建自定义配方分类。
4. 插件类没有被后羿之弓主入口引用；未安装 JEI 时不会进入该可选集成，核心注册、锻造与射箭保持独立。

---

## BG-007：金色声波被原版青色贴图乘成绿色

### 截图与代码证据

- 用户截图中的光圈在亮青、亮绿和深绿之间交替，几何与原版监守者声波一致，但颜色并非要求的四种金色。
- 代码确实调用了 `setColor`，四组目标值也分别是浅金、`#FDA901` 附近的金、琥珀和深金；构建后字节码同样包含这些数值，因此不是客户端注册遗漏。
- 原版 `sonic_boom_8.png` 的非透明像素为固定青色 RGB `(44, 227, 235)`，即 `#2CE3EB`；其它帧同属青色声波素材。
- 原版 `SingleQuadParticle` 会把贴图 RGB 与粒子的 `rCol/gCol/bCol` 相乘。以目标金色约 `(253,169,1)` 计算，结果约为 `(44,150,1)`，即深绿色；截图与该乘法结果吻合。

### 根因与排除项

`setColor` 是乘色，不是把贴图替换成目标颜色。青色贴图的红通道只有 44，任何不超过 1.0 的金色顶点值都不可能把红通道提升为金色所需的高值。继续微调四组 RGB、怀疑粒子 provider 或把颜色写得更红都无法从根本解决。

### 1.0.5 修复实现

1. 已保留原版声波的 16 帧几何、透明度和动画节奏，只把每个像素的 RGB 转成白色并保留原 alpha，资源放在 `houyis_bow` 命名空间。
2. 四个粒子 JSON 共用同一套 16 帧中性遮罩；继续使用现有四组金色 `setColor`，没有复制成 64 张重复贴图。
3. 保留原版 `SonicBoomParticle` 几何、尺寸、16 tick 生命周期和声音设置；未引入自定义 shader，避免扩大 Iris/Veil/Sable 渲染冲突面。
4. 构建后检查 JAR 包含 16 张中性声波帧，且四个粒子 JSON 不再引用 `minecraft:sonic_boom_*`；结果记录在本文件末尾。

---

## BG-008：连续快速射击后的粒子与落地箭堆积

### 玩家现象

- 多次连续射击后出现越来越明显的卡顿。
- 问题在快速装填和高等级多重箭下更容易放大。

### 代码证据

- 1.0.5 中每一支 `HouyisArrowEntity` 每 tick 最多调用 12 次 `ServerLevel.sendParticles`。
- 后羿弓最低满蓄力时间为 2 tick，理论上每秒可完成约 10 次满蓄力射击。
- 每次射击基础 3 支、最高 12 支箭，因此拖尾发送会随箭数线性增加。
- 满蓄力射击还固定生成 21 个金色声波步进粒子；这部分是用户要求保留的核心视觉，没有在本次削减。
- 原版落地箭约 1200 tick 后才清理；高速连续射击会让大量已停止拖尾的箭实体继续留在世界中参与实体管理。
- 满蓄力箭仍带原版暴击状态，客户端暴击粒子也会产生额外渲染成本，但不属于服务端逐粒子网络包。

### 推理与取舍

性能压力不是单一粒子颜色或声波贴图造成，而是“射速 × 单次箭数 × 每箭拖尾”与长时间落地实体共同放大。用户要求保留第一支箭的完整拖尾和金色声波，因此最小根因修复是让同一次射击只产生一条拖尾，并缩短专用箭的落地存活时间。

没有采用以下扩大方案：

- 不新增客户端专用批量粒子协议；现有粒子负载在单拖尾后已固定，不需要新网络结构。
- 不新增配置界面；5 秒和单拖尾是本次明确规则。
- 不降低第一支箭的拖尾密度，不删除金色声波，也不取消暴击判定。
- 不修改普通原版箭、其它 Mod 粒子或客户端粒子总上限。

### 1.0.6 修复实现

1. 在现有 `HouyisArrowEntity` 上复用已持久化的 `trailStopped`，新增最小 `disableTrail()` 入口。
2. `HouyisBowItem.shoot` 按实际成功创建的箭计数；第一支保留拖尾，第二支及以后立即关闭拖尾。
3. 使用实际 `fired` 计数而非原列表索引，避免空弹药项让第一支真实箭错误失去拖尾。
4. 覆写专用箭的 `tickDespawn()`，复用原版受保护字段 `inGroundTime`，在 100 tick 时清理。
5. 不新增实体数据字段、NBT 计时器、网络载荷、依赖或兼容层。

### 验证边界

- 源码实现已完成。
- 6 项测试强制重跑通过；clean 构建、版本元数据、字节码和 JAR 内容检查通过。
- 本地 JAR SHA-256：`00b42f69ffe755fb57a895ffec917ba0613e885384eef9aea5645de46bd3b29a`。
- Windows 已删除 1.0.5 并上传唯一活动的 1.0.6；远端哈希与本地一致。
- Rainplay 服务端已备份 1.0.5，上传 1.0.6 并通过服务器回读哈希验证；尚待用户重启服务端。
- 完整实例 30 秒连续射击测试待用户重启后完成。
- 用户实测时应确认：每次射击只有一条火箭拖尾；3 支和 12 支箭仍全部命中；落地方块的后羿箭约 5 秒消失；金色声波、声音、伤害、耐久和附魔没有回归。

## 构建、测试与部署记录

### 1.0.3 本地验证

- 命令：`./gradlew --no-daemon clean test jar`。
- 结果：构建成功，6 项单元测试全部通过。
- JAR：`build/libs/houyis-bow-1.0.3.jar`。
- SHA-256：`87719eadf99cd9636f1b24517054915015460540cb6a3fe1bbc899bb27fa1715`。
- JAR 内部 `neoforge.mods.toml`、Manifest 与 Gradle 版本均为 `1.0.3`。

### 曾尝试但未保留的测试路径

- 曾为 BG-002 添加纯 JUnit 的配方 codec 测试。
- 该测试加载 `Recipe` 接口时需要完整 Minecraft/NeoForge 注册表；在普通 JUnit 进程中引导原版注册表又因缺少 NeoForge `LoadingModList` 失败。
- 这不是游戏运行时错误。没有为了一个静态 codec 断言而引入不真实的完整游戏启动测试框架；该测试已移除，改以成功构建、字节码核验和完整实例运行测试作为验证链。

### Windows 部署

- 已将 `houyis-bow-1.0.3.jar` 上传到目标实例的 `mods` 目录，并校验远端 SHA-256 与本地一致。
- 原活动包 `houyis-bow-1.0.2.jar` 已改名为 `houyis-bow-1.0.2.jar.disabled`；`houyis-bow-1.0.1.jar.disabled` 也保留。
- 未传输源码，未改动任何其它 Mod、配置或存档。

### 1.0.4 Windows 部署

- 上传前确认目标 `mods` 目录存在、1.0.3 是唯一活动包，且 Windows 没有运行中的 Java/Minecraft 进程。
- 先以非 JAR 临时文件上传，远端 SHA-256 验证通过后再切换；若切换失败会回滚 1.0.3。
- 已将 1.0.3 改名为 `houyis-bow-1.0.3.jar.disabled`，并启用唯一的新包 `houyis-bow-1.0.4.jar`。
- 远端 SHA-256：`11172E5652F22D7E04A862E128F91D58D87A3A463296F4BEFD4BC5587A380395`，与本地一致。
- 远端活动后羿之弓 JAR 数量为 1，临时上传文件数量为 0；1.0.1～1.0.3 均保留为禁用备份。
- 未传输源码，未改动其它 Mod、配置、存档、地图或路径点，也未远程启动游戏。

### 1.0.5 Windows 部署

- 用户已明确要求替换目标实例中的旧版本。
- 上传前确认目标实例当前唯一活动包为 `houyis-bow-1.0.4.jar`，但 Minecraft `javaw.exe` 正在运行。
- 已将 1.0.5 作为 `houyis-bow-1.0.5.jar.uploading` 传入同一 `mods` 目录；远端 SHA-256 为 `5E9C46ACE949BC983DD3BADE9B60CA21EE396A19E87D260983BAF58EF1E31E32`，与本地产物一致。
- 为避免运行期间替换活动 Mod，首次检查时没有改名 1.0.4、没有启用 1.0.5，也没有终止用户游戏进程。
- 用户确认正常退出 Minecraft 后，检测到 Java 进程数为 0；随后将 1.0.4 改名为 `.jar.disabled` 备份，并把已校验临时文件切换为 `houyis-bow-1.0.5.jar`。
- 切换后远端活动后羿之弓 JAR 数量为 1，活动文件为 1.0.5，临时上传文件数量为 0；远端 SHA-256 再次校验一致。
- 未修改其它 Mod、配置、存档、地图或路径点，也未远程启动游戏。战斗页、JEI 配方和金色声波仍由用户进行完整实例验证。

## 1.0.5 本地实施记录

- BG-005：新增 `BuildCreativeModeTabContentsEvent` 监听，只把后羿之弓加入原版“战斗”标签页。
- BG-006：新增一个可选 JEI 插件并复用原版锻造分类；依赖为 `compileOnly`，不随 JAR 打包。
- BG-007：生成 16 张白色 RGB / 原 alpha 的声波蒙版，四种粒子共用这套帧，不新增 shader。
- 第一次蒙版转换直接在原版索引色 `BufferedImage` 上调用 `setRGB`。新增资源测试立即失败：期望白色，实际仍为 `#2CE3EB`。原因是索引色图像只能使用旧调色板，写入白色时又被映射回最接近的青色；这条尝试未保留在最终产物中。
- 最终改为新建 `TYPE_INT_ARGB` 图像，只复制原帧 alpha，并把 RGB 写为白色。逐像素核验结果：alpha 差异 0、可见非白像素 0、可见像素 1513。
- `./gradlew --no-daemon clean test jar` 成功，6 项测试全部通过；其中新增测试逐帧验证 16 张蒙版存在、可见且 RGB 为白色。
- JAR：`build/libs/houyis-bow-1.0.5.jar`；SHA-256：`5e9c46ace949bc983dd3bade9b60ca21ee396a19e87d260983baf58ef1e31e32`。
- JAR 核验：16 张蒙版；四个粒子 JSON 共 64 次引用全部指向 `houyis_bow:sonic_boom_mask_*`；原版青色引用为 0；JEI 插件类存在，但 `mezz/jei` 类打包数量为 0。
- `neoforge.mods.toml`、Manifest 与文件名版本均为 1.0.5；`git diff --check` 通过。
- 验证边界：本地完成编译、单测、资源、字节码和打包检查，且已上传 Windows 并完成远端哈希验证；尚未在包含 JEI 的完整实例进行界面与粒子实测。

## 下一次故障的续修清单

1. 完整实例必须验证战斗页、创造搜索、JEI 配方及 3/1/12 数量、实际锻造、四种金色声波和射箭；不能只验证 Mod 已加载。
2. 如果再次出现 `IllegalClassLoadError`：先确认目标实例只有新的活动 JAR，并核对异常具体类及 JAR 内容，不改其它 Mod。
3. 如果仍出现 `NorthstarConfigs` / `ConfigValue`：必须以完整日志确认它是否为第一异常，并核对当时集成服务器的 ready/running 状态；不凭末端 crash report 继续扩充守卫。
4. 每次修复前后都追加本文件的“证据 → 推理 → 方案 → 结果 → 验证状态”，并同步更新三份 `PROGRESS` 文档中对应的开发、操作和功能板块。
