private void translateMessage(String targetLanguage, String messageTextOG, String messageId) {
    // Create a new AsyncTask for the translation request
    new AsyncTask<Void, Void, Boolean>() {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // Prepare the request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("text", messageTextOG);
                requestBody.put("source_language", Variables.userLanguage);
                requestBody.put("target_language", targetLanguage);
                requestBody.put("mode", "single");
                requestBody.put("model", recipientTranslator.toLowerCase());
                requestBody.put("room_id", roomId);
                requestBody.put("message_id", messageId);

                // Make the API request
                URL url = new URL("https://musical-secondly-goat.ngrok-free.app/api/translate-db/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Send request body
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                return conn.getResponseCode() == HttpURLConnection.HTTP_OK;

            } catch (Exception e) {
                Log.e("ConversationModeActivity", "Translation error: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                // If translation fails, set message to original text
                messagesRef.child(roomId).child(messageId)
                    .child("message").setValue(messageTextOG.replace("\"", ""));
                Log.e("ConversationModeActivity", "Failed to translate message");
            }
        }
    }.execute();
} 