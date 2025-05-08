# SpeakForge - Android Mobile Application

## About SpeakForge

SpeakForge is an AI-Powered multilingual communication tool designed to eliminate language barriers by integrating real-time text and voice translation into chat-based conversations. The project addresses the growing need for seamless cross-lingual communication, particularly among international students, tourists, expatriates, and businesses operating in Cebu. By leveraging advanced AI-powered translation services, speech recognition technology, and intuitive user interfaces, SpeakForge facilitates effortless communication between Bisaya speaking communities and non-native speakers.

This repository contains the source code for the **SpeakForge Android Mobile Application**.

## Features

*   **Real-time Text Translation:** Instantly translate chat messages into multiple languages.
*   **Voice-to-Text & Translation:** Speak in your native language, and have it transcribed and translated for the other user.
*   **Text-to-Speech:** Hear translated messages in a synthesized voice.
*   **Multilingual Support:** Designed to support Bisaya and other languages.
*   **User-Friendly Interface:** Intuitive design for easy navigation and use.
*   **Secure Communication:** Ensuring user data privacy and security.

## Technology Stack

*   **Programming Language:** Java
*   **IDE:** Android Studio
*   **Translation & Speech Services:** AI-Powered APIs (Claude, DeepSeek, Gemini)
*   **Backend:** Firebase Backend Services (Firebase Authentication and Firebase Realtime-DB) and Django/Python server backend (see Other Repositories below)
*   **Database:** Firebase Realtime Database and Firebase Storage

## Other Project Repositories

*   **Mobile App (this repository):** [https://github.com/mhartkhiss/SpeakForge](https://github.com/mhartkhiss/SpeakForge)
*   **Web App (Admin Panel):** [https://github.com/Aazirr/speakforgeadmin.git](https://github.com/Aazirr/speakforgeadmin.git)
*   **Server Backend:** [https://github.com/mhartkhiss/SpeakForgeServer](https://github.com/mhartkhiss/SpeakForgeServer)

## Getting Started

Follow these instructions to get a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

*   [Android Studio](https://developer.android.com/studio) (latest stable version recommended)
*   Android SDK ( соответствующий API Level, usually downloaded via Android Studio's SDK Manager)
*   Git

### Installation & Running the App

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/mhartkhiss/SpeakForge.git
    cd SpeakForge
    ```
2.  **Open in Android Studio:**
    *   Launch Android Studio.
    *   Select "Open an existing Android Studio project" (or "File" > "Open...").
    *   Navigate to the cloned `SpeakForge` directory and select it.
3.  **Sync Project with Gradle Files:**
    *   Android Studio should automatically start syncing the project with Gradle. If not, click on "Sync Project with Gradle Files" (often a small elephant icon with a sync arrow in the toolbar or a notification prompt).
    *   This will download all the necessary dependencies defined in the `build.gradle` files.
5.  **Build the Project:**
    *   Go to "Build" > "Make Project" or "Build" > "Rebuild Project".
6.  **Run the App:**
    *   **Select a Run Configuration:** Ensure an Android App configuration is selected (usually `app` or `mobileapp-androidstudio-java.app`).
    *   **Choose a Device:**
        *   **Emulator:** If you have an Android Virtual Device (AVD) set up, select it from the device dropdown menu. You can create one via "Tools" > "AVD Manager".
        *   **Physical Device:** Connect your Android device to your computer via USB. Ensure USB Debugging is enabled in Developer Options on your device. Your device should appear in the dropdown.
    *   **Click the "Run" button** (green play icon).
    *   Android Studio will build the APK, install it on the selected device/emulator, and launch the app.

