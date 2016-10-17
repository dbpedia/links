from django.contrib import admin

from .models import Link, Rating, File

admin.site.register(Link)
admin.site.register(Rating)
admin.site.register(File)
