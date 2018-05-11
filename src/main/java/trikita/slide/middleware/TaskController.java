package trikita.slide.middleware;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import trikita.anvil.Anvil;
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

    protected Activity ctx;
    protected CompletableFuture<List<Slide>> generateSlidesTask;
    protected Handler generateSlidesHandler;

    private static final int REGENERATION_TIMEOUT = 300;

    public TaskController() {
        super();
        generateSlidesHandler = new Handler(msg -> {
            switch(msg.what) {
            case 0:
                Pair<Presentation, Consumer<List<Slide>>> p =
                        (Pair<Presentation, Consumer<List<Slide>>>) msg.obj;
                cancelAndRegenerateSlides(true, p.first, p.second);
                return true;
            case 1:
                Anvil.render();
                return true;
            }
            return false;
        });
    }

    public void setActivity(Activity act) { this.ctx = act; }

    private void cancelAndRegenerateSlides(boolean now, Presentation p, Consumer<List<Slide>> consumer) {
        if(generateSlidesTask != null)
            try {
                generateSlidesTask.cancel(true);
            } catch(CancellationException ignored) {
            }

        generateSlidesHandler.removeMessages(0);

        if(now) {
            generateSlidesTask = CompletableFuture.supplyAsync(() -> p)
                    .thenApplyAsync(new PlantUMLProcessor())
                    .thenApplyAsync(new SlideTemplateProcessor())
                    .thenApplyAsync(new MathTypeSetter(this.ctx))
                    .thenApplyAsync(new PresentationToSlidesProcessor());
            if (consumer != null)
                generateSlidesTask.thenAcceptAsync(consumer);
        } else {
            generateSlidesHandler.sendMessageDelayed(
                Message.obtain(generateSlidesHandler, 0, new Pair<>(p, consumer)),
                REGENERATION_TIMEOUT
            );
        }
    }

    public List<Slide> getGeneratedSlides(boolean blocking) {
        if(blocking && generateSlidesTask == null) {
            cancelAndRegenerateSlides(true, App.getState().getCurrentPresentation(), null);
        }

        if(blocking || (generateSlidesTask != null && generateSlidesTask.isDone())) {
            try {
                return generateSlidesTask.get();
            } catch (InterruptedException | ExecutionException | CancellationException ignored) {
            }
        }

        return new ArrayList<>();
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action, Store.NextDispatcher<Action<ActionType, ?>> nextDispatcher) {
        nextDispatcher.dispatch(action);
        switch(action.type) {
            case INIT:
            case SET_TEXT:
            case SET_COLOR_SCHEME:
            case CONFIGURE_PLANTUML:
            case SET_TEMPLATE:
            case SET_PDF_RESOLUTION:
                cancelAndRegenerateSlides(
                    false,
                    App.getState().getCurrentPresentation(),
                    slides -> Message.obtain(generateSlidesHandler, 1).sendToTarget()
                );
                break;
        }
    }
}
