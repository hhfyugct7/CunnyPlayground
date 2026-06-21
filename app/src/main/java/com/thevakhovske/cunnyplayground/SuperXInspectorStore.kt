package com.thevakhovske.cunnyplayground

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/** Listing metadata for one captured SuperX notification. */
data class SuperXRecordMeta(
    val id: String,
    val pkg: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val scene: String,
    val template: Int,
    val iconPath: String?,
    val pinned: Boolean
)

/** A single editable leaf field within a captured notification's serialized data. */
data class EditField(val path: List<String>, val type: String, val value: String)

/**
 * Captures every vivo SuperX (原子通知) notification seen on the device, serializes its full extras
 * (all `notification.superx.*` / `island.superx.*` keys, recursively, with icons saved as PNGs) to
 * disk so they can be inspected, edited, and replicated under our own package/NAVIGATION scene.
 *
 * Storage layout under filesDir/inspector/:
 *   records.json                 — array of [SuperXRecordMeta]
 *   <id>/data.json               — typed serialization of the superx extras bundle
 *   <id>/img_*.png, appicon.png  — extracted artwork
 */
object SuperXInspectorStore {
    private const val TAG = "SuperXInspector"
    const val ACTION_UPDATED = "com.thevakhovske.cunnyplayground.INSPECTOR_UPDATED"
    private const val MAX_RECORDS = 60
    private const val THROTTLE_MS = 1200L
    private val io = Executors.newSingleThreadExecutor()
    private val lastCaptureAt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun rootDir(context: Context) = File(context.filesDir, "inspector").apply { if (!exists()) mkdirs() }
    private fun recordsFile(context: Context) = File(rootDir(context), "records.json")
    private fun recordDir(context: Context, id: String) = File(rootDir(context), id).apply { if (!exists()) mkdirs() }

    /** True if a notification's extras carry any SuperX atomic-notification field. */
    fun isSuperX(extras: Bundle?): Boolean {
        if (extras == null) return false
        for (k in extras.keySet()) {
            if (k.startsWith("notification.superx") || k.startsWith("island.superx")) return true
        }
        return false
    }

    private fun sanitize(s: String): String = s.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(90)

    /** Serializes and persists a SuperX notification (off the main thread). Overwrites by sbn.key. */
    fun capture(context: Context, sbn: StatusBarNotification) {
        val appCtx = context.applicationContext
        val extras = sbn.notification.extras ?: return
        if (!isSuperX(extras)) return
        val id = sanitize(sbn.key ?: "${sbn.packageName}_${sbn.id}")
        // Throttle rapidly-updating atomic notifications (e.g. live navigation/delivery).
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - (lastCaptureAt[id] ?: 0L) < THROTTLE_MS) return
        lastCaptureAt[id] = now
        io.execute {
            try {
                val dir = recordDir(appCtx, id)
                dir.listFiles()?.forEach { if (it.name.startsWith("img_")) it.delete() }

                val counter = intArrayOf(0)
                val dataJson = JSONObject()
                for (key in extras.keySet()) {
                    if (key.startsWith("notification.superx") || key.startsWith("island.superx") || key.startsWith("notification.vcard")) {
                        // Per-key guard: a notification's nested bundles often contain the source
                        // app's / vivo's own Parcelable classes (Supplier, lightEffectInfo, …) that
                        // we can't deserialize. Capture everything else rather than aborting.
                        try {
                            @Suppress("DEPRECATION")
                            wrapValue(appCtx, extras.get(key), dir, counter)?.let { dataJson.put(key, it) }
                        } catch (e: Exception) {
                            dataJson.put(key, JSONObject().put("t", "u").put("v", "(unreadable: ${e.javaClass.simpleName})"))
                        }
                    }
                }

                fun safeBaseStr(key: String): String? =
                    try { extras.getBundle("notification.superx.baseInfos")?.getCharSequence(key)?.toString() } catch (e: Exception) { null }
                val title = (extras.getCharSequence("android.title")?.toString()
                    ?: safeBaseStr("notification.superx.baseInfos.title")
                    ?: sbn.packageName)
                val text = (extras.getCharSequence("android.text")?.toString()
                    ?: safeBaseStr("notification.superx.baseInfos.content")
                    ?: "")
                val scene = extras.getString("notification.superx.scene") ?: ""
                val template = extras.getInt("notification.superx.template", 0)
                val appLabel = try {
                    val ai = appCtx.packageManager.getApplicationInfo(sbn.packageName, 0)
                    appCtx.packageManager.getApplicationLabel(ai).toString()
                } catch (e: Exception) { sbn.packageName }

                var iconPath: String? = null
                try {
                    val d = appCtx.packageManager.getApplicationIcon(sbn.packageName)
                    val f = File(dir, "appicon.png")
                    FileOutputStream(f).use { d.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
                    iconPath = f.absolutePath
                } catch (e: Exception) {}

                File(dir, "data.json").writeText(dataJson.toString())

                // Update records.json (newest first, preserve pinned flag, prune unpinned beyond cap).
                val existing = readRecords(appCtx)
                var pinned = false
                val others = JSONArray()
                for (i in 0 until existing.length()) {
                    val r = existing.getJSONObject(i)
                    if (r.getString("id") == id) { pinned = r.optBoolean("pinned", false); continue }
                    others.put(r)
                }
                val meta = JSONObject().apply {
                    put("id", id)
                    put("pkg", sbn.packageName)
                    put("appLabel", appLabel)
                    put("title", title)
                    put("text", text)
                    put("postTime", System.currentTimeMillis())
                    put("scene", scene)
                    put("template", template)
                    put("iconPath", iconPath ?: JSONObject.NULL)
                    put("pinned", pinned)
                }
                val merged = JSONArray().put(meta)
                for (i in 0 until others.length()) merged.put(others.getJSONObject(i))
                val pruned = JSONArray()
                var count = 0
                for (i in 0 until merged.length()) {
                    val r = merged.getJSONObject(i)
                    if (count < MAX_RECORDS || r.optBoolean("pinned", false)) {
                        pruned.put(r); count++
                    } else {
                        recordDir(appCtx, r.getString("id")).deleteRecursively()
                    }
                }
                recordsFile(appCtx).writeText(pruned.toString())
                Log.d(TAG, "Captured SuperX from ${sbn.packageName} ($id)")
                // Notify any open Inspector screen to refresh live.
                try {
                    appCtx.sendBroadcast(Intent(ACTION_UPDATED).setPackage(appCtx.packageName))
                } catch (e: Exception) {}
            } catch (e: Exception) {
                Log.e(TAG, "capture failed", e)
            }
        }
    }

    private fun wrapValue(context: Context, value: Any?, dir: File, counter: IntArray): JSONObject? {
        if (value == null) return null
        return when (value) {
            is Boolean -> JSONObject().put("t", "b").put("v", value)
            is Int -> JSONObject().put("t", "i").put("v", value)
            is Long -> JSONObject().put("t", "l").put("v", value)
            is Float -> JSONObject().put("t", "d").put("v", value.toDouble())
            is Double -> JSONObject().put("t", "d").put("v", value)
            is String -> JSONObject().put("t", "s").put("v", value)
            is Bundle -> {
                // keySet()/get() unparcel the bundle and can throw BadParcelableException when it
                // holds classes we don't have. Guard the whole thing, and each entry individually.
                try {
                    val nested = JSONObject()
                    for (k in value.keySet()) {
                        try {
                            @Suppress("DEPRECATION")
                            wrapValue(context, value.get(k), dir, counter)?.let { nested.put(k, it) }
                        } catch (e: Exception) {
                            nested.put(k, JSONObject().put("t", "u").put("v", "(unreadable: ${e.javaClass.simpleName})"))
                        }
                    }
                    JSONObject().put("t", "bundle").put("v", nested)
                } catch (e: Exception) {
                    JSONObject().put("t", "u").put("v", "(unreadable bundle: ${e.javaClass.simpleName})")
                }
            }
            is Icon -> saveIcon(context, value, dir, counter)?.let { JSONObject().put("t", "icon").put("v", it) }
            is Bitmap -> saveBitmap(value, dir, counter)?.let { JSONObject().put("t", "icon").put("v", it) }
            is PendingIntent -> JSONObject().put("t", "pi")
            is ArrayList<*> -> when (value.firstOrNull()) {
                is Icon -> {
                    val arr = JSONArray()
                    value.forEach { ic -> (ic as? Icon)?.let { saveIcon(context, it, dir, counter)?.let { p -> arr.put(p) } } }
                    JSONObject().put("t", "icons").put("v", arr)
                }
                is String -> {
                    val arr = JSONArray(); value.forEach { arr.put(it as String) }
                    JSONObject().put("t", "sl").put("v", arr)
                }
                is PendingIntent -> JSONObject().put("t", "pilist").put("v", value.size)
                else -> JSONObject().put("t", "u").put("v", value.toString())
            }
            is CharSequence -> JSONObject().put("t", "cs").put("v", value.toString())
            else -> JSONObject().put("t", "u").put("v", value.toString())
        }
    }

    private fun saveIcon(context: Context, icon: Icon, dir: File, counter: IntArray): String? {
        return try {
            val bmp = icon.loadDrawable(context)?.toBitmap() ?: return null
            saveBitmap(bmp, dir, counter)
        } catch (e: Exception) { null }
    }

    private fun saveBitmap(bmp: Bitmap, dir: File, counter: IntArray): String? {
        return try {
            val f = File(dir, "img_${counter[0]++}.png")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            f.absolutePath
        } catch (e: Exception) { null }
    }

    fun readRecords(context: Context): JSONArray {
        return try {
            val f = recordsFile(context)
            if (f.exists()) JSONArray(f.readText()) else JSONArray()
        } catch (e: Exception) { JSONArray() }
    }

    fun list(context: Context): List<SuperXRecordMeta> {
        val arr = readRecords(context)
        val out = ArrayList<SuperXRecordMeta>()
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            out.add(
                SuperXRecordMeta(
                    id = r.getString("id"),
                    pkg = r.optString("pkg"),
                    appLabel = r.optString("appLabel"),
                    title = r.optString("title"),
                    text = r.optString("text"),
                    postTime = r.optLong("postTime"),
                    scene = r.optString("scene"),
                    template = r.optInt("template"),
                    iconPath = r.optString("iconPath").takeIf { it.isNotBlank() && it != "null" },
                    pinned = r.optBoolean("pinned", false)
                )
            )
        }
        return out
    }

    fun loadData(context: Context, id: String): JSONObject? {
        return try {
            val f = File(recordDir(context, id), "data.json")
            if (f.exists()) JSONObject(f.readText()) else null
        } catch (e: Exception) { null }
    }

    fun saveData(context: Context, id: String, json: JSONObject) {
        try { File(recordDir(context, id), "data.json").writeText(json.toString()) } catch (e: Exception) {}
    }

    fun setPinned(context: Context, id: String, pinned: Boolean) {
        val arr = readRecords(context)
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            if (r.getString("id") == id) r.put("pinned", pinned)
        }
        recordsFile(context).writeText(arr.toString())
    }

    fun delete(context: Context, id: String) {
        val arr = readRecords(context)
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            if (r.getString("id") != id) out.put(r)
        }
        recordsFile(context).writeText(out.toString())
        recordDir(context, id).deleteRecursively()
    }

    /** All saved image file paths referenced anywhere in the record data (recursively). */
    fun collectImages(json: JSONObject): List<String> {
        val out = ArrayList<String>()
        fun walk(o: JSONObject) {
            for (k in o.keys()) {
                val w = o.optJSONObject(k) ?: continue
                when (w.optString("t")) {
                    "icon" -> w.optString("v").takeIf { it.isNotBlank() }?.let { out.add(it) }
                    "icons" -> w.optJSONArray("v")?.let { a -> for (i in 0 until a.length()) out.add(a.getString(i)) }
                    "bundle" -> w.optJSONObject("v")?.let { walk(it) }
                }
            }
        }
        walk(json)
        return out
    }

    /** Flattens text/number/boolean leaves into editable fields keyed by their nested path. */
    fun flattenEditable(json: JSONObject): List<EditField> {
        val out = ArrayList<EditField>()
        fun walk(o: JSONObject, prefix: List<String>) {
            for (k in o.keys()) {
                val w = o.optJSONObject(k) ?: continue
                when (w.optString("t")) {
                    "bundle" -> w.optJSONObject("v")?.let { walk(it, prefix + k) }
                    "s", "cs", "i", "l", "d", "b" -> out.add(EditField(prefix + k, w.optString("t"), w.opt("v")?.toString() ?: ""))
                }
            }
        }
        walk(json, emptyList())
        return out
    }

    fun updateField(json: JSONObject, path: List<String>, newValue: String) {
        var cur = json
        for (i in 0 until path.size - 1) {
            cur = cur.optJSONObject(path[i])?.optJSONObject("v") ?: return
        }
        val leaf = cur.optJSONObject(path.last()) ?: return
        when (leaf.optString("t")) {
            "i" -> leaf.put("v", newValue.toIntOrNull() ?: 0)
            "l" -> leaf.put("v", newValue.toLongOrNull() ?: 0L)
            "d" -> leaf.put("v", newValue.toDoubleOrNull() ?: 0.0)
            "b" -> leaf.put("v", newValue.toBoolean())
            else -> leaf.put("v", newValue)
        }
    }

    /** Rebuilds a SuperX extras Bundle from saved JSON, forcing scene → NAVIGATION and a fresh create. */
    fun rebuildBundle(context: Context, id: String): Bundle? {
        val json = loadData(context, id) ?: return null
        val bundle = jsonToBundle(json)
        bundle.putString("notification.superx.scene", "NAVIGATION")
        bundle.putInt("notification.superx.operation", 0)
        bundle.putInt("notification.superx.changedRecord", 0)
        bundle.putBoolean("notification.superx.showNotify", true)
        return bundle
    }

    private fun jsonToBundle(json: JSONObject): Bundle {
        val b = Bundle()
        for (k in json.keys()) {
            val w = json.optJSONObject(k) ?: continue
            when (w.optString("t")) {
                "b" -> b.putBoolean(k, w.optBoolean("v"))
                "i" -> b.putInt(k, w.optInt("v"))
                "l" -> b.putLong(k, w.optLong("v"))
                "d" -> b.putDouble(k, w.optDouble("v"))
                "s" -> b.putString(k, w.optString("v"))
                "cs" -> b.putCharSequence(k, w.optString("v"))
                "sl" -> {
                    val a = w.optJSONArray("v"); val l = ArrayList<String>()
                    if (a != null) for (i in 0 until a.length()) l.add(a.getString(i))
                    b.putStringArrayList(k, l)
                }
                "bundle" -> w.optJSONObject("v")?.let { b.putBundle(k, jsonToBundle(it)) }
                "icon" -> BitmapFactory.decodeFile(w.optString("v"))?.let { b.putParcelable(k, Icon.createWithBitmap(it)) }
                "icons" -> {
                    val a = w.optJSONArray("v"); val l = ArrayList<Icon>()
                    if (a != null) for (i in 0 until a.length()) BitmapFactory.decodeFile(a.getString(i))?.let { l.add(Icon.createWithBitmap(it)) }
                    b.putParcelableArrayList(k, l)
                }
                // "pi", "pilist", "u" → cannot be reconstructed; intentionally skipped
            }
        }
        return b
    }
}
