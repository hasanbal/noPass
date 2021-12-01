from django.db import models

import uuid
import pyotp

gen_uid = lambda: ''.join([uuid.uuid4().hex for _ in range(4)])
gen_timed_pass = lambda: '-'.join([uuid.uuid4().hex[:8] for _ in range(7)])

class STCAClient(models.Model):
    uid = models.CharField(max_length=128) # Random generated user ID
    bio_id = models.CharField(max_length=3000) # Biometric user identification sequence
    time_seed = models.CharField(max_length=1024) # Time seed set by user

    @property
    def timed_secret(self):
        return pyotp.TOTP(str(self.time_seed)).now()

class STCAServerClientPair(models.Model):
    first_id = models.CharField(max_length=300)
    server_domain = models.CharField(max_length=300)

    client = models.ForeignKey(
        STCAClient,
        on_delete=models.CASCADE,
        related_name='auth_pairs'
    )

class STCATimedAuthenticationPerm(models.Model):
    login_uri = models.CharField(max_length=200) # Eg. https://facebook.com/login.php?foo.bar=2324
    pair_key = models.CharField(max_length=64, unique=True) # Random generated single time pair key
    is_permitted = models.BooleanField(default=False)

    client = models.ForeignKey(
        STCAClient,
        on_delete=models.CASCADE,
        related_name='auth_pairs',
        blank=True,
        null=True,
        default=None
    )