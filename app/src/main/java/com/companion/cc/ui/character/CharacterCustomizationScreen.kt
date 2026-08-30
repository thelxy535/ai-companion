package com.companion.cc.ui.character

import com.companion.cc.ui.designsystem.auroraScreenBackground
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.theme.TactileGesture
import com.companion.cc.ui.theme.rememberTactileAction
import androidx.compose.foundation.layout.padding

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
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃未保存修改？") },
            text = { Text("离开后，当前修改将不会保存。") },
            confirmButton = {
                TextButton(onClick = onNavigateBack) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("继续编辑") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 44.dp),
                title = { Text(if (characterId != null) "编辑角色" else "创建角色") },
                navigationIcon = {
                    IconButton(onClick = goBack, enabled = navigationEnabled) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 上一步按钮
                if (currentStep.ordinal > 0) {
                    OutlinedButton(
                        onClick = previousStep,
                        enabled = navigationEnabled
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("上一步")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // 下一步/完成按钮
                Button(
                    onClick = nextStep,
                    enabled = when (currentStep) {
                        CustomizationStep.BASIC_INFO -> isFormValid && saveState !is CharacterSaveState.Saving
                        CustomizationStep.EXAMPLES -> saveState !is CharacterSaveState.Saving
                        else -> saveState !is CharacterSaveState.Saving
                    }
                ) {
                    if (saveState is CharacterSaveState.Saving && currentStep == CustomizationStep.EXAMPLES) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (currentStep == CustomizationStep.EXAMPLES) "完成" else "下一步")
                        if (currentStep != CustomizationStep.EXAMPLES) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
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

        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("角色名称 *") },
            placeholder = { Text("给你的角色起个名字") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        OutlinedTextField(
            value = description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text("简短描述 *") },
            placeholder = { Text("一句话介绍这个角色") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        OutlinedTextField(
            value = backstory,
            onValueChange = { viewModel.updateBackstory(it) },
            label = { Text("背景故事 *") },
            placeholder = { Text("详细描述角色的背景、经历、性格等...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            maxLines = 10,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        OutlinedTextField(
            value = greetingMessage,
            onValueChange = { viewModel.updateGreetingMessage(it) },
            label = { Text("问候语") },
            placeholder = { Text("角色见到你时的第一句话") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
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
                }
        )
    }
}
