package com.example.appdev.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.R;
import com.example.appdev.models.Group;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupViewHolder> {

    private Context context;
    private List<Group> groups;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Group group);
    }

    public GroupListAdapter(Context context, List<Group> groups) {
        this.context = context;
        this.groups = groups;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groups.get(position);
        
        // Set group name
        holder.textViewGroupName.setText(group.getName());
        
        // Set group info (member count)
        int memberCount = group.getMembers() != null ? group.getMembers().size() : 0;
        holder.textViewGroupInfo.setText(memberCount + " members");
        
        // Set creation date
        if (group.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.textViewCreatedDate.setText("Created on " + sdf.format(new Date(group.getCreatedAt())));
        } else {
            holder.textViewCreatedDate.setVisibility(View.GONE);
        }
        
        // Load group image
        if (group.getGroupImageUrl() != null && !group.getGroupImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(group.getGroupImageUrl())
                    .placeholder(R.drawable.group_default_icon)
                    .into(holder.imageViewGroupPic);
        } else {
            holder.imageViewGroupPic.setImageResource(R.drawable.group_default_icon);
        }
        
        // Set admin badge visibility
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isAdmin = group.getMembers() != null && 
                group.getMembers().containsKey(currentUserId) && 
                Boolean.TRUE.equals(group.getMembers().get(currentUserId));
        
        holder.textViewAdminBadge.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageViewGroupPic;
        TextView textViewGroupName, textViewGroupInfo, textViewCreatedDate, textViewAdminBadge;
        
        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewGroupPic = itemView.findViewById(R.id.imageViewGroupPic);
            textViewGroupName = itemView.findViewById(R.id.textViewGroupName);
            textViewGroupInfo = itemView.findViewById(R.id.textViewGroupInfo);
            textViewCreatedDate = itemView.findViewById(R.id.textViewCreatedDate);
            textViewAdminBadge = itemView.findViewById(R.id.textViewAdminBadge);
        }
    }
}
