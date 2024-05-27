import firebase_admin
from firebase_admin import credentials

cred = credentials.Certificate("speakforgeadminproject/serviceAccountKey.json")
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://appdev-86a96-default-rtdb.asia-southeast1.firebasedatabase.app'  # Replace with your actual database URL
})