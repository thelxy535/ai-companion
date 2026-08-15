# 自定义角色统一数据流与记忆胶囊设计

**日期：** 2026-08-15  
**项目：** `D:\CC-Switch\cc-native-android`  
**来源会话：** `1bb397b3-d2bc-4568-af82-e627febf3c1f.jsonl`

## 1. 背景与目标

当前自定义角色已经可以写入 Room，但角色创建页、首页、聊天页和人格系统没有使用同一条数据链路：

- 创建/编辑 ViewModel 默认使用 `userId = "default"`，首页使用 `SettingsManager.userIdFlow` 的真实用户 ID，导致角色保存成功却无法出现在首页。
- 保存完成后页面通过 `isLoading` 间接推断是否导航，快速保存时可能错过状态变化，用户会反复点击并产生重复更新。
- 聊天页只加载自定义角色名称；人格设定仍依赖 `PersonalityManager` 的进程内 Map，重启后会回退到默认人格。
- 删除当前只删除角色行和内存注册，没有完整处理消息、记忆、情绪、标签关系、头像和缓存。

本阶段目标是把自定义角色变成真正的独立聊天对象：

1. 所有入口使用统一的真实用户身份和角色目录。
2. 创建、编辑、首页、导航和聊天使用同一份持久化数据。
3. 自定义人格从 Room 持久化配置解析，重启后仍然生效。
4. 每个角色的消息、记忆和情绪数据相互隔离。
5. 删除采用“彻底删除 + 可选记忆胶囊”流程。
6. 胶囊可在删除后保留、导出并恢复为新的角色实例。
7. 先测试后实现，并完成真实设备端到端验证。

本阶段不处理完整主题迁移和历史 v7→当前版本的数据迁移安全重构；两者在本阶段通过后作为后续阶段单独设计和实施。记忆胶囊需要新增 Room 表，因此本阶段允许增加一个**只创建新表、不重建或删除现有表**的独立前向迁移，并为该迁移补充测试；它不能使用破坏性迁移来掩盖失败。

## 2. 已确认的产品决策

### 2.1 角色模型

不把内置角色迁移到 Room。内置角色继续来自现有配置，自定义角色继续来自 Room；在领域层用统一的 `ChatCharacter` 表示两者。

### 2.2 角色标识

- 内置角色继续使用 `xiaocan`、`muse`。
- 自定义角色继续使用随机 UUID。
- 消息表已有 `companionId`，不修改其基本语义。
- 恢复胶囊时必须生成新的角色 UUID，不能复用已删除的旧 ID。

### 2.3 删除策略

用户选择“彻底删除”。删除确认后，原角色运行数据必须消失；但删除前可以生成并保留一个记忆胶囊。胶囊不是可运行角色，不会让已删除角色继续出现在首页或聊天列表。

### 2.4 记忆胶囊保存方式

胶囊保存在 App 内，并允许用户主动导出。胶囊默认保存角色设定、关系摘要和精选回忆，不默认复制完整聊天记录。应用内胶囊使用现有加密能力；导出时明确提示文件包含用户内容。

## 3. 领域架构

### 3.1 `CurrentUserProvider`

提供唯一的当前用户身份来源：

```kotlin
interface CurrentUserProvider {
    val userId: Flow<String>
    suspend fun requireUserId(): String
}
```

实现封装 `SettingsManager.userIdFlow`。所有角色相关 ViewModel、Repository 用例和目录查询依赖它，不再让 UI 传入或默认构造用户 ID。

现有 `userId == "default"` 的自定义角色需要提供一次安全归属迁移：当当前用户拥有该旧记录且没有冲突时，将其归属迁移到真实用户 ID；迁移失败时保留原记录并报告错误，不能静默删除。

### 3.2 `ChatCharacter`

```kotlin
sealed interface ChatCharacter {
    val id: String
    val name: String
    val avatar: String?
    val greeting: String
    val isCustom: Boolean
}
```

内置角色和自定义角色都转换为该模型。角色目录可以附加最新消息预览、最后活跃时间和在线状态，但这些展示字段不能改变角色的持久化身份。

### 3.3 `CharacterCatalog`

```kotlin
interface CharacterCatalog {
    fun observeCharacters(): Flow<List<ChatCharacter>>
    suspend fun getCharacter(id: String): ChatCharacter?
}
```

职责：

- 合并内置配置和当前用户的自定义角色。
- 统一映射名称、头像、欢迎语和类型。
- 根据真实用户 ID 查询自定义角色。
- 提供首页和角色管理页共同订阅的 Flow。
- 让修改、删除和重新打开 App 后的 UI 更新由 Room Flow 驱动，而不是页面手动刷新。

首页不再分别查询 Muse、小璨和自定义角色；它只订阅 `CharacterCatalog.observeCharacters()`，再结合消息摘要展示列表。

### 3.4 `CharacterPromptResolver`

```kotlin
interface CharacterPromptResolver {
    suspend fun resolve(characterId: String): ResolvedCharacterPrompt
}
```

解析规则：

- 内置角色使用现有 `CompanionConfigLoader`。
- 自定义角色通过 `CustomCharacterRepository.getCharacterById()` 读取最新 Room 数据。
- 自定义配置转换为现有 `CompanionConfig` 或等价的 Prompt 输入结构。
- 包含名称、描述、背景、Big Five 人格、行为规则、示例对话和欢迎语。
- 生成 Prompt 前再次读取配置，避免聊天 ViewModel 持有陈旧内存副本。
- `PersonalityManager` 可以保留内置配置能力，但不能再把内存 Map 作为自定义角色的唯一事实来源。

创建或编辑时可以更新缓存，但缓存只能优化读取，不能成为持久化来源。应用重启后不需要依赖先前的注册顺序。

## 4. 创建、编辑与导航流程

### 4.1 保存状态

使用明确的保存状态和一次性成功事件：

```kotlin
sealed interface CharacterSaveState {
    data object Idle : CharacterSaveState
    data object Saving : CharacterSaveState
    data class Success(val characterId: String) : CharacterSaveState
    data class Failure(val message: String) : CharacterSaveState
}
```

流程：

1. 页面提交表单。
2. ViewModel 从 `CurrentUserProvider` 获取真实用户 ID。
3. Repository 创建或更新角色。
4. 成功后发出一次 `Success(characterId)`。
5. 页面只消费一次成功事件并返回。
6. `CharacterCatalog` 的 Flow 自动更新首页和列表。

保存按钮在 `Saving` 状态禁用。重复点击不能产生多次数据库写入。失败时保留表单内容、停留在编辑页并显示错误。

### 4.2 创建

创建成功后回到主界面，首页立即出现新角色。新角色使用 UUID 作为聊天分区键。

### 4.3 编辑

编辑页通过角色 ID读取当前数据。成功更新后返回上一层，首页、角色列表和下一次聊天 Prompt 都从同一份 Room 数据得到新值。

### 4.4 聊天导航

导航只传角色 ID：

```text
home → chat/{characterId}
character_list → chat/{characterId}
```

聊天页通过 `CharacterCatalog.getCharacter(id)` 判断角色类型，不再依靠由多个页面分别传递的 `isCustomCharacter` 布尔值。角色不存在时显示失效状态并返回角色列表，不能静默回退到小璨。

## 5. 数据隔离与删除

### 5.1 角色范围

所有角色相关查询必须使用当前用户和角色 ID作为边界。逐项核对：

- `messages(userId, companionId)`
- 记忆和向量记忆
- 情绪、关系和交互时间
- 事件和标签关联
- 草稿、头像和内存缓存
- 统计、收藏和数据管理入口

自定义角色 UUID 不能进入内置角色的默认回退路径。

### 5.2 `CharacterDeletionService`

```kotlin
interface CharacterDeletionService {
    suspend fun previewCapsule(
        characterId: String,
        selection: CapsuleSelection,
    ): Result<CharacterMemoryCapsulePreview>

    suspend fun deletePermanently(
        characterId: String,
        selection: CapsuleSelection?,
    ): Result<DeletionReceipt>
}
```

`previewCapsule` 只用于告别页展示，不写入数据库。真正删除时由 `deletePermanently` 完成两个有明确边界的阶段；文件系统和 DataStore 不能伪装成 Room 原子事务的一部分。

**阶段 A：单一 Room 事务**

1. 校验角色属于当前用户。
2. 根据用户选择生成并保存最终胶囊（未选择保留胶囊时跳过）。
3. 删除消息标签关联及该角色消息。
4. 删除 Room 内的向量记忆、情绪/关系状态、交互时间和事件。
5. 删除角色实体。
6. 写入一条持久化的 `CharacterCleanupTask`，记录仍需清理的头像、草稿和进程内缓存键。
7. 提交事务并返回删除收据。

胶囊保存或任一 Room 删除失败时，整个事务回滚，原角色继续可用。

**阶段 B：幂等外部清理**

Room 提交后，清理头像文件、DataStore 偏好、草稿和进程内缓存，然后将 `CharacterCleanupTask` 标为完成。外部清理失败时不虚构数据库回滚；保留任务并在应用启动或后台重试，直到清理完成。已删除角色不会因为清理重试重新出现在首页。

不可恢复删除必须保留确认对话框。

## 6. 记忆胶囊与复活

### 6.1 胶囊内容

新增 `CharacterMemoryCapsuleEntity` 及 DAO/Repository，至少包含：

- 胶囊 ID和版本号
- 随机复活 Token
- 原角色显示信息和人格快照
- 关系摘要
- 用户选择的精选回忆
- 互动统计
- 创建、删除、导出和恢复时间
- 胶囊状态（可恢复、已恢复、已归档）

原始角色 ID只作为历史来源标记，恢复时生成新的角色 UUID。

### 6.2 告别页

删除前展示角色摘要和回忆预览。用户可以选择：

- 保留角色设定
- 保留关系摘要
- 保留精选记忆
- 导出胶囊
- 生成复活凭证

默认不复制全部聊天历史。导出操作明确提示内容可能包含个人信息。

### 6.3 恢复

恢复胶囊提供两种主要模式：

- **重新认识**：恢复角色设定和关系摘要，开始全新聊天。
- **带着回忆回来**：在新角色下恢复用户选择的记忆片段。

恢复不能自动把旧聊天历史混入新角色。完整历史如需支持，必须作为另一个明确的导入动作，并仍使用新的角色 UUID。

无效、已使用或损坏的 Token 不得创建半成品角色；恢复失败时不改变原胶囊。

## 7. 错误处理

- 无法获取当前用户 ID：阻止创建、修改和删除，显示明确错误。
- 角色查询为空：聊天页显示“角色不存在”，不回退到默认角色。
- 保存失败：不导航、不清空表单。
- 胶囊保存失败：阻止删除。
- 删除事务失败：整体回滚。
- 恢复 Token 无效：不写入角色表。
- 数据归属迁移失败：保留旧记录并记录可诊断日志。
- Room Flow异常：页面显示可重试状态，不构造空的“成功”角色。

## 8. 测试与验收

### 8.1 单元测试

1. `CurrentUserProvider` 始终返回 SettingsManager 的真实 ID。
2. 旧 `default` 角色归属迁移成功和冲突失败场景。
3. 角色目录合并内置与自定义角色并保持唯一 ID。
4. 创建角色后目录 Flow 发出角色。
5. 修改角色后名称、头像和 Prompt 配置同步。
6. 快速连续保存只执行一次更新。
7. 重启后 Prompt Resolver 仍能读取自定义人格。
8. 两个 UUID 角色的消息、记忆和情绪状态互相隔离。
9. 胶囊在删除前正确保存所选内容。
10. 删除失败时事务回滚。
11. 删除成功后所有运行数据消失。
12. 恢复胶囊生成新 UUID并保留隔离。

### 8.2 设备端到端验收

```text
创建角色
→ 首页显示
→ 进入聊天
→ 发送消息并验证自定义人格
→ 返回并再次进入
→ 修改名称/人格
→ 验证首页和聊天同步
→ 重启 App
→ 验证角色仍然存在
→ 删除前选择回忆
→ 生成胶囊并导出
→ 确认彻底删除
→ 验证所有入口消失
→ 用胶囊恢复新角色
→ 验证新角色拥有独立聊天历史
```

### 8.3 阶段完成条件

- 编译和现有单元测试通过。
- 新增角色数据流和删除/恢复测试全部通过。
- 设备端端到端流程完成并留有日志证据。
- 未修改应用版本号。
- 主题系统和 Room 迁移安全仍作为后续阶段，不得用本阶段结果代替它们的验收。

## 9. 后续阶段

### 阶段二：语义主题

- 将 `LocalSemanticColors` 真正注入 `CCTheme`。
- 迁移聊天、角色、统计、数据管理、设置和导航栏的硬编码颜色。
- 统一暗色/浅色层级和系统栏。

### 阶段三：数据库迁移安全

- 移除 `fallbackToDestructiveMigration()`。
- 补充 v7→v10 schema 与数据保留测试。
- 做覆盖安装前后消息、收藏、图片和标签的计数与样本哈希比对。

完成阶段一并验证后，才进入阶段二；完成阶段二后再处理阶段三。
