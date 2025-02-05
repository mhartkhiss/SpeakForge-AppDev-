package com.example.appdev.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdev.R;
import com.example.appdev.models.TranslationHistory;
import com.example.appdev.translators.TranslatorType;

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
        
        // Set languages with fallback values
        String sourceLanguage = history.getSourceLanguage();
        if (sourceLanguage == null || sourceLanguage.isEmpty()) {
            sourceLanguage = "English"; // Default fallback
        }
        holder.sourceLanguage.setText(sourceLanguage);
        holder.targetLanguage.setText(history.getTargetLanguage());
        
        // Get translator type from history
        String translatorId = history.getTranslator();
        if (translatorId == null || translatorId.isEmpty()) {
            translatorId = "google"; // Default fallback
        }
        
        // Use TranslatorType to get translator info
        TranslatorType translatorType = TranslatorType.fromId(translatorId);
        holder.translatorIcon.setImageResource(translatorType.getIconResourceId());
        holder.translatorName.setText(translatorType.getDisplayName());
        
        // Set texts
        holder.originalText.setText(history.getOriginalText());
        holder.translatedText.setText(history.getTranslatedText());
        holder.dateTime.setText(dateFormat.format(new Date(history.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sourceLanguage, targetLanguage, originalText, translatedText, dateTime, translatorName;
        ImageView translatorIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            sourceLanguage = itemView.findViewById(R.id.sourceLanguage);
            targetLanguage = itemView.findViewById(R.id.targetLanguage);
            originalText = itemView.findViewById(R.id.originalText);
            translatedText = itemView.findViewById(R.id.translatedText);
            dateTime = itemView.findViewById(R.id.dateTime);
            translatorIcon = itemView.findViewById(R.id.translatorIcon);
            translatorName = itemView.findViewById(R.id.translatorName);
        }
    }
} 