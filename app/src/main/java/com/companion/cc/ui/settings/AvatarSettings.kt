package com.companion.cc.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.companion.cc.ui.components.Avatar
import com.companion.cc.ui.components.V9PMActionButton
import com.companion.cc.ui.components.V9PMDialogSurface
import com.companion.cc.ui.components.V9PMIconButton
import com.companion.cc.ui.theme.tactileClickable

/**
 * 头像设置对话框
 * 支持选择图片、拍照或清除头像
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSettingsDialog(
    currentAvatarUrl: String?,
    title: String,
    onDismiss: () -> Unit,
    onAvatarSelected: (Uri?) -> Unit,
    onClearAvatar: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onAvatarSelected(uri)
        }
    }

    // 拍照
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            onAvatarSelected(photoUri)
        }
    }

    fun launchCamera() {
        val file = java.io.File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        photoUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        takePictureLauncher.launch(photoUri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
    }

    fun startCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    V9PMDialogSurface(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                V9PMIconButton(Icons.Default.Close, "关闭", onDismiss, size = 42.dp, iconSize = 18.dp)
            }

            Avatar(
                avatarUrl = currentAvatarUrl,
                emoji = null,
                size = 120.dp,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                V9PMActionButton(
                    label = "相册",
                    icon = Icons.Default.Photo,
                    onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f),
                    height = 44.dp
                )
                V9PMActionButton(
                    label = "拍照",
                    icon = Icons.Default.CameraAlt,
                    onClick = ::startCamera,
                    enabled = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
                    modifier = Modifier.weight(1f),
                    height = 44.dp
                )
            }

            if (currentAvatarUrl != null) {
                V9PMActionButton(
                    label = "清除头像",
                    onClick = {
                        onClearAvatar()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    destructive = true,
                    height = 44.dp
                )
            }

            V9PMActionButton(label = "取消", onClick = onDismiss, modifier = Modifier.fillMaxWidth(), height = 40.dp)
        }
    }
}

/**
 * 头像设置项
 * 用于设置页面中显示
 */
@Composable
fun AvatarSettingItem(
    title: String,
    avatarUrl: String?,
    defaultEmoji: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .tactileClickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            avatarUrl = avatarUrl,
            emoji = defaultEmoji,
            size = 48.dp,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (avatarUrl != null) "已自定义" else "使用默认",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "编辑",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
