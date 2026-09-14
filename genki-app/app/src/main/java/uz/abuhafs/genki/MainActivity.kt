package uz.abuhafs.genki

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val port = 8723
    private lateinit var web: WebView
    private var server: GameServer? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        goFullscreen()

        web = WebView(this)
        setContentView(web)

        val prefs = getSharedPreferences("genki", Context.MODE_PRIVATE)
        val url = prefs.getString("baseUrl", null)
        val key = prefs.getString("appKey", null)

        if (url.isNullOrBlank() || key.isNullOrBlank()) askForServer() else start(url, key)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun start(baseUrl: String, appKey: String) {
        val s = GameServer(this, port, baseUrl, appKey)
        server = s
        s.start(0, false)

        thread {
            val ok = s.refreshManifest()
            runOnUiThread {
                if (!ok && s.cachedCount() == 0) {
                    toast(getString(R.string.no_server))
                }
            }
        }

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.setBackgroundColor(0xFF000000.toInt())
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(this, "Android")
        web.loadUrl("http://127.0.0.1:$port/")
    }

    /** First launch: ask for the server address and key. */
    private fun askForServer() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 8)
        }
        val urlField = EditText(this).apply {
            hint = getString(R.string.hint_url)
            setText("https://abu-hafs.uz/games/")
        }
        val keyField = EditText(this).apply { hint = getString(R.string.hint_key) }
        box.addView(urlField)
        box.addView(keyField)

        AlertDialog.Builder(this)
            .setTitle(R.string.setup_title)
            .setView(box)
            .setCancelable(false)
            .setPositiveButton(R.string.save) { _, _ ->
                val u = urlField.text.toString().trim()
                val k = keyField.text.toString().trim()
                getSharedPreferences("genki", Context.MODE_PRIVATE).edit()
                    .putString("baseUrl", u).putString("appKey", k).apply()
                start(u, k)
            }
            .show()
    }

    /** Long-press anywhere on the menu to reach this. */
    private fun showSettings() {
        val s = server ?: return
        val mb = s.cachedBytes() / 1048576
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setMessage(getString(R.string.cached_fmt, s.cachedCount(), mb))
            .setPositiveButton(R.string.download_all) { _, _ -> downloadAll() }
            .setNeutralButton(R.string.change_server) { _, _ ->
                getSharedPreferences("genki", Context.MODE_PRIVATE).edit().clear().apply()
                askForServer()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun downloadAll() {
        val s = server ?: return
        toast(getString(R.string.download_started))
        thread {
            s.downloadAll { done, total ->
                if (done % 25 == 0 || done == total) {
                    runOnUiThread { toast("$done / $total") }
                }
            }
            runOnUiThread { toast(getString(R.string.download_done)) }
        }
    }

    private fun goFullscreen() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()
    }

    /** Back button returns to the Genki menu rather than leaving the app. */
    override fun onBackPressed() {
        web.loadUrl("http://127.0.0.1:$port/")
    }

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    /** Called from JavaScript by the faint gear button in player.html. */
    @android.webkit.JavascriptInterface
    fun openSettings() = runOnUiThread { showSettings() }
}
