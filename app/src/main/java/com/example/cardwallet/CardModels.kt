package com.example.cardwallet

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

enum class Brand(val label: String, val groups: List<Int>, val maxLength: Int, val cvvLength: Int) {
    VISA("Visa", listOf(4, 4, 4, 4, 3), 19, 3),
    MASTERCARD("Mastercard", listOf(4, 4, 4, 4), 16, 3),
    AMEX("American Express", listOf(4, 6, 5), 15, 4),
    DISCOVER("Discover", listOf(4, 4, 4, 4, 3), 19, 3),
    UNIONPAY("UnionPay", listOf(4, 4, 4, 4, 3), 19, 3),
    JCB("JCB", listOf(4, 4, 4, 4, 3), 19, 3),
    DINERS("Diners Club", listOf(4, 4, 4, 4), 16, 3),
    UNKNOWN("Card", listOf(4, 4, 4, 4, 3), 19, 3)
}

private fun prefix(n: String, k: Int): Int = if (n.length >= k) (n.take(k).toIntOrNull() ?: -1) else -1

/** Detects the card network from the leading digits (IIN ranges). */
fun detectBrand(n: String): Brand {
    if (n.isEmpty()) return Brand.UNKNOWN
    val p2 = prefix(n, 2)
    val p3 = prefix(n, 3)
    val p4 = prefix(n, 4)
    return when {
        n.startsWith("4") -> Brand.VISA
        p2 in 51..55 -> Brand.MASTERCARD
        p4 in 2221..2720 -> Brand.MASTERCARD
        n.length < 4 && p2 in 22..27 -> Brand.MASTERCARD
        p2 == 34 || p2 == 37 -> Brand.AMEX
        n.startsWith("62") -> Brand.UNIONPAY
        p4 == 6011 || p2 == 65 || p3 in 644..649 -> Brand.DISCOVER
        p4 in 3528..3589 -> Brand.JCB
        p3 in 300..305 || p2 == 36 || p2 == 38 -> Brand.DINERS
        else -> Brand.UNKNOWN
    }
}

fun luhnValid(n: String): Boolean {
    if (n.isEmpty()) return false
    var sum = 0
    var alt = false
    for (i in n.length - 1 downTo 0) {
        var d = n[i] - '0'
        if (alt) {
            d *= 2
            if (d > 9) d -= 9
        }
        sum += d
        alt = !alt
    }
    return sum % 10 == 0
}

fun isValidNumber(n: String): Boolean {
    val brand = detectBrand(n)
    val lengthOk = if (brand == Brand.AMEX) n.length == 15 else n.length in 13..19
    return lengthOk && luhnValid(n)
}

fun isValidExpiry(e: String): Boolean {
    if (e.length != 5) return false
    val mm = e.substring(0, 2).toIntOrNull() ?: return false
    val yy = e.substring(3, 5).toIntOrNull() ?: return false
    if (mm !in 1..12) return false
    val cal = java.util.Calendar.getInstance()
    val year = cal.get(java.util.Calendar.YEAR) % 100
    val month = cal.get(java.util.Calendar.MONTH) + 1
    return yy > year || (yy == year && mm >= month)
}

fun breakPositions(brand: Brand, len: Int): List<Int> {
    val res = mutableListOf<Int>()
    var acc = 0
    for (g in brand.groups) {
        acc += g
        if (acc < len) res.add(acc) else break
    }
    return res
}

fun formatNumber(n: String, brand: Brand = detectBrand(n)): String {
    val sb = StringBuilder()
    var i = 0
    for (g in brand.groups) {
        if (i >= n.length) break
        if (sb.isNotEmpty()) sb.append(' ')
        sb.append(n.substring(i, minOf(n.length, i + g)))
        i += g
    }
    if (i < n.length) sb.append(' ').append(n.substring(i))
    return sb.toString()
}

data class CardItem(
    val id: Long,
    val bank: String,
    val holder: String,
    val number: String,
    val expiry: String,
    val cvv: String,
    val colorIdx: Int
) {
    val brand: Brand get() = detectBrand(number)
    val formatted: String get() = formatNumber(number, brand)
    val masked: String get() = "•••• •••• •••• " + number.takeLast(4)
}

data class AppSettings(
    val appLock: Boolean = false,
    val autoLock: Boolean = true,
    val authToReveal: Boolean = false,
    val blockScreenshots: Boolean = true,
    val autoHide: Boolean = true,
    val clearClipboard: Boolean = true,
    val haptics: Boolean = true
)

/** Cards and settings are stored in AES-256 encrypted preferences on this device only. */
class CardStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "cards_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun load(): List<CardItem> {
        val arr = JSONArray(prefs.getString("cards", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            CardItem(
                o.getLong("id"), o.getString("bank"), o.getString("holder"),
                o.getString("number"), o.getString("expiry"), o.getString("cvv"),
                o.getInt("color")
            )
        }
    }

    fun save(list: List<CardItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("bank", it.bank); put("holder", it.holder)
                put("number", it.number); put("expiry", it.expiry)
                put("cvv", it.cvv); put("color", it.colorIdx)
            })
        }
        prefs.edit().putString("cards", arr.toString()).apply()
    }

    fun loadSettings(): AppSettings = AppSettings(
        appLock = prefs.getBoolean("s_appLock", false),
        autoLock = prefs.getBoolean("s_autoLock", true),
        authToReveal = prefs.getBoolean("s_authToReveal", false),
        blockScreenshots = prefs.getBoolean("s_blockScreenshots", true),
        autoHide = prefs.getBoolean("s_autoHide", true),
        clearClipboard = prefs.getBoolean("s_clearClipboard", true),
        haptics = prefs.getBoolean("s_haptics", true)
    )

    fun saveSettings(s: AppSettings) {
        prefs.edit()
            .putBoolean("s_appLock", s.appLock)
            .putBoolean("s_autoLock", s.autoLock)
            .putBoolean("s_authToReveal", s.authToReveal)
            .putBoolean("s_blockScreenshots", s.blockScreenshots)
            .putBoolean("s_autoHide", s.autoHide)
            .putBoolean("s_clearClipboard", s.clearClipboard)
            .putBoolean("s_haptics", s.haptics)
            .apply()
    }
}
