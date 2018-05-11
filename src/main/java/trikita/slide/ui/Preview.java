package trikita.slide.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import trikita.anvil.Anvil;
import trikita.jedux.Action;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.Presentation;
import trikita.slide.Slide;

public class Preview extends View implements View.OnTouchListener {

    Activity ctx;

    public Preview(Context context) {
        super(context);
        this.ctx = (Activity)context;
        this.setOnTouchListener(this);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if ((w * 9/16) > h) {
            w = h * 16/9;
        } else {
            h = w * 9/16;
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
    }

    protected void onDraw(Canvas canvas) {
        Presentation p = App.getState().getCurrentPresentation();
        int page = p.pageForCursor(App.getMainLayout().cursor());

        CompletableFuture<Slide> slide = App.getBuildController().build(p, page, canvas.getWidth());
        if(slide.isDone())
            try {
                slide.get().render(canvas, Style.SLIDE_FONT, false);
            } catch(Exception ignored) {
                ignored.printStackTrace();
            }
        else {
            canvas.drawColor(Style.COLOR_SCHEMES[p.colorScheme()][1]);
            slide.handleAsync((r,e) -> {
                if(e != null) App.getBuildController().reportFailure(slide);
                Anvil.render();
                return e;
            });
        }
    }

    private float downX;
    private float downY;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()){
        case MotionEvent.ACTION_DOWN:
            downX = event.getX();
            downY = event.getY();
            return true;

        case MotionEvent.ACTION_UP:
            float upX = event.getX();
            float upY = event.getY();

            float deltaX = downX - upX;
            float deltaY = downY - upY;

            // Click
            if(Math.abs(deltaX) < 90 && Math.abs(deltaY) < 90) {
                if(App.getState().presentationMode())
                    App.dispatch(new Action<>(ActionType.CLOSE_PRESENTATION));
                else
                    App.dispatch(new Action<>(ActionType.OPEN_PRESENTATION));
                return true;
            }

            //HORIZONTAL SCROLL
            if(Math.abs(deltaX) > Math.abs(deltaY))
            {
                int min_distance = 100;
                if(Math.abs(deltaX) > min_distance){
                    // left or right
                    if(deltaX < 0)
                    {
                        App.dispatch(new Action<>(ActionType.PREV_PAGE));
                        return true;
                    }
                    if(deltaX > 0) {
                        App.dispatch(new Action<>(ActionType.NEXT_PAGE));
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
