package bdic.comp3011j.readpaper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.controls.DocumentActivity;

public class CustomDocumentActivity extends DocumentActivity {

    private static ViewerConfig viewerConfig;
    private static final long MAX_DURATION_FOR_CLICKS = 300; // Define the maximum time interval for a valid click
    private int consecutiveClickCount = 0;
    private long lastClickTime = 0;
    Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finishActivityTask = new Runnable() {
        @Override
        public void run() {
            // Finish the Activity after one minute
            finish();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        // Start counting when the user leaves the Activity
        handler.postDelayed(finishActivityTask, 60000); // Execute the run method after 60000 milliseconds (1 minute)
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cancel counting when the user returns to the Activity
        handler.removeCallbacks(finishActivityTask);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Check for consecutive clicks logic
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < MAX_DURATION_FOR_CLICKS) {
                consecutiveClickCount++;
                Log.d("CustomDocumentActivity", "Tap count: " + consecutiveClickCount); // Add log output
                if (consecutiveClickCount == 5) {
                    Intent intent = new Intent(this, ChatActivity.class);
                    startActivity(intent);
                    consecutiveClickCount = 0; // Reset click count
                    return true; // If you want to prevent further event propagation
                }
            } else {
                consecutiveClickCount = 1; // Reset click count
            }
            lastClickTime = currentTime;
        }
        // If you don't want to prevent event propagation, call the super class's dispatchTouchEvent
        return super.dispatchTouchEvent(event);
    }

    private static void configViewerConfig(Context context) {
        viewerConfig = new ViewerConfig.Builder()
                .openUrlCachePath(context.getCacheDir().getAbsolutePath())
                .showAnnotationsList(true)
                .annotationsListFilterEnabled(true)
                .autoSortUserBookmarks(true)
                .fullscreenModeEnabled(true)
                .openUrlPasswordCheckEnabled(true) // Enable password check for all files
                .outlineListEditingEnabled(true) // Enable outline editing
                .quickBookmarkCreation(true) // Bookmarks
                .showQuickNavigationButton(true) // Show quick navigation button
                .tabletLayoutEnabled(true) // Tablet layout
                .multiTabEnabled(true) // Multi-tab
                .build();
    }

    public static void viewPDF(String url, Context context) {
        final Uri uri = Uri.parse(url);
        configViewerConfig(context);
        Intent intent = CustomDocumentActivity.IntentBuilder.fromActivityClass(context, CustomDocumentActivity.class)
                .withUri(uri)
                .usingConfig(viewerConfig)
                .build();
        context.startActivity(intent);
    }
}
