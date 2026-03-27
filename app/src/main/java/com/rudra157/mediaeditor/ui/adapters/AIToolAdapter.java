package com.rudra157.mediaeditor.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rudra157.mediaeditor.R;
import com.rudra157.mediaeditor.ui.AIToolsActivity.AITool;

import java.util.List;

/**
 * Adapter for AI tools grid
 */
public class AIToolAdapter extends RecyclerView.Adapter<AIToolAdapter.AIToolViewHolder> {
    
    private final List<AITool> aiTools;
    private final OnToolClickListener listener;

    public interface OnToolClickListener {
        void onToolClick(AITool tool);
    }

    public AIToolAdapter(List<AITool> aiTools, OnToolClickListener listener) {
        this.aiTools = aiTools;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AIToolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_ai_tool, parent, false);
        return new AIToolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AIToolViewHolder holder, int position) {
        AITool tool = aiTools.get(position);
        holder.bind(tool);
    }

    @Override
    public int getItemCount() {
        return aiTools.size();
    }

    class AIToolViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final ImageView iconImageView;
        private final TextView nameTextView;

        public AIToolViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
        }

        public void bind(AITool tool) {
            nameTextView.setText(tool.getNameResId());
            iconImageView.setImageResource(tool.getIconResId());
            
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToolClick(tool);
                }
            });
        }
    }
}
