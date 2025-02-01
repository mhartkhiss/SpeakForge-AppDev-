package com.example.appdev.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.example.appdev.R;
import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {
    private List<String> languages;
    private OnLanguageSelectedListener listener;

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(String language);
    }

    public LanguageAdapter(List<String> languages, OnLanguageSelectedListener listener) {
        this.languages = languages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        String language = languages.get(position);
        holder.btnLanguage.setText(language);
        holder.btnLanguage.setOnClickListener(v -> listener.onLanguageSelected(language));
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    static class LanguageViewHolder extends RecyclerView.ViewHolder {
        MaterialButton btnLanguage;

        LanguageViewHolder(View itemView) {
            super(itemView);
            btnLanguage = (MaterialButton) itemView;
        }
    }
} 