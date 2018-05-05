package trikita.slide.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class MathView {

    private WebView mMaths;
    private Map<Integer,CompletableFuture<Bitmap>> typesetTasks;
    private Map<Integer,Integer> typesetFgs;
    private int nextTypesetTaskId;
    private int curFg;
    private Handler mHandler;

    MathView() {
        this.typesetTasks = new HashMap<>();
        this.typesetFgs = new HashMap<>();
        nextTypesetTaskId = curFg = 0;

        this.mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                onTypeset(msg.what);
                return true;
            }
        });
    }

    void setWebView(WebView v) {
        if(this.mMaths == v)
            return;

        if(this.typesetTasks.size() == 0 && this.mMaths != null) {
            this.mMaths.removeJavascriptInterface("Android");
        }

        this.mMaths = v;
        v.getSettings().setJavaScriptEnabled(true);
        v.addJavascriptInterface(new MathViewJS(this.mHandler), "Android");
        v.loadUrl("file:///android_asset/www/mathview.html");
    }

    public void colorScheme(int fg, int bg) {
        curFg = fg;
        this.mMaths.loadUrl("javascript:colorScheme('"+Integer.toHexString(fg)+"','"+Integer.toHexString(bg)+"');");
    }

    public CompletableFuture<Bitmap> typeset(String math) {
        this.mMaths.loadUrl("javascript:typeset("+nextTypesetTaskId+", '"+math+"');");

        typesetTasks.put(nextTypesetTaskId, new CompletableFuture<>());
        typesetFgs.put(nextTypesetTaskId, curFg);

        return typesetTasks.get(nextTypesetTaskId++);
        /*int[] colorScheme = Style.COLOR_SCHEMES[App.getState().getCurrentPresentation().colorScheme()];
        String fg = Integer.toHexString(colorScheme[0] - 0xff000000);
        String bg = Integer.toHexString(colorScheme[1] - 0xff000000);
        String fin = this.mCtx.getString(R.string.math_template)
                .replace("__PAGEBG__", pageBg)
                .replace("__BG__", bg)
                .replace("__FG__", fg)
                .replace("__MATH__", math);
        Log.d("MATHTYPESETTING", fin);
        this.mMaths.loadDataWithBaseURL( "file:///android_asset/www/", fin,
            null, null, null
        );*/
    }

    private void onTypeset(int formulaId) {
        if(formulaId == -1) {
            this.typeset("ax^2 + bx + c = 0");
            return;
        }

        CompletableFuture<Bitmap> future = typesetTasks.remove(formulaId);
        int fg = typesetFgs.remove(formulaId);

        Bitmap bmp = Bitmap.createBitmap(this.mMaths.getWidth(), this.mMaths.getHeight(), Bitmap.Config.ARGB_8888);
        this.mMaths.draw(new Canvas(bmp));

        //if(typesetTasks.size() == 0)
        //    this.mMaths.setVisibility(View.INVISIBLE);

        // crop bitmap from right and bottom
        boolean stop;

        // from right
        int right = bmp.getWidth()-1;
        for(stop = false; !stop && right >= 0; --right) {
            stop = bmp.getPixel(right,0) != fg;
        }
        ++right;

        // from right
        int bottom = bmp.getHeight()-1;
        for(stop = false; !stop && bottom >= 0; --bottom) {
            stop = bmp.getPixel(0,bottom) != fg;
        }
        ++bottom;

        if(right == 0 || bottom == 0)
            future.cancel(true);

        future.complete(Bitmap.createBitmap(bmp, 0, 0, right, bottom));
    }

    private static class MathViewJS {
        private Handler mHandler;

        MathViewJS(Handler m) {
            this.mHandler = m;
        }

        @JavascriptInterface
        void onTypeset(int formulaId) {
            Message.obtain(this.mHandler, formulaId).sendToTarget();
        }
    }

}
