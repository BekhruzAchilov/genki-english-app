package uz.abuhafs.genki

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Serves three things on 127.0.0.1:
 *
 *   /                -> player.html from assets
 *   /ruffle/<file>   -> the Ruffle web build from assets
 *   /games/<file>    -> a game, from local cache, downloading it if missing
 *
 * Because everything is served over http, Flash's relative paths
 * (loadMovie("ANIMAL.SWF")) resolve exactly as they did on the Windows disc.
 * Lookups are case-insensitive, which fixes CAN.swf / xmas.swf without
 * renaming anything on the server.
 */
class GameServer(
    private val ctx: Context,
    port: Int,
    private val baseUrl: String,   // e.g. https://abu-hafs.uz/games/
    private val appKey: String
) : NanoHTTPD("127.0.0.1", port) {

    private val cacheDir = File(ctx.filesDir, "games").apply { mkdirs() }

    /** lowercase name -> real name on the server, from manifest.json */
    private var manifest: Map<String, String> = emptyMap()

    /** lowercase name -> file already on disk */
    private val local = HashMap<String, File>()

    init {
        indexLocalFiles()
        loadManifestFromCache()
    }

    private fun indexLocalFiles() {
        local.clear()
        cacheDir.walkTopDown().filter { it.isFile }.forEach {
            local[it.relativeTo(cacheDir).path.replace('\\', '/').lowercase()] = it
        }
    }

    private fun loadManifestFromCache() {
        val f = File(cacheDir, "manifest.json")
        if (f.exists()) parseManifest(f.readText())
    }

    private fun parseManifest(text: String) {
        val map = HashMap<String, String>()
        val arr = JSONObject(text).getJSONArray("files")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            map[o.getString("key")] = o.getString("path")
        }
        manifest = map
    }

    /** Downloads manifest.json. Called on startup when online. */
    fun refreshManifest(): Boolean = try {
        val bytes = fetch("manifest.json")
        File(cacheDir, "manifest.json").writeBytes(bytes)
        parseManifest(String(bytes, Charsets.UTF_8))
        true
    } catch (e: Exception) {
        false
    }

    private fun fetch(remotePath: String): ByteArray {
        val conn = URL(baseUrl.trimEnd('/') + "/" + remotePath)
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.setRequestProperty("X-App-Key", appKey)
        conn.inputStream.use { return it.readBytes() }
    }

    override fun serve(session: IHTTPSession): Response {
        val path = session.uri.trimStart('/')

        return when {
            path.isEmpty() || path == "index.html" -> asset("player.html", "text/html")
            path.startsWith("ruffle/") -> asset(path, mimeOf(path))
            path.startsWith("games/") -> game(path.removePrefix("games/"))
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
        }
    }

    private fun game(name: String): Response {
        val key = name.lowercase()

        local[key]?.let { return fileResponse(it) }

        // Not cached. Work out the real filename and download it.
        val remote = manifest[key] ?: name
        return try {
            val bytes = fetch(remote)
            val out = File(cacheDir, remote)
            out.parentFile?.mkdirs()
            out.writeBytes(bytes)
            local[key] = out
            newFixedLengthResponse(
                Response.Status.OK, mimeOf(name), ByteArrayInputStream(bytes), bytes.size.toLong()
            )
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "missing: $name")
        }
    }

    private fun fileResponse(f: File) = newFixedLengthResponse(
        Response.Status.OK, mimeOf(f.name), FileInputStream(f), f.length()
    )

    private fun asset(path: String, mime: String): Response {
        return try {
            val s = ctx.assets.open(path)
            newChunkedResponse(Response.Status.OK, mime, s)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "no asset: $path")
        }
    }

    private fun mimeOf(name: String) = when (name.substringAfterLast('.').lowercase()) {
        "html" -> "text/html"
        "js", "mjs" -> "text/javascript"
        "wasm" -> "application/wasm"      // must be exact or Ruffle will not start
        "swf" -> "application/x-shockwave-flash"
        "json" -> "application/json"
        "css" -> "text/css"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "mp3" -> "audio/mpeg"
        "svg" -> "image/svg+xml"
        else -> "application/octet-stream"
    }

    /** How much has been cached so far, for the settings screen. */
    fun cachedBytes(): Long = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun cachedCount(): Int = local.size

    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        local.clear()
        manifest = emptyMap()
    }

    /** Downloads everything in the manifest. Run off the UI thread. */
    fun downloadAll(progress: (done: Int, total: Int) -> Unit) {
        val entries = manifest.entries.toList()
        entries.forEachIndexed { i, (key, remote) ->
            if (!local.containsKey(key)) {
                try {
                    val bytes = fetch(remote)
                    val out = File(cacheDir, remote)
                    out.parentFile?.mkdirs()
                    out.writeBytes(bytes)
                    local[key] = out
                } catch (_: Exception) {
                    // skip and carry on; it will be fetched on demand later
                }
            }
            progress(i + 1, entries.size)
        }
    }
}
