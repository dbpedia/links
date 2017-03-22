# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import, print_function
from django.db import models
from django.utils.encoding import python_2_unicode_compatible
from dbpedia_links_rating.users.models import User

from django.contrib.contenttypes.fields import GenericForeignKey
from django.contrib.contenttypes.models import ContentType

from random import randint
from django.db.models.aggregates import Count


@python_2_unicode_compatible
class Link(models.Model):
    subject = models.CharField(max_length=200)
    object = models.CharField(max_length=200)
    predicate = models.CharField(max_length=200)
    score = models.IntegerField(default=0)

    def __str__(self):
        return self.subject + " -- " + self.predicate + " -- " + self.object + " -- " + str(self.score)

    @staticmethod
    def randomComplete():
        count = Link.objects.count()
        #count = Link.aggregate(count=Count('id'))['count']
        random_index = randint(0, count - 1)
        return Link.objects.all()[random_index]

    @staticmethod
    def randomNotRated(request):
        count = Link.objects.exclude(rating__user=request.user).count()
        if count > 0:
            random_index = randint(0, count - 1)
            return Link.objects.exclude(rating__user=request.user)[random_index]
        else:
            return None



@python_2_unicode_compatible
class Rating(models.Model):
    link = models.ForeignKey(Link, on_delete=models.CASCADE)
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    RATING = (
        (-1, 'No'),
        (0, 'Unsure'),
        (1, 'Yes'),
    )
    rating = models.IntegerField(default=0, choices=RATING)

    def __int__(self):
        return self.rating


@python_2_unicode_compatible
class File(models.Model):
    file = models.FileField(upload_to='links/%Y/%m/%d')
