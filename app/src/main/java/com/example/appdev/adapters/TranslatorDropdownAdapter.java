package com.example.appdev.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.appdev.R;
import com.example.appdev.translators.TranslatorType;
import java.util.List;

public class TranslatorDropdownAdapter extends ArrayAdapter<TranslatorType> {
    private final LayoutInflater inflater;

    public TranslatorDropdownAdapter(@NonNull Context context, @NonNull List<TranslatorType> translators) {
        super(context, 0, translators);
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.translator_dropdown_item, parent, false);
        }

        ImageView iconView = convertView.findViewById(R.id.translatorIcon);
        TextView nameView = convertView.findViewById(R.id.translatorName);

        TranslatorType translator = getItem(position);
        if (translator != null) {
            iconView.setImageResource(translator.getIconResourceId());
            nameView.setText(translator.getDisplayName());
        }

        return convertView;
    }
} 