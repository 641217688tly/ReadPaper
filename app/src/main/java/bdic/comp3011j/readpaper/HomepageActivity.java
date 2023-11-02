package bdic.comp3011j.readpaper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.controls.DocumentActivity;

public class HomepageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        jumpToPDF();
    }

    private void jumpToPDF() {
        // Set the cache location using the config to store the cache file
        ViewerConfig config = new ViewerConfig.Builder()
                .openUrlCachePath(this.getCacheDir().getAbsolutePath())
                .showAnnotationsList(true)
                .build();

        // https://arxiv.org/pdf/2308.09239.pdf
        // https://pdftron.s3.amazonaws.com/downloads/pl/PDFTRON_mobile_about.pdf
        final Uri uri = Uri.parse("https://arxiv.org/pdf/2308.09239.pdf");

        //DocumentActivity.openDocument(this, uri, config);

        Intent intent = DocumentActivity.IntentBuilder.fromActivityClass(this, DocumentActivity.class)
                .withUri(uri)
                .usingConfig(config)
                //.usingTheme(R.style.PDFTronAppTheme)
                .build();
        startActivity(intent);
    }

}