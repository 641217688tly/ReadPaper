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

public class CustomDocumentActivity extends DocumentActivity{

    private static ViewerConfig viewerConfig;
    private static final long MAX_DURATION_FOR_CLICKS = 300; // 定义有效点击的最大间隔时间
    private int consecutiveClickCount = 0;
    private long lastClickTime = 0;
    Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finishActivityTask = new Runnable() {
        @Override
        public void run() {
            // 当一分钟过去后，结束Activity
            finish();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        // 用户离开Activity时开始计时
        handler.postDelayed(finishActivityTask, 60000); // 40000毫秒后执行run方法
    }


    @Override
    protected void onResume() {
        super.onResume();
        // 用户返回Activity时取消计时
        handler.removeCallbacks(finishActivityTask);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // 检测连续点击逻辑
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < MAX_DURATION_FOR_CLICKS) {
                consecutiveClickCount++;
                Log.d("CustomDocumentActivity", "Tap count: " + consecutiveClickCount); // 添加日志输出
                if (consecutiveClickCount == 5) {
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

    private static void configViewerConfig(Context context){
        viewerConfig = new ViewerConfig.Builder()
                .openUrlCachePath(context.getCacheDir().getAbsolutePath())
                .showAnnotationsList(true)
                .annotationsListFilterEnabled(true)
                .autoSortUserBookmarks(true)
                .fullscreenModeEnabled(true)
                .openUrlPasswordCheckEnabled(true) // Enable password check for all files
                .outlineListEditingEnabled(true) // Enable outline editing
                .quickBookmarkCreation(true)  // 书签
                .showQuickNavigationButton(true) // 显示快速导航按钮
                .tabletLayoutEnabled(true) // 平板布局
                .multiTabEnabled(true) // 多标签
                .build();
    }

    public static void viewPDF(String url, Context context){
        final Uri uri = Uri.parse(url);
        configViewerConfig(context);
        Intent intent = CustomDocumentActivity.IntentBuilder.fromActivityClass(context, CustomDocumentActivity.class)
                .withUri(uri)
                .usingConfig(viewerConfig)
                .build();
        context.startActivity(intent);
    }

}
