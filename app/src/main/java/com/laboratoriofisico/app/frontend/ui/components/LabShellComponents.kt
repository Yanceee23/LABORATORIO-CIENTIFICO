package com.laboratoriofisico.app.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.CelesteLight
import com.laboratoriofisico.app.core.theme.DividerSubtle
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.PetrolDark

@Composable
fun LabGlobalHeader(
    onNavigateToResultados: () -> Unit,
    onNavigateToAjustes: () -> Unit,
    onNavigateToTutor: (() -> Unit)? = null,
    onNavigateToDocumentos: (() -> Unit)? = null,
    onNavigateToEstudio: (() -> Unit)? = null,
    onLogoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(if (onLogoClick != null) Modifier.clickable(onClick = onLogoClick) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PetrolDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.header_brand),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.4.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp
                    ),
                    color = PetrolDark
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (onNavigateToTutor != null) {
                    LabHeaderIconButton(
                        icon = Icons.Default.SmartToy,
                        contentDescription = stringResource(R.string.nav_tutor),
                        onClick = onNavigateToTutor
                    )
                }
                if (onNavigateToDocumentos != null) {
                    LabHeaderIconButton(
                        icon = Icons.Default.PictureAsPdf,
                        contentDescription = stringResource(R.string.documentos_title),
                        onClick = onNavigateToDocumentos
                    )
                }
                if (onNavigateToEstudio != null) {
                    LabHeaderIconButton(
                        icon = Icons.Default.MenuBook,
                        contentDescription = stringResource(R.string.estudio_title),
                        onClick = onNavigateToEstudio
                    )
                }
                LabHeaderIconButton(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = stringResource(R.string.resultados_title),
                    onClick = onNavigateToResultados
                )
                LabHeaderIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.ajustes_title),
                    onClick = onNavigateToAjustes
                )
            }
        }
        HorizontalDivider(color = DividerSubtle, thickness = 1.dp)
    }
}

@Composable
private fun LabHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = GraphiteMedium,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun LabBackLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.volver_link),
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = GraphiteMedium
    )
}

@Composable
fun LabScreenTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Graphite
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = GraphiteMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun LabSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
        color = GraphiteMedium
    )
}

@Composable
fun LabIconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backgroundColor: Color = CelesteLight,
    iconTint: Color = PetrolDark
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun LabSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.06f))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .background(Color.White, shape)
            .padding(16.dp)
    ) {
        content()
    }
}
