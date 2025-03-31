package com.example.appdev.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.R;
import com.example.appdev.models.User;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder> {

    private Context context;
    private List<User> members;
    private String groupId;
    private String currentUserId;
    private boolean isCurrentUserAdmin = false;
    private String groupCreatorId; // Store the creator ID

    public GroupMemberAdapter(Context context, List<User> members, String groupId) {
        this.context = context;
        this.members = members;
        this.groupId = groupId;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Get group details including creator
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId);
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get group creator
                if (snapshot.child("createdBy").exists()) {
                    groupCreatorId = snapshot.child("createdBy").getValue(String.class);
                }
                
                // Check if current user is admin
                if (snapshot.child("members").child(currentUserId).exists()) {
                    Boolean isAdmin = snapshot.child("members").child(currentUserId).getValue(Boolean.class);
                    isCurrentUserAdmin = isAdmin != null && isAdmin;
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User member = members.get(position);
        
        // Set member info
        holder.textViewUsername.setText(member.getUsername());
        
        // Show admin status if applicable
        if (member.isAdmin()) {
            holder.textViewStatus.setText("Admin");
            holder.textViewStatus.setVisibility(View.VISIBLE);
        } else {
            holder.textViewStatus.setVisibility(View.GONE);
        }
        
        // Load profile picture
        if (member.getProfilePictureUrl() != null && !member.getProfilePictureUrl().isEmpty() 
                && !member.getProfilePictureUrl().equals("none")) {
            Glide.with(context)
                    .load(member.getProfilePictureUrl())
                    .placeholder(R.drawable.default_userpic)
                    .into(holder.imageViewProfilePic);
        } else {
            holder.imageViewProfilePic.setImageResource(R.drawable.default_userpic);
        }
        
        // Handle more button visibility and click
        if (isCurrentUserAdmin && !member.getUserId().equals(currentUserId)) {
            holder.buttonMore.setVisibility(View.VISIBLE);
            holder.buttonMore.setOnClickListener(v -> showMemberOptions(v, member));
        } else if (member.getUserId().equals(currentUserId)) {
            // Don't show more button for current user
            holder.buttonMore.setVisibility(View.INVISIBLE);
        } else {
            // Non-admins can't see more button for other members
            holder.buttonMore.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }
    
    private void showMemberOptions(View view, User member) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.group_member_menu);
        
        // Check if this member is the group creator
        boolean isGroupCreator = member.getUserId().equals(groupCreatorId);
        
        // Disable options for group creator if current user is not the creator
        if (isGroupCreator && !currentUserId.equals(groupCreatorId)) {
            popupMenu.getMenu().findItem(R.id.action_make_admin).setEnabled(false);
            popupMenu.getMenu().findItem(R.id.action_remove_member).setEnabled(false);
        }
        
        // Check if member is already admin and update menu item text
        if (member.isAdmin()) {
            popupMenu.getMenu().findItem(R.id.action_make_admin).setTitle("Remove as Admin");
        } else {
            popupMenu.getMenu().findItem(R.id.action_make_admin).setTitle("Make Admin");
        }
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_make_admin) {
                toggleAdminStatus(member);
                return true;
            } else if (id == R.id.action_remove_member) {
                removeMember(member);
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    private void toggleAdminStatus(User member) {
        // Check if this member is the group creator - prevent other admins from changing creator's status
        if (member.getUserId().equals(groupCreatorId) && !currentUserId.equals(groupCreatorId)) {
            CustomNotification.showNotification(context, "Cannot change the group owner's admin status", false);
            return;
        }
        
        DatabaseReference memberRef = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId).child("members").child(member.getUserId());
        
        // Toggle admin status
        boolean newStatus = !member.isAdmin();
        memberRef.setValue(newStatus)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        member.setAdmin(newStatus);
                        notifyDataSetChanged();
                        String message = newStatus ? 
                                member.getUsername() + " is now an admin" :
                                member.getUsername() + " is no longer an admin";
                        CustomNotification.showNotification(context, message, true);
                    } else {
                        CustomNotification.showNotification(context, "Failed to update admin status", false);
                    }
                });
    }
    
    private void removeMember(User member) {
        // Check if this member is the group creator - prevent removal of the creator by other admins
        if (member.getUserId().equals(groupCreatorId) && !currentUserId.equals(groupCreatorId)) {
            CustomNotification.showNotification(context, "Cannot remove the group owner", false);
            return;
        }
        
        DatabaseReference memberRef = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId).child("members").child(member.getUserId());
        
        memberRef.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        members.remove(member);
                        notifyDataSetChanged();
                        CustomNotification.showNotification(context, 
                                member.getUsername() + " removed from group", true);
                    } else {
                        CustomNotification.showNotification(context, "Failed to remove member", false);
                    }
                });
    }
    
    static class MemberViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageViewProfilePic;
        TextView textViewUsername, textViewStatus;
        ImageButton buttonMore;
        
        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProfilePic = itemView.findViewById(R.id.imageViewProfilePic);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            buttonMore = itemView.findViewById(R.id.buttonMore);
        }
    }
}
