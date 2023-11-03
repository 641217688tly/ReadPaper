package bdic.comp3011j.readpaper.Adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import bdic.comp3011j.readpaper.BmobEntity.Paper;
import bdic.comp3011j.readpaper.HomepageActivity;
import bdic.comp3011j.readpaper.R;


public class PaperAdapter extends RecyclerView.Adapter<PaperAdapter.ViewHolder> {

    private List<Paper> paperList;
    private Context context;
    private OnRecyclerItemClickListener onClickListener;


    public PaperAdapter(List<Paper> paperList, Context context) {
        this.paperList = paperList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = View.inflate(context, R.layout.selecter_homepage, null);
        // Make sure it matches the parent width
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (paperList == null) {
            return;
        }
        Paper paper = paperList.get(position);
        holder.tvTitle.setText(paper.getTitle());
        holder.tvAuthor.setText(paper.getAuthor());
        holder.tvCreatedTime.setText(paper.getCreatedAt());

        // Set the button click listeners
        holder.btnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(context instanceof HomepageActivity){
                    ((HomepageActivity)context).viewPDF(paper.getUrl());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (paperList == null) {
            return 0;
        } else {
            return paperList.size();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle, tvAuthor, tvCreatedTime;
        private Button btnEdit, btnDelete, btnView;

        public ViewHolder(@NonNull View itemView) { // itemView is : View.inflate(context, R.layout.selecter_homepage, null);
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvCreatedTime = itemView.findViewById(R.id.tvCreatedTime);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnView = itemView.findViewById(R.id.btnView);

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
