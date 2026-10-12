package com.laboratoriofisico.app.frontend.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

@Composable
fun RichMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    renderMath: Boolean = false
) {
    val blocks = remember(markdown, renderMath) {
        parseMarkdownBlocks(markdown, renderMath)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> MarkdownInlineText(
                    text = block.text,
                    heading = true,
                    renderMath = renderMath
                )
                is MarkdownBlock.Paragraph -> MarkdownInlineText(
                    text = block.text,
                    renderMath = renderMath
                )
                is MarkdownBlock.Bullet -> MarkdownInlineText(
                    text = "• ${block.text}",
                    renderMath = renderMath
                )
                is MarkdownBlock.Numbered -> MarkdownInlineText(
                    text = "${block.number}. ${block.text}",
                    renderMath = renderMath
                )
                is MarkdownBlock.Math -> LatexMathView(
                    latex = block.latex,
                    displayMode = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MarkdownAnalysisText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    RichMarkdownText(markdown = markdown, modifier = modifier, renderMath = false)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarkdownInlineText(
    text: String,
    renderMath: Boolean,
    heading: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    val style = if (heading) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val segments = remember(text, renderMath) {
        if (renderMath) splitInlineMath(text) else listOf(InlineSegment.Text(text))
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEach { segment ->
            when (segment) {
                is InlineSegment.Text -> {
                    val annotated = remember(segment.value, heading, primary) {
                        buildAnnotatedString {
                            appendMarkdownInline(
                                text = segment.value,
                                heading = heading,
                                headingColor = primary
                            )
                        }
                    }
                    if (annotated.isNotBlank()) {
                        Text(text = annotated, style = style)
                    }
                }
                is InlineSegment.Math -> LatexMathView(
                    latex = segment.latex,
                    displayMode = false,
                    modifier = Modifier.width(estimatedInlineMathWidth(segment.latex)).height(42.dp)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LatexMathView(
    latex: String,
    displayMode: Boolean,
    modifier: Modifier = Modifier
) {
    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
    val html = remember(latex, displayMode, foreground) {
        katexHtml(
            latex = latex,
            displayMode = displayMode,
            textColor = "#${foreground.toUInt().toString(16).takeLast(6).padStart(6, '0')}"
        )
    }
    Surface(
        modifier = modifier
            .widthIn(min = if (displayMode) Dp.Unspecified else 56.dp)
            .background(background, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = background
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (displayMode) 88.dp else 42.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.Transparent.toArgb())
                    settings.javaScriptEnabled = true
                    settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = false
                    isHorizontalScrollBarEnabled = false
                    isVerticalScrollBarEnabled = false
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(
                    "https://cdn.jsdelivr.net/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        )
    }
}

private fun parseMarkdownBlocks(markdown: String, renderMath: Boolean): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = StringBuilder()
    val math = StringBuilder()
    var inMathBlock = false

    fun flushParagraph() {
        val text = paragraph.toString().trim()
        if (text.isNotBlank()) blocks.add(MarkdownBlock.Paragraph(text))
        paragraph.clear()
    }

    markdown.lineSequence().forEach { rawLine ->
        val trimmed = rawLine.trim()
        if (renderMath && trimmed == "$$") {
            if (inMathBlock) {
                blocks.add(MarkdownBlock.Math(math.toString().trim()))
                math.clear()
                inMathBlock = false
            } else {
                flushParagraph()
                inMathBlock = true
            }
            return@forEach
        }
        if (inMathBlock) {
            math.appendLine(rawLine)
            return@forEach
        }
        when {
            trimmed.isBlank() -> flushParagraph()
            trimmed.startsWith("#") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Heading(trimmed.trimStart('#').trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Bullet(trimmed.drop(2).trim()))
            }
            ORDERED_LIST_REGEX.matches(trimmed) -> {
                flushParagraph()
                val match = ORDERED_LIST_REGEX.find(trimmed)
                val number = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
                val text = match?.groupValues?.getOrNull(2).orEmpty()
                blocks.add(MarkdownBlock.Numbered(number, text))
            }
            trimmed.endsWith(":") && trimmed.length <= 70 -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Heading(trimmed))
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(trimmed)
            }
        }
    }
    if (inMathBlock && math.isNotBlank()) {
        blocks.add(MarkdownBlock.Math(math.toString().trim()))
    }
    flushParagraph()
    return blocks
}

private fun splitInlineMath(text: String): List<InlineSegment> {
    val segments = mutableListOf<InlineSegment>()
    var cursor = 0
    while (cursor < text.length) {
        val start = text.indexOf('$', cursor)
        if (start == -1) {
            segments.add(InlineSegment.Text(text.substring(cursor)))
            break
        }
        val end = text.indexOf('$', start + 1)
        if (end == -1) {
            segments.add(InlineSegment.Text(text.substring(cursor)))
            break
        }
        if (start > cursor) segments.add(InlineSegment.Text(text.substring(cursor, start)))
        val latex = text.substring(start + 1, end).trim()
        if (latex.isNotBlank()) segments.add(InlineSegment.Math(latex))
        cursor = end + 1
    }
    return segments
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendMarkdownInline(
    text: String,
    heading: Boolean,
    headingColor: Color
) {
    var cursor = 0
    var bold = false
    if (heading) {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = headingColor)) {
            appendMarkdownInline(text, heading = false, headingColor = headingColor)
        }
        return
    }
    while (cursor < text.length) {
        val marker = text.indexOf("**", startIndex = cursor)
        if (marker == -1) {
            appendStyledMarkdownChunk(text.substring(cursor), bold)
            return
        }
        appendStyledMarkdownChunk(text.substring(cursor, marker), bold)
        bold = !bold
        cursor = marker + 2
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendStyledMarkdownChunk(
    chunk: String,
    bold: Boolean
) {
    if (chunk.isEmpty()) return
    if (bold) {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(chunk)
        }
    } else {
        append(chunk)
    }
}

private fun katexHtml(latex: String, displayMode: Boolean, textColor: String): String {
    val quotedLatex = JSONObject.quote(latex)
    val fallbackLatex = escapeHtml(latex)
    return """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css">
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js"></script>
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background: transparent;
                    color: $textColor;
                    overflow: hidden;
                    font-size: ${if (displayMode) "19px" else "16px"};
                }
                #math {
                    width: 100%;
                    min-height: ${if (displayMode) "70px" else "32px"};
                    display: flex;
                    align-items: center;
                    justify-content: ${if (displayMode) "center" else "flex-start"};
                    box-sizing: border-box;
                }
                .katex-display {
                    margin: 0;
                }
            </style>
        </head>
        <body>
            <div id="math">$fallbackLatex</div>
            <script>
                try {
                    katex.render($quotedLatex, document.getElementById("math"), {
                        displayMode: $displayMode,
                        throwOnError: false
                    });
                } catch (error) {
                    document.getElementById("math").textContent = $quotedLatex;
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun estimatedInlineMathWidth(latex: String): Dp =
    (latex.length * 9).coerceIn(72, 320).dp

private fun escapeHtml(text: String): String =
    text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

private sealed interface MarkdownBlock {
    data class Heading(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Numbered(val number: Int, val text: String) : MarkdownBlock
    data class Math(val latex: String) : MarkdownBlock
}

private sealed interface InlineSegment {
    data class Text(val value: String) : InlineSegment
    data class Math(val latex: String) : InlineSegment
}

private val ORDERED_LIST_REGEX = Regex("""^(\d+)\.\s+(.+)$""")
