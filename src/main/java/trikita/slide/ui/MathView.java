package trikita.slide.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import trikita.slide.Presentation;

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
    }

    public CompletableFuture<Bitmap> typeset(String math) {
        TypesetTask task = new TypesetTask(math);
        typesetTasks.add(task);
        Message.obtain(mHandler, 5).sendToTarget();
        return task.result;
    }

    private void onTypeset() {
        CompletableFuture<Bitmap> future = currentTask.result;

        Bitmap bmp = Bitmap.createBitmap(this.webView.getWidth(), this.webView.getHeight(), Bitmap.Config.ARGB_8888);
        this.webView.draw(new Canvas(bmp));

        // crop bitmap from right and bottom
        boolean stop;

        // from right
        int right = bmp.getWidth() - 1;
        for (stop = false; !stop && right >= 0; --right) {
            stop = bmp.getPixel(right, 0) != Style.COLOR_SCHEMES[presentation.colorScheme()][0];
        }
        ++right;

        // from right
        int bottom = bmp.getHeight() - 1;
        for (stop = false; !stop && bottom >= 0; --bottom) {
            stop = bmp.getPixel(0, bottom) != Style.COLOR_SCHEMES[presentation.colorScheme()][0];
        }
        ++bottom;

        if (right == 0 || bottom == 0)
            future.cancel(true);

        future.complete(Bitmap.createBitmap(bmp, 0, 0, right, bottom));

        this.currentTask = null;
        Message.obtain(mHandler, 5).sendToTarget();
    }

    private void onLoaded() {
        int[] colorScheme = Style.COLOR_SCHEMES[presentation.colorScheme()];
        this.webView.loadUrl("javascript:init('"
            +Integer.toHexString(colorScheme[0]).substring(2)+"','"
            +Integer.toHexString(colorScheme[1]).substring(2)
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
