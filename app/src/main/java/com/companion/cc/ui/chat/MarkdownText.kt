package com.companion.cc.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.companion.cc.ui.theme.LocalVisualTheme

/**
 * 简单的 Markdown 渲染器
 * 支持：粗体、斜体、代码块、行内代码
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val visualTheme = LocalVisualTheme.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        parseMarkdown(text).forEach { block ->
            when (block) {
                is MarkdownBlock.Text -> {
                    Text(
                        text = parseInlineMarkdown(block.content, surfaceVariant),
                        style = MaterialTheme.typography.bodyLarge,
                        color = color
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            )
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = highlightCode(
                                code = block.content,
                                language = block.language,
                                keywordColor = visualTheme.tokens.chart.trend,
                                stringColor = visualTheme.tokens.status.success
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 简单的代码高亮
 * 支持常见编程语言的关键词高亮
 */
private fun highlightCode(
    code: String,
    language: String,
    keywordColor: androidx.compose.ui.graphics.Color,
    stringColor: androidx.compose.ui.graphics.Color
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val keywords = when (language.lowercase()) {
            "kotlin", "kt" -> listOf(
                "fun", "val", "var", "class", "object", "interface", "if", "else", "when",
                "for", "while", "return", "import", "package", "private", "public", "protected"
            )
            "java" -> listOf(
                "public", "private", "protected", "static", "final", "class", "interface",
                "if", "else", "for", "while", "return", "import", "package", "void", "int", "String"
            )
            "python", "py" -> listOf(
                "def", "class", "if", "else", "elif", "for", "while", "return", "import",
                "from", "as", "try", "except", "with", "lambda", "yield"
            )
            "javascript", "js" -> listOf(
                "function", "const", "let", "var", "if", "else", "for", "while", "return",
                "import", "export", "class", "async", "await", "try", "catch"
            )
            else -> emptyList()
        }

        if (keywords.isNotEmpty()) {
            // 简单的关键词高亮
            val words = code.split(Regex("\\b"))
            words.forEach { word ->
                if (word in keywords) {
                    withStyle(SpanStyle(
                        color = keywordColor,
                        fontWeight = FontWeight.Bold
                    )) {
                        append(word)
                    }
                } else {
                    // 字符串高亮
                    if (word.startsWith("\"") && word.endsWith("\"")) {
                        withStyle(SpanStyle(
                            color = stringColor
                        )) {
                            append(word)
                        }
                    } else {
                        append(word)
                    }
                }
            }
        } else {
            append(code)
        }
    }
}

private sealed class MarkdownBlock {
    data class Text(val content: String) : MarkdownBlock()
    data class CodeBlock(val content: String, val language: String = "") : MarkdownBlock()
}

private fun parseMarkdown(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 检测代码块
        if (line.trim().startsWith("```")) {
            val language = line.trim().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++

            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }

            if (codeLines.isNotEmpty()) {
                blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), language))
            }
            i++
        } else {
            // 普通文本
            blocks.add(MarkdownBlock.Text(line))
            i++
        }
    }

    return blocks
}

private fun parseInlineMarkdown(
    text: String,
    surfaceVariant: androidx.compose.ui.graphics.Color
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val inlineCodeRegex = Regex("`([^`]+)`")
        val boldRegex = Regex("\\*\\*([^*]+)\\*\\*")
        val italicRegex = Regex("\\*([^*]+)\\*")

        // 简单实现：按顺序处理
        var processedText = text

        // 处理粗体
        boldRegex.findAll(processedText).forEach { match ->
            val before = processedText.substring(0, match.range.first)
            append(before)

            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }

            processedText = processedText.substring(match.range.last + 1)
            currentIndex = 0
        }

        // 添加剩余文本
        if (processedText.isNotEmpty()) {
            // 处理行内代码
            var remainingText = processedText
            inlineCodeRegex.findAll(remainingText).forEach { match ->
                val before = remainingText.substring(0, match.range.first)
                append(before)

                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = surfaceVariant
                    )
                ) {
                    append(match.groupValues[1])
                }

                remainingText = remainingText.substring(match.range.last + 1)
            }

            if (remainingText.isNotEmpty()) {
                append(remainingText)
            }
        }
    }
}
