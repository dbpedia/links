from django.apps import AppConfig


class RatingConfig(AppConfig):
    name = 'dbpedia_links_rating.rating'
    verbose_name = "Rating"

    def ready(self):
        """Override this to put in:
            Users system checks
            Users signal registration
        """
        pass
