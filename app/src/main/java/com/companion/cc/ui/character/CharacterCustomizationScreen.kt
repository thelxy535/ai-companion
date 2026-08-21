package com.companion.cc.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.theme.GlassSurface

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
    var currentStep by remember { mutableStateOf(CustomizationStep.BASIC_INFO) }

    // 实时获取表单字段以验证
    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val backstory by viewModel.backstory.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val isFormValid = name.isNotBlank() && description.isNotBlank() && backstory.isNotBlank()

    LaunchedEffect(characterId) {
        if (characterId != null) {
            viewModel.loadCharacterForEdit(characterId)
        }
    }

    // 监听保存成功后返回
    LaunchedEffect(saveState) {
        if (saveState is CharacterSaveState.Success) {
            viewModel.consumeSaveResult()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                useStrongFill = true
            ) {
                TopAppBar(
                title = { Text(if (characterId != null) "编辑角色" else "创建角色") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            }
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

            Divider()

            // 导航按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 上一步按钮
                if (currentStep.ordinal > 0) {
                    OutlinedButton(
                        onClick = {
                            currentStep = CustomizationStep.values()[currentStep.ordinal - 1]
                        }
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
                    onClick = {
                        if (currentStep == CustomizationStep.EXAMPLES) {
                            viewModel.saveCharacter()
                        } else {
                            currentStep = CustomizationStep.values()[currentStep.ordinal + 1]
                        }
                    },
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            // 步骤圆圈
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            when {
                                index < currentStep -> MaterialTheme.colorScheme.primary
                                index == currentStep -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                ) {
                    if (index < currentStep) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                index == currentStep -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        index == currentStep -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // 连接线
            if (index < steps.size - 1) {
                Divider(
                    modifier = Modifier
                        .width(24.dp)
                        .padding(bottom = 32.dp),
                    color = if (index < currentStep) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
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
            modifier = Modifier.fillMaxWidth()
        )
    }
}
