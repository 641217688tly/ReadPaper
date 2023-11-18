package bdic.comp3011j.readpaper;

import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import com.pspdfkit.ui.PdfActivity;

public class CustomPdfActivity extends PdfActivity {

    private static final long MAX_DURATION_FOR_CLICKS = 500; // 定义有效点击的最大间隔时间，如1000毫秒
    private int consecutiveClickCount = 0;
    private long lastClickTime = 0;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // 检测连续点击逻辑
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < MAX_DURATION_FOR_CLICKS) {
                consecutiveClickCount++;
                Log.d("CustomDocumentActivity", "Tap count: " + consecutiveClickCount); // 添加日志输出
                if (consecutiveClickCount == 5) {
                    Toast.makeText(this, "Starting ChatActivity...", Toast.LENGTH_SHORT).show(); // 显示Toast消息
                    Log.d("CustomDocumentActivity", "Starting ChatActivity"); // 添加日志输出
                    Intent intent = new Intent(this, ChatActivity.class);
                    startActivity(intent);
                    consecutiveClickCount = 0; // 重置点击计数
                    return true; // 如果你想阻止事件进一步传递
                }
            } else {
                consecutiveClickCount = 1; // 重置点击计数
            }
            lastClickTime = currentTime;
        }
        // 如果不想阻止事件继续传递，调用超类的dispatchTouchEvent
        return super.dispatchTouchEvent(event);
    }


    private void handleConsecutiveClicks() {
        long currentTime = System.currentTimeMillis();

        // 如果两次点击之间的时间小于设定的最大间隔，则增加点击次数
        if (currentTime - lastClickTime < MAX_DURATION_FOR_CLICKS) {
            consecutiveClickCount++;
        } else {
            // 如果不是连续点击，则重置点击次数
            consecutiveClickCount = 1;
        }

        // 更新上一次点击时间
        lastClickTime = currentTime;

        // 如果达到5次连续点击，则执行跳转
        if (consecutiveClickCount == 5) {
            // 跳转到目标Activity
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
            // 重置点击次数
            consecutiveClickCount = 0;
        }
    }
}
