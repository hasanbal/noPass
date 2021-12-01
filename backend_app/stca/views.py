from django.core.exceptions import ObjectDoesNotExist

from rest_framework.views import APIView, Response

from .models import STCAClient, STCATimedAuthenticationPerm, STCAServerClientPair, gen_uid, gen_timed_pass

from tldextract import extract

REGISTER_CLI_REQ_FIELDS = [
    'bio_id',
    'seed',
]

GEN_AUTHPERM_REQ_FIELDS = [
    'login_uri',
    'pair_key',
]

PERM_AUTHPERM_REQ_FIELDS = [
    'bio_id',
    'tpass',
    'login_uri',
    'pair_key',
]

GET_TIMED_PASS_REQ_FIELDS = [
    'login_uri',
    'pair_key',
]

class RegisterClientAPIView(APIView):
    def post(self, request):
        for field in REGISTER_CLI_REQ_FIELDS:
            if field not in request.data:
                return Response({
                    "code": "missingfield",
                    "field": field
                }, 400)

        try:
            STCAClient.objects.get(bio_id=request.data['bio_id'])
        except ObjectDoesNotExist:
            pass
        else:
            return Response({
                    "code": "uexists"
                }, 400)

        new_client = STCAClient.objects.create(
            uid=gen_uid(),
            bio_id=request.data['bio_id'],
            time_seed=request.data['seed']
        )

        return Response({
            "_id": new_client.id,
            "uid": new_client.uid
        })
    
class GenerateTimedAuthPermHold(APIView):
    def post(self, request):
        for field in GEN_AUTHPERM_REQ_FIELDS:
            if field not in request.data:
                return Response({
                    "code": "missingfield",
                    "field": field
                }, 400)

        try:
            STCATimedAuthenticationPerm.objects.get(
                login_uri=request.data['login_uri'],
                pair_key=request.data['pair_key']
            )
        except ObjectDoesNotExist:
            perm = STCATimedAuthenticationPerm.objects.create(
                login_uri=request.data['login_uri'],
                pair_key=request.data['pair_key']
            )
        else:
            return Response({
                "code": "permexists"
            }, 400)

        return Response({
            "perm_id": perm.id,
            "is_permitted": perm.is_permitted
        }, 201)

class PermitTimedAuthPerm(APIView):
    def post(self, request):
        for field in GEN_AUTHPERM_REQ_FIELDS:
            if field not in request.data:
                return Response({
                    "code": "missingfield",
                    "field": field
                }, 400)

        try:
            client = STCAClient.objects.get(bio_id=request.data['bio_id'])
        except ObjectDoesNotExist:
            return Response({
                    "code": "userdne"
                }, 401)

        if client.timed_secret != request.data['tpass']:
            return Response({
                    "code": "passwddoesnotmatch"
                }, 401)

        try:
            auth_perm = STCATimedAuthenticationPerm.objects.get(
                login_uri=request.data['login_uri'],
                pair_key=request.data['pair_key']
            )
        except ObjectDoesNotExist:
            return Response({
                    "code": "permdne"
                }, 404)
        
        auth_perm.is_permitted = True
        auth_perm.client = client
        auth_perm.save()

        return Response({}, 200)

class GetTimedAuthPass(APIView):
    def post(self, request):
        for field in GET_TIMED_PASS_REQ_FIELDS:
            if field not in request.data:
                return Response({
                    "code": "missingfield",
                    "field": field
                }, 400)

        try:
            auth_perm = STCATimedAuthenticationPerm.objects.get(
                login_uri=request.data['login_uri'],
                pair_key=request.data['pair_key']
            )
        except ObjectDoesNotExist:
            return Response({
                    "code": "permdne"
                }, 404)

        if not auth_perm.is_permitted:
            return Response({
                "code": "notpermittedyet"
            }, 401)

        _, d, s = extract(auth_perm.login_uri)
        server_domain = d + "." + s

        try:
            auth_pair = STCAServerClientPair.objects.get(server_domain=server_domain, client=auth_perm.client)
        except ObjectDoesNotExist:
            return Response({
                    "code": "pairdne"
                }, 404)

        auth_perm.delete()

        return Response({
            "timed_pass": gen_timed_pass(),
            "first_id": auth_pair.first_id
        }, 200)