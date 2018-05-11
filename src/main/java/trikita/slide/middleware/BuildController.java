package trikita.slide.middleware;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import trikita.anvil.Anvil;
import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.Presentation;
import trikita.slide.Slide;
import trikita.slide.State;
import trikita.slide.functions.MathTypeSetter;
import trikita.slide.functions.PlantUMLProcessor;
import trikita.slide.functions.SlideTemplateProcessor;

public class BuildController implements Store.Middleware<Action<ActionType, ?>, State> {

    protected Activity ctx;
    protected Map<Pair<Integer,Integer>, CompletableFuture<Slide>> buildCache;
    protected Handler buildHandler;

    private static final int REGENERATION_TIMEOUT = 300;

    public BuildController() {
        super();
        buildCache = new ConcurrentHashMap<>();
        buildHandler = new Handler(msg -> {
            switch(msg.what) {
            case 0:
                cancelBuildsAndInvalidateBuildCache(true);
                return true;
            }
            return false;
        });
    }
    
    private BuildController cancelBuildsAndInvalidateBuildCache(boolean now) {
        buildHandler.removeMessages(0);
        if(now) {
            Set<Pair<Integer, Integer>> keys = buildCache.keySet();
            for (Pair<Integer, Integer> key : keys) {
                CompletableFuture<Slide> slide = buildCache.get(key);
                if (!slide.isDone()) slide.cancel(true);
                buildCache.remove(key);
            }
            Anvil.render();
        } else {
            buildHandler.sendMessageDelayed(
                Message.obtain(buildHandler, 0),
                REGENERATION_TIMEOUT
            );
        }
        return this;
    }

    public BuildController reportFailure(CompletableFuture<Slide> future) {
        Set<Map.Entry<Pair<Integer,Integer>,CompletableFuture<Slide>>> es = buildCache.entrySet();
        for(Map.Entry<Pair<Integer,Integer>,CompletableFuture<Slide>> e : es) {
            if(e.getValue() == future) {
                buildCache.remove(e.getKey(), e.getValue());
                break;
            }
        }
        return this;
    }

    public CompletableFuture<Slide> build(Presentation p, int page, int width) {
        Pair<Integer,Integer> cacheKey = new Pair<>(page,width);
        if(buildCache.containsKey(cacheKey))
            return buildCache.get(cacheKey);

        CompletableFuture<Slide.Builder> builder =
            CompletableFuture.supplyAsync(() -> p.slideBuilder(page,width))
                .thenApplyAsync(new SlideTemplateProcessor())
                .thenApplyAsync(new MathTypeSetter(ctx));

        if(p.plantUMLEnabled())
            builder = builder.thenApplyAsync(new PlantUMLProcessor());

        CompletableFuture<Slide> future = builder.thenApplyAsync(Slide.Builder::build);
        buildCache.put(cacheKey, future);
        return future;
    }

    public void setActivity(Activity act) { this.ctx = act; }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action, Store.NextDispatcher<Action<ActionType, ?>> nextDispatcher) {
        nextDispatcher.dispatch(action);
        switch(action.type) {
            case SET_TEXT:
            case SET_COLOR_SCHEME:
            case CONFIGURE_PLANTUML:
            case SET_TEMPLATE:
                cancelBuildsAndInvalidateBuildCache(false);
                break;
        }
    }
}
