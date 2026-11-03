package com.example.cardwallet

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // স্ক্রিনশট / রিসেন্ট অ্যাপ প্রিভিউ থেকে কার্ড লুকানো
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
    var cards by remember { mutableStateOf(store.load()) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun persist(list: List<CardItem>) { cards = list; store.save(list) }
    fun copy(label: String, value: String) {
        clip.setText(AnnotatedString(value))
        Toast.makeText(ctx, "$label কপি হয়েছে", Toast.LENGTH_SHORT).show()
    }

    val selected = cards.firstOrNull { it.id == selectedId } ?: cards.firstOrNull()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("আমার কার্ড", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("সব কার্ড এক জায়গায়, নিরাপদে", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(20.dp))

            if (selected == null) {
                Box(
                    Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) { Text("কোনো কার্ড নেই। নতুন কার্ড যোগ করুন।", color = Color.Gray) }
            } else {
                CardStack(cards, selected, reveal) { selectedId = it; reveal = false }
            }

            Spacer(Modifier.height(16.dp))

            // অ্যাকশন বার
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionItem("＋", "যোগ") { showAdd = true }
                ActionItem(if (reveal) "🙈" else "👁", if (reveal) "লুকান" else "দেখুন") {
                    if (selected != null) reveal = !reveal
                }
                ActionItem("⧉", "কপি") { selected?.let { copy("কার্ড নম্বর", it.number) } }
                ActionItem("🗑", "মুছুন") { if (selected != null) confirmDelete = true }
            }

            if (selected != null) {
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(18.dp)
                ) {
                    Text("কার্ডের বিবরণ", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    DetailRow("ব্যাংক", selected.bank, null)
                    DetailRow("নাম", selected.holder) { copy("নাম", selected.holder) }
                    DetailRow("কার্ড নম্বর", if (reveal) selected.formatted else selected.masked) {
                        copy("কার্ড নম্বর", selected.number)
                    }
                    DetailRow("মেয়াদ", selected.expiry) { copy("মেয়াদ", selected.expiry) }
                    DetailRow("CVV", if (reveal) selected.cvv else "•••") { copy("CVV", selected.cvv) }
                    Text("কপি করতে যেকোনো সারিতে ট্যাপ করুন", fontSize = 11.sp, color = Color.Gray)
                }
            }

            if (cards.size > 1) {
                Spacer(Modifier.height(16.dp))
                Text("সব কার্ড", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                cards.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { selectedId = c.id; reveal = false }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(14.dp).clip(CircleShape)
                                .background(cardColors[c.colorIdx % cardColors.size][0])
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.bank, fontWeight = FontWeight.Medium)
                            Text(c.masked, fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(c.brand, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAdd) {
        AddCardDialog(
            colorIdx = cards.size,
            onDismiss = { showAdd = false },
            onSave = { c -> persist(cards + c); selectedId = c.id; showAdd = false }
        )
    }
    if (confirmDelete && selected != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("কার্ড মুছবেন?") },
            text = { Text("${selected.bank} (${selected.masked}) স্থায়ীভাবে মুছে যাবে।") },
            confirmButton = {
                TextButton(onClick = {
                    persist(cards.filter { it.id != selected.id })
                    selectedId = null; reveal = false; confirmDelete = false
                }) { Text("মুছুন") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("বাতিল") } }
        )
    }
}

@Composable
fun CardStack(cards: List<CardItem>, selected: CardItem, reveal: Boolean, onSelect: (Long) -> Unit) {
    val others = cards.filter { it.id != selected.id }.take(2)
    val peek = 34.dp
    Box(Modifier.fillMaxWidth().height(200.dp + peek * others.size)) {
        for (k in others.indices.reversed()) {
            val c = others[k]
            BankCard(
                c, false,
                Modifier.offset(y = peek * (others.size - 1 - k)).clickable { onSelect(c.id) }
            )
        }
        BankCard(selected, reveal, Modifier.offset(y = peek * others.size))
    }
}

@Composable
fun BankCard(card: CardItem, reveal: Boolean, modifier: Modifier = Modifier) {
    val colors = cardColors[card.colorIdx % cardColors.size]
    val fg = if (card.colorIdx % cardColors.size == 1) Color(0xFF1A1A1A) else Color.White
    Column(
        modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors)).padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(card.bank, color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(card.brand, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Text(
            if (reveal) card.formatted else card.masked,
            color = fg, fontSize = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Name", color = fg.copy(alpha = 0.7f), fontSize = 10.sp)
                Text(card.holder, color = fg, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Exp. Date", color = fg.copy(alpha = 0.7f), fontSize = 10.sp)
                Text(card.expiry, color = fg, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ActionItem(icon: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 20.sp) }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun DetailRow(label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
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
        title = { Text("নতুন কার্ড") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(bank, { bank = it }, label = { Text("ব্যাংক / কার্ডের নাম") }, singleLine = true)
                OutlinedTextField(holder, { holder = it }, label = { Text("কার্ড হোল্ডারের নাম") }, singleLine = true)
                OutlinedTextField(
                    number, { number = it.filter(Char::isDigit).take(19) },
                    label = { Text("কার্ড নম্বর") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    expiry, { v ->
                        val d = v.filter(Char::isDigit).take(4)
                        expiry = if (d.length > 2) d.take(2) + "/" + d.drop(2) else d
                    },
                    label = { Text("মেয়াদ (MM/YY)") }, singleLine = true,
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
                    bank.isBlank() || holder.isBlank() -> error = "নাম ও ব্যাংক লিখুন"
                    number.length < 12 -> error = "কার্ড নম্বর সঠিক নয়"
                    expiry.length != 5 -> error = "মেয়াদ MM/YY ফরম্যাটে দিন"
                    else -> onSave(
                        CardItem(System.currentTimeMillis(), bank.trim(), holder.trim(),
                            number, expiry, cvv, colorIdx)
                    )
                }
            }) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
