package trikita.slide.functions;

import android.text.TextUtils;

import net.sourceforge.plantuml.code.AsciiEncoder;
import net.sourceforge.plantuml.code.URLEncoder;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.Deflater;

import trikita.slide.Presentation;
import trikita.slide.Slide;

public class PlantUMLProcessor implements Callable<Slide.Builder> {
    protected final Slide.Builder p;

    public PlantUMLProcessor(Slide.Builder b) {
        this.p = b;
    }

    @Override
    public Slide.Builder call() {
        return parsePlantUML(p);
    }

    private final String startUML = "@startuml";

    private boolean isStartUML(String s) {
        return s.trim().toLowerCase().startsWith(startUML);
    }

    private boolean isEndUML(String s) {
        return s.trim().toLowerCase().startsWith("@enduml");
    }

    private Slide.Builder parsePlantUML(Slide.Builder p) {
        final String par = p.text;

        List<String> outLines = new ArrayList<>();

        boolean inUML = false;
        List<String> umlLines = null;
        String bgArgs = "";

        for (String line : par.split("\n")) {
            if (inUML) {
                if (isEndUML(line)) {
                    outLines.add(processPlantUML(p, TextUtils.join("\n", umlLines)) + bgArgs);
                    inUML = false;
                    umlLines = null;
                    bgArgs = "";
                } else {
                    if(Presentation.possibleLineBreak(line))
                        umlLines.add("");
                    else
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

        if(umlLines != null) {
           outLines.add(processPlantUML(p, TextUtils.join("\n", umlLines)) + bgArgs);
        }

        return p.withText(TextUtils.join("\n", outLines));
    }

    private String processPlantUML(Slide.Builder p, String s) {
        try {
            String payload = p.presentation.plantUMLTemplateBefore() + s + p.presentation.plantUMLTemplateAfter();
            return "@" + p.presentation.plantUMLEndPoint() +
                    "/" + encodePlantUML(payload.trim());
        } catch (Exception e) {
            return "@http://s2.quickmeme.com/img/17/17637236ce6b1eb8a807f5b871c81b0269d72ef2a89265e1b23cf3f8e741a6d2.jpg";
        }
    }

    private String encodePlantUML(String s) throws UnsupportedEncodingException {
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
