package trikita.slide.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.concurrent.CompletableFuture;

import trikita.slide.Presentation;
import trikita.slide.functions.Crop;

public class MathView {

    private WebView webView;
    private final Handler mHandler;
    private final Presentation presentation;
    private final String maths;
    private final CompletableFuture<Bitmap> result;

    public MathView(Activity act, Presentation p, String maths) {
        this.presentation = p;
        this.maths = maths;
        this.result = new CompletableFuture<>();

        this.mHandler = new Handler(Looper.getMainLooper(), msg -> {
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

        act.runOnUiThread(() -> {
            this.webView = new WebView(act);
            this.webView.layout(0, 0, p.getPdfWidth(act), p.getPdfHeight(act));
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
        final int[] colorScheme = Style.COLOR_SCHEMES[presentation.colorScheme()];
        this.webView.loadUrl("javascript:typeset('"
            + Integer.toHexString(colorScheme[0]).substring(2) + "','"
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
