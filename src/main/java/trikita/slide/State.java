package trikita.slide;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.List;

import trikita.jedux.Action;
import trikita.jedux.Store;

@Value.Immutable
@Gson.TypeAdapters
public abstract class State {

    @Nullable
    public abstract String uri();

    public abstract String text();
    public abstract int page();
    public abstract int cursor();

    public abstract boolean presentationMode();
    public abstract boolean toolbarShown();

    public abstract int colorScheme();

    public abstract boolean plantUMLEnabled();
    public abstract String plantUMLEndPoint();

    public abstract String plantUMLTemplateBefore();
    public abstract String plantUMLTemplateAfter();

    public abstract String templateBefore();
    public abstract String templateAfter();

    public abstract int pdfResolution();

    @Value.Lazy
    public List<Slide> slides() {
        return Slide.parse(text());
    }

    static class Reducer implements Store.Reducer<Action<ActionType, ?>, State> {
        public State reduce(Action<ActionType, ?> a, State s) {
            switch (a.type) {
                case LOAD_DOCUMENT:
                    return ImmutableState.copyOf(s)
                        .withUri((a.value).toString());
                case SET_TEXT:
                    return ImmutableState.copyOf(s).withText((String) a.value);
                case SET_CURSOR:
                    String text = s.text().substring(0, (Integer) a.value);
                    return ImmutableState.copyOf(s)
                        .withPage(Slide.parse(text).size()-1)
                        .withCursor((Integer) a.value);
                case NEXT_PAGE:
                    return ImmutableState.copyOf(s)
                            .withPage(Math.min(s.page()+1, s.slides().size()-1));
                case PREV_PAGE:
                    return ImmutableState.copyOf(s)
                            .withPage(Math.max(s.page()-1, 0));
                case OPEN_PRESENTATION:
                    return ImmutableState.copyOf(s).withPresentationMode(true);
                case CLOSE_PRESENTATION:
                    return ImmutableState.copyOf(s).withPresentationMode(false);
                case TOGGLE_TOOLBAR:
                    return ImmutableState.copyOf(s).withToolbarShown(!s.toolbarShown());
                case SET_COLOR_SCHEME:
                    return ImmutableState.copyOf(s).withColorScheme((Integer) a.value);
                case CONFIGURE_PLANTUML:
                    Pair<Boolean,Pair<String,Pair<String,String>>> configuration = (Pair<Boolean,Pair<String,Pair<String,String>>>)a.value;
                    boolean enabled = configuration.first;
                    String endPoint = configuration.second.first;
                    String templateBefore = configuration.second.second.first;
                    String templateAfter = configuration.second.second.second;
                    return ImmutableState.copyOf(s)
                            .withPlantUMLEnabled(enabled)
                            .withPlantUMLEndPoint(endPoint)
                            .withPlantUMLTemplateBefore(templateBefore)
                            .withPlantUMLTemplateAfter(templateAfter);
                case SET_TEMPLATE:
                    Pair<String,String> beforeAfter = (Pair<String,String>)a.value;
                    return ImmutableState.copyOf(s)
                            .withTemplateBefore(beforeAfter.first)
                            .withTemplateAfter(beforeAfter.second);
                case SET_PDF_RESOLUTION:
                    return ImmutableState.copyOf(s)
                            .withPdfResolution((Integer)a.value);
            }
            return s;
        }
    }

    static class Default {
        public static State build(Context c) {
            return ImmutableState.builder()
                    .text(c.getString(R.string.tutorial_text))
                    .page(0)
                    .cursor(0)
                    .colorScheme(0)
                    .presentationMode(false)
                    .toolbarShown(true)
                    .plantUMLEnabled(false)
                    .plantUMLEndPoint("https://plantuml.nitorio.us/png")
                    .plantUMLTemplateBefore("skinparam backgroundcolor transparent\nskinparam dpi 300\n")
                    .plantUMLTemplateAfter("")
                    .templateBefore("")
                    .templateAfter("")
                    .pdfResolution(1)
                    .build();
        }
    }
}
