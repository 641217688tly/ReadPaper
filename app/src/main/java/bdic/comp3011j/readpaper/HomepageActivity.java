package bdic.comp3011j.readpaper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import bdic.comp3011j.readpaper.BmobEntity.Chat;
import bdic.comp3011j.readpaper.BmobEntity.Paper;
import bdic.comp3011j.readpaper.Util.QueryCallback;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.datatype.BmobPointer;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UpdateListener;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import lombok.val;

public class HomepageActivity extends AppCompatActivity {

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
        if (item.getItemId() == R.id.addPaper) {
            // 用户点击了添加按钮
            Intent intent = new Intent(this, AddPaperActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.logout) {
            // 用户点击了注销按钮
            BmobUser.logOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void viewPDF(String url, Context context) {
        CustomDocumentActivity.viewPDF(url, context);
    }

    public void deletePaper(Paper paper, List<Paper> paperList, int position) {
        // 创建一个 AlertDialog 进行确认
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this paper?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> { // User clicked YES, so proceed with deletion
                    // 首先尝试从云服务器中删除论文文件
                    BmobFile file = new BmobFile();
                    file.setUrl(paper.getUrl()); // 设置文件的URL
                    file.delete(new UpdateListener() {
                        @Override
                        public void done(BmobException e) {
                            if (e == null) {
                                Toast.makeText(HomepageActivity.this, "Paper PDF File deleted successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                // 文件删除失败(可能是因为当前用户没有上传过pdf文件导致的)
                                Toast.makeText(HomepageActivity.this, "File delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    // 之后尝试删除与当前Paper相关的所有聊天记录
                    queryPaperRelatedChats(paper, new QueryCallback<Chat>() { // 先查询得到与当前Paper相关的所有聊天记录
                        @Override
                        public void onSuccess(List<Chat> chatList) {
                            if (chatList.size() > 0 && chatList != null) { // 如果当前Paper有历史聊天记录,则遍历所有聊天记录并删除
                                for (int i = 0; i < chatList.size(); i++) {
                                    Chat chat = chatList.get(i);
                                    if (i == chatList.size() - 1) {
                                        chat.delete(new UpdateListener() {
                                            @Override
                                            public void done(BmobException e) {
                                                if (e == null) {
                                                    // 聊天记录删除成功,接下来删除Paper对象
                                                    Toast.makeText(HomepageActivity.this, "Related Chats delete successfully!", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    // 聊天记录删除失败
                                                    Toast.makeText(HomepageActivity.this, "Chat delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                                paper.delete(new UpdateListener() {
                                                    @Override
                                                    public void done(BmobException e) {
                                                        if (e == null) {
                                                            // Paper删除成功
                                                            Toast.makeText(HomepageActivity.this, "Paper deleted successfully", Toast.LENGTH_SHORT).show();
                                                            paperList.remove(position); // 从列表中移除
                                                            paperAdapter.notifyItemRemoved(position); // 通知Adapter
                                                        } else {
                                                            // Paper删除失败
                                                            Toast.makeText(HomepageActivity.this, "Paper delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    } else {
                                        chat.delete(new UpdateListener() {
                                            @Override
                                            public void done(BmobException e) {
                                                if (e == null) {
                                                    // 聊天记录删除成功
                                                } else {
                                                    // 聊天记录删除失败
                                                    Toast.makeText(HomepageActivity.this, "Chat delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                }
                            } else { // 如果当前论文目前没有相关的聊天记录
                                paper.delete(new UpdateListener() {
                                    @Override
                                    public void done(BmobException e) {
                                        if (e == null) {
                                            // Paper删除成功
                                            Toast.makeText(HomepageActivity.this, "Paper deleted successfully", Toast.LENGTH_SHORT).show();
                                            paperList.remove(position); // 从列表中移除
                                            paperAdapter.notifyItemRemoved(position); // 通知Adapter
                                        } else {
                                            // Paper删除失败
                                            Toast.makeText(HomepageActivity.this, "Paper delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }

                        @Override
                        public void onFail(String reason) {
                            // 查询失败
                            Toast.makeText(HomepageActivity.this, "Query failed: " + reason, Toast.LENGTH_SHORT).show();
                        }
                    });

                })
                .setNegativeButton(android.R.string.no, null) // Do nothing on clicking NO
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void queryPaperRelatedChats(Paper paper, QueryCallback<Chat> callback) {
        // 从Bmob数据库中的Chat表获取所有与当前Paper有关的聊天记录
        BmobQuery<Chat> query = new BmobQuery<>();
        query.addWhereEqualTo("paper", paper);
        query.order("createdAt"); // 按创建时间升序排列
        query.findObjects(new FindListener<Chat>() {
            @Override
            public void done(List<Chat> chatList, BmobException e) {
                if (e == null) {
                    callback.onSuccess(chatList);
                } else {
                    callback.onFail("Query Failed: " + e.getMessage());
                }
            }
        });
    }
}

