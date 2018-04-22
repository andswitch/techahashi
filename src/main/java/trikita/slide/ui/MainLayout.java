package trikita.slide.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import trikita.anvil.Anvil;
import trikita.anvil.RenderableView;
import trikita.jedux.Action;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.R;

import static trikita.anvil.DSL.FILL;
import static trikita.anvil.DSL.START;
import static trikita.anvil.DSL.TOP;
import static trikita.anvil.DSL.WRAP;
import static trikita.anvil.DSL.alignParentBottom;
import static trikita.anvil.DSL.background;
import static trikita.anvil.DSL.button;
import static trikita.anvil.DSL.centerHorizontal;
import static trikita.anvil.DSL.centerInParent;
import static trikita.anvil.DSL.dip;
import static trikita.anvil.DSL.frameLayout;
import static trikita.anvil.DSL.gravity;
import static trikita.anvil.DSL.init;
import static trikita.anvil.DSL.linearLayout;
import static trikita.anvil.DSL.margin;
import static trikita.anvil.DSL.onClick;
import static trikita.anvil.DSL.onTextChanged;
import static trikita.anvil.DSL.relativeLayout;
import static trikita.anvil.DSL.size;
import static trikita.anvil.DSL.text;
import static trikita.anvil.DSL.textView;
import static trikita.anvil.DSL.v;
import static trikita.anvil.DSL.visibility;

public class MainLayout extends RenderableView {

    private Editor mEditor;

    public MainLayout(Context c) {
        super(c);
    }

    public void view() {
        if (App.getState().presentationMode()) {
            presentation();
        } else {
            editor();
        }
    }

    private void editor() {
        relativeLayout(() -> {
            Style.Editor.background();

            v(Editor.class, () -> {
                size(FILL, FILL);
                gravity(TOP | START);
                text(App.getState().text());
                Style.Editor.textStyle();
                background(null);
                init(() -> {
                    mEditor = Anvil.currentView();
                    mEditor.setOnSelectionChangedListener(pos -> {
                        App.dispatch(new Action<>(ActionType.SET_CURSOR, pos));
                    });
                });
                onTextChanged(chars -> {
                    String s = chars.toString();
                    App.dispatch(new Action<>(ActionType.SET_TEXT, s));
                    App.dispatch(new Action<>(ActionType.SET_CURSOR, mEditor.getSelectionStart()));
                });
            });

            textView(() -> {
                Style.Editor.menuButton();
                onClick(this::onOpenMenu);
            });

            frameLayout(() -> {
                Style.Editor.previewContainer();

                v(Preview.class, () -> {
                    Style.Editor.previewSize();
                    onClick(v -> App.dispatch(new Action<>(ActionType.OPEN_PRESENTATION)));
                    Anvil.currentView().invalidate();
                });
            });
        });
    }

    private void presentation() {
        relativeLayout(() -> {
            size(FILL, FILL);
            Style.Preview.background(App.getState().colorScheme());

            v(Preview.class, () -> {
                size(FILL, WRAP);
                centerInParent();
                Anvil.currentView().invalidate();
            });

            linearLayout(() -> {
                size(FILL, FILL);

                Style.Preview.touchPlaceholder(v -> App.dispatch(new Action<>(ActionType.PREV_PAGE)));
                Style.Preview.touchPlaceholder(v -> App.dispatch(new Action<>(ActionType.TOGGLE_TOOLBAR)));
                Style.Preview.touchPlaceholder(v -> App.dispatch(new Action<>(ActionType.NEXT_PAGE)));
            });

            button(() -> {
                Style.Preview.button(App.getState().colorScheme());
                margin(0, 0, 0, dip(25));
                alignParentBottom();
                centerHorizontal();
                visibility(App.getState().toolbarShown());
                onClick(v -> App.dispatch(new Action<>(ActionType.CLOSE_PRESENTATION)));
            });
        });
    }

    private void onOpenMenu(View v) {
        PopupMenu menu = new PopupMenu(v.getContext(), v);
        menu.getMenuInflater().inflate(R.menu.overflow_popup, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_open) {
                App.dispatch(new Action<>(ActionType.OPEN_DOCUMENT, (Activity) v.getContext()));
            } else if (item.getItemId() == R.id.menu_insert_image) {
                App.dispatch(new Action<>(ActionType.PICK_IMAGE, (Activity) v.getContext()));
            } else if (item.getItemId() == R.id.menu_style) {
                openStylePicker();
            //} else if (item.getItemId() == R.id.menu_settings) {
            } else if (item.getItemId() == R.id.menu_export_pdf) {
                App.dispatch(new Action<>(ActionType.CREATE_PDF, (Activity) v.getContext()));
            } else if (item.getItemId() == R.id.menu_config_plantuml) {
                openConfigPlantUMLDialog();
            } else if (item.getItemId() == R.id.menu_plantuml_docs) {
                openPlantUMLDocs();
            }
            return true;
        });
        menu.show();
    }

    private void openStylePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
            .setTitle(getContext().getString(R.string.dlg_style_picker_title))
            .setCancelable(true)
            .setView(new StylePicker(getContext()))
            .setPositiveButton(getContext().getString(R.string.btn_ok), (arg0, arg1) -> {});
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void openConfigPlantUMLDialog() {
        LinearLayout contents = new LinearLayout(getContext());
        contents.setOrientation(LinearLayout.VERTICAL);

        TextView warning = new TextView(getContext());
        warning.setText(R.string.pm_warning);
        contents.addView(warning);

        CheckBox enable = new CheckBox(getContext());
        enable.setChecked(App.getState().plantUMLEnabled());
        enable.setText(R.string.pm_enable);
        contents.addView(enable);

        EditText txtUrl = new EditText(getContext());
        txtUrl.setHint(R.string.pm_plantuml_endpoint);
        txtUrl.setText(App.getState().plantUMLEndPoint());
        contents.addView(txtUrl);

        TextView preamble = new TextView(getContext());
        preamble.setText(R.string.pm_plantuml_preamble);
        contents.addView(preamble);

        EditText txtPreamble = new EditText(getContext());
        txtPreamble.setSingleLine(false);
        txtPreamble.setText(App.getState().plantUMLPreamble());
        contents.addView(txtPreamble);

        new AlertDialog.Builder(getContext())
            .setTitle(R.string.om_config_plantuml)
            .setView(contents)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    App.dispatch(new Action<>(ActionType.CONFIGURE_PLANTUML,
                        new Pair<>(enable.isChecked(),
                            new Pair<>(txtUrl.getText().toString(), txtPreamble.getText().toString()))));
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }

    private static void openPlantUMLDocs() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("https://plantuml.com"));
        App._startActivity(i);
    }
}
