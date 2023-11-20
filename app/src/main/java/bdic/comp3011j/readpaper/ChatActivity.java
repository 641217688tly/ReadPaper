package bdic.comp3011j.readpaper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse;
import com.unfbx.chatgpt.entity.chat.Message;
import com.unfbx.chatgpt.utils.TikTokensUtil;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private TextView tvResponse;
    private Paper currentPaper;
    private List<Chat> chatList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // 初始化组件
        etPrompt = findViewById(R.id.etPrompt);
        btSend = findViewById(R.id.btSend);
        tvResponse = findViewById(R.id.tvResponse);

        // 加载数据
        currentPaper = ((AppApplication) getApplicationContext()).getCurrentPaper();
        chatList = new ArrayList<>();
        loadHistoryChats(currentPaper);
        initView();

        // 设置监听器
        btSend.setOnClickListener(this);
    }

    private void loadHistoryChats(Paper currentPaper) {
        // 从Bmob数据库中的Chat表获取所有与当前Paper有关的聊天记录
        queryPaperRelatedChats(currentPaper, new QueryCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> queryResult) {
                // 假如当前Paper没有聊天记录,则插入一条系统消息,用作后续更新聊天语境
                if (queryResult.size() == 0 || queryResult == null) {
                    insertChat("", MessageType.SYSTEM, new InsertCallback() {
                        @Override
                        public void onSuccess(String objectId) {
                            chatList.add(new Chat(objectId, "", MessageType.SYSTEM.toString()));
                        }
                        @Override
                        public void onFail(String errorMessage) {
                            Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
                }
            }
            @Override
            public void onFail(String errorMessage) {
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initView() {
        //TODO

    }

    @Override
    public void onClick(View view) {
        if (view == btSend) { // 发送新消息
            String prompt = etPrompt.getText().toString();
            chatWithOpenAI(prompt);
        }
    }

    private void chatWithOpenAI(String prompt) {
        new Thread(() -> {
            // 根据设备类型设置代理:
            Proxy proxy;
            String deviceType = getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("device", "Physical");
            int proxyPort = Integer.parseInt(getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("proxyPort", "7890"));
            proxy = deviceType.equals("Physical") ?
                    new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", proxyPort)) :
                    new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.0.2.2", proxyPort));

            // 创建OpenAI客户端
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .proxy(proxy)
                    .build();
            OpenAiClient openAiClient = OpenAiClient.builder()
                    .apiKey(Arrays.asList("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"))
                    .okHttpClient(okHttpClient)
                    .build();

            // 构建本次交互的消息列表
            List<Message> messages = new ArrayList<>();
            constructChatMessages(prompt, new QueryCallback<Message>() {
                @Override
                public void onSuccess(List<Message> result) {
                    messages.addAll(result);
                }
                @Override
                public void onFail(String errorMessage) {
                    Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
            messages.add(Message.builder()
                    .role(Message.Role.USER)
                    .content(prompt)
                    .build());

            // 构造ChatCompletion对象以加载聊天记录以及模型参数
            ChatCompletion chatCompletion = ChatCompletion
                    .builder()
                    .messages(messages)
                    .model(ChatCompletion.Model.GPT_4.getName())
                    //.maxTokens(4096 - TikTokensUtil.tokens(ChatCompletion.Model.GPT_4.getName(), messages))
                    .build();

            // 获取OpenAI的响应
            try {
                ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
                String response = chatCompletionResponse.getChoices().get(0).getMessage().getContent();

                // 向数据库和chatList中插入Openai的回复
                insertChat(response, MessageType.ASSISTANT, new InsertCallback() {
                    @Override
                    public void onSuccess(String objectId) {
                        chatList.add(new Chat(objectId, response, MessageType.ASSISTANT.toString()));
                    }

                    @Override
                    public void onFail(String errorMessage) {
                        Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });

                runOnUiThread(() -> tvResponse.setText(response)); //TODO 有待修改
                // 更新系统消息(即,语境)
                updateSystemMessage();
            } catch (Exception e) {
                Log.e("ChatActivity", "Error interacting with OpenAI: ", e);
            }
        }).start();
    }


    private void updateSystemMessage() { //用于更新系统消息(即,语境)

    }

    private void constructChatMessages(String prompt, QueryCallback<Message> callback) {
        List<Message> messages = new ArrayList<>();
        // 向messages中添加历史聊天记录(有待修改)
        for (Chat chat : chatList) {
            Message.Builder messageBuilder = Message.builder().content(chat.getContent());
            if (chat.getType().equals(MessageType.USER.toString())) {
                messageBuilder.role(Message.Role.USER);
            } else if (chat.getType().equals(MessageType.ASSISTANT.toString())) {
                messageBuilder.role(Message.Role.ASSISTANT);
            } else if (chat.getType().equals(MessageType.SYSTEM.toString())) {
                messageBuilder.role(Message.Role.SYSTEM);
            }
            messages.add(messageBuilder.build());
        }
        // 向messages中添加本次的用户输入
        insertChat(prompt, MessageType.USER, new InsertCallback() { // 先向数据库中添加本次用户输入
            @Override
            public void onSuccess(String objectId) { // 添加完成后,向chatList和messages中添加本次用户输入
                chatList.add(new Chat(objectId, prompt, MessageType.USER.toString()));
                messages.add(Message.builder()
                        .role(Message.Role.USER)
                        .content(prompt)
                        .build());
                callback.onSuccess(messages);
            }
            @Override
            public void onFail(String errorMessage) {
                callback.onFail(errorMessage);
            }
        });
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
