package trikita.slide.functions;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;

import java.util.function.BiConsumer;

import trikita.slide.R;
import trikita.slide.ui.Style;

public class LoadingScreenRenderer implements BiConsumer<Canvas,String> {
    private final Context ctx;
    private final int fg, bg;

    public LoadingScreenRenderer(Context ctx, int fg, int bg) {
        this.ctx = ctx;
        this.fg = fg;
        this.bg = bg;
    }

    @Override
    public void accept(Canvas canvas, String s) {
        canvas.drawColor(bg);

        int dstSz = Math.max(1, canvas.getWidth() / 10);

        Bitmap gears = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.gears);
        canvas.drawBitmap(Bitmap.createScaledBitmap(gears, dstSz, dstSz, false), 0, 0, null);

        Paint p = new TextPaint();
        p.setColor(fg);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(dstSz);

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2) - ((p.descent() + p.ascent()) / 2));
        canvas.drawText(s, xPos, yPos, p);
    }
}
