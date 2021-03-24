from google.cloud import storage
from google.cloud import asset_v1
import grpc
from grpc import RpcError
import google.rpc as rpc
from grpc_status import rpc_status
from google.rpc import status_pb2, error_details_pb2, code_pb2
from google.cloud.exceptions import GoogleCloudError
from google.api_core.exceptions import Forbidden, InternalServerError, ServiceUnavailable
import google.api_core.exceptions

import argparse

import sys, getopt

storage_client = storage.Client()

def basic(bucket_name,object_name):

    bucket = storage_client.bucket(bucket_name)
    try:
        blob = bucket.get_blob(object_name)
        print(blob)
    except GoogleCloudError as err:
        print(err.code)
        print([err]) 
        print("Details: ")
        for e in err.errors:
            for k, v in e.items():
                print(k, v)
    except Exception as err:
        print(err)

def extended(scope,checkResource,identity):

    client = asset_v1.AssetServiceClient()

    try:
        response = client.analyze_iam_policy(
            request={
                "analysis_query":{
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
        

    # https://googleapis.dev/python/google-api-core/latest/exceptions.html
    # https://grpc.github.io/grpc/python/grpc.html
    # https://github.com/grpc/grpc/blob/master/examples/python/errors/client.py#L33
    except GoogleCloudError as err:    
        print(err) 
        print(err.code)
        print(err.message)
        print(err.grpc_status_code)
        print("........................")
        for e in err.errors:
          meta = e.trailing_metadata()
          for m in meta:
            if (m.key =='google.rpc.help-bin'):
              info = error_details_pb2.Help()
              info.ParseFromString(m.value)
              for l in info.links:
                print('     Help Url: ', l.url)
                print('     Help Description: ', l.description)

def usage():
    print ('\nUsage: main.py  '
           '--mode=basic|extended  '
           '--gcsBucket=  '
           '--gcsObject=  '
           '--checkResource='
           '--scope='
           '--identity=\n')

if __name__ == '__main__':
    try:
        opts, args = getopt.getopt(sys.argv[1:], None,
                                   ["mode=","gcsBucket=","gcsObject=","scope=","checkResource=","identity="])
    except getopt.GetoptError:
        usage()
        sys.exit(1)

    mode = ""
    gcsBucket = ""
    gcsObject = ""
    scope = ""
    checkResource = ""
    identity = ""

    for opt, arg in opts:
        if opt == "--mode":
            mode = arg
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

    if not mode:
        print('mode must be specified .')
        usage()
        sys.exit(1)

    if mode =="basic":
        if gcsBucket == "" or gcsObject == "":
          print("Both --gcsObject= and --gcsBucket= must be specified with --mode=rest")
          usage()
          sys.exit(1)
        basic(gcsBucket,gcsObject)

    if mode =="extended":
        if scope == "" or checkResource == "" or identity =="":
          print("--scope= and --checkResource= and --identity= must be specified with --mode=grpc")
          usage()
          sys.exit(1)
        extended(scope,checkResource,identity)