# -*- coding: utf-8 -*-
from __future__ import absolute_import, unicode_literals

from django.conf.urls import url

from . import views

app_name = 'rating'

urlpatterns = [

    url(
        regex = r'^$',
        view = views.RatingView.as_view(),
        name = 'rating'),
]
