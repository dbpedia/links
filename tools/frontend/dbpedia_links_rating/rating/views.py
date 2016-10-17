# -*- coding: utf-8 -*-
from __future__ import absolute_import, unicode_literals
from django.views.generic import TemplateView, UpdateView, DetailView
from django.contrib.auth.mixins import LoginRequiredMixin
from django.db import transaction

from django.contrib import messages
from django.shortcuts import redirect

from .models import Link, Rating
from .forms import CreateRatingForm

class RatingView(LoginRequiredMixin, TemplateView):
    template_name = 'rating/rating_index.html'
    model = Link

    def post(self, request, *args):
        return self.create(request)

    def get(self, request, *args, **kwargs):
        context = self.get_context_data(request=request)
        return super(TemplateView, self).render_to_response(context)

    def get_context_data(self, **kwargs):
        #context = super(RatingView, self).get_context.data(**kwargs)
        context = {}
        if 'request' in kwargs:
            context['link'] = Link.randomNotRated(kwargs['request'])
        context['form'] = CreateRatingForm()
        return context

    def create(self, request):
        link_id = int(request.POST.get('link_id'))
        return_url = request.POST.get('return_url')
        try:
            with transaction.atomic():
                # Current user HAS rated this object
                # Updates his rating and total score
                rating = Rating.objects.get(user=request.user, link_id=link_id )
                rating.rating = request.POST.get('rating')
                rating.save()

                #ratings = Rating.objects.filter(link_id = link_id).all()
                link = Link.objects.get(id=link_id)
                link.score += int(request.POST['rating'])
                link.save()
                messages.success(request, 'Score updated succesfully')
        except Rating.DoesNotExist:
            # Current user has NOT rated this object
            # Saves first new rating

            with transaction.atomic():
                Rating.objects.create(rating=request.POST['rating'], link_id=link_id, user=request.user)
                link = Link.objects.get(id=link_id)
                link.score += int(request.POST['rating'])
                link.save()
                messages.success(request, 'Score updated succesfully')

        return redirect(return_url)
