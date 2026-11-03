package com.example.cardwallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Card network logo: Mastercard circles, Visa wordmark, or text for others. */
@Composable
fun BrandLogo(brand: Brand, tint: Color, modifier: Modifier = Modifier) {
    when (brand) {
        Brand.MASTERCARD -> Box(modifier.size(width = 38.dp, height = 24.dp)) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFEB001B)))
            Box(
                Modifier.align(Alignment.CenterEnd).size(24.dp).clip(CircleShape)
                    .background(Color(0xCCF79E1B))
            )
        }
        Brand.VISA -> Text(
            "VISA", modifier = modifier, color = tint, fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
        )
        Brand.UNKNOWN -> Icon(
            Icons.Filled.CreditCard, contentDescription = null, tint = tint,
            modifier = modifier.size(24.dp)
        )
        else -> Text(
            brand.label.uppercase(), modifier = modifier, color = tint,
            fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1
        )
    }
}

@Composable
fun BankCard(card: CardItem, reveal: Boolean, modifier: Modifier = Modifier) {
    val idx = card.colorIdx % cardColors.size
    val fg = if (idx == 1) Color(0xFF1A1A1A) else Color.White
    val number = when {
        card.number.isEmpty() -> "•••• •••• •••• ••••"
        reveal -> card.formatted
        else -> card.masked
    }
    Column(
        modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(cardColors[idx]))
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                card.bank, color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                maxLines = 1, modifier = Modifier.weight(1f)
            )
            BrandLogo(card.brand, fg)
        }
        Text(
            number, color = fg,
            fontSize = if (reveal && card.number.length > 16) 18.sp else 22.sp,
            fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false
        )
        Row(Modifier.fillMaxWidth().padding(end = 118.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Name", color = fg.copy(alpha = 0.7f), fontSize = 10.sp)
                Text(card.holder, color = fg, fontSize = 13.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Exp. Date", color = fg.copy(alpha = 0.7f), fontSize = 10.sp)
                Text(card.expiry.ifEmpty { "MM/YY" }, color = fg, fontSize = 13.sp)
            }
        }
    }
}

/** Shows the card number in groups (4-4-4-4, or 4-6-5 for Amex) while the stored value stays digits only. */
class CardNumberTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val breaks = breakPositions(detectBrand(digits), digits.length)
        val out = StringBuilder()
        digits.forEachIndexed { i, c ->
            if (i in breaks) out.append(' ')
            out.append(c)
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + breaks.count { it < offset }
            override fun transformedToOriginal(offset: Int): Int {
                var spaces = 0
                breaks.forEachIndexed { k, b -> if (b + k < offset) spaces++ }
                return (offset - spaces).coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}

@Composable
fun IconBadge(icon: ImageVector, size: Dp = 38.dp, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) 0.88f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s }
            .clickable(interactionSource = src, indication = null) { onClick() }
    ) {
        IconBadge(icon, size = 54.dp)
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.Gray, fontSize = 11.sp)
            Text(value, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1)
        }
        if (onClick != null) {
            Icon(
                Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        content = content
    )
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f },
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF8E8E93),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun BottomNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Icons.Filled.Home to "Home",
        Icons.Filled.CreditCard to "Cards",
        Icons.Filled.Settings to "Settings"
    )
    Row(
        modifier.clip(CircleShape).background(Color(0xFF111111))
            .border(1.dp, Color(0x33FFFFFF), CircleShape).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { i, item ->
            val sel = tab == i
            val bg by animateColorAsState(if (sel) Color.White else Color.Transparent, label = "navbg")
            Row(
                Modifier.clip(CircleShape).background(bg).clickable { onTab(i) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    item.first, contentDescription = item.second,
                    tint = if (sel) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                AnimatedVisibility(visible = sel) {
                    Text(
                        "  " + item.second, color = Color.Black,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                }
            }
        }
    }
}
