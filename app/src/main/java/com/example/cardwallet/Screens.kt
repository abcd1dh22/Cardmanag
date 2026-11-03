package com.example.cardwallet

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WalletApp(store: CardStore, host: SecurityHost) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var cards by remember { mutableStateOf(store.load()) }
    var settings by remember { mutableStateOf(store.loadSettings()) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(settings.appLock && host.lockAvailable) }
    var needsPrompt by remember { mutableStateOf(locked) }
    var authBusy by remember { mutableStateOf(false) }

    val selected = cards.firstOrNull { it.id == selectedId } ?: cards.firstOrNull()
    val ordered = if (selected == null) cards else {
        val i = cards.indexOfFirst { it.id == selected.id }
        cards.drop(i) + cards.take(i)
    }

    fun toast(msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    fun persist(list: List<CardItem>) {
        cards = list
        store.save(list)
    }

    fun updateSettings(s: AppSettings) {
        settings = s
        store.saveSettings(s)
    }

    fun copy(label: String, value: String) {
        clip.setText(AnnotatedString(value))
        toast("$label copied")
        if (settings.clearClipboard) {
            scope.launch {
                delay(30_000)
                if (clip.getText()?.text == value) clip.setText(AnnotatedString(""))
            }
        }
    }

    fun select(id: Long) {
        if (id != selected?.id) {
            if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            selectedId = id
            reveal = false
        }
    }

    fun promptUnlock() {
        if (authBusy) return
        authBusy = true
        host.authenticate("Unlock Card Wallet") { ok ->
            authBusy = false
            needsPrompt = false
            if (ok) locked = false
        }
    }

    fun toggleReveal() {
        if (selected == null) return
        if (reveal) {
            reveal = false
        } else if (settings.authToReveal && host.lockAvailable) {
            host.authenticate("Show card details") { ok -> if (ok) reveal = true }
        } else {
            reveal = true
        }
    }

    fun setAppLock(on: Boolean) {
        if (on && !host.lockAvailable) {
            toast("Set up a fingerprint or screen lock in your phone settings first")
            return
        }
        host.authenticate(if (on) "Enable app lock" else "Disable app lock") { ok ->
            if (ok) updateSettings(settings.copy(appLock = on))
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    reveal = false
                    if (settings.appLock && settings.autoLock && host.lockAvailable) {
                        locked = true
                        needsPrompt = true
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (locked && needsPrompt) promptUnlock()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settings.blockScreenshots) { host.setSecure(settings.blockScreenshots) }
    LaunchedEffect(reveal, selected?.id, settings.autoHide) {
        if (reveal && settings.autoHide) {
            delay(30_000)
            reveal = false
        }
    }

    BackHandler(enabled = showAdd) { showAdd = false }
    BackHandler(enabled = !showAdd && !locked && tab != 0) { tab = 0 }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (locked) {
            LockScreen(onUnlock = { promptUnlock() })
        } else {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (tab) {
                                    0 -> "Hello!"
                                    1 -> "All cards"
                                    else -> "Settings"
                                },
                                fontSize = 28.sp, fontWeight = FontWeight.Bold
                            )
                            Text("Your cards, securely stored", fontSize = 12.sp, color = Color.Gray)
                        }
                        RoundIconButton(if (settings.appLock) Icons.Filled.Lock else Icons.Filled.Settings) {
                            if (settings.appLock && host.lockAvailable) {
                                reveal = false
                                needsPrompt = false
                                locked = true
                            } else {
                                tab = 2
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    when (tab) {
                        0 -> HomeTab(
                            ordered = ordered,
                            selected = selected,
                            reveal = reveal,
                            onSelect = { select(it) },
                            onSwipe = { dir ->
                                if (ordered.size > 1) select(if (dir > 0) ordered[1].id else ordered.last().id)
                            },
                            onAdd = { showAdd = true },
                            onToggleReveal = { toggleReveal() },
                            onCopy = { label, value -> copy(label, value) },
                            onDelete = { confirmDelete = true }
                        )
                        1 -> CardsTab(cards, onPick = { select(it); tab = 0 }, onAdd = { showAdd = true })
                        else -> SettingsTab(
                            settings = settings,
                            lockAvailable = host.lockAvailable,
                            cardCount = cards.size,
                            onChange = { updateSettings(it) },
                            onToggleLock = { setAppLock(it) },
                            onWipe = { confirmWipe = true }
                        )
                    }
                    Spacer(Modifier.height(110.dp))
                }

                BottomNav(tab, { tab = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))

                AnimatedVisibility(
                    visible = showAdd,
                    enter = slideInVertically(tween(350)) { it } + fadeIn(tween(250)),
                    exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(200))
                ) {
                    AddCardScreen(
                        defaultColor = cards.size,
                        onClose = { showAdd = false },
                        onSave = { c ->
                            persist(cards + c)
                            selectedId = c.id
                            reveal = false
                            tab = 0
                            showAdd = false
                        }
                    )
                }
            }
        }
    }

    if (confirmDelete && selected != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete card?") },
            text = { Text("${selected.bank} (${selected.masked}) will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    persist(cards.filter { it.id != selected.id })
                    selectedId = null
                    reveal = false
                    confirmDelete = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Delete all cards?") },
            text = { Text("All ${cards.size} saved cards will be permanently deleted from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    persist(emptyList())
                    selectedId = null
                    reveal = false
                    confirmWipe = false
                }) { Text("Delete all") }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Fingerprint, contentDescription = null, tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("Card Wallet is locked", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Authenticate to view your cards", color = Color.Gray, fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onUnlock,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
            modifier = Modifier.height(50.dp)
        ) { Text("Unlock", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun HomeTab(
    ordered: List<CardItem>,
    selected: CardItem?,
    reveal: Boolean,
    onSelect: (Long) -> Unit,
    onSwipe: (Int) -> Unit,
    onAdd: () -> Unit,
    onToggleReveal: () -> Unit,
    onCopy: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    if (selected == null) {
        EmptyState(onAdd)
        return
    }
    val current: CardItem = selected
    CardStack(ordered, reveal, onSelect, onSwipe, onAdd)
    Spacer(Modifier.height(16.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface).padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionItem(Icons.Filled.Add, "Add", onAdd)
        ActionItem(
            if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            if (reveal) "Hide" else "Show", onToggleReveal
        )
        ActionItem(Icons.Filled.ContentCopy, "Copy") { onCopy("Card number", current.number) }
        ActionItem(Icons.Filled.Delete, "Delete", onDelete)
    }
    Spacer(Modifier.height(16.dp))
    AnimatedContent(
        targetState = current,
        transitionSpec = {
            (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 12 }) togetherWith fadeOut(tween(150))
        },
        label = "details"
    ) { card: CardItem ->
        DetailsCard(card, reveal, onCopy)
    }
}

@Composable
fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(Icons.Filled.CreditCard, size = 64.dp)
        Spacer(Modifier.height(14.dp))
        Text("No cards yet", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(
            "Add your first card to keep its details safe and handy.",
            color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onAdd,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
        ) { Text("Add card") }
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
        DetailRow(Icons.Filled.AccountBalance, "Bank", card.bank, null)
        DetailRow(Icons.Filled.Person, "Name", card.holder) { onCopy("Name", card.holder) }
        DetailRow(
            Icons.Filled.CreditCard, "Card number (${card.brand.label})",
            if (reveal) card.formatted else card.masked
        ) { onCopy("Card number", card.number) }
        DetailRow(Icons.Filled.DateRange, "Expiry", card.expiry) { onCopy("Expiry", card.expiry) }
        DetailRow(Icons.Filled.Lock, "CVV", if (reveal) card.cvv else "•••") { onCopy("CVV", card.cvv) }
        Spacer(Modifier.height(4.dp))
        Text("Tap a row to copy", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun CardsTab(cards: List<CardItem>, onPick: (Long) -> Unit, onAdd: () -> Unit) {
    if (cards.isEmpty()) {
        EmptyState(onAdd)
        return
    }
    cards.forEach { c ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onPick(c.id) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(cardColors[c.colorIdx % cardColors.size])),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CreditCard, contentDescription = null, tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.bank, fontWeight = FontWeight.Medium)
                Text(c.masked, fontSize = 12.sp, color = Color.Gray)
            }
            BrandLogo(c.brand, tint = Color.Gray)
        }
    }
}

@Composable
fun SettingsTab(
    settings: AppSettings,
    lockAvailable: Boolean,
    cardCount: Int,
    onChange: (AppSettings) -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onWipe: () -> Unit
) {
    SectionTitle("Security")
    SettingsCard {
        SettingRow(
            Icons.Filled.Fingerprint, "App lock",
            if (lockAvailable) "Fingerprint, face or screen lock to open the app"
            else "Set up a screen lock on your phone first",
            settings.appLock
        ) { onToggleLock(it) }
        SettingRow(
            Icons.Filled.Timer, "Lock when app is closed",
            "Lock again every time you leave the app",
            settings.autoLock, enabled = settings.appLock
        ) { onChange(settings.copy(autoLock = it)) }
        SettingRow(
            Icons.Filled.Visibility, "Authenticate to show details",
            "Ask for fingerprint before revealing number and CVV",
            settings.authToReveal, enabled = lockAvailable
        ) { onChange(settings.copy(authToReveal = it)) }
    }
    SectionTitle("Privacy")
    SettingsCard {
        SettingRow(
            Icons.Filled.Security, "Block screenshots",
            "Hide the app in screenshots and recent apps",
            settings.blockScreenshots
        ) { onChange(settings.copy(blockScreenshots = it)) }
        SettingRow(
            Icons.Filled.VisibilityOff, "Auto-hide details",
            "Hide number and CVV again after 30 seconds",
            settings.autoHide
        ) { onChange(settings.copy(autoHide = it)) }
        SettingRow(
            Icons.Filled.ContentCopy, "Clear clipboard",
            "Remove copied card data after 30 seconds",
            settings.clearClipboard
        ) { onChange(settings.copy(clearClipboard = it)) }
    }
    SectionTitle("General")
    SettingsCard {
        SettingRow(
            Icons.Filled.Vibration, "Haptic feedback",
            "Vibrate lightly when switching cards",
            settings.haptics
        ) { onChange(settings.copy(haptics = it)) }
    }
    SectionTitle("Data")
    SettingsCard {
        Row(
            Modifier.fillMaxWidth().clickable { onWipe() }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(Icons.Filled.DeleteForever, tint = Color(0xFFE5484D))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Delete all cards", color = Color(0xFFE5484D), fontWeight = FontWeight.Medium)
                Text("$cardCount saved on this device", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Info, contentDescription = null, tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Cards are encrypted (AES-256) and stored only on this device. Version 1.1",
            color = Color.Gray, fontSize = 11.sp
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
    val peek = 40.dp

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
                            scaleX = scale
                            scaleY = scale
                            alpha = a
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
                        }
                )
            }
        }

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
            ) {
                Icon(
                    Icons.Filled.Add, contentDescription = null, tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("Add Card", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun AddCardScreen(defaultColor: Int, onClose: () -> Unit, onSave: (CardItem) -> Unit) {
    var bank by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(defaultColor % cardColors.size) }
    var error by remember { mutableStateOf<String?>(null) }
    val brand = detectBrand(number)
    val fieldShape = RoundedCornerShape(16.dp)

    fun validate(): String? {
        if (bank.isBlank()) return "Enter the bank or card name"
        if (holder.isBlank()) return "Enter the card holder name"
        if (!isValidNumber(number)) return "The card number looks invalid"
        if (!isValidExpiry(expiry)) return "Enter a valid expiry date (MM/YY)"
        if (cvv.length != brand.cvvLength) return "CVV must be ${brand.cvvLength} digits"
        return null
    }

    Surface(
        Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Filled.Close, onClose)
                Spacer(Modifier.width(12.dp))
                Text("New card", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))

            BankCard(
                CardItem(
                    0, bank.ifBlank { "Bank name" }, holder.ifBlank { "CARD HOLDER" },
                    number, expiry, cvv, color
                ),
                reveal = true
            )
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                cardColors.forEachIndexed { i, colors ->
                    Box(
                        Modifier.size(32.dp).clip(CircleShape)
                            .background(colors[0])
                            .border(
                                if (i == color) 3.dp else 0.dp,
                                MaterialTheme.colorScheme.onBackground, CircleShape
                            )
                            .clickable { color = i }
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = bank, onValueChange = { bank = it },
                label = { Text("Bank / card name") },
                leadingIcon = { Icon(Icons.Filled.AccountBalance, contentDescription = null) },
                singleLine = true, shape = fieldShape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = holder, onValueChange = { holder = it },
                label = { Text("Card holder name") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true, shape = fieldShape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = number,
                onValueChange = { v ->
                    val d = v.filter { it.isDigit() }
                    number = d.take(detectBrand(d).maxLength)
                },
                label = { Text("Card number") },
                leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null) },
                trailingIcon = {
                    if (number.isNotEmpty()) {
                        BrandLogo(brand, MaterialTheme.colorScheme.onSurface, Modifier.padding(end = 12.dp))
                    }
                },
                supportingText = {
                    Text(
                        if (number.isEmpty()) "Visa, Mastercard and more are detected automatically"
                        else brand.label
                    )
                },
                visualTransformation = CardNumberTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, shape = fieldShape,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { v ->
                        val d = v.filter { it.isDigit() }.take(4)
                        expiry = if (d.length > 2) d.take(2) + "/" + d.drop(2) else d
                    },
                    label = { Text("Expiry (MM/YY)") },
                    leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, shape = fieldShape,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cvv,
                    onValueChange = { v -> cvv = v.filter { it.isDigit() }.take(brand.cvvLength) },
                    label = { Text("CVV") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true, shape = fieldShape,
                    modifier = Modifier.weight(1f)
                )
            }
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error ?: "", color = Color(0xFFE5484D), fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val err = validate()
                    if (err != null) {
                        error = err
                    } else {
                        onSave(
                            CardItem(
                                System.currentTimeMillis(), bank.trim(), holder.trim(),
                                number, expiry, cvv, color
                            )
                        )
                    }
                },
                shape = fieldShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("Save card", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(32.dp))
        }
    }
}
