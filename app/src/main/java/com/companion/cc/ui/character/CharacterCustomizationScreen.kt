package com.companion.cc.ui.character

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.components.V9PMActionButton
import com.companion.cc.ui.components.V9PMDialogSurface
import com.companion.cc.ui.components.V9PMTextField
import com.companion.cc.ui.components.V9PMTopBar
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.theme.TactileGesture
import com.companion.cc.ui.theme.rememberTactileAction
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme

/**
 * 自定义步骤枚举
 */
enum class CustomizationStep(val title: String) {
    BASIC_INFO("基本信息"),
    PERSONALITY("人格设定"),
    BEHAVIOR("行为风格"),
    EXAMPLES("示例对话")
}

/**
 * 角色自定义主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCustomizationScreen(
    characterId: String? = null,
    onNavigateBack: () -> Unit,
    onTestChat: () -> Unit = {},
    viewModel: CharacterCustomizationViewModel = hiltViewModel()
) {
    var currentStep by rememberSaveable { mutableStateOf(CustomizationStep.BASIC_INFO) }

    // 实时获取表单字段以验证
    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val backstory by viewModel.backstory.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val isSaving = saveState is CharacterSaveState.Saving
    val navigationEnabled = canNavigateWhileSaving(isSaving)
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val isFormValid = name.isNotBlank() && description.isNotBlank() && backstory.isNotBlank()
    val requestBack = {
        if (shouldConfirmDiscard(viewModel.hasUnsavedChanges(), isSaving)) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }
    val goBack = rememberTactileAction(enabled = navigationEnabled) { requestBack() }
    val previousStep = rememberTactileAction(enabled = navigationEnabled) {
        currentStep = CustomizationStep.values()[currentStep.ordinal - 1]
    }
    val nextStep = rememberTactileAction(enabled = !isSaving) {
        if (currentStep == CustomizationStep.EXAMPLES) {
            viewModel.saveCharacter()
        } else {
            currentStep = CustomizationStep.values()[currentStep.ordinal + 1]
        }
    }

    LaunchedEffect(characterId) {
        if (characterId != null) {
            viewModel.loadCharacterForEdit(characterId)
        } else {
            viewModel.startNewCharacter()
        }
    }

    // 监听保存成功后返回
    LaunchedEffect(saveState) {
        if (saveState is CharacterSaveState.Success) {
            viewModel.consumeSaveResult()
            onNavigateBack()
        }
    }

    BackHandler(enabled = navigationEnabled) {
        requestBack()
    }

    if (showDiscardDialog) {
        V9PMDialogSurface(onDismissRequest = { showDiscardDialog = false }) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("放弃未保存修改？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("离开后，当前修改将不会保存。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    V9PMActionButton(
                        label = "继续编辑",
                        onClick = { showDiscardDialog = false },
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    V9PMActionButton(
                        label = "放弃",
                        onClick = onNavigateBack,
                        destructive = true,
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            V9PMTopBar(
                title = { Text(if (characterId != null) "编辑角色" else "创建角色", color = MaterialTheme.colorScheme.onSurface) },
                onNavigateBack = goBack,
                actions = emptyList(),
                modifier = Modifier.padding(top = 44.dp)
            )
        },
        containerColor = Color.Transparent,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 步骤指示器
            StepIndicator(
                steps = CustomizationStep.values().map { it.title },
                currentStep = currentStep.ordinal,
                onStepClick = { index ->
                    currentStep = CustomizationStep.values()[index]
                },
                modifier = Modifier.padding(16.dp)
            )

            Divider()

            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    CustomizationStep.BASIC_INFO -> BasicInfoStep(viewModel)
                    CustomizationStep.PERSONALITY -> PersonalityStep(viewModel)
                    CustomizationStep.BEHAVIOR -> BehaviorStep(viewModel)
                    CustomizationStep.EXAMPLES -> ExamplesStep(viewModel)
                }
            }

            if (saveState is CharacterSaveState.Failure) {
                Text(
                    text = (saveState as CharacterSaveState.Failure).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Divider()

            // 导航按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .imePadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 上一步按钮
                if (currentStep.ordinal > 0) {
                    V9PMActionButton(
                        label = "上一步",
                        icon = Icons.Default.ArrowBack,
                        onClick = previousStep,
                        enabled = navigationEnabled,
                        modifier = Modifier.weight(1f),
                        height = 44.dp
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // V9PM：试聊——创建前测试对话（草稿不入库，可感受脾气后返回调整）
                if (currentStep == CustomizationStep.EXAMPLES) {
                    V9PMActionButton(
                        label = "试聊",
                        onClick = {
                            viewModel.buildDraft()?.let { draft ->
                                com.companion.cc.ui.character.CharacterPreviewStore.put(draft)
                                onTestChat()
                            }
                        },
                        enabled = navigationEnabled,
                        modifier = Modifier.weight(1f),
                        height = 44.dp
                    )
                }

                // 下一步/完成按钮
                V9PMActionButton(
                    label = if (currentStep == CustomizationStep.EXAMPLES) "完成" else "下一步",
                    icon = if (currentStep == CustomizationStep.EXAMPLES) null else Icons.Default.ArrowForward,
                    onClick = nextStep,
                    enabled = when (currentStep) {
                        CustomizationStep.BASIC_INFO -> isFormValid && saveState !is CharacterSaveState.Saving
                        CustomizationStep.EXAMPLES -> saveState !is CharacterSaveState.Saving
                        else -> saveState !is CharacterSaveState.Saving
                    },
                    modifier = Modifier.weight(1f),
                    height = 44.dp
                )
            }
        }
    }
}

/**
 * 步骤指示器
 */
@Composable
fun StepIndicator(
    steps: List<String>,
    currentStep: Int,
    onStepClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, title ->
            val status = when {
                index < currentStep -> "已完成"
                index == currentStep -> "当前步骤"
                else -> "未开始"
            }
            val foreground = when {
                index <= currentStep -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val stepModifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = "$title，第 ${index + 1} 步，共 ${steps.size} 步，$status"
                    if (index <= currentStep) role = Role.Button
                }
                .then(
                    if (index <= currentStep) {
                        Modifier.clickable { onStepClick(index) }
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = 4.dp)

            Column(
                modifier = stepModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (index < currentStep) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = foreground
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = foreground,
                            fontWeight = if (index == currentStep) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            }
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = foreground,
                        fontWeight = if (index == currentStep) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        maxLines = 1
                    )
                }

                Divider(
                    modifier = Modifier.width(28.dp),
                    thickness = if (index == currentStep) 2.dp else 1.dp,
                    color = when {
                        index == currentStep -> MaterialTheme.colorScheme.primary
                        index < currentStep -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                )
            }
        }
    }
}

/**
 * 基本信息步骤
 */
@Composable
fun BasicInfoStep(viewModel: CharacterCustomizationViewModel) {
    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val backstory by viewModel.backstory.collectAsState()
    val greetingMessage by viewModel.greetingMessage.collectAsState()
    val scenario by viewModel.scenario.collectAsState()
    val alternateGreetings by viewModel.alternateGreetings.collectAsState()
    val creator by viewModel.creator.collectAsState()
    val characterVersion by viewModel.characterVersion.collectAsState()
    val tags by viewModel.tags.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "让我们先从基本信息开始",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

            V9PMTextField(
                value = name,
                onValueChange = viewModel::updateName,
                label = "角色名称 *",
                placeholder = "给你的角色起个名字",
                modifier = Modifier.fillMaxWidth()
            )

            V9PMTextField(
                value = description,
                onValueChange = viewModel::updateDescription,
                label = "简短描述 *",
                placeholder = "一句话介绍这个角色",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2
            )

            V9PMTextField(
                value = backstory,
                onValueChange = viewModel::updateBackstory,
                label = "背景故事 *",
                placeholder = "详细描述角色的背景、经历、性格等...",
                modifier = Modifier.fillMaxWidth().height(200.dp),
                singleLine = false,
                maxLines = 10
            )

            V9PMTextField(
                value = greetingMessage,
                onValueChange = viewModel::updateGreetingMessage,
                label = "问候语",
                placeholder = "角色见到你时的第一句话",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2
            )

            V9PMTextField(
                value = scenario,
                onValueChange = viewModel::updateScenario,
                label = "当前场景",
                placeholder = "你们正在什么地方、处于什么情境？",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4
            )

            V9PMTextField(
                value = alternateGreetings.joinToString("\n"),
                onValueChange = { value ->
                    viewModel.updateAlternateGreetings(value.lines().map(String::trim).filter(String::isNotBlank).take(5))
                },
                label = "备用问候语（可选）",
                placeholder = "每行一条，最多 5 条",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 5
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V9PMTextField(
                    value = creator,
                    onValueChange = viewModel::updateCreator,
                    label = "作者（可选）",
                    placeholder = "你的名字或昵称",
                    modifier = Modifier.weight(1f)
                )
                V9PMTextField(
                    value = characterVersion,
                    onValueChange = viewModel::updateCharacterVersion,
                    label = "版本",
                    placeholder = "1.0",
                    modifier = Modifier.weight(1f)
                )
            }

            V9PMTextField(
                value = tags.joinToString(", "),
                onValueChange = { value ->
                    viewModel.updateTags(value.split(",", "，").map(String::trim).filter(String::isNotBlank).distinct().take(12))
                },
                label = "标签（可选）",
                placeholder = "例如：科幻、温柔、冒险",
                modifier = Modifier.fillMaxWidth()
            )

        Text(
            "* 为必填项",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 人格设定步骤
 */
@Composable
fun PersonalityStep(viewModel: CharacterCustomizationViewModel) {
    val personality by viewModel.personality.collectAsState()
    val naturalDescription by viewModel.naturalPersonalityDescription.collectAsState()
    val rhythm by viewModel.rhythm.collectAsState()
    var memoryPreferenceMenuOpen by remember { mutableStateOf(false) }
    val memoryPreferenceOptions = listOf("普通记忆先问我", "自动记住日常偏好", "只记住共同经历", "低打扰模式")
    val selectedMemoryPreference = personality.customTraits[
        com.companion.cc.domain.model.PersonalityTraits.MEMORY_PREFERENCE_KEY
    ].orEmpty().ifBlank { "普通记忆先问我" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "调整人格特质",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            "基于心理学五大人格理论，通过滑块调整角色的核心特质。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = naturalDescription,
            onValueChange = viewModel::updateNaturalPersonalityDescription,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("人格底色（用你自己的话描述）") },
            placeholder = { Text("例如：慢热，熟悉以后会主动分享小事，嘴硬但很在意对方") },
            minLines = 3,
            maxLines = 6,
            supportingText = { Text("这段话会影响表达和相处方式，不会把角色锁死成固定台词。") }
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedMemoryPreference,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { memoryPreferenceMenuOpen = true },
                readOnly = true,
                label = { Text("记忆偏好") },
                supportingText = { Text("决定这个角色整理记忆时更主动还是更克制，不会自动保存承诺或敏感内容。") },
                trailingIcon = {
                    IconButton(onClick = { memoryPreferenceMenuOpen = !memoryPreferenceMenuOpen }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "选择记忆偏好")
                    }
                }
            )
            DropdownMenu(
                expanded = memoryPreferenceMenuOpen,
                onDismissRequest = { memoryPreferenceMenuOpen = false }
            ) {
                memoryPreferenceOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.updateCustomPersonalityTrait(
                                com.companion.cc.domain.model.PersonalityTraits.MEMORY_PREFERENCE_KEY,
                                option
                            )
                            memoryPreferenceMenuOpen = false
                        }
                    )
                }
            }
        }

        listOf(
            com.companion.cc.domain.character.CharacterNuanceKeys.LANGUAGE_HABITS to "例如：偶尔省略主语，熟悉后会轻轻吐槽",
            com.companion.cc.domain.character.CharacterNuanceKeys.EXPRESSION_BOUNDARIES to "例如：不使用宝宝式称呼，不连续追问",
            com.companion.cc.domain.character.CharacterNuanceKeys.RELATIONSHIP_DISTANCE to "例如：慢慢靠近，不会刚认识就过分亲密",
            com.companion.cc.domain.character.CharacterNuanceKeys.CONFLICT_STYLE to "例如：先安静一下，被认真回应后才慢慢松动",
            com.companion.cc.domain.character.CharacterNuanceKeys.PROACTIVE_HABITS to "例如：看到有趣的小店或天气变化时会想起对方"
        ).forEach { (key, hint) ->
            OutlinedTextField(
                value = personality.customTraits[key].orEmpty(),
                onValueChange = { viewModel.updateCustomPersonalityTrait(key, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(key) },
                placeholder = { Text(hint) },
                minLines = 2,
                maxLines = 4
            )
        }

        PersonalitySlider(
            label = "开放性",
            description = "对新体验和想法的接受程度",
            value = personality.openness,
            onValueChange = { viewModel.updatePersonalityTrait("openness", it) }
        )

        PersonalitySlider(
            label = "尽责性",
            description = "负责任和自律程度",
            value = personality.conscientiousness,
            onValueChange = { viewModel.updatePersonalityTrait("conscientiousness", it) }
        )

        PersonalitySlider(
            label = "外向性",
            description = "社交活跃度和表达热情",
            value = personality.extraversion,
            onValueChange = { viewModel.updatePersonalityTrait("extraversion", it) }
        )

        PersonalitySlider(
            label = "宜人性",
            description = "友好、合作和同理心程度",
            value = personality.agreeableness,
            onValueChange = { viewModel.updatePersonalityTrait("agreeableness", it) }
        )

        PersonalitySlider(
            label = "情绪稳定性",
            description = "情绪稳定和抗压能力",
            value = 1f - personality.neuroticism,
            onValueChange = { viewModel.updatePersonalityTrait("neuroticism", 1f - it) }
        )

        Text("生活节奏与相处方式", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        PersonalitySlider("日夜节奏", if (rhythm.wakeHour >= 9f) "偏夜间活跃" else "偏日间活跃", if (rhythm.wakeHour >= 9f) 0.85f else 0.35f) {
            val night = it >= 0.6f
            viewModel.updateRhythm(rhythm.copy(wakeHour = if (night) 10f else 7f, sleepHour = if (night) 2f else 22.5f))
        }
        PersonalitySlider("社交电量", "影响主动开口的底气，不是固定提醒", rhythm.socialBattery) {
            viewModel.updateRhythm(rhythm.copy(socialBattery = it))
        }
        PersonalitySlider("记忆眷恋", "旧事停留多久，只在相关时自然想起", rhythm.memoryStickiness) {
            viewModel.updateRhythm(rhythm.copy(memoryStickiness = it))
        }
        PersonalitySlider("情绪恢复", "从疲惫或不愉快中缓过来的速度", rhythm.recoverySpeed) {
            viewModel.updateRhythm(rhythm.copy(recoverySpeed = it))
        }
    }
}

/**
 * 人格滑块组件
 */
@Composable
fun PersonalitySlider(
    label: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "$label：${(value * 100).toInt()}%"
                    stateDescription = "当前值 ${(value * 100).toInt()}%"
                },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            )
        )
    }
}
