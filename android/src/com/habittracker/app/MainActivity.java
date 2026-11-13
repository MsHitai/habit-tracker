package com.habittracker.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

// Hosts the tracker UI from assets/www and persists its JSON state in SharedPreferences.
public class MainActivity extends Activity {
    private WebView web;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("habits", MODE_PRIVATE);

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#fbf8f3"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient());
        setContentView(web);
        web.loadUrl("file:///android_asset/www/index.html");
    }

    class Bridge {
        @JavascriptInterface
        public String load() {
            return prefs.getString("state", "");
        }

        @JavascriptInterface
        public void save(String json) {
            prefs.edit().putString("state", json).commit();
            HabitWidget.refresh(MainActivity.this);
        }

        @JavascriptInterface
        public void haptic() {
            web.post(new Runnable() {
                @Override
                public void run() {
                    web.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
            });
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("window.onAndroidBack ? window.onAndroidBack() : false", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String handled) {
                if (!"true".equals(handled)) finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        // The date may have rolled over while the app was in the background.
        web.evaluateJavascript("window.onAppResume && window.onAppResume()", null);
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }
}
