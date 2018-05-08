package trikita.slide.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import trikita.slide.Presentation;
import trikita.slide.functions.Crop;

public class MathView {

    private WebView webView;
    private boolean webViewLoaded;
    private List<TypesetTask> typesetTasks;
    private TypesetTask currentTask;
    private Handler mHandler;
    private Presentation presentation;

    public MathView(Context ctx, Presentation p) {
        this.typesetTasks = new LinkedList<>();
        this.currentTask = null;
        this.presentation = p;

        this.mHandler = new Handler(msg -> {
            switch(msg.what) {
                case 0:
                    onLoaded();
                    break;
                case 1:
                    onTypeset();
                    break;
                case 5:
                    sendOneTaskIfPossible();
                    break;
            }
            return true;
        });

        this.webViewLoaded = false;
        this.webView = new WebView(ctx);
        this.webView.layout(0, 0, p.getPdfWidth(ctx), p.getPdfHeight(ctx));
        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.addJavascriptInterface(new MathViewJS(this.mHandler), "Android");
        this.webView.loadUrl("file:///android_asset/www/mathview.html");
        this.webView.setBackgroundColor(Color.TRANSPARENT);
        this.webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
    }

    public CompletableFuture<Bitmap> typeset(String math) {
        TypesetTask task = new TypesetTask(math);
        typesetTasks.add(task);
        Message.obtain(mHandler, 5).sendToTarget();
        return task.result;
    }

    private void onTypeset() {
        final CompletableFuture<Bitmap> future = currentTask.result;
        final Bitmap bmp = Bitmap.createBitmap(this.webView.getWidth(), this.webView.getHeight(), Bitmap.Config.ARGB_8888);
        this.webView.draw(new Canvas(bmp));

        CompletableFuture.runAsync(() -> future.complete(new Crop().apply(bmp)));

        this.currentTask = null;
        this.sendOneTaskIfPossible();
    }

    private void onLoaded() {
        int[] colorScheme = Style.COLOR_SCHEMES[presentation.colorScheme()];
        this.webView.loadUrl("javascript:init('"
            +Integer.toHexString(colorScheme[0]).substring(2)
        +"')");
        this.webViewLoaded = true;
        this.sendOneTaskIfPossible();
    }

    private void sendOneTaskIfPossible() {
        if(this.currentTask != null || !this.webViewLoaded || this.typesetTasks.isEmpty())
            return;

        this.currentTask = this.typesetTasks.remove(0);

        this.webView.loadUrl("javascript:typeset('"+this.currentTask.maths+"')");
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

    static class TypesetTask {
        final String maths;
        final CompletableFuture<Bitmap> result;

        TypesetTask(String maths) {
            this.maths = maths;
            this.result = new CompletableFuture<>();
        }
    }

}
