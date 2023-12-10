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

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1; // Permission request code
    private static final int PICK_PDF_FILE = 2;  // Request code to identify results in onActivityResult
    private EditText etTitle, etAuthor, etUrl;
    private RadioGroup rgFileType;
    private Button btnChooseFile, btnAddPaper;
    private ProgressBar pbFileUpload;
    private Uri fileUri;  // Store the Uri of the PDF file uploaded/selected by the user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_paper);

        // Initialize components
        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        etUrl = findViewById(R.id.etUrl);
        rgFileType = findViewById(R.id.rgFileType);
        btnChooseFile = findViewById(R.id.btnChooseFile);
        btnAddPaper = findViewById(R.id.btnAddPaper);
        pbFileUpload = findViewById(R.id.pbFileUpload);

        // Set listeners
        rgFileType.setOnCheckedChangeListener(this);
        btnChooseFile.setOnClickListener(this);
        btnAddPaper.setOnClickListener(this);
    }

    // Handle the change of RadioGroup state
    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if (radioGroup.getId() == R.id.rgFileType) {
            // Show or hide relevant components based on the user's choice of adding a paper
            if (checkedId == R.id.rbUrl) {
                etUrl.setVisibility(View.VISIBLE);
                btnChooseFile.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbFile) {
                etUrl.setVisibility(View.GONE);
                btnChooseFile.setVisibility(View.VISIBLE);
            }
        }
    }

    // Handle click events
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnChooseFile) {
            // Create an Intent to choose a file
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                // Permission granted
                openFilePicker();
            }
        } else if (view.getId() == R.id.btnAddPaper) {
            // Add a paper
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

    // Runtime permission request callback
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open file picker
                openFilePicker();
            } else {
                // Permission denied, explain to the user
                Toast.makeText(this, "Permission Denied. Please allow Read External Storage to choose a file.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Handle the result of the file selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_FILE && resultCode == RESULT_OK && data != null) { // User selected a file and file data is not null
            // Get the Uri of the selected file
            fileUri = data.getData();
            Toast.makeText(this, "File selected: " + fileUri.getPath(), Toast.LENGTH_SHORT).show();
        }
    }

    // Method to add a paper
    private void insertPaper() {
        // Get user input data
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String url = etUrl.getText().toString().trim();

        // Check input completeness
        if (title.isEmpty() || author.isEmpty() || (etUrl.getVisibility() == View.VISIBLE && url.isEmpty() && fileUri == null)) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        // Get the currently logged-in user
        BmobUser currentUser = BmobUser.getCurrentUser(BmobUser.class);
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a Paper object and set its attributes
        Paper paper = new Paper();
        paper.setUser(currentUser);
        paper.setTitle(title);
        paper.setAuthor(author);

        // Depending on the user's choice of adding method, set URL or file
        if (etUrl.getVisibility() == View.VISIBLE) { // User entered a URL
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
        } else if (fileUri != null) { // User uploaded a file
            uploadFileAndSavePaper(fileUri, paper);
        }
    }

    // Upload a file and save a Paper object
    private void uploadFileAndSavePaper(Uri fileUri, Paper paper) {
        String filePath = copyFileToInternalStorage(fileUri, "pdfs");
        if (filePath == null) {
            Toast.makeText(this, "File path is invalid", Toast.LENGTH_SHORT).show();
            return;
        }
        final BmobFile bmobFile = new BmobFile(new File(filePath));
        pbFileUpload.setVisibility(View.VISIBLE);  // Show progress bar
        bmobFile.uploadblock(new UploadFileListener() {
            @Override
            public void done(BmobException e) {
                pbFileUpload.setVisibility(View.GONE); // Hide progress bar after upload
                new File(filePath).delete(); // Delete temporary file
                if (e == null) {
                    // Upload file successful, get file URL and save Paper object
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
                    // Upload file failed
                    Toast.makeText(AddPaperActivity.this, "Failed to upload file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProgress(Integer value) {
                // Update UI with progress value
                pbFileUpload.setProgress(value);
            }
        });
    }

    // Copy file to internal storage
    private String copyFileToInternalStorage(Uri uri, String newDirName) {
        Uri returnUri = uri;
        Cursor returnCursor = getContentResolver().query(returnUri, null, null, null, null);
        // Get the column indexes of the data in the Cursor, move to the first row in the Cursor, get the data, and display it.
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
