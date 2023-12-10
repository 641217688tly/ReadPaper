package bdic.comp3011j.readpaper;

import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import com.pspdfkit.ui.PdfActivity;

public class CustomPdfActivity extends PdfActivity {

    private static final long MAX_DURATION_FOR_CLICKS = 300; // Define the maximum time interval for a valid click
    private int consecutiveClickCount = 0;
    private long lastClickTime = 0;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Check for consecutive clicks logic
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < MAX_DURATION_FOR_CLICKS) {
                consecutiveClickCount++;
                Log.d("CustomDocumentActivity", "Tap count: " + consecutiveClickCount); // Add log output
                if (consecutiveClickCount == 5) {
                    Toast.makeText(this, "Starting ChatActivity...", Toast.LENGTH_SHORT).show(); // Display a Toast message
                    Log.d("CustomDocumentActivity", "Starting ChatActivity"); // Add log output
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

    private void handleConsecutiveClicks() {
        long currentTime = System.currentTimeMillis();

        // If the time interval between two clicks is less than the set maximum duration, increment the click count
        if (currentTime - lastClickTime < MAX_DURATION_FOR_CLICKS) {
            consecutiveClickCount++;
        } else {
            // If it's not a consecutive click, reset the click count
            consecutiveClickCount = 1;
        }

        // Update the timestamp of the last click
        lastClickTime = currentTime;

        // If 5 consecutive clicks are reached, perform the navigation
        if (consecutiveClickCount == 5) {
            // Navigate to the target Activity
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
            // Reset the click count
            consecutiveClickCount = 0;
        }
    }
}
