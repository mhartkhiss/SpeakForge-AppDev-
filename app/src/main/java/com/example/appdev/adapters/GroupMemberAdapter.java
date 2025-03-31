package com.example.appdev.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder> implements Filterable {

    private Context context;
    private List<User> members;
    private List<User> membersOriginal; // For filtering
    private String groupId;
    private String currentUserId;
    private boolean isCurrentUserAdmin = false;
    private String groupCreatorId; // Store the creator ID

    public GroupMemberAdapter(Context context, List<User> members, String groupId) {
        this.context = context;
        this.members = members;
        this.membersOriginal = new ArrayList<>(members);
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
                
                // Sort members after getting admin status
                sortMembersList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    // Method to sort members list based on specific criteria
    private void sortMembersList() {
        if (members == null || members.isEmpty()) return;
        
        // Create a copy for sorting
        List<User> sortedList = new ArrayList<>(members);
        
        // Sort the list according to priority
        Collections.sort(sortedList, new Comparator<User>() {
            @Override
            public int compare(User user1, User user2) {
                // Current user always comes first
                if (user1.getUserId().equals(currentUserId)) return -1;
                if (user2.getUserId().equals(currentUserId)) return 1;
                
                // Group creator (owner) comes next
                boolean isUser1Creator = user1.getUserId().equals(groupCreatorId);
                boolean isUser2Creator = user2.getUserId().equals(groupCreatorId);
                if (isUser1Creator && !isUser2Creator) return -1;
                if (!isUser1Creator && isUser2Creator) return 1;
                
                // Admin priority over regular members
                if (user1.isAdmin() && !user2.isAdmin()) return -1;
                if (!user1.isAdmin() && user2.isAdmin()) return 1;
                
                // Alphabetical sorting for same category (admin or member)
                return user1.getUsername().compareToIgnoreCase(user2.getUsername());
            }
        });
        
        // Update the list and notify changes
        members.clear();
        members.addAll(sortedList);
        membersOriginal = new ArrayList<>(sortedList);
        notifyDataSetChanged();
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
        
        // Show appropriate status (Owner or Admin)
        if (member.isAdmin()) {
            // Check if this member is the group creator (owner)
            if (member.getUserId() != null && member.getUserId().equals(groupCreatorId)) {
                holder.textViewStatus.setText("Owner");
            } else {
                holder.textViewStatus.setText("Admin");
            }
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
        
        // Check if current user is admin to show/hide more options
        if (isCurrentUserAdmin) {
            // Don't show options for yourself
            if (member.getUserId().equals(currentUserId)) {
                holder.buttonMore.setVisibility(View.GONE);
            } else {
                holder.buttonMore.setVisibility(View.VISIBLE);
                
                holder.buttonMore.setOnClickListener(v -> {
                    PopupMenu popupMenu = new PopupMenu(context, holder.buttonMore);
                    popupMenu.inflate(R.menu.group_member_options);
                    
                    // Get menu items
                    android.view.Menu menu = popupMenu.getMenu();
                    
                    // Check if member is already admin
                    boolean isAdmin = member.isAdmin();
                    menu.findItem(R.id.action_make_admin).setVisible(!isAdmin);
                    menu.findItem(R.id.action_remove_admin).setVisible(isAdmin);
                    
                    // Don't allow removing group creator
                    if (member.getUserId().equals(groupCreatorId)) {
                        menu.findItem(R.id.action_remove_member).setVisible(false);
                        menu.findItem(R.id.action_remove_admin).setVisible(false);
                    }
                    
                    popupMenu.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.action_make_admin) {
                            makeAdmin(member, true);
                            return true;
                        } else if (itemId == R.id.action_remove_admin) {
                            makeAdmin(member, false);
                            return true;
                        } else if (itemId == R.id.action_remove_member) {
                            removeMember(member);
                            return true;
                        } else {
                            return false;
                        }
                    });
                    
                    popupMenu.show();
                });
            }
        } else {
            holder.buttonMore.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return members != null ? members.size() : 0;
    }
    
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<User> filteredList = new ArrayList<>();
                
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(membersOriginal);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    
                    for (User user : membersOriginal) {
                        if (user.getUsername().toLowerCase().contains(filterPattern)) {
                            filteredList.add(user);
                        }
                    }
                }
                
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }
            
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                members.clear();
                members.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }
    
    public void updateData(List<User> newMembers) {
        this.members.clear();
        this.members.addAll(newMembers);
        this.membersOriginal = new ArrayList<>(newMembers);
        sortMembersList();
    }
    
    // Add this method to filter members based on search query
    public void filterMembers(String query) {
        getFilter().filter(query);
    }
    
    // Add this method to update the original list for filtering
    public void updateOriginalList() {
        this.membersOriginal = new ArrayList<>(members);
    }
    
    private void makeAdmin(User member, boolean isAdmin) {
        DatabaseReference memberRef = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId).child("members").child(member.getUserId());
        
        memberRef.setValue(isAdmin)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update local user object
                        member.setAdmin(isAdmin);
                        notifyDataSetChanged();
                        
                        if (isAdmin) {
                            CustomNotification.showNotification(context, 
                                    member.getUsername() + " is now an admin", true);
                        } else {
                            CustomNotification.showNotification(context, 
                                    member.getUsername() + " is no longer an admin", true);
                        }
                    } else {
                        CustomNotification.showNotification(context, "Failed to update admin status", false);
                    }
                });
    }
    
    private void removeMember(User member) {
        DatabaseReference memberRef = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId).child("members").child(member.getUserId());
        
        memberRef.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        members.remove(member);
                        membersOriginal.remove(member);
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
        ImageView buttonMore;
        
        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProfilePic = itemView.findViewById(R.id.imageViewProfilePic);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            buttonMore = itemView.findViewById(R.id.buttonMore);
        }
    }
}
