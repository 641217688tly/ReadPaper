package bdic.comp3011j.readpaper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.controls.DocumentActivity;

public class CustomDocumentActivity extends DocumentActivity{

    private static ViewerConfig viewerConfig;

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
        Intent intent = CustomDocumentActivity.IntentBuilder.fromActivityClass(context, DocumentActivity.class)
                .withUri(uri)
                .usingConfig(viewerConfig)
                .build();
        context.startActivity(intent);
    }
}
