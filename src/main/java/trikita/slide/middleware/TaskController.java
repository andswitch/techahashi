package trikita.slide.middleware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.Presentation;
import trikita.slide.Slide;
import trikita.slide.State;
import trikita.slide.functions.PlantUMLProcessor;
import trikita.slide.functions.PresentationToSlidesProcessor;
import trikita.slide.functions.SlideTemplateProcessor;

public class TaskController implements Store.Middleware<Action<ActionType, ?>, State> {

    private CompletableFuture<List<Slide>> generateSlidesTask;

    public TaskController(Presentation p) {
        super();
        cancelAndRegenerateSlides(p);
    }

    private void cancelAndRegenerateSlides(Presentation p) {
        if(generateSlidesTask != null)
            generateSlidesTask.cancel(true);

        generateSlidesTask = CompletableFuture.supplyAsync(() -> p)
            .thenApplyAsync(new PlantUMLProcessor())
            .thenApplyAsync(new SlideTemplateProcessor())
            .thenApplyAsync(new PresentationToSlidesProcessor());
    }

    public List<Slide> getGeneratedSlides() {
        return generateSlidesTask.getNow(new ArrayList<>());
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> actionTypeAction, Store.NextDispatcher<Action<ActionType, ?>> nextDispatcher) {
        nextDispatcher.dispatch(actionTypeAction);
        switch(actionTypeAction.type) {
            case SET_TEXT:
            case SET_COLOR_SCHEME:
            case CONFIGURE_PLANTUML:
            case SET_TEMPLATE:
                cancelAndRegenerateSlides(App.getState().getCurrentPresentation());
                break;
        }
    }
}
