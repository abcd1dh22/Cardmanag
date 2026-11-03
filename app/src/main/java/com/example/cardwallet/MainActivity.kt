package com.example.cardwallet

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide cards from screenshots and the recent-apps preview
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val store = CardStore(applicationContext)
        setContent { AppTheme { WalletApp(store) } }
    }
}

private val cardColors = listOf(
    listOf(Color(0xFF9B3FD1), Color(0xFF7B2CBF)),
    listOf(Color(0xFFFFD25E), Color(0xFFF5B82E)),
    listOf(Color(0xFF2D6CDF), Color(0xFF1B4DB3)),
    listOf(Color(0xFF1FA67A), Color(0xFF137A5A)),
    listOf(Color(0xFFE5484D), Color(0xFFB4323A))
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(
        background = Color.Black, surface = Color(0xFF151515),
        onBackground = Color.White, onSurface = Color.White,
        primary = Color.White, onPrimary = Color.Black
    ) else lightColorScheme(
        background = Color(0xFFF1F1F1), surface = Color.White,
        onBackground = Color.Black, onSurface = Color.Black,
        primary = Color.Black, onPrimary = Color.White
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun WalletApp(store: CardStore) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var cards by remember { mutableStateOf(store.load()) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }

    fun persist(list: List<CardItem>) { cards = list; store.save(list) }
    fun copy(label: String, value: String) {
        clip.setText(AnnotatedString(value))
        Toast.makeText(ctx, "$label copied", Toast.LENGTH_SHORT).show()
    }

    val selected = cards.firstOrNull { it.id == selectedId } ?: cards.firstOrNull()
    val ordered = if (selected == null) cards else {
        val i = cards.indexOfFirst { it.id == selected.id }
        cards.drop(i) + cards.take(i)
    }

    fun select(id: Long) {
        if (id != selected?.id) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            selectedId = id
            reveal = false
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon("≡")
                    Spacer(Modifier.weight(1f))
                    RoundIcon("🔔")
                }
                Spacer(Modifier.height(14.dp))
                Text("Hello!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Keep your cards safe", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(18.dp))

                if (tab == 0) {
                    if (selected == null) {
                        Box(
                            Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) { Text("No cards yet. Tap + to add one.", color = Color.Gray) }
                    } else {
                        CardStack(
                            ordered = ordered,
                            reveal = reveal,
                            onSelect = { select(it) },
                            onSwipe = { dir ->
                                if (ordered.size > 1) select(if (dir > 0) ordered[1].id else ordered.last().id)
                            },
                            onAdd = { showAdd = true }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionItem("＋", "Add") { showAdd = true }
                        ActionItem(if (reveal) "🙈" else "👁", if (reveal) "Hide" else "Show") {
                            if (selected != null) reveal = !reveal
                        }
                        ActionItem("⧉", "Copy") { selected?.let { copy("Card number", it.number) } }
                        ActionItem("🗑", "Delete") { if (selected != null) confirmDelete = true }
                    }

                    if (selected != null) {
                        val current: CardItem = selected
                        Spacer(Modifier.height(16.dp))
                        AnimatedContent(
                            targetState = current,
                            transitionSpec = {
                                (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 12 }) togetherWith
                                    fadeOut(tween(150))
                            },
                            label = "details"
                        ) { card: CardItem ->
                            DetailsCard(card, reveal) { label, value -> copy(label, value) }
                        }
                    }
                } else {
                    Text("All cards", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (cards.isEmpty()) Text("No cards yet.", color = Color.Gray)
                    cards.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { select(c.id); tab = 0 }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(cardColors[c.colorIdx % cardColors.size])),
                                contentAlignment = Alignment.Center
                            ) { Text("💳", fontSize = 16.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.bank, fontWeight = FontWeight.Medium)
                                Text(c.masked, fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(c.brand, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
                Spacer(Modifier.height(110.dp))
            }

            BottomNav(tab, { tab = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))
        }
    }

    if (showAdd) {
        AddCardDialog(
            colorIdx = cards.size,
            onDismiss = { showAdd = false },
            onSave = { c -> persist(cards + c); selectedId = c.id; reveal = false; tab = 0; showAdd = false }
        )
    }
    if (confirmDelete && selected != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete card?") },
            text = { Text("${selected.bank} (${selected.masked}) will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    persist(cards.filter { it.id != selected.id })
                    selectedId = null; reveal = false; confirmDelete = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun CardStack(
    ordered: List<CardItem>,
    reveal: Boolean,
    onSelect: (Long) -> Unit,
    onSwipe: (Int) -> Unit,
    onAdd: () -> Unit
) {
    val visible = minOf(ordered.size, 3)
    val peek = 36.dp

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val enter by animateFloatAsState(
        if (appeared) 1f else 0f, tween(700, easing = FastOutSlowInEasing), label = "enter"
    )
    var drag by remember { mutableFloatStateOf(0f) }

    Box(
        Modifier.fillMaxWidth().height(200.dp + peek * (visible - 1))
            .graphicsLayer { alpha = enter; translationY = (1f - enter) * 80.dp.toPx() }
            .pointerInput(ordered.size) {
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f },
                    onVerticalDrag = { _, d -> drag += d },
                    onDragEnd = { if (abs(drag) > 60.dp.toPx()) onSwipe(if (drag < 0) 1 else -1) }
                )
            }
    ) {
        ordered.forEachIndexed { p, card ->
            key(card.id) {
                val shown = p < visible
                val y by animateDpAsState(
                    if (shown) peek * (visible - 1 - p) else 0.dp,
                    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "y"
                )
                val scale by animateFloatAsState(
                    if (shown) 1f - 0.04f * p else 0.9f,
                    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "scale"
                )
                val a by animateFloatAsState(if (shown) 1f else 0f, tween(350), label = "alpha")

                BankCard(
                    card = card,
                    reveal = reveal && p == 0,
                    modifier = Modifier
                        .offset(y = y)
                        .zIndex((ordered.size - p).toFloat())
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale; alpha = a
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = shown
                        ) {
                            if (p == 0) {
                                if (ordered.size > 1) onSelect(ordered[1].id)
                            } else {
                                onSelect(card.id)
                            }
                            Unit
                        }
                )
            }
        }

        // "Add Card" pill on the front card, like the reference design
        Row(
            Modifier.align(Alignment.BottomEnd).zIndex(100f).padding(end = 6.dp, bottom = 6.dp)
                .clip(CircleShape)
                .background(Color(0xFF111111))
                .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                .clickable { onAdd() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) { Text("+", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(Modifier.width(8.dp))
            Text("Add Card", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BankCard(card: CardItem, reveal: Boolean, modifier: Modifier = Modifier) {
    val idx = card.colorIdx % cardColors.size
    val fg = if (idx == 1) Color(0xFF1A1A1A) else Color.White
    Column(
        modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(cardColors[idx]))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(card.bank, color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("**** **** **** " + card.number.takeLast(4), color = fg, fontSize = 11.sp)
        }
        Column {
            Text(card.brand, color = fg.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(
                if (reveal) card.formatted else card.masked,
                color = fg, fontSize = if (reveal && card.number.length > 16) 18.sp else 22.sp,
                fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false
            )
        }
        Row(Modifier.fillMaxWidth().padding(end = 118.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Name", color = fg.copy(alpha = 0.7f), fontSize = 10.sp)
                Text(card.holder, color = fg, fontSize = 13.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Exp. Date", color = fg.copy(alpha = 0.7f), fontSize = 10.sp)
                Text(card.expiry, color = fg, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun DetailsCard(card: CardItem, reveal: Boolean, onCopy: (String, String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface).padding(18.dp)
    ) {
        Text("Card Details", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        DetailRow("🏦", "Bank", card.bank, null)
        DetailRow("👤", "Name", card.holder) { onCopy("Name", card.holder) }
        DetailRow("💳", "Card number", if (reveal) card.formatted else card.masked) {
            onCopy("Card number", card.number)
        }
        DetailRow("📅", "Expiry", card.expiry) { onCopy("Expiry", card.expiry) }
        DetailRow("🔒", "CVV", if (reveal) card.cvv else "•••") { onCopy("CVV", card.cvv) }
        Spacer(Modifier.height(4.dp))
        Text("Tap a row to copy", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun DetailRow(icon: String, label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 15.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.Gray, fontSize = 11.sp)
            Crossfade(targetState = value, label = "value") {
                Text(it, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1)
            }
        }
        if (onClick != null) Text("⧉", color = Color.Gray)
    }
}

@Composable
fun RoundIcon(text: String) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 18.sp) }
}

@Composable
fun ActionItem(icon: String, label: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.88f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s }
            .clickable(interactionSource = src, indication = null) { onClick() }
    ) {
        Box(
            Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 20.sp) }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun BottomNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf("🏠" to "Home", "💳" to "Cards")
    Row(
        modifier.clip(CircleShape).background(Color(0xFF111111))
            .border(1.dp, Color(0x33FFFFFF), CircleShape).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val sel = tab == i
            val bg by animateColorAsState(if (sel) Color.White else Color.Transparent, label = "navbg")
            Row(
                Modifier.clip(CircleShape).background(bg).clickable { onTab(i) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 16.sp)
                AnimatedVisibility(visible = sel) {
                    Text("  $label", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun AddCardDialog(colorIdx: Int, onDismiss: () -> Unit, onSave: (CardItem) -> Unit) {
    var bank by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New card") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(bank, { bank = it }, label = { Text("Bank / card name") }, singleLine = true)
                OutlinedTextField(holder, { holder = it }, label = { Text("Card holder name") }, singleLine = true)
                OutlinedTextField(
                    number, { number = it.filter(Char::isDigit).take(19) },
                    label = { Text("Card number") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    expiry, { v ->
                        val d = v.filter(Char::isDigit).take(4)
                        expiry = if (d.length > 2) d.take(2) + "/" + d.drop(2) else d
                    },
                    label = { Text("Expiry (MM/YY)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    cvv, { cvv = it.filter(Char::isDigit).take(4) },
                    label = { Text("CVV") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                error?.let { Text(it, color = Color(0xFFE5484D), fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    bank.isBlank() || holder.isBlank() -> error = "Enter bank and holder name"
                    number.length < 12 -> error = "Invalid card number"
                    expiry.length != 5 -> error = "Use MM/YY format"
                    else -> onSave(
                        CardItem(System.currentTimeMillis(), bank.trim(), holder.trim(),
                            number, expiry, cvv, colorIdx)
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
