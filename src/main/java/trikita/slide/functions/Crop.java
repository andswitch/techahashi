package trikita.slide.functions;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public class Crop implements Function<Bitmap,Bitmap> {
    @Override
    public Bitmap apply(Bitmap bitmap) {
        final int c = 0x00000000;

        CompletableFuture<Integer> fx1 = CompletableFuture.supplyAsync(() -> findBorder(bitmap, c, 0, 1, 0));
        CompletableFuture<Integer> fy1 = CompletableFuture.supplyAsync(() -> findBorder(bitmap, c, 0, 0, 1));
        CompletableFuture<Integer> fx2 = CompletableFuture.supplyAsync(() -> findBorder(bitmap, c, bitmap.getWidth()-1, -1, 0));
        CompletableFuture<Integer> fy2 = CompletableFuture.supplyAsync(() -> findBorder(bitmap, c, bitmap.getHeight()-1, 0, -1));

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

    private int findBorder(Bitmap bmp, int c, int start, int dx, int dy) {
        dx = Math.min(1, Math.max(-1, dx));
        dy = Math.min(1, Math.max(-1, dy));

        if((dx != 0 && dy != 0) || (dx == 0 && dy == 0))
            return -1;

        int px = dx != 0 ? start : 0;
        int py = dy != 0 ? start : 0;

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

    private static class LineIsSolidData {
        AtomicInteger reportedTasks = new AtomicInteger(0);
        AtomicBoolean anomalyFound = new AtomicBoolean(false);
    }

    private boolean lineIsSolid(Bitmap bmp, int c, int p, int dx, int dy) {
        final CompletableFuture<Boolean> f = new CompletableFuture<>();
        final int e = dx != 0 ? bmp.getWidth() : bmp.getHeight();

        final LineIsSolidData lisd = new LineIsSolidData();

        for(int s = 0; !lisd.anomalyFound.get() && s < e; ++s) {
            final int ss = s;
            CompletableFuture.runAsync(() -> {
                if(lisd.anomalyFound.get()) return;

                if(bmp.getPixel(dx != 0 ? ss : p, dy != 0 ? ss : p) != c) {
                    if (lisd.anomalyFound.compareAndSet(false, true))
                        f.complete(false);
                    return;
                }

                if(lisd.reportedTasks.incrementAndGet() == e)
                    f.complete(true);
            });
        }

        try {
            return f.get();
        } catch (InterruptedException | ExecutionException | CancellationException e1) {
            return false;
        }
    }
}
