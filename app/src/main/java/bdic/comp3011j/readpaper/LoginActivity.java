package bdic.comp3011j.readpaper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.controls.DocumentActivity;

import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.LogInListener;
import cn.bmob.v3.listener.SaveListener;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    private SharedPreferences sharedPreferences;
    private RadioGroup rgLoginType;
    private EditText etEmail, etPassword, etProxyPort;
    private Button btnLogin,btnGoToRegister;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences("ReadPaper", Context.MODE_PRIVATE);
        rgLoginType = findViewById(R.id.rgLoginType);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etProxyPort = findViewById(R.id.etProxyPort);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        // 注册按钮监听事件
        btnLogin.setOnClickListener(this);
        btnGoToRegister.setOnClickListener(this);
        rgLoginType.setOnCheckedChangeListener(this);

        loadUserInfo();
    }

    private void loadUserInfo() {
        String email = sharedPreferences.getString("email", "");
        String password = sharedPreferences.getString("password", "");
        String device = sharedPreferences.getString("device", "Physical");
        String proxyPort = sharedPreferences.getString("proxyPort", "");

        etEmail.setText(email);
        etPassword.setText(password);

        if ("Virtual".equals(device)) {
            rgLoginType.check(R.id.rbVirtualDevice);
            etProxyPort.setText(proxyPort);
            etProxyPort.setVisibility(View.VISIBLE);
        } else {
            rgLoginType.check(R.id.rbPhysicalDevice);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if (checkedId == R.id.rbVirtualDevice) {
            etProxyPort.setVisibility(View.VISIBLE);
        } else {
            etProxyPort.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnLogin) {
            loginUser();
        } else if (view.getId() == R.id.btnGoToRegister) {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        BmobUser.loginByAccount(email, password, new LogInListener<BmobUser>() {
            @Override
            public void done(BmobUser bmobUser, BmobException e) {
                if (e == null) { // 成功登录
                    Toast.makeText(LoginActivity.this, "Login Success!", Toast.LENGTH_SHORT).show();
                    saveUserInfo(email, password);

                    Intent intent = new Intent(LoginActivity.this, AddPaperActivity.class);
                    startActivity(intent);
                } else { // 登录失败
                    Toast.makeText(LoginActivity.this, "Failed Login: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void saveUserInfo(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("password", password);
        int checkedRadioButtonId = rgLoginType.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.rbVirtualDevice) {
            editor.putString("device", "Virtual");
            editor.putString("proxyPort", etProxyPort.getText().toString().trim());
        } else {
            editor.putString("device", "Physical");
        }
        editor.apply();
    }

}