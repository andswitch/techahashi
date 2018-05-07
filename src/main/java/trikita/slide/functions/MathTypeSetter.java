package trikita.slide.functions;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import trikita.slide.ImmutablePresentation;
import trikita.slide.Presentation;
import trikita.slide.ui.MathView;

public class MathTypeSetter implements Function<Presentation,Presentation> {

    protected Context ctx;
    protected MathView mv;
    protected Handler mathHandler;
    protected CompletableFuture<Boolean> mvInited;

    protected static final int INIT_PRESENTATION = 0;

    public MathTypeSetter(Context ctx) {
        this.ctx = ctx;
        this.mvInited = new CompletableFuture<>();
        this.mathHandler = new Handler(msg -> {
            switch(msg.what) {
                case INIT_PRESENTATION:
                    doInitPresentation((Presentation)msg.obj);
                    return true;
            }
            return false;
        });
    }

    @Override
    public Presentation apply(Presentation p) {
        Message.obtain(mathHandler, INIT_PRESENTATION, p).sendToTarget();

        try {
            mvInited.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return ImmutablePresentation
            .copyOf(p)
            .withText(Presentation.joinPages(
                Arrays.stream(p.pages())
                        .map(this::typesetMath)
                        .collect(Collectors.toList())
                        .toArray(new String[]{})
            ));
    }

    protected void doInitPresentation(Presentation p) {
        this.mv = new MathView(this.ctx, p);
        this.mvInited.complete(true);
    }

    protected static final String startMath = "@startmath";

    protected static final boolean isStartMath(String s) {
        return s.startsWith(startMath);
    }

    protected static final boolean isEndMath(String s) {
        return s.startsWith("@endmath");
    }

    protected String typesetMath(String par) {
        List<String> outLines = new ArrayList<>();

        boolean inMath = false;
        List<String> mathLines = null;
        String bgArgs = "";

        for (String line : par.split("\n")) {
            if (inMath) {
                if (isEndMath(line)) {
                    outLines.add(processMath(TextUtils.join("<br>", mathLines)) + bgArgs);
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
            outLines.add(processMath(TextUtils.join("<br>", mathLines)) + bgArgs);
        }

        return TextUtils.join("\n", outLines);
    }

    protected String processMath(String mathLines) {
        CompletableFuture<Bitmap> bmpf = mv.typeset(
            mathLines.trim()
        );

        try {
            Bitmap bmp = bmpf.get();
            return Presentation.asBackground(bmpUri(bmp));
        } catch (Exception e) {
            return mathLines;
        }
    }

    protected Uri bmpUri(Bitmap bmp) throws IOException {
        File bmpFile = File.createTempFile("bmp", ".png", this.ctx.getCacheDir());
        OutputStream os = new FileOutputStream(bmpFile);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
        return Uri.fromFile(bmpFile);
    }
}
