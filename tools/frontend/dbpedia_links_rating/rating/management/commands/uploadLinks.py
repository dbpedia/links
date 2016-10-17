from __future__ import unicode_literals

from django.core.management import BaseCommand

from dbpedia_links_rating.rating.models import Link

class Command(BaseCommand):

    help = "Read out a .nt file and create links"

    def add_arguments(self, parser):
        parser.add_argument('file', type=str)

    missing_args_message = "Please insert the filepath."

    def handle(self, *args, **options):
        f = open(options['file'], 'r', encoding="utf8")
        for line in f:
            link =Link.objects.create(subject=line.split('>')[0].split('<')[1], predicate=line.split('>')[1].split('<')[1], object=line.split('>')[2].split('<')[1])
            link.save()
        f.close()
