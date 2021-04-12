from google.cloud import storage
from google.cloud import asset_v1
from google.cloud import pubsub_v1
import grpc
from grpc import RpcError
import google.rpc as rpc
from grpc_status import rpc_status
from google.rpc import status_pb2, error_details_pb2, code_pb2
from google.cloud.exceptions import GoogleCloudError
from google.api_core.exceptions import Forbidden, InternalServerError, ServiceUnavailable
import google.api_core.exceptions
import googleapiclient.errors
import argparse
from pprint import pprint
from googleapiclient import discovery
from oauth2client.client import GoogleCredentials

import sys
import getopt

from gcp_error_handler.gcp_errors import GCPError

storage_client = storage.Client()


def gcs(bucket_name, object_name):

    bucket = storage_client.bucket(bucket_name)
    try:
        blob = bucket.get_blob(object_name)
        print(blob)
    except google.cloud.exceptions.GoogleCloudError as err:

        # Using Default Error
        print(err)
        print(err.code)
        print(err.message)
        print(err.response)
        print(err.grpc_status_code)
        print("Details: ")
        for e in err.errors:
            if err.grpc_status_code == None:
                for k, v in e.items():
                    print(k, v)

        print("========================================")
        # Using AutoParser
        ee = GCPError(err)
        print(ee)
        print(ee.code)
        print(ee.message)
        print(ee.response)
        print(ee.grpc_status_code)
        print("Details: ")
        for e in ee.errors:
            if ee.grpc_status_code == None:
                for k, v in e.items():
                    print(k, v)
    except Exception as err:
        print(err)


def pubsub(projectID):

    publisher = pubsub_v1.PublisherClient()
    try:
        
      project_path = f"projects/{projectID}"

      for topic in publisher.list_topics(request={"project": project_path}):
        print(topic)
    except google.cloud.exceptions.GoogleCloudError as err:

        # Using Default Error
        print(err)
        print(err.code)
        print(err.message)
        print(err.response)
        print(err.grpc_status_code)
        print("Details: ")
        for e in err.errors:
            if err.grpc_status_code == None:
                for k, v in e.items():
                    print(k, v)

        print("========================================")
        # Using AutoParser
        ee = GCPError(err)
        print(ee)
        print(ee.code)
        print(ee.message)
        print(ee.response)
        print(ee.grpc_status_code)
        print("Details: ")
        for e in ee.errors:
            if ee.grpc_status_code == None:
                for k, v in e.items():
                    print(k, v)
    except Exception as err:
        print(err)


def compute(project, zone):
    try:
        credentials = GoogleCredentials.get_application_default()
        service = discovery.build('compute', 'v1', credentials=credentials)
        request = service.instances().list(project=project, zone=zone)
        while request is not None:
            response = request.execute()
            for instance in response['items']:
                pprint(instance)
            request = service.instances().list_next(
                previous_request=request, previous_response=response)
    except googleapiclient.errors.HttpError as err:

        # Using Default Error
        print(err.content.decode('utf-8'))
        print(err.resp)
        print(err.uri)
        print(err.error_details)

        print("========================================")

        # Using AutoParser
        ee = GCPError(err)
        print(ee.content.decode('utf-8'))
        print(ee.resp)
        print(ee.uri)
        print(ee.error_details)
        # print(ee)
    except Exception as err:
        print(err)


def asset(scope, checkResource, identity):

    client = asset_v1.AssetServiceClient()

    try:
        response = client.analyze_iam_policy(
            request={
                "analysis_query": {
                    "scope": scope,
                    "resource_selector": {
                        "full_resource_name": checkResource
                    },
                    "identity_selector": {
                        "identity": identity,
                    }
                }
            }
        )

        print(response)

    except google.cloud.exceptions.GoogleCloudError as err:
        # Using Default Error
        print(err)
        print(err.code)
        print(err.message)
        print(err.response)
        print(err.grpc_status_code)
        print("Details: ")
        for e in err.errors:
            if (err.grpc_status_code != None):
                meta = e.trailing_metadata()
                for m in meta:
                    if (m.key == 'google.rpc.help-bin'):
                        info = error_details_pb2.Help()
                        info.ParseFromString(m.value)
                        for l in info.links:
                            print('     Help Url: ', l.url)
                            print('     Help Description: ', l.description)

        print("========================================")

        # Using AutoParser
        ee = GCPError(err,prettyprint=False)
        print(ee)  # export GOOGLE_ENABLE_ERROR_DETAIL=true
        print(ee.code)
        print(ee.message)
        print(ee.response)
        print(ee.grpc_status_code)
        print("Details: ")
        for e in ee.errors:
            if (ee.is_google_cloud_error):
                info = ee.get_google_rpc_help
                if info != None:
                    for l in info.links:
                        print('     Help Url: ', l.url)
                        print('     Help Description: ', l.description)

                info = ee.get_google_rpc_badrequest
                if info != None:
                    for l in info.field_violations:
                        print('     BadRequest Field: ', l.field)
                        print('     BadRequest Description: ', l.description)


def usage():
    print('\nUsage: main.py  '
          '--mode=basic|extended  '
          '--gcsBucket=  '
          '--gcsObject=  '
          '--checkResource='
          '--scope='
          '--projectID='
          '--identity=\n')


if __name__ == '__main__':
    try:
        opts, args = getopt.getopt(sys.argv[1:], None,
                                   ["api=", "gcsBucket=","projectID=","gcsObject=", "scope=", "checkResource=", "identity=", "zone="])
    except getopt.GetoptError:
        usage()
        sys.exit(1)

    api = ""
    gcsBucket = ""
    gcsObject = ""
    scope = ""
    checkResource = ""
    identity = ""
    projectID = ""
    zone = ""    

    for opt, arg in opts:
        if opt == "--api":
            api = arg
        if opt == "--gcsBucket":
            gcsBucket = arg
        if opt == "--gcsObject":
            gcsObject = arg
        if opt == "--scope":
            scope = arg
        if opt == "--checkResource":
            checkResource = arg
        if opt == "--identity":
            identity = arg
        if opt == "--projectID":
            projectID = arg
        if opt == "--zone":
            zone = arg            

    if not api:
        print('api must be specified (gcs|compute|pubsub|asset).')
        usage()
        sys.exit(1)

    if api == "compute":
        if projectID == "":
            print("Both projectID cannot be null if  --api=compute")
            usage()
            sys.exit(1)
        compute(projectID,zone)

    if api == "gcs":
        if gcsBucket == "" or gcsObject == "":
            print("Both --gcsObject= and --gcsBucket= must be specified with --api=gcs")
            usage()
            sys.exit(1)
        gcs(gcsBucket, gcsObject)

    if api == "pubsub":
        if projectID == "":
            print("Both projectID cannot be null if  --api=pubsub")
            usage()
            sys.exit(1)
        pubsub(projectID)        

    if api == "asset":
        if scope == "" or checkResource == "" or identity == "":
            print(
                "--scope= and --checkResource= and --identity= must be specified with --api=asset")
            usage()
            sys.exit(1)
        asset(scope, checkResource, identity)
