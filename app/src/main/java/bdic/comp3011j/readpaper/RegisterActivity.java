package bdic.comp3011j.readpaper;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etUsername, etEmail, etPassword, etPasswordConfirm;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        // 设置按钮监听时间
        btnRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnRegister) {
            registerUser();
        }
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();
        // 确保用户两次输入的密码相同
        if (!password.equals(passwordConfirm)) {
            Toast.makeText(RegisterActivity.this, "The two passwords are different", Toast.LENGTH_SHORT).show();
            return;
        }
        // 确保用户使用的邮箱未被注册过
        BmobQuery<BmobUser> query = new BmobQuery<>();
        query.addWhereEqualTo("email", email);
        query.findObjects(new FindListener<BmobUser>() {
            @Override
            public void done(List<BmobUser> list, BmobException e) {
                if (e == null) {
                    if (list.size() > 0) { // 该邮箱已被注册
                        Toast.makeText(RegisterActivity.this, "The email address has been registered", Toast.LENGTH_SHORT).show();
                    } else { // 该邮箱未被注册
                        BmobUser user = new BmobUser();
                        user.setUsername(username);
                        user.setEmail(email);
                        user.setPassword(password);
                        user.signUp(new SaveListener<BmobUser>() {
                            @Override
                            public void done(BmobUser bmobUser, BmobException e) {
                                if (e == null) {
                                    Toast.makeText(RegisterActivity.this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Fail to register: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Query failure: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


}