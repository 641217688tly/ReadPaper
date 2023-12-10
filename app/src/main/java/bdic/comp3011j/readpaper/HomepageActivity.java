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
            // Display a message for not logged in and navigate to the login page
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
            // Display a message for not logged in and navigate to the login page
            Toast.makeText(this, "Please Login!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomepageActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        // Retrieve all papers related to the current user from the Bmob database
        BmobQuery<Paper> query = new BmobQuery<>();
        query.addWhereEqualTo("user", new BmobPointer(user));
        query.order("-createdAt"); // Order by creation time in descending order
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
        // Load menu resources (add button)
        getMenuInflater().inflate(R.menu.menu_homepage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle menu item click events
        if (item.getItemId() == R.id.addPaper) {
            // User clicked the add button
            Intent intent = new Intent(this, AddPaperActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.logout) {
            // User clicked the logout button
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
        // Create an AlertDialog for confirmation
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this paper?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> { // User clicked YES, so proceed with deletion
                    // First attempt to delete the paper file from the cloud server
                    BmobFile file = new BmobFile();
                    file.setUrl(paper.getUrl()); // Set the file's URL
                    file.delete(new UpdateListener() {
                        @Override
                        public void done(BmobException e) {
                            if (e == null) {
                                Toast.makeText(HomepageActivity.this, "Paper PDF File deleted successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                // File deletion failed (possibly because the current user has not uploaded a pdf file)
                                Toast.makeText(HomepageActivity.this, "File delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    // Then attempt to delete all chat records related to the current Paper
                    queryPaperRelatedChats(paper, new QueryCallback<Chat>() { // First, query and retrieve all chat records related to the current Paper
                        @Override
                        public void onSuccess(List<Chat> chatList) {
                            if (chatList.size() > 0 && chatList != null) { // If there are historical chat records for the current Paper, iterate through them and delete
                                for (int i = 0; i < chatList.size(); i++) {
                                    Chat chat = chatList.get(i);
                                    if (i == chatList.size() - 1) {
                                        chat.delete(new UpdateListener() {
                                            @Override
                                            public void done(BmobException e) {
                                                if (e == null) {
                                                    // Chat record deleted successfully, then delete the Paper object
                                                    Toast.makeText(HomepageActivity.this, "Related Chats delete successfully!", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    // Chat record deletion failed
                                                    Toast.makeText(HomepageActivity.this, "Chat delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                                paper.delete(new UpdateListener() {
                                                    @Override
                                                    public void done(BmobException e) {
                                                        if (e == null) {
                                                            // Paper deleted successfully
                                                            Toast.makeText(HomepageActivity.this, "Paper deleted successfully", Toast.LENGTH_SHORT).show();
                                                            paperList.remove(position); // Remove from the list
                                                            paperAdapter.notifyItemRemoved(position); // Notify the Adapter
                                                        } else {
                                                            // Paper deletion failed
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
                                                    // Chat record deleted successfully
                                                } else {
                                                    // Chat record deletion failed
                                                    Toast.makeText(HomepageActivity.this, "Chat delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                }
                            } else { // If the current paper currently has no related chat records
                                paper.delete(new UpdateListener() {
                                    @Override
                                    public void done(BmobException e) {
                                        if (e == null) {
                                            // Paper deleted successfully
                                            Toast.makeText(HomepageActivity.this, "Paper deleted successfully", Toast.LENGTH_SHORT).show();
                                            paperList.remove(position); // Remove from the list
                                            paperAdapter.notifyItemRemoved(position); // Notify the Adapter
                                        } else {
                                            // Paper deletion failed
                                            Toast.makeText(HomepageActivity.this, "Paper delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }

                        @Override
                        public void onFail(String reason) {
                            // Query failed
                            Toast.makeText(HomepageActivity.this, "Query failed: " + reason, Toast.LENGTH_SHORT).show();
                        }
                    });

                })
                .setNegativeButton(android.R.string.no, null) // Do nothing on clicking NO
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void queryPaperRelatedChats(Paper paper, QueryCallback<Chat> callback) {
        // Retrieve all chat records related to the current Paper from the Bmob database's Chat table
        BmobQuery<Chat> query = new BmobQuery<>();
        query.addWhereEqualTo("paper", paper);
        query.order("createdAt"); // Order by creation time in ascending order
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
