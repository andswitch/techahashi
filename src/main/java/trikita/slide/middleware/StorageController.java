package trikita.slide.middleware;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.ArrayAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.Presentation;
import trikita.slide.R;
import trikita.slide.Slide;
import trikita.slide.State;
import trikita.slide.ui.Style;


public class StorageController implements Store.Middleware<Action<ActionType, ?>, State> {
    public static final int PICK_IMAGE_REQUEST_CODE = 44;
    public static final int EXPORT_PDF_REQUEST_CODE = 46;

    private Context mContext = null;

    private static final String PDF_EXPORT_NOTIFICATION_CHANNEL = "SLIDEPDFEXP";
    private static final int PDF_EXPORT_NOTIFICATION_ID = 0;
    private final NotificationManagerCompat notificationManager;
    private final NotificationCompat.Builder mBuilder;

    public StorageController(Context c) {
        mContext = c;

        notificationManager = NotificationManagerCompat.from(mContext);
        mBuilder = new NotificationCompat.Builder(mContext, PDF_EXPORT_NOTIFICATION_CHANNEL);
        mBuilder.setContentTitle(c.getString(R.string.notifications_title))
            .setContentText(c.getString(R.string.notifications_exporting_pdf))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action, Store.NextDispatcher<Action<ActionType, ?>> next) {
        next.dispatch(action);
        if (action.type == ActionType.CREATE_PDF) {
            createPdf((Activity)action.value);
            return;
        } else if (action.type == ActionType.EXPORT_PDF) {
            int PROGRESS_MAX = App.getTaskController().getGeneratedSlides().size();
            mBuilder.setProgress(PROGRESS_MAX, 0, false);
            notificationManager.notify(PDF_EXPORT_NOTIFICATION_ID, mBuilder.build());
            new PdfExportTask(store.getState().getCurrentPresentation(), (Uri) action.value, mContext, notificationManager, mBuilder).execute();
            return;
        } else if (action.type == ActionType.PICK_IMAGE) {
            pickImage((Activity) action.value);
            return;
        } else if (action.type == ActionType.INSERT_IMAGE) {
            Presentation p = store.getState().getCurrentPresentation();
            String s = p.text();
            int c = App.getMainLayout().cursor();
            String chunk = s.substring(0, c);
            int startOfLine = chunk.lastIndexOf("\n");
            if (startOfLine == -1) {
                s = "@"+(action.value).toString()+"\n"+s;
            } else {
                s = s.substring(0, startOfLine+1)+"@"+(action.value).toString()+"\n"+s.substring(startOfLine+1);
            }
            App.dispatch(new Action<>(ActionType.SET_TEXT, s));
            return;
        } /*else if(action.type == ActionType.MATH_TYPESET_COMPLETE) {
            try {
                File f = File.createTempFile("bmp",".png", mContext.getCacheDir());
                OutputStream s = new FileOutputStream(f);
                Bitmap bmp = (Bitmap)action.value;
                bmp.compress(Bitmap.CompressFormat.PNG, 100, s);
                s.close();
                App.dispatch(new Action<>(ActionType.INSERT_IMAGE, f.toURI()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    }

    private void createPdf(Activity a) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, a.getString(R.string.pdf_name_prefix)+getTimestamp());
        a.startActivityForResult(intent, EXPORT_PDF_REQUEST_CODE);
    }

    private static class PdfExportTask extends AsyncTask<Void, Integer, Boolean> {

        private final Context context;
        private final Presentation p;
        private final Uri uri;
        private final NotificationManagerCompat notificationManager;
        private final NotificationCompat.Builder mBuilder;

        PdfExportTask(Presentation p, Uri uri, Context ctx, NotificationManagerCompat notificationManager, NotificationCompat.Builder mBuilder) {
            this.context = ctx;
            this.p = p;
            this.uri = uri;
            this.notificationManager = notificationManager;
            this.mBuilder = mBuilder;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            PdfDocument document = new PdfDocument();
            ParcelFileDescriptor pfd = null;
            List<Slide> slides = App.getTaskController().getGeneratedSlides();
            try {
                String[] r = context.getResources().getStringArray(R.array.pdf_widths);
                int width = Integer.parseInt(r[p.pdfResolution()]);

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, width * 9 / 16, 1).create();
                Bitmap bmp = Bitmap.createBitmap(pageInfo.getPageWidth(), pageInfo.getPageHeight(), Bitmap.Config.ARGB_8888);

                int i = 1;
                for (Slide slide : slides) {
                    Canvas c = new Canvas(bmp);
                    c.drawColor(Style.COLOR_SCHEMES[p.colorScheme()][1]);
                    slide.render(
                            c,
                            c.getWidth(), c.getHeight(),
                            Style.SLIDE_FONT,
                            Style.COLOR_SCHEMES[p.colorScheme()][0],
                            Style.COLOR_SCHEMES[p.colorScheme()][1],
                            true);
                    PdfDocument.Page page = document.startPage(pageInfo);
                    page.getCanvas().drawBitmap(bmp, 0, 0, null);
                    document.finishPage(page);
                    publishProgress(i++, slides.size()+1);
                }

                pfd = context.getContentResolver().openFileDescriptor(uri, "w");
                if (pfd != null) {
                    FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                    document.writeTo(fos);
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                document.close();
                if (pfd != null) {
                    try { pfd.close(); } catch (IOException ignored) {}
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mBuilder.setProgress(progress[1], progress[0], false);
            notificationManager.notify(PDF_EXPORT_NOTIFICATION_ID, mBuilder.build());
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            super.onPostExecute(ok);
            if (!ok)
                mBuilder.setContentText(context.getString(R.string.failed_export_pdf));
            else
                mBuilder.setContentText(context.getString(R.string.completed_export_pdf));
            mBuilder.setProgress(0,0,false);
            notificationManager.notify(PDF_EXPORT_NOTIFICATION_ID, mBuilder.build());
        }
    }

    private String getTimestamp() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmm");
        return df.format(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis());
    }

    private void pickImage(Activity a) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        a.startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE);
    }
}
