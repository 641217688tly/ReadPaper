package bdic.comp3011j.readpaper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse;
import com.unfbx.chatgpt.entity.chat.Message;
import com.unfbx.chatgpt.function.KeyRandomStrategy;
import com.unfbx.chatgpt.utils.TikTokensUtil;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import bdic.comp3011j.readpaper.Adapter.ChatAdapter;
import bdic.comp3011j.readpaper.Application.AppApplication;
import bdic.comp3011j.readpaper.BmobEntity.Chat;
import bdic.comp3011j.readpaper.BmobEntity.MessageType;
import bdic.comp3011j.readpaper.BmobEntity.Paper;
import bdic.comp3011j.readpaper.Util.InsertCallback;
import bdic.comp3011j.readpaper.Util.QueryCallback;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;
import okhttp3.OkHttpClient;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etPrompt;
    private Button btSend;
    private Paper currentPaper;
    private List<Chat> chatList;
    private RecyclerView rvChat;
    private ChatAdapter chatAdapter;
    Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finishActivityTask = new Runnable() {
        @Override
        public void run() {
            // 当一分钟过去后，结束Activity
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 初始化组件
        etPrompt = findViewById(R.id.etPrompt);
        btSend = findViewById(R.id.btSend);
        // 加载数据
        currentPaper = AppApplication.getCurrentPaper();
        chatList = new ArrayList<>();
        // 设置监听器
        btSend.setOnClickListener(this);
        loadHistoryChats(currentPaper);
    }

    private void loadHistoryChats(Paper currentPaper) {
        // 从Bmob数据库中的Chat表获取所有与当前Paper有关的聊天记录
        queryPaperRelatedChats(currentPaper, new QueryCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> queryResult) {
                chatList.clear();
                // 假如当前Paper没有聊天记录,则插入一条系统消息,用作后续更新聊天语境
                if (queryResult.size() == 0 || queryResult == null) {
                    insertChat("", MessageType.SYSTEM, new InsertCallback() {
                        @Override
                        public void onSuccess(String objectId) {
                            chatList.add(0, new Chat(objectId, "", MessageType.SYSTEM.toString()));
                            initView();
                        }

                        @Override
                        public void onFail(String errorMessage) {
                            Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                } else { // 当前Paper有聊天记录,则直接加载
                    for (Chat chat : queryResult) {
                        if (chat.getType().equals(MessageType.SYSTEM.toString())) {
                            chatList.add(0, chat);
                        } else {
                            chatList.add(chat);
                        }
                    }
                    initView();
                }
            }

            @Override
            public void onFail(String errorMessage) {
                Toast.makeText(ChatActivity.this, "History Chats failed load: " + errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void initView() {
        if (!BmobUser.isLogin()) {
            // 弹出未登录消息,跳转到登录界面
            Toast.makeText(this, "Please Login!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        rvChat = findViewById(R.id.rvChat);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.addItemDecoration(new DividerItemDecoration(rvChat.getContext(), DividerItemDecoration.VERTICAL));
        chatAdapter = new ChatAdapter(chatList, ChatActivity.this);
        rvChat.setAdapter(chatAdapter);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 通知数据集改变并滚动到最后一条
                chatAdapter.notifyItemInserted(chatList.size() - 1);
                rvChat.scrollToPosition(chatList.size() - 1);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 用户离开Activity时开始计时
        handler.postDelayed(finishActivityTask, 60000); // 60000毫秒后执行run方法
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 用户返回Activity时取消计时
        handler.removeCallbacks(finishActivityTask);
    }

    @Override
    public void onClick(View view) {
        if (view == btSend) { // 发送新消息
            String prompt = etPrompt.getText().toString();
            Log.d("ChatActivity", "prompt: " + prompt);
            if (!prompt.isEmpty()) {
                chatWithOpenAI(prompt);
            }
        }
    }

    private void chatWithOpenAI(String prompt) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 根据设备类型设置代理并创建OpenAI客户端:
                OpenAiClient openAiClient;
                String deviceType = getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("device", "Physical");
                if (deviceType.equals("Physical")) {
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(120, TimeUnit.SECONDS)
                            .writeTimeout(120, TimeUnit.SECONDS)
                            .readTimeout(120, TimeUnit.SECONDS)
                            .build();
                    // 创建OpenAI客户端
                    openAiClient = OpenAiClient.builder()
                            .okHttpClient(okHttpClient)
                            .apiKey(Arrays.asList("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", "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"))
                            .keyStrategy(new KeyRandomStrategy())
                            .apiHost("https://api.openai-proxy.com")
                            .build();
                } else {
                    // 自定义网络代理和okHttpClient
                    int proxyPort = Integer.parseInt(getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("proxyPort", "7890"));
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.0.2.2", proxyPort));
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            //.proxy(proxy)
                            .connectTimeout(120, TimeUnit.SECONDS)
                            .writeTimeout(120, TimeUnit.SECONDS)
                            .readTimeout(120, TimeUnit.SECONDS)
                            .build();
                    // 创建OpenAI客户端
                    openAiClient = OpenAiClient.builder()
                            .okHttpClient(okHttpClient)
                            .apiKey(Arrays.asList("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", "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"))
                            .keyStrategy(new KeyRandomStrategy())
                            .apiHost("https://api.openai-proxy.com")
                            .build();
                }

                // 构建本次交互的消息列表
                List<Message> messages = constructChatMessages(prompt);
                for (Message message : messages) {
                    Log.d("ChatActivity", "Message Role: " + message.getRole());
                    Log.d("ChatActivity", "Message Content: " + message.getContent());
                }
                // 构造ChatCompletion对象以加载聊天记录以及模型参数
                ChatCompletion chatCompletion = ChatCompletion
                        .builder()
                        .messages(messages)
                        .model(ChatCompletion.Model.GPT_3_5_TURBO_16K.getName())
                        //.maxTokens(4096 - TikTokensUtil.tokens(ChatCompletion.Model.GPT_3_5_TURBO_16K.getName(), messages))
                        .build();

                // 获取OpenAI的响应
                try {
                    ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
                    String response = chatCompletionResponse.getChoices().get(0).getMessage().getContent();

                    // 向数据库和chatList中插入用户的提问和Openai的回复
                    if (response != null && !response.isEmpty()) {
                        // 插入用户的提问
                        insertChat(prompt, MessageType.USER, new InsertCallback() { // 先向数据库中添加本次用户输入
                            @Override
                            public void onSuccess(String objectId) { // 添加完成后,向chatList和messages中添加本次用户输入
                                chatList.add(new Chat(objectId, prompt, MessageType.USER.toString()));

                                // 插入OpenAI的响应(保证了类型为USER的Chat总是在类型为ASSISTANT的Chat之前被插入数据库)
                                insertChat(response, MessageType.ASSISTANT, new InsertCallback() {
                                    @Override
                                    public void onSuccess(String objectId) {
                                        chatList.add(new Chat(objectId, response, MessageType.ASSISTANT.toString()));
                                        // 更新UI界面
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // 通知数据集改变并滚动到最后一条
                                                chatAdapter.notifyItemInserted(chatList.size() - 1);
                                                rvChat.scrollToPosition(chatList.size() - 1);
                                                etPrompt.setText(""); // 清空输入框
                                            }
                                        });
                                        updateSystemMessage();
                                    }

                                    @Override
                                    public void onFail(String errorMessage) {
                                        Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onFail(String errorMessage) {
                                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("ChatActivity", "Error interacting with OpenAI: ", e);
                }
            }
        }).start();
    }


    private void updateSystemMessage() { //用于更新系统消息(即,语境)

    }

    private List<Message> constructChatMessages(String prompt) {
        List<Message> messages = new ArrayList<>();
        //TODO 向messages中添加历史聊天记录(有待修改)
        for (Chat chat : chatList) {
            Message.Builder messageBuilder = Message.builder().content(chat.getContent());
            if (chat.getType().equals(MessageType.USER.toString())) {
                messageBuilder.role(Message.Role.USER);
            } else if (chat.getType().equals(MessageType.ASSISTANT.toString())) {
                messageBuilder.role(Message.Role.ASSISTANT);
            } else if (chat.getType().equals(MessageType.SYSTEM.toString())) {
                messageBuilder.role(Message.Role.SYSTEM);
            }
            if (chat.getType().equals(MessageType.SYSTEM.toString())) {
                messages.add(0, messageBuilder.build());
            } else {
                messages.add(messageBuilder.build());
            }
        }
        // 向messages中添加本次的用户输入
        messages.add(Message.builder()
                .role(Message.Role.USER)
                .content(prompt)
                .build());
        return messages;
    }

    //-----------------------------------以下为增删改查操作--------------------------------------------

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

    private void insertChat(String content, MessageType type, InsertCallback callback) {
        Chat newChat = new Chat();
        newChat.setPaper(currentPaper);
        newChat.setContent(content);
        newChat.setType(type.toString());
        newChat.save(new SaveListener<String>() {
            @Override
            public void done(String objectId, BmobException e) {
                if (e == null) {
                    callback.onSuccess(objectId);
                } else {
                    callback.onFail("Failed to insert new chat: " + e.getMessage());
                }
            }
        });
    }
}
