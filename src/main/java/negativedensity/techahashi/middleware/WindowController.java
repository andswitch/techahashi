package negativedensity.techahashi.middleware;

import android.view.View;
import android.view.Window;

import trikita.jedux.Action;
import trikita.jedux.Store;
import negativedensity.techahashi.ActionType;
import negativedensity.techahashi.App;
import negativedensity.techahashi.State;
import negativedensity.techahashi.ui.Style;

public class WindowController implements Store.Middleware<Action<ActionType, ?>, State> {
    private Window mWindow;

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action,
                         Store.NextDispatcher<Action<ActionType, ?>> next) {
        next.dispatch(action);

        switch (action.type) {
            case SET_WINDOW:
                mWindow = (Window)action.value;
                if(mWindow != null) {
                    mWindow.setStatusBarColor(Style.COLOR_SCHEMES[App.getState().getCurrentPresentation().colorScheme()][1]);
                }
                return;
        }

        if (mWindow != null) {
            switch (action.type) {
                case SET_COLOR_SCHEME:
                case PREVIOUS_PRESENTATION:
                case NEXT_PRESENTATION:
                case REMOVE_PRESENTATION:
                    mWindow.setStatusBarColor(Style.COLOR_SCHEMES[store.getState().getCurrentPresentation().colorScheme()][1]);
                    break;
                case OPEN_PRESENTATION:
                    int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                    mWindow.getDecorView().setSystemUiVisibility(uiOptions);
                    break;
                case CLOSE_PRESENTATION:
                    mWindow.getDecorView().setSystemUiVisibility(0);
                    break;
            }
        }
    }
}
