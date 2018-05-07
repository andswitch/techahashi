package trikita.slide.middleware;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.Presentation;
import trikita.slide.Slide;
import trikita.slide.State;
import trikita.slide.functions.MathTypeSetter;
import trikita.slide.functions.PlantUMLProcessor;
import trikita.slide.functions.PresentationToSlidesProcessor;
import trikita.slide.functions.SlideTemplateProcessor;

public class TaskController implements Store.Middleware<Action<ActionType, ?>, State> {

    protected Context ctx;
    protected CompletableFuture<List<Slide>> generateSlidesTask;

    public TaskController(Context c) {
        super();
        this.ctx = c;
    }

    private void cancelAndRegenerateSlides(Presentation p) {
        if(generateSlidesTask != null)
            generateSlidesTask.cancel(true);

        generateSlidesTask = CompletableFuture.supplyAsync(() -> p)
            .thenApplyAsync(new PlantUMLProcessor())
            .thenApplyAsync(new SlideTemplateProcessor())
            .thenApplyAsync(new MathTypeSetter(this.ctx))
            .thenApplyAsync(new PresentationToSlidesProcessor());
    }

    public List<Slide> getGeneratedSlides(boolean blocking, Consumer<List<Slide>> consumer) {
        if(generateSlidesTask == null) {
            cancelAndRegenerateSlides(App.getState().getCurrentPresentation());
        }

        if(blocking || generateSlidesTask.isDone()) {
            try {
                return generateSlidesTask.get();
            } catch (InterruptedException | ExecutionException e) {
                return new ArrayList<>();
            }
        } else if(consumer != null) {
            generateSlidesTask.thenAcceptAsync(consumer);
        }
        return null;
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> actionTypeAction, Store.NextDispatcher<Action<ActionType, ?>> nextDispatcher) {
        nextDispatcher.dispatch(actionTypeAction);
        switch(actionTypeAction.type) {
            case SET_TEXT:
            case SET_COLOR_SCHEME:
            case CONFIGURE_PLANTUML:
            case SET_TEMPLATE:
            case SET_PDF_RESOLUTION:
                cancelAndRegenerateSlides(App.getState().getCurrentPresentation());
                break;
        }
    }
}
