package io.margy.aichat.ui

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Маленький Markdown-рендер без внешних библиотек:
 * заголовки, списки, цитаты, код, **жирный**, *курсив*, `код`, ~~зачёркнутый~~, [ссылки](url).
 */
fun markdownToAnnotated(
    text: String,
    base: Color,
    codeBg: Color,
    link: Color,
    heading: Color,
    muted: Color
): AnnotatedString = buildAnnotatedString {
    val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
    val lines = normalized.split('\n')
    var inCode = false

    lines.forEachIndexed { index, raw ->
        if (index > 0) append('\n')
        val line = raw.trimEnd()
        val trimmed = line.trimStart()

        if (trimmed.startsWith("```")) {
            inCode = !inCode
            return@forEachIndexed
        }
        if (inCode) {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = codeBg,
                    color = base,
                    fontSize = 13.sp
                )
            ) { append(line) }
            return@forEachIndexed
        }

        when {
            trimmed.startsWith("### ") -> withStyle(
                SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = heading)
            ) { appendInline(trimmed.removePrefix("### "), codeBg, link) }

            trimmed.startsWith("## ") -> withStyle(
                SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = heading)
            ) { appendInline(trimmed.removePrefix("## "), codeBg, link) }

            trimmed.startsWith("# ") -> withStyle(
                SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = heading)
            ) { appendInline(trimmed.removePrefix("# "), codeBg, link) }

            trimmed.startsWith("> ") -> {
                withStyle(SpanStyle(color = muted)) { append("▎ ") }
                withStyle(SpanStyle(color = muted, fontStyle = FontStyle.Italic)) {
                    appendInline(trimmed.removePrefix("> "), codeBg, link)
                }
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                withStyle(SpanStyle(color = link)) { append("•  ") }
                appendInline(trimmed.substring(2), codeBg, link)
            }

            NumberedLine.matches(trimmed) -> {
                val marker = NumberedLine.find(trimmed)?.value.orEmpty()
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = link)) { append(marker) }
                appendInline(trimmed.removePrefix(marker), codeBg, link)
            }

            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                withStyle(SpanStyle(color = muted)) { append("────────────") }
            }

            else -> appendInline(line, codeBg, link)
        }
    }
}

private val NumberedLine = Regex("^\\d{1,2}[.)] ")

private fun AnnotatedString.Builder.appendInline(source: String, codeBg: Color, link: Color) {
    var i = 0
    while (i < source.length) {
        val rest = source.substring(i)
        when {
            rest.startsWith("**") -> {
                val end = rest.indexOf("**", 2)
                if (end > 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(rest.substring(2, end)) }
                    i += end + 2
                } else {
                    append(rest)
                    return
                }
            }

            rest.startsWith("__") -> {
                val end = rest.indexOf("__", 2)
                if (end > 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(rest.substring(2, end)) }
                    i += end + 2
                } else {
                    append(rest)
                    return
                }
            }

            rest.startsWith("~~") -> {
                val end = rest.indexOf("~~", 2)
                if (end > 2) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(rest.substring(2, end)) }
                    i += end + 2
                } else {
                    append(rest)
                    return
                }
            }

            rest.startsWith("`") -> {
                val end = rest.indexOf('`', 1)
                if (end > 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBg,
                            fontSize = 14.sp
                        )
                    ) { append(rest.substring(1, end)) }
                    i += end + 1
                } else {
                    append(rest)
                    return
                }
            }

            rest.startsWith("*") || rest.startsWith("_") -> {
                val marker = rest[0]
                val end = rest.indexOf(marker, 1)
                if (end > 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(rest.substring(1, end)) }
                    i += end + 1
                } else {
                    append(rest)
                    return
                }
            }

            rest.startsWith("[") -> {
                val m = LinkRegex.find(rest)
                if (m != null) {
                    withStyle(
                        SpanStyle(color = link, textDecoration = TextDecoration.Underline)
                    ) { append(m.groupValues[1]) }
                    i += m.value.length
                } else {
                    append(rest)
                    return
                }
            }

            else -> {
                append(source[i])
                i++
            }
        }
    }
}

private val LinkRegex = Regex("^\\[([^\\]]+)]\\(([^)]+)\\)")

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val scheme = MaterialTheme.colorScheme
    val annotated = remember(text, scheme, color) {
        markdownToAnnotated(
            text = text,
            base = color,
            codeBg = scheme.surfaceVariant.copy(alpha = 0.55f),
            link = scheme.primary,
            heading = scheme.onSurface,
            muted = scheme.onSurfaceVariant
        )
    }
    SelectionContainer {
        Text(
            text = annotated,
            modifier = modifier,
            color = color,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp)
        )
    }
}
