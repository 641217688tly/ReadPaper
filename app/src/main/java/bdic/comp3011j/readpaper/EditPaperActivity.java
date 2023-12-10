package bdic.comp3011j.readpaper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import bdic.comp3011j.readpaper.BmobEntity.Paper;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadFileListener;

public class EditPaperActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1; // Permission request code
    private static final int PICK_PDF_FILE = 2;  // Request code for recognizing the result in onActivityResult

    private EditText etTitle, etAuthor, etUrl;
    private RadioGroup rgFileType;
    private RadioButton rbUrl, rbFile;
    private Button btnChooseFile, btnUpdatePaper;
    private ProgressBar pbFileUpload;
    private Uri fileUri;  // Store the Uri of the PDF file uploaded/selected by the user
    private Paper currentPaper; // This should be passed from the previous activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_paper);

        // Initialize components
        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        etUrl = findViewById(R.id.etUrl);
        rgFileType = findViewById(R.id.rgFileType);
        rbUrl = findViewById(R.id.rbUrl);
        rbFile = findViewById(R.id.rbFile);
        btnChooseFile = findViewById(R.id.btnChooseFile);
        pbFileUpload = findViewById(R.id.pbFileUpload);
        btnUpdatePaper = findViewById(R.id.btnUpdatePaper);
        currentPaper = (Paper) getIntent().getSerializableExtra("paper");
        // Fill in paper information
        etTitle.setText(currentPaper.getTitle());
        etAuthor.setText(currentPaper.getAuthor());
        rbUrl.setChecked(true); // Default selection is URL
        etUrl.setVisibility(View.VISIBLE);
        etUrl.setText(currentPaper.getUrl());
        // Set listeners
        rgFileType.setOnCheckedChangeListener(this);
        btnChooseFile.setOnClickListener(this);
        btnUpdatePaper.setOnClickListener(this);
    }

    // Callback method for handling RadioGroup state changes
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

    // Callback method for handling click events
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnChooseFile) {
            // Create an Intent to choose a file
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Permission has not been granted
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                // Permission has been granted
                openFilePicker();
            }
        } else if (view.getId() == R.id.btnUpdatePaper) {
            updatePaper();
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

    // Runtime permission request callback method
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open the file picker
                openFilePicker();
            } else {
                // Permission denied, explain to the user
                Toast.makeText(this, "Permission Denied. Please allow Read External Storage to choose a file.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Callback for handling returned results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_FILE && resultCode == RESULT_OK && data != null) { // User selected a file and file data is not null
            // Get the Uri of the selected file
            fileUri = data.getData();
            Toast.makeText(this, "File selected: " + fileUri.getPath(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePaper() {
        // Get user input data
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String newUrl = etUrl.getText().toString().trim();
        String oldUrl = currentPaper.getUrl();
        // Validate the inputs
        if (title.isEmpty() || author.isEmpty() || (etUrl.getVisibility() == View.VISIBLE && newUrl.isEmpty() && fileUri == null)) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }
        // Update the Paper object
        currentPaper.setTitle(title);
        currentPaper.setAuthor(author);
        if (rbFile.isChecked() && fileUri != null) { // Selected the file upload method to update the URL
            // Upload the file and update the Paper object
            uploadFileAndUpdatePaper(oldUrl);
        } else if (rbUrl.isChecked() && !newUrl.isEmpty() && fileUri == null) { // User chose to manually input the URL to update
            // Update the URL of the Paper
            currentPaper.setUrl(newUrl);
            currentPaper.update(currentPaper.getObjectId(), new UpdateListener() {
                @Override
                public void done(BmobException e) {
                    if (e == null) {
                        Toast.makeText(EditPaperActivity.this, "Paper updated successfully", Toast.LENGTH_SHORT).show();
                        // The old URL in the Bmob cloud database might correspond to a PDF file, so to free up database space, it should be deleted
                        if (!newUrl.equals(oldUrl)) {
                            removeFileFromCloud(currentPaper.getUrl());
                        }
                        finish(); // Close the activity and go back
                    } else {
                        Toast.makeText(EditPaperActivity.this, "Failed to update paper: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // Upload a file and save the Paper object
    private void uploadFileAndUpdatePaper(String oldUrl) {
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
                pbFileUpload.setVisibility(View.GONE); // Hide the progress bar after upload
                new File(filePath).delete(); // Delete the temporary file
                if (e == null) { // File uploaded successfully
                    // Update the file's URL
                    currentPaper.setUrl(bmobFile.getFileUrl());
                    // Update the Paper object
                    currentPaper.update(currentPaper.getObjectId(), new UpdateListener() {
                        @Override
                        public void done(BmobException e) {
                            if (e == null) {
                                Toast.makeText(EditPaperActivity.this, "Paper updated successfully", Toast.LENGTH_SHORT).show();
                                // The old URL in the Bmob cloud database might correspond to a PDF file, so to free up database space, it should be deleted
                                if (!bmobFile.getFileUrl().equals(oldUrl)) {
                                    removeFileFromCloud(currentPaper.getUrl());
                                }
                                finish(); // Close the activity and go back
                            } else {
                                Toast.makeText(EditPaperActivity.this, "Failed to update paper: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    // File upload failed
                    Toast.makeText(EditPaperActivity.this, "Failed to upload file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProgress(Integer value) {
                // Update the UI with progress value
                pbFileUpload.setProgress(value);
            }
        });
    }

    // Copy a file to internal storage
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

    private void removeFileFromCloud(String oldUrl) {
        BmobFile file = new BmobFile();
        file.setUrl(oldUrl); //This url is obtained using the bmobFile.getUrl() method after the file has been successfully uploaded.
        file.delete(new UpdateListener() {
            @Override
            public void done(BmobException e) {
                if (e == null) {
                    Toast.makeText(EditPaperActivity.this, "Old Paper File is removed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
