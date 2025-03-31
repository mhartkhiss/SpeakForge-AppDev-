package com.example.appdev.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.R;
import com.example.appdev.models.User;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddMembersAdapter extends RecyclerView.Adapter<AddMembersAdapter.MemberViewHolder> {

    private Context context;
    private List<User> contacts;
    private HashMap<String, Boolean> selectedUsers;
    private OnSelectionChangedListener selectionChangedListener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public AddMembersAdapter(Context context, List<User> contacts, HashMap<String, Boolean> selectedUsers) {
        this.context = context;
        this.contacts = contacts;
        this.selectedUsers = selectedUsers;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_select_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = contacts.get(position);
        
        // Set user info
        holder.textViewUsername.setText(user.getUsername());
        holder.textViewEmail.setText(user.getEmail());
        
        // Load profile picture
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty() 
                && !user.getProfilePictureUrl().equals("none")) {
            Glide.with(context)
                    .load(user.getProfilePictureUrl())
                    .placeholder(R.drawable.default_userpic)
                    .into(holder.imageViewProfilePic);
        } else {
            holder.imageViewProfilePic.setImageResource(R.drawable.default_userpic);
        }
        
        // Set checkbox state without triggering listener
        holder.checkBoxSelect.setOnCheckedChangeListener(null);
        holder.checkBoxSelect.setChecked(selectedUsers.containsKey(user.getUserId()));
        
        // Set up checkbox listener
        holder.checkBoxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Select user
                selectedUsers.put(user.getUserId(), false);
            } else {
                // Deselect user
                selectedUsers.remove(user.getUserId());
            }
            
            // Notify selection change
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(selectedUsers.size());
            }
        });
        
        // Make entire row clickable
        holder.itemView.setOnClickListener(v -> {
            holder.checkBoxSelect.setChecked(!holder.checkBoxSelect.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageViewProfilePic;
        TextView textViewUsername, textViewEmail;
        CheckBox checkBoxSelect;
        
        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProfilePic = itemView.findViewById(R.id.imageViewProfilePic);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            checkBoxSelect = itemView.findViewById(R.id.checkBoxSelect);
        }
    }
}
