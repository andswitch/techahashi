package trikita.slide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.Gravity;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import net.sourceforge.plantuml.code.AsciiEncoder;
import net.sourceforge.plantuml.code.URLEncoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

import trikita.anvil.Anvil;

public class Slide {

    private static class Background {
        private static final Map<String, Integer> GRAVITY = new HashMap<>();

        static {
            GRAVITY.put("left", Gravity.LEFT);
            GRAVITY.put("top", Gravity.TOP);
            GRAVITY.put("right", Gravity.RIGHT);
            GRAVITY.put("bottom", Gravity.BOTTOM);
            GRAVITY.put("center", Gravity.CENTER);
            GRAVITY.put("w", Gravity.LEFT);
            GRAVITY.put("n", Gravity.TOP);
            GRAVITY.put("e", Gravity.RIGHT);
            GRAVITY.put("s", Gravity.BOTTOM);
            GRAVITY.put("nw", Gravity.LEFT | Gravity.TOP);
            GRAVITY.put("sw", Gravity.LEFT | Gravity.BOTTOM);
            GRAVITY.put("ne", Gravity.RIGHT | Gravity.TOP);
            GRAVITY.put("se", Gravity.RIGHT | Gravity.BOTTOM);
        }

        private final String url;
        private final float scale;
        private final int gravity;

        public Background(String bg) {
            int g = Gravity.CENTER;
            float scale = 1f;
            String url = null;
            for (String part : bg.split("\\s+")) {
                if (part.endsWith("%")) {
                    try {
                        scale = Float.parseFloat(part.substring(0, part.length() - 1)) / 100;
                    } catch (NumberFormatException ignored) {
                    }
                } else if (GRAVITY.containsKey(part)) {
                    g = GRAVITY.get(part);
                } else if (part.contains("://")) {
                    url = part;
                }
            }
            this.url = url;
            this.scale = scale;
            this.gravity = g;
        }
    }

    private final List<Background> backgrounds = new ArrayList<>();
    private final Map<String, CacheTarget> bitmaps = new HashMap<>();
    private final SpannableStringBuilder text = new SpannableStringBuilder();

    private Slide(String s) {
        int emSpanStart = -1;
        int codeSpanStart = -1;
        for (String line : s.split("\n")) {
            if (line.startsWith("@")) {
                backgrounds.add(new Background(line.substring(1)));
            } else if (line.startsWith("#")) {
                int start = text.length();
                text.append(line.substring(1)).append('\n');
                text.setSpan(new RelativeSizeSpan(1.6f), start, text.length(), 0);
                text.setSpan(new StyleSpan(Typeface.BOLD), start, text.length(), 0);
            } else if (line.startsWith("  ")) {
                int start = text.length();
                text.append(line.substring(2)).append('\n');
                text.setSpan(new TypefaceSpan("monospace"), start, text.length(), 0);
            } else {
                if (line.startsWith(".")) {
                    line = line.substring(1);
                }
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    // Handle emphasis
                    if (c == '*') {
                        if (emSpanStart == -1) {
                            emSpanStart = text.length();
                        } else {
                            if (emSpanStart != text.length()) {
                                text.setSpan(new StyleSpan(Typeface.BOLD), emSpanStart, text.length(), 0);
                            } else {
                                text.append('*');
                            }
                            emSpanStart = -1;
                        }
                    // Handle inline code
                    } else if (c == '`') {
                        if (codeSpanStart == -1) {
                            codeSpanStart = text.length();
                        } else {
                            if (codeSpanStart != text.length()) {
                                text.setSpan(new TypefaceSpan("monospace"), codeSpanStart, text.length(), 0);
                            } else {
                                text.append('`');
                            }
                            codeSpanStart = -1;
                        }
                    } else {
                        text.append(c);
                    }
                }
                text.append('\n');
            }
        }
        if (text.length() > 0 && text.charAt(text.length() - 1) == '\n') {
            text.delete(text.length() - 1, text.length());
        }
    }

    private static class CacheTarget implements Target {
        private Bitmap cacheBitmap;

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            cacheBitmap = bitmap;
            if (from != Picasso.LoadedFrom.MEMORY) {
                Anvil.render();
            }
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }

        public Bitmap getCacheBitmap() {
            return cacheBitmap;
        }
    }

    public void render(Context c, Canvas canvas, int width, int height, String typeface, int fg, int bg, boolean blocking) {
        TextPaint textPaint = new TextPaint();
        canvas.drawColor(bg);
        textPaint.setColor(fg);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(typeface, Typeface.NORMAL));

        for (Background img : backgrounds) {
            if (img.url != null) {
                Bitmap b = null;
                CacheTarget cacheTarget = new CacheTarget();
                bitmaps.put(img.url, cacheTarget);
                RequestCreator request = Picasso.get()
                        .load(img.url);
                if (img.scale > 0) {
                    request = request.resize((int) (width * img.scale), (int) (height * img.scale));
                } else {
                    request = request.resize(width, height);
                }
                request = request.centerInside();
                if (blocking) {
                    try {
                        b = request.get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    request.into(cacheTarget);
                    b = cacheTarget.getCacheBitmap();
                }
                if (b != null) {
                    Rect r = new Rect();
                    Gravity.apply(img.gravity, b.getWidth(), b.getHeight(),
                            new Rect(0, 0, width, height), r);
                    canvas.drawBitmap(b, r.left, r.top, textPaint);
                }
            }
        }

        float margin = 0.1f;

        int w = (int) (width * (1 - margin * 2));
        int h = (int) (height * (1 - margin * 2));

        for (int textSize = height; textSize > 1; textSize--) {
            textPaint.setTextSize(textSize);
            if (StaticLayout.getDesiredWidth(text, textPaint) <= w) {
                StaticLayout layout = new StaticLayout(text, textPaint, w, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
                if (layout.getHeight() >= h) {
                    continue;
                }
                int l = 0;
                for (int i = 0; i < layout.getLineCount(); i++) {
                    int m = (int) (width - layout.getLineWidth(i)) / 2;
                    if (i == 0 || m < l) {
                        l = m;
                    }
                }
                canvas.translate(l, (height - layout.getHeight()) / 2);

                textPaint.setColor(bg);
                textPaint.setStyle(Paint.Style.STROKE);
                textPaint.setStrokeWidth(8);
                layout = new StaticLayout(text, textPaint, w, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
                layout.draw(canvas);
                textPaint.setColor(fg);
                textPaint.setStyle(Paint.Style.FILL);
                textPaint.setStrokeWidth(0);
                layout = new StaticLayout(text, textPaint, w, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
                layout.draw(canvas);
                return;
            }
        }
    }

    public static List<Slide> parse(String s) {
        List<Slide> slides = new ArrayList<>();

        String[] paragraphs = s.split("(\n){2,}");

        if(App.getState().plantUMLEnabled())
            paragraphs = joinPlantUMLDiagrams(paragraphs);

        for (String par : paragraphs) {
            String finalPar = (App.getState().templateBefore() + par + App.getState().templateAfter()).trim();
            if(App.getState().plantUMLEnabled())
                slides.add(new Slide(parsePlantUML(finalPar)));
            else
                slides.add(new Slide(finalPar));
        }
        return slides;
    }

    private static String[] joinPlantUMLDiagrams(String[] paragraphs) {
        List<String> finalParagraphs = new ArrayList<>();

        boolean inUMLDiagram = false;

        for (String par : paragraphs) {
            if(inUMLDiagram) {
                String p = finalParagraphs.get(finalParagraphs.size()-1);
                p += "\n" + par;
                finalParagraphs.set(finalParagraphs.size()-1, p);
            } else {
                finalParagraphs.add(par);
            }

            for(String line : par.split("\n")) {
                if(isStartUML(line)) inUMLDiagram = true;
                else if(isEndUML(line)) inUMLDiagram = false;
            }
        }

        return finalParagraphs.toArray(new String[]{});
    }

    private static String startUML = "@startuml";

    private static boolean isStartUML(String s) {
        return s.trim().toLowerCase().startsWith(startUML);
    }

    private static boolean isEndUML(String s) {
        return s.trim().toLowerCase().startsWith("@enduml");
    }

    private static String parsePlantUML(String par) {
        List<String> outLines = new ArrayList<>();

        boolean inUML = false;
        List<String> umlLines = null;
        String bgArgs = "";

        for (String line : par.split("\n")) {
            if (inUML) {
                if (isEndUML(line)) {
                    outLines.add(processPlantUML(TextUtils.join("\n", umlLines)) + bgArgs);
                    inUML = false;
                    umlLines = null;
                    bgArgs = "";
                } else {
                    umlLines.add(line);
                }
            } else {
                if (isStartUML(line)) {
                    umlLines = new ArrayList<>();
                    inUML = true;
                    bgArgs = line.trim().substring(startUML.length());
                    if(!bgArgs.isEmpty() && bgArgs.charAt(0) != ' ') bgArgs = " " + bgArgs;
                } else {
                    outLines.add(line);
                }
            }
        }

        return TextUtils.join("\n", outLines);
    }

    private static String processPlantUML(String s) {
        try {
            String payload = App.getState().plantUMLTemplateBefore() + s + App.getState().plantUMLTemplateAfter();
            return "@" + App.getState().plantUMLEndPoint() +
                    "/" + encodePlantUML(payload.trim());
        } catch (Exception e) {
            return "@http://s2.quickmeme.com/img/17/17637236ce6b1eb8a807f5b871c81b0269d72ef2a89265e1b23cf3f8e741a6d2.jpg";
        }
    }

    private static String encodePlantUML(String s) throws UnsupportedEncodingException {
        // Deflate
        byte[] input = s.getBytes("UTF-8");
        byte[] output = new byte[input.length];
        Deflater compressor = new Deflater(Deflater.BEST_COMPRESSION, true);
        compressor.setInput(input);
        compressor.finish();
        int compressedSize = compressor.deflate(output);
        compressor.end();

        byte[] finalOutput = new byte[compressedSize];
        System.arraycopy(output, 0, finalOutput, 0, compressedSize);

        URLEncoder encoder = new AsciiEncoder();
        return encoder.encode(finalOutput);
    }
}
