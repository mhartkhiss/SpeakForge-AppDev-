# Firebase Realtime Database Structure - SpeakForge App

This document describes the structure of the Firebase Realtime Database used in the SpeakForge application, a language translation app.

## Database Root Structure

```
speakforge-appdev/
│
├── users/
│   └── {userId}/
│       ├── userId: String
│       ├── username: String
│       ├── email: String
│       ├── profileImageUrl: String
│       ├── language: String
│       ├── accountType: String
│       ├── createdAt: String (timestamp)
│       ├── lastLoginDate: String (timestamp)
│       ├── translator: String (default: "google")
│       ├── lastMessage: String
│       ├── lastMessageTime: Long
│       └── contactsettings/
│           └── {contactUserId}/
│               └── translateMessages: Boolean
│
├── messages/
│   └── {roomId}/  (composed of user1id_user2id)
│       └── {messageId}/
│           ├── messageId: String
│           ├── message: String (original message)
│           ├── timestamp: Long
│           ├── senderId: String
│           ├── senderLanguage: String (language of the sender)
│           ├── translationMode: String (formal or casual)
│           ├── translationState: String (TRANSLATING, TRANSLATED, REMOVED, or null)
│           └── translations/
│               ├── translation1: String (first translation of the message)
│               ├── translation2: String (second translation of the message)
│               └── translation3: String (third translation of the message)
│
├── groups/
│   └── {groupId}/
│       ├── groupId: String
│       ├── name: String
│       ├── description: String
│       ├── createdAt: Long
│       ├── createdBy: String (userId of creator)
│       ├── groupImageUrl: String
│       ├── lastMessage: String
│       ├── lastMessageTime: Long
│       ├── lastMessageSenderId: String
│       └── members/
│           └── {userId}: Boolean (true if admin, false if regular member)
│
├── group_messages/
│   └── {groupId}/
│       └── {messageId}/
│           ├── messageId: String
│           ├── message: String (original message)
│           ├── timestamp: Long
│           ├── senderId: String
│           ├── senderLanguage: String (language of the sender)
│           ├── translationMode: String (formal or casual)
│           └── translations/
│               └── {language}: String (message translated to specific language, including original in sender's language)
│
```

## Details of Key Nodes

### Users

The `users` node stores information about registered users:

- **userId**: Unique identifier for the user (Firebase Auth UID)
- **username**: Display name of the user
- **email**: User's email address
- **profileImageUrl**: URL to the user's profile image (stored in Firebase Storage)
- **language**: User's preferred language
- **accountType**: Type of user account (e.g., free, premium)
- **createdAt**: Timestamp of account creation
- **lastLoginDate**: Timestamp of the last login
- **translator**: Preferred translation service (default: "google")
- **lastMessage**: Last message sent by the user
- **lastMessageTime**: Timestamp of the last message
- **contactsettings**: Contains per-contact preferences and settings
  - **{contactUserId}**: User ID of the contact for which settings are defined
    - **translateMessages**: Boolean flag indicating whether to translate messages for this contact

### Messages

The `messages` node organizes messages by conversation room:

- **roomId**: Unique identifier for a conversation room (composed of user1id_user2id)
- **messageId**: Unique identifier for each message in the room
  - **messageId**: String identifier of the message
  - **message**: Original message content
  - **timestamp**: When the message was sent
  - **senderId**: User ID of the message sender
  - **senderLanguage**: The language of the sender
  - **translationMode**: Formal or casual translation style
  - **translationState**: Current state of message translation
    - **TRANSLATING**: Message is currently being translated
    - **TRANSLATED**: Message has been successfully translated
    - **REMOVED**: Translations have been removed
    - **null**: No translation or translation failed
  - **translations**: Map of translations for the message
    - **translation1**: First translation variant of the message
    - **translation2**: Second translation variant of the message
    - **translation3**: Third translation variant of the message

### Groups

The `groups` node stores information about group conversations:

- **groupId**: Unique identifier for the group
- **name**: Display name of the group
- **description**: Description of the group
- **createdAt**: Timestamp of group creation
- **createdBy**: User ID of the group creator
- **groupImageUrl**: URL to the group's image
- **lastMessage**: Last message sent in the group
- **lastMessageTime**: Timestamp of the last message
- **lastMessageSenderId**: User ID of the last message sender
- **members**: Map of user IDs to boolean values
  - **{userId}**: Boolean value indicating admin status (true = admin, false = regular member)

### Group Messages

The `group_messages` node organizes messages by group conversation:

- **groupId**: Unique identifier for a group
- **messageId**: Unique identifier for each message in the group
  - **messageId**: String identifier of the message
  - **message**: Original message content
  - **timestamp**: When the message was sent
  - **senderId**: User ID of the message sender
  - **senderLanguage**: The language of the sender
  - **translationMode**: Formal or casual translation style
  - **translations**: Map of language codes to translated message strings
    - **{language}**: Message translated to specific language, including original in sender's language

## Local Application Data

The following data structures are maintained locally in the application and not stored in Firebase:

### Translation History

Translation history is stored locally on the device:

- **originalText**: The text before translation
- **translatedText**: The text after translation
- **sourceLanguage**: The language of the original text
- **targetLanguage**: The language the text was translated to
- **translator**: The translation service used (e.g., "google")
- **timestamp**: When the translation was performed

### Languages

The supported languages data is stored locally on the device:

- **languageCode**: The code identifier for the language (e.g., "en", "es")
- **name**: The English name of the language
- **nativeName**: The name of the language in its native form

## Authentication

The application uses Firebase Authentication with email/password sign-in method. User accounts are created and managed through Firebase Auth, with additional user information stored in the Realtime Database.

## Storage References

Profile images and other media content are stored in Firebase Storage, with references to these files stored in the Realtime Database. 