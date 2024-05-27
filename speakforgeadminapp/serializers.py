from rest_framework import serializers

class TranslatorSerializer(serializers.Serializer):
    text = serializers.CharField()
    target_language = serializers.CharField()