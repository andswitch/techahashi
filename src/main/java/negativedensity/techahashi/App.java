package negativedensity.techahashi;

import android.app.Activity;
import android.app.Application;

import trikita.anvil.Anvil;
import trikita.jedux.Action;
import trikita.jedux.Store;
import negativedensity.techahashi.middleware.BuildController;
import negativedensity.techahashi.middleware.PersistanceController;
import negativedensity.techahashi.middleware.StorageController;
import negativedensity.techahashi.middleware.WindowController;
import negativedensity.techahashi.ui.MainLayout;
import negativedensity.techahashi.State;

public class App extends Application {
    public static App instance;

    private Store<Action<ActionType, ?>, State> store;
    private BuildController buildController;
    private MainLayout mainLayout;

    @Override
    public void onCreate() {
        super.onCreate();
        App.instance = this;

        PersistanceController persistanceController = new PersistanceController(this);
        State initialState = persistanceController.getSavedState();
        if (initialState == null) {
            initialState = State.Default.build(this);
        }

        this.buildController = new BuildController();

        this.store = new Store<>(new State.Reducer(),
            initialState,
            persistanceController,
            new WindowController(),
            new StorageController(this),
            this.buildController
        );

        this.store.subscribe(Anvil::render);
    }

    public static State getState() {
        return instance.store.getState();
    }

    public static BuildController getBuildController() { return instance.buildController; }

    public static MainLayout getMainLayout() { return instance.mainLayout; }

    public static void setMainLayout(MainLayout m) { instance.mainLayout = m; }

    public static void setMainActivity(Activity m) {
        instance.buildController.setActivity(m);
    }

    public static void dispatch(Action<ActionType, ?> action) {
        instance.store.dispatch(action);
    }
}
