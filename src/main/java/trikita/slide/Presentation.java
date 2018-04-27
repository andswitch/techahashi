package trikita.slide;

import android.content.Context;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Gson.TypeAdapters
public abstract class Presentation {

    public abstract int page();
    public abstract int cursor();

    public abstract String text();

    public abstract int colorScheme();

    public abstract String plantUMLEndPoint();
    public abstract boolean plantUMLEnabled();

    public abstract String plantUMLTemplateBefore();
    public abstract String plantUMLTemplateAfter();

    public abstract String templateBefore();
    public abstract String templateAfter();

    public abstract int pdfResolution();

    @Value.Lazy
    public List<Slide> slides() {
        return Slide.parse(text());
    }

    public static class Default {
        public static ImmutablePresentation build(Context c) {
            return ImmutablePresentation.builder()
                    .page(0)
                    .cursor(0)
                    .text(c.getString(R.string.tutorial_text))
                    .colorScheme(0)
                    .plantUMLEndPoint("https://plantuml.nitorio.us/png")
                    .plantUMLEnabled(false)
                    .plantUMLTemplateBefore("skinparam backgroundcolor transparent\nskinparam dpi 300\n")
                    .plantUMLTemplateAfter("")
                    .templateBefore("")
                    .templateAfter("")
                    .pdfResolution(1)
                    .build();
        }
    }
}
