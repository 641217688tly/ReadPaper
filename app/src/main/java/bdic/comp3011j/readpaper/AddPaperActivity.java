package bdic.comp3011j.readpaper;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import bdic.comp3011j.readpaper.BmobEntity.Paper;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UploadFileListener;

public class AddPaperActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1; // 权限请求码
    private static final int PICK_PDF_FILE = 2;  // 请求码，用于在 onActivityResult 中识别返回的结果
    private EditText etTitle, etAuthor, etUrl;
    private RadioGroup rgFileType;
    private Button btnChooseFile, btnAddPaper;
    private ProgressBar pbFileUpload;
    private Uri fileUri;  // 存储用户所上传/选中的PDF文件的Uri

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_paper);

        // 初始化组件
        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        etUrl = findViewById(R.id.etUrl);
        rgFileType = findViewById(R.id.rgFileType);
        btnChooseFile = findViewById(R.id.btnChooseFile);
        btnAddPaper = findViewById(R.id.btnAddPaper);
        pbFileUpload = findViewById(R.id.pbFileUpload);
        // 设置监听器
        rgFileType.setOnCheckedChangeListener(this);
        btnChooseFile.setOnClickListener(this);
        btnAddPaper.setOnClickListener(this);
    }

    // 处理 RadioGroup 状态变化的回调方法
    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if(radioGroup.getId() == R.id.rgFileType){
            // 根据用户选择的添加论文方式，显示或隐藏相关组件
            if (checkedId == R.id.rbUrl) {
                etUrl.setVisibility(View.VISIBLE);
                btnChooseFile.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbFile) {
                etUrl.setVisibility(View.GONE);
                btnChooseFile.setVisibility(View.VISIBLE);
            }
        }
    }

    // 处理点击事件的回调方法
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnChooseFile) {
            // 创建选择文件的 Intent
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 权限未被授予
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                // 权限已被授予
                openFilePicker();
            }
        } else if (view.getId() == R.id.btnAddPaper) {
            // 添加论文
            insertPaper();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a PDF"), PICK_PDF_FILE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    // 运行时权限请求回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，打开文件选择器
                openFilePicker();
            } else {
                // 权限被拒绝，向用户解释
                Toast.makeText(this, "Permission Denied. Please allow Read External Storage to choose file.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 处理返回的结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_FILE && resultCode == RESULT_OK && data != null) { // 用户选中的文件且文件的数据不为null
            // 获取选中文件的 Uri
            fileUri = data.getData();
            Toast.makeText(this, "File selected: " + fileUri.getPath(), Toast.LENGTH_SHORT).show();
        }
    }

    // 添加论文的方法
    private void insertPaper() {
        // 获取用户输入的数据
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String url = etUrl.getText().toString().trim();

        // 检查输入的完整性
        if (title.isEmpty() || author.isEmpty() || (etUrl.getVisibility() == View.VISIBLE && url.isEmpty() && fileUri == null)) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        // 获取当前登录的用户
        BmobUser currentUser = BmobUser.getCurrentUser(BmobUser.class);
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        // 创建 Paper 对象并设置属性
        Paper paper = new Paper();
        paper.setUser(currentUser);
        paper.setTitle(title);
        paper.setAuthor(author);
        // 根据用户选择的添加方式，设置 URL 或文件
        if (etUrl.getVisibility() == View.VISIBLE) { // 用户输入了URL
            paper.setUrl(url);
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
        } else if (fileUri != null) { // 用户上传了文件
            uploadFileAndSavePaper(fileUri, paper);
        }
    }

    // 上传文件并保存Paper对象
    private void uploadFileAndSavePaper(Uri fileUri, Paper paper) {
        String filePath = copyFileToInternalStorage(fileUri, "pdfs");
        if (filePath == null) {
            Toast.makeText(this, "File path is invalid", Toast.LENGTH_SHORT).show();
            return;
        }
        final BmobFile bmobFile = new BmobFile(new File(filePath));
        pbFileUpload.setVisibility(View.VISIBLE);  // 显示进度条
        bmobFile.uploadblock(new UploadFileListener() {
            @Override
            public void done(BmobException e) {
                pbFileUpload.setVisibility(View.GONE); // 上传结束后隐藏进度条
                new File(filePath).delete(); // 删除临时文件
                if (e == null) {
                    // 上传文件成功，获取文件的URL并保存Paper对象
                    paper.setUrl(bmobFile.getFileUrl());
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
                } else {
                    // 上传文件失败
                    Toast.makeText(AddPaperActivity.this, "Failed to upload file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProgress(Integer value) {
                // 可以使用进度值更新UI
                pbFileUpload.setProgress(value);
            }
        });
    }

    // 复制文件到内部存储
    private String copyFileToInternalStorage(Uri uri, String newDirName) {
        Uri returnUri = uri;
        Cursor returnCursor = getContentResolver().query(returnUri, null, null, null, null);
        /*
         * Get the column indexes of the data in the Cursor,
         * move to the first row in the Cursor, get the data,
         * and display it.
         */
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        File file = new File(getFilesDir(), newDirName);
        if (!file.exists()) {
            file.mkdir();
        }
        File fileCopy = new File(file, name);
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(fileCopy);
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        returnCursor.close();
        return fileCopy.getPath();
    }

}
