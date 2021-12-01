from django.contrib import admin

from .models import STCAClient, STCATimedAuthenticationPerm

admin.site.register(STCAClient)
admin.site.register(STCATimedAuthenticationPerm)