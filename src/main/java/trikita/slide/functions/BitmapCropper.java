package trikita.slide.functions;

import android.graphics.Bitmap;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class BitmapCropper implements BiFunction<Bitmap,Integer,Bitmap> {
    @Override
    public Bitmap apply(Bitmap bitmap, Integer fontSize) {
        final int c = 0x00000000;

        CompletableFuture<Integer> fx1 =
            CompletableFuture.supplyAsync(() -> findBorder(bitmap, fontSize, c, 0, 1, 0));
        CompletableFuture<Integer> fy1 =
            CompletableFuture.supplyAsync(() -> findBorder(bitmap, fontSize, c, 0, 0, 1));
        CompletableFuture<Integer> fx2 =
            CompletableFuture.supplyAsync(() -> findBorder(bitmap, fontSize, c, bitmap.getWidth()-1, -1, 0));
        CompletableFuture<Integer> fy2 =
            CompletableFuture.supplyAsync(() -> findBorder(bitmap, fontSize, c, bitmap.getHeight()-1, 0, -1));

        try {
            int x1 = fx1.get();
            int y1 = fy1.get();
            int x2 = fx2.get();
            int y2 = fy2.get();

            return Bitmap.createBitmap(bitmap,
                x1, y1, x2-x1+1, y2-y1+1
            );
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int findBorder(Bitmap bmp, int fontSize, int c, int start, int dx, int dy) {
        dx = Math.min(1, Math.max(-1, dx));
        dy = Math.min(1, Math.max(-1, dy));

        if((dx != 0 && dy != 0) || (dx == 0 && dy == 0))
            return -1;

        final int fontSizePx = Math.max(1, (int)((double)bmp.getWidth() / 100.0 * (double)fontSize));
        final int stepSize = Math.max(1, (int)Math.ceil((double)fontSizePx * 0.05));

        int px = dx != 0 ? start : 0;
        int py = dy != 0 ? start : 0;

        // phase 1: approximate with stepSize
        while(lineIsSolid(bmp, c, dx != 0 ? px : py, dy, dx)) {
            if (dy != 0) {
                py += dy*stepSize;
                if(py < 0 || py >= bmp.getHeight()) {
                    break;
                }
            } else {
                px += dx*stepSize;
                if(px < 0 || px >= bmp.getWidth()) {
                    break;
                }
            }
        }

        if(dy != 0)
            py = Math.max(0, Math.min(bmp.getHeight()-1, py - dy*stepSize));
        else
            px = Math.max(0, Math.min(bmp.getWidth()-1, px - dx*stepSize));

        // phase 2: pinpoint with 1px steps
        while(lineIsSolid(bmp, c, dx != 0 ? px : py, dy, dx)) {
            if (dy != 0) {
                py += dy;
                if(py < 0) {
                    return 0;
                } else if(py >= bmp.getHeight()) {
                    return bmp.getHeight()-1;
                }
            } else {
                px += dx;
                if(px < 0) {
                    return 0;
                } else if(px >= bmp.getWidth()) {
                    return bmp.getWidth()-1;
                }
            }
        }

        if(dy != 0) return py;
        else return px;
    }

    private boolean lineIsSolid(Bitmap bmp, int c, int p, int dx, int dy) {
        final int e = dx != 0 ? bmp.getWidth() : bmp.getHeight();

        for(int s = 0; s < e; ++s)
            if(bmp.getPixel(dx != 0 ? s : p, dy != 0 ? s : p) != c)
                return false;

        return true;
    }
}
