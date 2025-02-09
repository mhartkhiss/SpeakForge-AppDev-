package com.example.appdev.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.example.appdev.R;
import com.example.appdev.models.Languages;
import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
    private List<String> languages;
    private OnLanguageSelectedListener listener;

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(String language);
    }

    public LanguageAdapter(Context context, OnLanguageSelectedListener listener) {
        this.languages = Languages.getAllLanguages();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MaterialButton button = (MaterialButton) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new ViewHolder(button);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String language = languages.get(position);
        holder.button.setText(language);
        holder.button.setOnClickListener(v -> listener.onLanguageSelected(language));
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialButton button;

        ViewHolder(MaterialButton button) {
            super(button);
            this.button = button;
        }
    }
} 