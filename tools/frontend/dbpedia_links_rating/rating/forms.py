from django import forms

RANGES = [
    ('-1', 'No'),
    ('0', 'Unsure'),
    ('1', 'Yes'),
]


class CreateRatingForm(forms.Form):
    rating = forms.ChoiceField(widget=forms.RadioSelect, choices=RANGES)
