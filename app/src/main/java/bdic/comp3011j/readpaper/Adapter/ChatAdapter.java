package bdic.comp3011j.readpaper.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import bdic.comp3011j.readpaper.BmobEntity.Chat;
import bdic.comp3011j.readpaper.R;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder>{

    private List<Chat> chatList;
    private Context context;
    private ChatAdapter.OnRecyclerItemClickListener onClickListener;

    public ChatAdapter(List<Chat> chatList, Context context) {
        this.chatList = chatList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = View.inflate(context, R.layout.selecter_chat, null);
        // Make sure it matches the parent width
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (chatList == null) {
            return;
        }
        Chat chat = chatList.get(position);
        holder.tvRole.setText(chat.getType());
        holder.tvContent.setText(chat.getContent());

        // Set the button click listeners
    }

    @Override
    public int getItemCount() {
        if (chatList == null) {
            return 0;
        } else {
            return chatList.size();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvRole, tvContent;

        public ViewHolder(@NonNull View itemView) { // itemView is : View.inflate(context, R.layout.selecter_chat, null);
            super(itemView);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvContent = itemView.findViewById(R.id.tvContent);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onClickListener != null) {
                        onClickListener.OnRecyclerItemClick(getAdapterPosition());
                    }
                }
            });
        }
    }

    public void setOnRecyclerItemClickListener(OnRecyclerItemClickListener onRecyclerItemClickListener) {
        this.onClickListener = onRecyclerItemClickListener;
    }

    public interface OnRecyclerItemClickListener { // 设置Item的点击监听器的接口
        void OnRecyclerItemClick(int position);
    }
}
