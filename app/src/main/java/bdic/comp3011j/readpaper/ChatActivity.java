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
import cn.bmob.v3.listener.UpdateListener;
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
            // Finish the Activity after 120 seconds
            finish();
        }
    };

    private final int UPDATE_CONTEXT_THRESHOLD = 20;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize components
        etPrompt = findViewById(R.id.etPrompt);
        btSend = findViewById(R.id.btSend);
        // Load data
        currentPaper = AppApplication.getCurrentPaper();
        chatList = new ArrayList<>();
        // Set listeners
        btSend.setOnClickListener(this);
        loadHistoryChats(currentPaper);
    }

    private void loadHistoryChats(Paper currentPaper) {
        // Retrieve all chat records related to the current Paper from the Bmob database's Chat table
        queryPaperRelatedChats(currentPaper, new QueryCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> queryResult) {
                chatList.clear();
                // If there are no chat records for the current Paper, insert a system message as a context updater
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
                } else { // If there are chat records for the current Paper, load them directly
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
                Toast.makeText(ChatActivity.this, "History Chats failed to load: " + errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void initView() {
        if (!BmobUser.isLogin()) {
            // Show a message for not logged in and navigate to the login screen
            Toast.makeText(this, "Please Log in!", Toast.LENGTH_SHORT).show();
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
                // Notify the dataset change and scroll to the last item
                chatAdapter.notifyItemInserted(chatList.size() - 1);
                rvChat.scrollToPosition(chatList.size() - 1);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Start a timer when the user leaves the Activity
        handler.postDelayed(finishActivityTask, 120000); // Execute the run method after 120,000 milliseconds (120 seconds)
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cancel the timer when the user returns to the Activity
        handler.removeCallbacks(finishActivityTask);
    }

    @Override
    public void onClick(View view) {
        if (view == btSend) { // Send a new message
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
                // Set up a proxy based on the device type and create an OpenAI client:
                OpenAiClient openAiClient;
                String deviceType = getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("device", "Physical");
                if (deviceType.equals("Physical")) {
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(120, TimeUnit.SECONDS)
                            .writeTimeout(120, TimeUnit.SECONDS)
                            .readTimeout(120, TimeUnit.SECONDS)
                            .build();
                    // Create the OpenAI client
                    openAiClient = OpenAiClient.builder()
                            .okHttpClient(okHttpClient)
                            .apiKey(Arrays.asList("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", "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"))
                            .keyStrategy(new KeyRandomStrategy())
                            .apiHost("https://api.openai-proxy.com")
                            .build();
                } else {
                    // Set up a custom network proxy and OkHttpClient
                    int proxyPort = Integer.parseInt(getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("proxyPort", "7890"));
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.0.2.2", proxyPort));
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            //.proxy(proxy)
                            .connectTimeout(120, TimeUnit.SECONDS)
                            .writeTimeout(120, TimeUnit.SECONDS)
                            .readTimeout(120, TimeUnit.SECONDS)
                            .build();
                    // Create the OpenAI client
                    openAiClient = OpenAiClient.builder()
                            .okHttpClient(okHttpClient)
                            .apiKey(Arrays.asList("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", "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"))
                            .keyStrategy(new KeyRandomStrategy())
                            .apiHost("https://api.openai-proxy.com")
                            .build();
                }

                // Construct a list of messages for this interaction
                List<Message> messages = constructChatMessages(prompt, chatList);

                for (Message message : messages) {
                    Log.d("ChatActivity", "Message Role: " + message.getRole());
                    Log.d("ChatActivity", "Message Content: " + message.getContent());
                }
                // Construct a ChatCompletion object to load chat history and model parameters
                ChatCompletion chatCompletion = ChatCompletion
                        .builder()
                        .messages(messages)
                        .model(ChatCompletion.Model.GPT_3_5_TURBO_16K.getName())
                        //.maxTokens(4096 - TikTokensUtil.tokens(ChatCompletion.Model.GPT_3_5_TURBO_16K.getName(), messages))
                        .build();

                // Get a response from OpenAI
                try {
                    ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
                    String response = chatCompletionResponse.getChoices().get(0).getMessage().getContent();

                    // Insert the user's question and OpenAI's response into the database and chatList
                    if (response != null && !response.isEmpty()) {
                        // Insert the user's question into the database first
                        insertChat(prompt, MessageType.USER, new InsertCallback() {
                            @Override
                            public void onSuccess(String objectId) { // After adding, add the user input to chatList and messages
                                chatList.add(new Chat(objectId, prompt, MessageType.USER.toString()));
                                Log.d("ChatActivity", "USER Content: " + chatList.get(chatList.size() - 1).getContent());
                                // Insert OpenAI's response (ensures that the type USER is always inserted into the database before ASSISTANT)
                                insertChat(response, MessageType.ASSISTANT, new InsertCallback() {
                                    @Override
                                    public void onSuccess(String objectId) {
                                        chatList.add(new Chat(objectId, response, MessageType.ASSISTANT.toString()));
                                        Log.d("ChatActivity", "ASSISTANT Content: " + chatList.get(chatList.size() - 1).getContent());
                                        // Update the UI
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // Notify the dataset change and scroll to the last item
                                                chatAdapter.notifyItemInserted(chatList.size() - 1);
                                                rvChat.scrollToPosition(chatList.size() - 1);
                                                etPrompt.setText(""); // Clear the input field
                                            }
                                        });
                                        messages.add(Message.builder()
                                                .role(Message.Role.ASSISTANT)
                                                .content(response)
                                                .build());
                                        updateSystemMessage(messages);
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


    private void updateSystemMessage(List<Message> messages) { // Used to update the system message (i.e., context)
        // Retrieve all chat records related to the current Paper from the Bmob database's Chat table
        queryPaperRelatedChats(currentPaper, new QueryCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> queryResult) {
                if ((queryResult.size() - 1) % UPDATE_CONTEXT_THRESHOLD == 0) { // If (queryResult.size() - 1) % UPDATE_CONTEXT_THRESHOLD = 0, it indicates the need to update the system message
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // Set up a proxy based on the device type and create an OpenAI client:
                            OpenAiClient openAiClient;
                            String deviceType = getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("device", "Physical");
                            if (deviceType.equals("Physical")) {
                                OkHttpClient okHttpClient = new OkHttpClient.Builder()
                                        .connectTimeout(120, TimeUnit.SECONDS)
                                        .writeTimeout(120, TimeUnit.SECONDS)
                                        .readTimeout(120, TimeUnit.SECONDS)
                                        .build();
                                // Create the OpenAI client
                                openAiClient = OpenAiClient.builder()
                                        .okHttpClient(okHttpClient)
                                        .apiKey(Arrays.asList("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", "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"))
                                        .keyStrategy(new KeyRandomStrategy())
                                        .apiHost("https://api.openai-proxy.com")
                                        .build();
                            } else {
                                // Set up a custom network proxy and OkHttpClient
                                int proxyPort = Integer.parseInt(getSharedPreferences("ReadPaper", Context.MODE_PRIVATE).getString("proxyPort", "7890"));
                                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.0.2.2", proxyPort));
                                OkHttpClient okHttpClient = new OkHttpClient.Builder()
                                        //.proxy(proxy)
                                        .connectTimeout(120, TimeUnit.SECONDS)
                                        .writeTimeout(120, TimeUnit.SECONDS)
                                        .readTimeout(120, TimeUnit.SECONDS)
                                        .build();
                                // Create the OpenAI client
                                openAiClient = OpenAiClient.builder()
                                        .okHttpClient(okHttpClient)
                                        .apiKey(Arrays.asList("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", "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"))
                                        .keyStrategy(new KeyRandomStrategy())
                                        .apiHost("https://api.openai-proxy.com")
                                        .build();
                            }

                            // Add a message to request a summary of our previous chat
                            messages.add(Message.builder()
                                    .role(Message.Role.USER)
                                    .content("Please summarize our previous chat")
                                    .build());

                            // Construct a ChatCompletion object to load chat history and model parameters
                            ChatCompletion chatCompletion = ChatCompletion
                                    .builder()
                                    .messages(messages)
                                    .model(ChatCompletion.Model.GPT_3_5_TURBO_16K.getName())
                                    //.maxTokens(4096 - TikTokensUtil.tokens(ChatCompletion.Model.GPT_3_5_TURBO_16K.getName(), messages))
                                    .build();

                            // Get a response from OpenAI
                            try {
                                ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
                                String response = chatCompletionResponse.getChoices().get(0).getMessage().getContent();
                                // Update the content of SYSTEM
                                Chat systemChat = chatList.get(0);
                                systemChat.setContent(response);
                                systemChat.update(systemChat.getObjectId(), new UpdateListener() {
                                    @Override
                                    public void done(BmobException e) {
                                        if (e == null) {
                                            chatList.get(0).setContent(response);
                                            Log.d("ChatActivity", "Update SYSTEM Content Success!");
                                        } else {
                                            Log.d("ChatActivity", "Update SYSTEM Content Failed: " + e.getMessage());
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("ChatActivity", "Error interacting with OpenAI: ", e);
                            }
                        }
                    }).start();
                }
            }
            @Override
            public void onFail(String errorMessage) {
                Toast.makeText(ChatActivity.this, "History Chats failed to load: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<Message> constructChatMessages(String prompt, List<Chat> chatList) {
        List<Message> messages = new ArrayList<>();
        // Calculate the number of chat records that have already been covered by the context
        int coveredChatRange = ((chatList.size() - 1) / UPDATE_CONTEXT_THRESHOLD) * UPDATE_CONTEXT_THRESHOLD;
        // Calculate the number of chat records to be added to messages
        int reminder = (chatList.size() - 1) - coveredChatRange;

        // Add system context
        messages.add(0, Message.builder()
                .role(Message.Role.SYSTEM)
                .content(chatList.get(0).getContent())
                .build());
        if (reminder > 0) {
            List<Chat> remindingChats = chatList.subList(chatList.size() - reminder, chatList.size());
            for (Chat chat : remindingChats) {
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
        }
        // Add the user's input for this session to messages
        messages.add(Message.builder()
                .role(Message.Role.USER)
                .content(prompt)
                .build());
        return messages;
    }

//-----------------------------------Below are CRUD (Create, Read, Update, Delete) operations--------------------------------------------

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