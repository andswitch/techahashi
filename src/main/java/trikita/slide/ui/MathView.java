package trikita.slide.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.concurrent.CompletableFuture;

import trikita.slide.Presentation;
import trikita.slide.Slide;
import trikita.slide.functions.Crop;

public class MathView {

    private WebView webView;
    private Handler mHandler;
    private final Slide.Builder builder;
    private final String maths;
    private final CompletableFuture<Bitmap> result;

    public MathView(Activity act, Slide.Builder b, String maths) {
        this.builder = b;
        this.maths = maths;
        this.result = new CompletableFuture<>();

        act.runOnUiThread(() -> {
            this.mHandler = new Handler(msg -> {
                switch (msg.what) {
                    case 0:
                        onLoaded();
                        break;
                    case 1:
                        onTypeset();
                        break;
                }
                return true;
            });

            this.webView = new WebView(act);
            this.webView.layout(0, 0, b.width, b.height);
            this.webView.getSettings().setJavaScriptEnabled(true);
            this.webView.addJavascriptInterface(new MathViewJS(this.mHandler), "Android");
            this.webView.loadUrl("file:///android_asset/www/mathview.html");
            this.webView.setBackgroundColor(Color.TRANSPARENT);
            this.webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        });
    }

    private void onTypeset() {
        final Bitmap bmp = Bitmap.createBitmap(this.webView.getWidth(), this.webView.getHeight(), Bitmap.Config.ARGB_8888);
        this.webView.draw(new Canvas(bmp));
        CompletableFuture.runAsync(() -> result.complete(new Crop().apply(bmp)));
    }

    private void onLoaded() {
        Log.d("SLIDE","onLoaded");
        int fg = Style.COLOR_SCHEMES[builder.presentation.colorScheme()][0];
        this.webView.loadUrl("javascript:typeset('"
            + Integer.toHexString(fg).substring(2) + "','"
            + maths.trim()
        + "')");
    }

    public CompletableFuture<Bitmap> futureBitmap() {
        return result;
    }

    private static class MathViewJS {
        private Handler mHandler;

        MathViewJS(Handler m) {
            this.mHandler = m;
        }

        @JavascriptInterface
        void onTypeset() {
            Message.obtain(this.mHandler, 1).sendToTarget();
        }

        @JavascriptInterface
        void onLoaded() {
            Message.obtain(this.mHandler, 0).sendToTarget();
        }
    }

}
