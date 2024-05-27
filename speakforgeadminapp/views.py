from django.http import HttpResponse
from django.shortcuts import render
from firebase_admin import db
from django.http import JsonResponse
from firebase_admin import auth
from django.core.mail import send_mail
from django.conf import settings
from django.contrib.auth import authenticate, login
from django.contrib.auth import logout as auth_logout
from django.shortcuts import redirect
from django.contrib.auth.decorators import login_required
from googletrans import Translator
from django.views.decorators.csrf import csrf_exempt
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from .serializers import TranslatorSerializer


def index(request):
    return HttpResponse("Hello, world. You're at the speakforge index.")

@login_required
def user_list(request):

    ref = db.reference('users')
    users = ref.get()
    
    user_list = [{'id': key, **value} for key, value in users.items()]
    
    return render(request, 'speakforgeadminapp/user_list.html', {'users': user_list})

@login_required
def delete_user(request):
    if request.method == 'POST':
        user_id = request.POST.get('user_id')
        try:
            # Delete user from Firebase Authentication
            auth.delete_user(user_id)

            # Reference to the Firebase Realtime Database
            ref = db.reference('users') 
            ref.child(user_id).delete()

            # Delete related conversation IDs from the 'messages' node
            messages_ref = db.reference('messages')
            messages = messages_ref.get()

            if messages:
                for convo_id, convo_data in messages.items():
                    if user_id in convo_id.split('_'):
                        messages_ref.child(convo_id).delete()

            return JsonResponse({'message': 'User and related conversations deleted successfully'})
        except Exception as e:
            return JsonResponse({'error': f'Error deleting user: {str(e)}'}, status=500)
    else:
        return JsonResponse({'error': 'Invalid request method'}, status=400)
    

@login_required
def reset_password(request):
    if request.method == 'POST':
        email = request.POST.get('email')
        try:
            # Generate password reset link
            link = auth.generate_password_reset_link(email)

            # Send password reset email
            send_mail(
                'Password Reset Request for SpeakForge',
                f'Here is your password reset link: {link}',
                settings.EMAIL_HOST_USER,
                [email],
                fail_silently=False,
            )

            return JsonResponse({'message': 'Password reset email sent successfully.'})
        except auth.AuthError as e:
            return JsonResponse({'error': f'Error sending password reset email: {str(e)}'}, status=500)
        except Exception as e:
            return JsonResponse({'error': f'Error: {str(e)}'}, status=500)
    else:
        return JsonResponse({'error': 'Invalid request method'}, status=400)

@login_required
def set_user_account_type(request):
    if request.method == 'POST':
        user_id = request.POST.get('user_id')
        new_account_type = request.POST.get('account_type')
        try:
            # Update the user account type in Firebase Realtime Database
            ref = db.reference(f'users/{user_id}')
            ref.update({'accountType': new_account_type})

            return JsonResponse({'message': 'User account type updated successfully'})
        except Exception as e:
            return JsonResponse({'error': f'Error updating user account type: {str(e)}'}, status=500)
    else:
        return JsonResponse({'error': 'Invalid request method'}, status=400)
    

from firebase_admin import auth
from datetime import datetime
@login_required
def create_new_user(request):
    if request.method == 'POST':
        email = request.POST.get('email')
        password = request.POST.get('password')
        try:
            # Create user in Firebase Authentication
            user = auth.create_user(
                email=email,
                password=password
            )
            #get the current time
            current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            # Set default values in Firebase Realtime Database
            ref = db.reference('users')
            ref.child(user.uid).set({
                'userId': user.uid,
                'username': email,  
                'email': email,
                'language': None,
                'lastLoginDate': current_time, 
                'createdAt': current_time,  
                'profileImageUrl': 'none',
                'accountType': 'free', 
                'translator': 'google'
            })

            return JsonResponse({'message': 'User created successfully'})
        except Exception as e:
            return JsonResponse({'error': f'Error creating user: {str(e)}'}, status=500)
    else:
        return JsonResponse({'error': 'Invalid request method'}, status=400)
    


def login_page(request):
    return render(request, 'speakforgeadminapp/login.html')


def login_view(request):
    if request.method == 'POST':
        username = request.POST.get('username')
        password = request.POST.get('password')

        user = authenticate(request, username=username, password=password)
        if user is not None:
            login(request, user)
            return JsonResponse({'message': 'Login successful'})
        else:
            return JsonResponse({'error': 'Invalid username or password'}, status=400)
    else:
        return JsonResponse({'error': 'Invalid request method'}, status=400)
    

def logout_view(request):
    auth_logout(request)
    return redirect('login')




@api_view(['POST'])
def translate_text(request):
    if request.method == 'POST':
        serializer = TranslatorSerializer(data=request.data)
        if serializer.is_valid():
            text = serializer.validated_data.get('text')
            target_language = serializer.validated_data.get('target_language')

            # Check if target_language is provided
            if not target_language:
                return Response({'error': 'Target language not provided'}, status=status.HTTP_400_BAD_REQUEST)

            # Map "Bisaya" to "cebuano" and "Tagalog" to "Filipino"
            if target_language.lower() == "bisaya":
                target_language = "cebuano"
            elif target_language.lower() == "tagalog":
                target_language = "filipino"

            # Translate text
            translator = Translator()
            try:
                translated_text = translator.translate(text, dest=target_language).text
                return Response({'translated_text': translated_text})
            except ValueError as e:
                return Response({'error': str(e)}, status=status.HTTP_400_BAD_REQUEST)
            except Exception as e:
                print(f'Error translating text: {str(e)}')
                return Response({'error': 'An error occurred during translation'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        else:
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    else:
        return Response({'error': 'Method not allowed'}, status=status.HTTP_405_METHOD_NOT_ALLOWED)

