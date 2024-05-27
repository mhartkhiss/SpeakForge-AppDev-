from django.urls import path
from . import views

urlpatterns = [
    path('', views.login_page, name='login'),
    path('dashboard/', views.user_list, name='user_list'),
    path('delete-user/', views.delete_user, name='delete_user'),
    path('reset-password/', views.reset_password, name='reset_password'),
    path('set-user-account-type/', views.set_user_account_type, name='set_user_account_type'),
    path('create-user/', views.create_new_user, name='create_new_user'),
    path('login/', views.login_page, name='login'),
    path('login-request/', views.login_view, name='login-request'),
    path('logout/', views.logout_view, name='logout'),
    path('translate/', views.translate_text, name='translate'),

    
]
