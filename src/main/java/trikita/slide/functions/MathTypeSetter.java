package trikita.slide.functions;

public class MathTypeSetter {

//    private Context mCtx;
//    private WebView mMaths;
//
//    public MathTypeSetter(Context ctx, WebView v) {
//        this.mCtx = ctx;
//
//        this.mMaths = v;
//        this.mMaths.getSettings().setJavaScriptEnabled(true);
//        this.mMaths.setWebViewClient(new MathViewClient(this));
//    }
//
//    @Override
//    public Presentation apply(Presentation p) {
//        return null;
//    }
//
//    public void typeSet(String math) {
//        int[] colorScheme = Style.COLOR_SCHEMES[App.getState().getCurrentPresentation().colorScheme()];
//        String fg = Integer.toHexString(colorScheme[0] - 0xff000000);
//        String bg = Integer.toHexString(colorScheme[1] - 0xff000000);
//        String pageBg = Integer.toHexString(0xffffff - (colorScheme[1] - 0xff000000));
//        String fin = this.mCtx.getString(R.string.math_template)
//                .replace("__PAGEBG__", pageBg)
//                .replace("__BG__", bg)
//                .replace("__FG__", fg)
//                .replace("__MATH__", math);
//        Log.d("MATHTYPESETTING", fin);
//        this.mMaths.loadDataWithBaseURL( "file:///android_asset/www/", fin,
//            null, null, null
//        );
//    }
//
//    public void prepareForDrawing() {
//        this.mMaths.setVisibility(View.VISIBLE);
//    }
//
//    private void onPageFinished() {
//        Bitmap bmp = Bitmap.createBitmap(this.mMaths.getWidth(), this.mMaths.getHeight(), Bitmap.Config.ARGB_8888);
//        this.mMaths.draw(new Canvas(bmp));
//        //this.mMaths.setVisibility(View.INVISIBLE);
//
//        // crop bitmap from right and bottom
//        int bg = Style.COLOR_SCHEMES[App.getState().getCurrentPresentation().colorScheme()][1];
//        int colorToCrop = 0xffffff - (bg - 0xff000000) + 0xff000000;
//        boolean stop;
//
//        // from right
//        int right = bmp.getWidth()-1;
//        for(stop = false; !stop && right >= 0; --right) {
//            stop = bmp.getPixel(right,0) != colorToCrop;
//        }
//        ++right;
//
//        // from right
//        int bottom = bmp.getHeight()-1;
//        for(stop = false; !stop && bottom >= 0; --bottom) {
//            stop = bmp.getPixel(0,bottom) != colorToCrop;
//        }
//        ++bottom;
//
//        //App.dispatch(new Action<>(ActionType.MATH_TYPESET_COMPLETE, Bitmap.createBitmap(bmp, 0, 0, right, bottom)));
//    }
//
//    private static class MathViewClient extends WebViewClient {
//        private MathTypeSetter mHndlr;
//
//        MathViewClient(MathTypeSetter mHdnlr) {
//            this.mHndlr = mHdnlr;
//        }
//
//        @Override
//        public void onPageFinished(WebView v, String url) {
//            mHndlr.prepareForDrawing();
//            Log.i("MATHTYPESETTER", "Page finished");
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mHndlr.onPageFinished();
//                }
//            }, 600);
//        }
//    }

}
