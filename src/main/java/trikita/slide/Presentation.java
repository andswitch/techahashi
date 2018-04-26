package trikita.slide;

import android.content.Context;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import trikita.jedux.Action;
import trikita.jedux.Store;

@Value.Immutable
@Gson.TypeAdapters
public abstract class Presentation {
    public abstract String text();

    static class Reducer implements Store.Reducer<Action<ActionType, ?>, Presentation> {
        public Presentation reduce(Action<ActionType, ?> a, Presentation s) {
            switch (a.type) {
                case SET_TEXT:
                    return ImmutablePresentation.copyOf(s).withText((String) a.value);
            }
            return s;
        }
    }

    public static class Default {
        public static Presentation build(Context c) {
            return ImmutablePresentation.builder()
                    .text(c.getString(R.string.tutorial_text))
                    .build();
        }
    }
}
