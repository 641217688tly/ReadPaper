package bdic.comp3011j.readpaper;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse;
import com.unfbx.chatgpt.entity.chat.Message;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;

import okhttp3.OkHttpClient;

public class ChatActivity extends AppCompatActivity {

    private EditText userInput;
    private Button sendButton;
    private TextView aiResponse;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        userInput = findViewById(R.id.user_input);
        sendButton = findViewById(R.id.send_button);
        aiResponse = findViewById(R.id.ai_response);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = userInput.getText().toString();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 创建 OkHttpClient 用于设置代理
                        Proxy proxy;
                        if (getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("device", "Physical").equals("Physical")) {
                            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", Integer.parseInt(getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("proxyPort", "7890"))));
                        } else {
                            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.0.2.2", Integer.parseInt(getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("proxyPort", "7890"))));
                        }

                        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                                .proxy(proxy)
                                .build();

                        // 初始化 OpenAI 客户端并使用上述 OkHttpClient
                        OpenAiClient openAiClient = OpenAiClient.builder()
                                .apiKey(Arrays.asList("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"))  // 注意：不要在代码中明文显示API密钥
                                .okHttpClient(okHttpClient)
                                .build();

                        Message message = Message.builder()
                                .role(Message.Role.USER)
                                .content(input)
                                .build();

                        try {
                            ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(
                                    ChatCompletion.builder().messages(Arrays.asList(message)).build()
                            );

                            String response = chatCompletionResponse.getChoices().get(0).getMessage().getContent();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    aiResponse.setText(response);
                                }
                            });
                        } catch (Exception e) {
                            Log.e("ChatActivity", "Error interacting with OpenAI: ", e);
                        }
                    }
                }).start();
            }
        });
    }
}
