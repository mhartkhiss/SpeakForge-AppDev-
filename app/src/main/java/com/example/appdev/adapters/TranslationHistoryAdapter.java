package com.example.appdev.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdev.R;
import com.example.appdev.models.TranslationHistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TranslationHistoryAdapter extends RecyclerView.Adapter<TranslationHistoryAdapter.ViewHolder> {
    private List<TranslationHistory> historyList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());

    public TranslationHistoryAdapter(List<TranslationHistory> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.translation_history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TranslationHistory history = historyList.get(position);
        holder.targetLanguage.setText(history.getTargetLanguage());
        holder.originalText.setText(history.getOriginalText());
        holder.translatedText.setText(history.getTranslatedText());
        holder.dateTime.setText(dateFormat.format(new Date(history.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView targetLanguage, originalText, translatedText, dateTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            targetLanguage = itemView.findViewById(R.id.targetLanguage);
            originalText = itemView.findViewById(R.id.originalText);
            translatedText = itemView.findViewById(R.id.translatedText);
            dateTime = itemView.findViewById(R.id.dateTime);
        }
    }
} 