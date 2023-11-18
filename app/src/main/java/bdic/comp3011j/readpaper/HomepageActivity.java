package bdic.comp3011j.readpaper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.document.download.DownloadJob;
import com.pspdfkit.document.download.DownloadRequest;
import com.pspdfkit.document.download.Progress;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.PdfFragment;

import java.io.File;
import java.util.List;

import bdic.comp3011j.readpaper.Adapter.PaperAdapter;
import bdic.comp3011j.readpaper.BmobEntity.Paper;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobPointer;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import lombok.val;

public class HomepageActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView rvPaper;
    private PaperAdapter paperAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        initView();
    }

    private void initView() {
        if (!BmobUser.isLogin()) {
            // 弹出未登录消息,跳转到登录界面
            Toast.makeText(this, "Please Login!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomepageActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        rvPaper = findViewById(R.id.rvPaper);
        rvPaper.setLayoutManager(new LinearLayoutManager(this));
        rvPaper.addItemDecoration(new DividerItemDecoration(rvPaper.getContext(), DividerItemDecoration.VERTICAL));
    }

    // Separate method for loading papers
    private void loadPapers() {
        BmobUser user;
        if (BmobUser.isLogin()) {
            user = BmobUser.getCurrentUser();
        } else {
            // 弹出未登录消息,跳转到登录界面
            Toast.makeText(this, "Please Login!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomepageActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        // 从Bmob数据库中获取与当前用户相关的所有Paper
        BmobQuery<Paper> query = new BmobQuery<>();
        query.addWhereEqualTo("user", new BmobPointer(user));
        query.order("-createdAt"); // 按创建时间降序排列
        query.findObjects(new FindListener<Paper>() {
            @Override
            public void done(List<Paper> papers, BmobException e) {
                if (e == null) {
                    paperAdapter = new PaperAdapter(papers, HomepageActivity.this);
                    rvPaper.setAdapter(paperAdapter);
                    Toast.makeText(HomepageActivity.this, "Query Success. " + papers.size() + " papers found.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HomepageActivity.this, "Query Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void viewPDF(String url, Context context) {
        if(true){ // 暂时先使用PDFTron的SDK
            CustomDocumentActivity.viewPDF(url, context);
        }else{ // PSPDFKit的SDK
            final DownloadRequest request = new DownloadRequest.Builder(context)
                    .uri(url)
                    .build();
            final DownloadJob job = DownloadJob.startDownload(request);

            final PdfActivityConfiguration config = new PdfActivityConfiguration
                    .Builder(context)
                    .build();
            job.setProgressListener(new DownloadJob.ProgressListenerAdapter() {
                @Override public void onProgress( Progress progress) {
                    //progressBar.setProgress((int) (100 * progress.bytesReceived / (float) progress.totalBytes));
                }

                @Override public void onComplete(File output) {
                    //PdfFragment pdfFragment = PdfFragment.newImageInstance(Uri.fromFile(output), config.getConfiguration());
                    //pdfFragment.addDocumentListener();
                    Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(output))
                            .configuration(config)
                            .activityClass(CustomPdfActivity.class)
                            .build();
                    startActivity(intent);
                }

                @Override public void onError( Throwable exception) {
                    //handleDownloadError(exception);
                }
            });
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadPapers(); // Refresh papers every time the activity is resumed
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 加载菜单资源（添加按钮）
        getMenuInflater().inflate(R.menu.menu_homepage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理菜单项的点击事件
        int id = item.getItemId();
        if (id == R.id.addPaper) {
            // 用户点击了添加按钮
            Intent intent = new Intent(this, AddPaperActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {

    }

}

