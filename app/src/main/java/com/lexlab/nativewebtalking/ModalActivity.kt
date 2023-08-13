package com.lexlab.nativewebtalking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import org.json.JSONObject

class ModalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modal)

        setupWebView()
    }

    fun setupWebView() {
        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true

        webView.webViewClient = MyWebViewClient()

        webView.loadUrl("file:///android_asset/index.html")
        val jsBridge = JSBridge()
        jsBridge.messageReceived = {
            val json = JSONObject(it)

            val action =  json.getString("action")
            val message =  json.getString("message")

            //Received message from webview in native, process data
            Log.d("HEHE", "message ---> $message")
            Log.d("HEHE", "action ---> $action")

            // Kotlin switch syntax
            if (action == "toggle") {
                // Read the switch element value in the loaded HTML
                val script = "document.getElementById(\"value\").innerText = \"$message\""

                webView.evaluateJavascript(script, {
                    val result = it.let {
                        android.util.Log.d("HEHE", "Label is updated with message: $it")
                    }?.run {
                        android.util.Log.d("HEHE", "An error occurred")
                    }
                })
            }
        }
        webView.addJavascriptInterface(jsBridge, "JSBridge")

        val additionBtn = findViewById<Button>(R.id.additionBtn)
        additionBtn.setOnClickListener {

            // Call the javascript addition function directly from the loaded HTML
            val a = 3
            val b = 4
            val script = "addition($a,$b)"

            webView.evaluateJavascript(script, {
                // Kotlin syntax of iOS if-let
                val result = it.let {
                    Log.d("HEHE","The $a + $b = $it")
                } ?: run {
                    Log.d("HEHE","Error in addition")
                }
            })
        }
    }
}

class MyWebViewClient: WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        val js = """
            var _selector = document.querySelector('input[name=myCheckbox]');
            _selector.addEventListener('change', function(event) {
                var message = (_selector.checked) ? "Toggle Switch is on" : "Toggle Switch is off";
                let responseData = {
                            "message": message,
                            "action": "toggle"
                        };
                // For native iOS
                if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.toggleMessageHandler) {
                    window.webkit.messageHandlers.toggleMessageHandler.postMessage(responseData);
                }
                // For native Android
                else {
                    JSBridge.showMessageInNative(JSON.stringify(responseData));
                }
            });
        """

        view?.evaluateJavascript(js, {
            Log.d("HEHE", it)
        })
    }

}
class JSBridge {

    lateinit var messageReceived: (data: String) -> Unit

    @JavascriptInterface
    fun showMessageInNative(data: String){
        // Execute handler in main thread
        Handler(Looper.getMainLooper()).post {
            messageReceived(data)
        }
    }

    @JavascriptInterface
    fun showMessageInNativeHTML(data: String) {
        val json = JSONObject(data)
        //Received message from webview in native, process data
        Log.d("HEHE2", json.getString("message"))
        Log.d("HEHE2", json.getString("action"))
    }

}