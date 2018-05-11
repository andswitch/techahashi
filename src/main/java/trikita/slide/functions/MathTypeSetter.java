package trikita.slide.functions;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import trikita.slide.App;
import trikita.slide.ImmutablePresentation;
import trikita.slide.Presentation;
import trikita.slide.Slide;
import trikita.slide.ui.MathView;

public class MathTypeSetter implements Function<Slide.Builder,Slide.Builder> {

    protected Activity ctx;

    public MathTypeSetter(Activity ctx) {
        this.ctx = ctx;
    }

    @Override
    public Slide.Builder apply(Slide.Builder b) {
        return typesetMath(b);
    }

    protected static final String startMath = "@startmath";

    protected static final boolean isStartMath(String s) {
        return s.startsWith(startMath);
    }

    protected static final boolean isEndMath(String s) {
        return s.startsWith("@endmath");
    }

    protected Slide.Builder typesetMath(Slide.Builder b) {
        final String par = b.text;

        List<String> outLines = new ArrayList<>();

        boolean inMath = false;
        List<String> mathLines = null;
        String bgArgs = "";

        for (String line : par.split("\n")) {
            if (inMath) {
                if (isEndMath(line)) {
                    outLines.add(processMath(b, TextUtils.join("<br>", mathLines)) + bgArgs);
                    inMath = false;
                    mathLines = null;
                    bgArgs = "";
                } else {
                    if (Presentation.possibleLineBreak(line))
                        mathLines.add("");
                    else
                        mathLines.add(line);
                }
            } else {
                if (isStartMath(line)) {
                    mathLines = new ArrayList<>();
                    inMath = true;
                    bgArgs = line.trim().substring(startMath.length());
                    if (!bgArgs.isEmpty() && bgArgs.charAt(0) != ' ') bgArgs = " " + bgArgs;
                } else {
                    outLines.add(line);
                }
            }
        }

        if (mathLines != null) {
            outLines.add(processMath(b, TextUtils.join("<br>", mathLines)) + bgArgs);
        }

        return b.withText(TextUtils.join("\n", outLines));
    }

    protected String processMath(Slide.Builder b, String mathLines) {
        CompletableFuture<Bitmap> bmpf = new MathView(this.ctx, b, mathLines.trim()).futureBitmap();

        try {
            Bitmap bmp = bmpf.get(30, TimeUnit.SECONDS);
            return Presentation.asBackground(bmpUri(bmp));
        } catch(TimeoutException e) {
            throw new CancellationException(e.getMessage());
        } catch (Exception ignored) {
        }

        return mathLines;
    }

    protected Uri bmpUri(Bitmap bmp) throws IOException {
        File bmpFile = File.createTempFile("bmp", ".png", this.ctx.getCacheDir());
        OutputStream os = new FileOutputStream(bmpFile);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
        return Uri.fromFile(bmpFile);
    }
}
