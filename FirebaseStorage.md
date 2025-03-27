# Firebase Storage Structure - SpeakForge App

This document describes the structure of Firebase Storage used in the SpeakForge application for storing images and other media.

## Storage Root Structure

```
speakforge-storage/
│
└── profile_pictures/
    └── {userId}  # User's profile image stored with their UID as the filename
```

## Storage Details

### Profile Pictures

Profile pictures are stored using the following approach:

- **Storage Path**: `profile_pictures/{userId}`
- **File Naming**: Each profile image is stored under the user's Firebase Authentication UID
- **File Types**: The app accepts any image type (image/*) for profile pictures
- **Access Control**: Images are accessible via publicly accessible URLs generated after upload

## Upload Process

The profile picture upload process works as follows:

1. User selects an image from their device gallery
2. The app uploads the image to Firebase Storage at path `profile_pictures/{userId}`
3. After successful upload, the app retrieves the download URL
4. The download URL is then stored in the Firebase Realtime Database under the user's profile:
   ```
   users/{userId}/profileImageUrl: "https://firebasestorage.googleapis.com/..."
   ```

## Media Loading

The app uses the Glide library to load and display profile images:

1. Profile images are loaded from the URLs stored in the user profile
2. Default placeholder images are used while images are loading
3. If a user doesn't have a profile image or if the URL is invalid, a default image (`default_userpic`) is displayed

## Code Implementation

The main class responsible for profile picture management is `ChangeProfilePicControl.java`, which:

1. Handles image selection from device gallery
2. Uploads the selected image to Firebase Storage
3. Retrieves the download URL
4. Updates the user's profile in the Realtime Database with the new image URL

## Relationship with Realtime Database

The Firebase Storage system is tightly integrated with the Realtime Database:

- Image URLs are stored in the user profile data structure
- The `profileImageUrl` field in the User model points to the Firebase Storage location
- This approach allows for efficient loading and caching of images while keeping database queries lightweight

## Security Considerations

- Firebase Storage security rules should be configured to allow only authenticated users to upload their own profile pictures
- Download URLs are public, allowing images to be loaded in the app UI without authentication tokens
- The profile picture upload functionality is restricted to logged-in users (non-guest accounts) 