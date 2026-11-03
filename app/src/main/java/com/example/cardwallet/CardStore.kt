package com.example.cardwallet

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

data class CardItem(
    val id: Long,
    val bank: String,
    val holder: String,
    val number: String,
    val expiry: String,
    val cvv: String,
    val colorIdx: Int
) {
    val brand: String
        get() = when {
            number.startsWith("4") -> "VISA"
            number.startsWith("5") || number.startsWith("2") -> "MASTERCARD"
            number.startsWith("3") -> "AMEX"
            else -> "CARD"
        }
    val formatted: String get() = number.chunked(4).joinToString(" ")
    val masked: String get() = "•••• •••• •••• " + number.takeLast(4)
}

/** কার্ডের তথ্য এনক্রিপ্টেড স্টোরেজে (AES-256) সংরক্ষণ হয় */
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
}
