package bdic.comp3011j.readpaper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

import bdic.comp3011j.readpaper.BmobEntity.Paper;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;

public class AddPaperActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private static final int PICK_PDF_FILE = 2;
    private EditText etTitle, etAuthor, etUrl;
    private RadioGroup rgFileType;
    private Button btnChooseFile, btnAddPaper;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_paper);

        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        etUrl = findViewById(R.id.etUrl);
        rgFileType = findViewById(R.id.rgFileType);
        btnChooseFile = findViewById(R.id.btnChooseFile);
        btnAddPaper = findViewById(R.id.btnAddPaper);

        rgFileType.setOnCheckedChangeListener(this);
        btnChooseFile.setOnClickListener(this);
        btnAddPaper.setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if (checkedId == R.id.rbUrl) {
            etUrl.setVisibility(View.VISIBLE);
            btnChooseFile.setVisibility(View.GONE);
        } else if (checkedId == R.id.rbFile) {
            etUrl.setVisibility(View.GONE);
            btnChooseFile.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnChooseFile) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            startActivityForResult(intent, PICK_PDF_FILE);
        } else if (view.getId() == R.id.btnAddPaper) {
            addPaper();
        }
    }

    private void addPaper() {
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String url = etUrl.getText().toString().trim();

        if (title.isEmpty() || author.isEmpty() || (etUrl.getVisibility() == View.VISIBLE && url.isEmpty() && fileUri == null)) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        BmobUser currentUser = BmobUser.getCurrentUser(BmobUser.class);
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        Paper paper = new Paper();
        paper.setUser(currentUser);
        paper.setTitle(title);
        paper.setAuthor(author);

        if (etUrl.getVisibility() == View.VISIBLE) {
            paper.setUrl(url);
            savePaper(paper);
        } else if (fileUri != null) {
            BmobFile bmobFile = new BmobFile(new File(fileUri.getPath()));
            paper.setFile(bmobFile);
            savePaper(paper);
        }
    }

    private void savePaper(Paper paper) {
        paper.save(new SaveListener<String>() {
            @Override
            public void done(String objectId, BmobException e) {
                if (e == null) {
                    Toast.makeText(AddPaperActivity.this, "Paper added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddPaperActivity.this, "Failed to add paper: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_FILE && resultCode == RESULT_OK && data != null) {
            fileUri = data.getData();
            Toast.makeText(this, "File selected: " + fileUri.getPath(), Toast.LENGTH_SHORT).show();
        }
    }

}