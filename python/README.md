## GCP Error Details custom handler (python)


The python library contained here wraps both `google.cloud.exceptions` and `googleapiclient.errors` and provides convenience methods to extract and print any embedded errors.

The objective of the library is to display by default the _same_ message structure as a developer would see as if the library is not used.  That is, you should see the same output if you use the wrapper or not but with the distinction that you _can_ extract the details using the wrapper methods provided:

* Constructor:

```python
from gcp_error_handler.gcp_errors import GCPError

try:
  ...
except google.cloud.exceptions.GoogleCloudError as err:
  ee = GCPError(err)
  print(ee)


try:
  ...
except googleapiclient.errors.HttpError as err:
  ee = GCPError(err)
  print(ee)    
```


Methods for Error Details:

* `get_google_rpc_help`
* `get_google_rpc_badrequest`
* `get_google_rpc_errorinfo`
* `get_google_rpc_preconditionfailure`

Usage:

```python

        ee = GCPError(err)
        print(ee)
        print("Details: ")
        for e in ee.errors:
            if (ee.grpc_status_code != None):
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
```

In the following examples, the various libraries throw different error types, some of which surface details 

* `google.cloud.exceptions.GoogleCloudError`: Asset API (error details present), PubSub, GCS
* `googleapiclient.errors.HttpError`: Compute Engine

### Usage

```bash
virtualenv env
source env/bin/activate
pip install -r requirements.txt

export GOOGLE_APPLICATION_CREDENTIALS=/path/to/svc_account.json
```

#### Asset Inventory (google.cloud.exceptions.GoogleCloudError)

The asset inventory api below will return error detail information.  For example, if you run the following snippet, 

```bash
$ python main.py --api=asset    --checkResource="//cloudresourcemanager.googleapis.com/projects/fabled-ray-104117"      --identity=user:admin@domain.com     --scope=projects/fabled-ray-104117

403 Request denied by Cloud IAM.
HTTPStatus.FORBIDDEN
Request denied by Cloud IAM.
<_InactiveRpcError of RPC that terminated with:
	status = StatusCode.PERMISSION_DENIED
	details = "Request denied by Cloud IAM."
	debug_error_string = "{"created":"@1617712473.042385905","description":"Error received from peer ipv4:172.217.12.234:443","file":"src/core/lib/surface/call.cc","file_line":1067,"grpc_message":"Request denied by Cloud IAM.","grpc_status":7}"
>
StatusCode.PERMISSION_DENIED
Details: 
     Help Url:  https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
     Help Description:  To check permissions required for this RPC:
     Help Url:  https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
     Help Description:  To get a valid organization id:
     Help Url:  https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects
     Help Description:  To get a valid folder or project id:

========================================


403 Request denied by Cloud IAM.
HTTPStatus.FORBIDDEN
Request denied by Cloud IAM.
<_InactiveRpcError of RPC that terminated with:
	status = StatusCode.PERMISSION_DENIED
	details = "Request denied by Cloud IAM."
	debug_error_string = "{"created":"@1617712473.042385905","description":"Error received from peer ipv4:172.217.12.234:443","file":"src/core/lib/surface/call.cc","file_line":1067,"grpc_message":"Request denied by Cloud IAM.","grpc_status":7}"
>
StatusCode.PERMISSION_DENIED
Details: 
     Help Url:  https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
     Help Description:  To check permissions required for this RPC:
     Help Url:  https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
     Help Description:  To get a valid organization id:
     Help Url:  https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects
     Help Description:  To get a valid folder or project id:
```


First notice that the tet before and after the delimiter (`===========`) is identical.  That is intended behavior for the library where by default its a no-op:

Notice the corresponding print() statements for the default handler and the enhanced log error handler (autoParser)

```python
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
            if (ee.grpc_status_code != None):
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

```

Now, the whole point of the library is so that a user does NOT have to unmarshall using the gRPC metadata...thats where the `GCPError()` wraper comes into the picture and provides convenience methods


also note the `prettyprint=` flag which you can also enable with an env-var `$export GOOGLE_ENABLE_ERROR_DETAIL=true`

The output will show the details in a readable json format
```bash

========================================

{
    "GoogleCloudError": "403 Request denied by Cloud IAM.",
    "google.rpc.help-bin": {
        "links": [
            {
                "description": "To check permissions required for this RPC:",
                "url": "https://cloud.google.com/asset-inventory/docs/access-control#required_permissions"
            },
            {
                "description": "To get a valid organization id:",
                "url": "https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id"
            },
            {
                "description": "To get a valid folder or project id:",
                "url": "https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects"
            }
        ]
    }
}
HTTPStatus.FORBIDDEN
Request denied by Cloud IAM.
<_InactiveRpcError of RPC that terminated with:
	status = StatusCode.PERMISSION_DENIED
	details = "Request denied by Cloud IAM."
	debug_error_string = "{"created":"@1617712623.777316881","description":"Error received from peer ipv4:172.217.164.170:443","file":"src/core/lib/surface/call.cc","file_line":1067,"grpc_message":"Request denied by Cloud IAM.","grpc_status":7}"
>
StatusCode.PERMISSION_DENIED
Details: 
     Help Url:  https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
     Help Description:  To check permissions required for this RPC:
     Help Url:  https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
     Help Description:  To get a valid organization id:
     Help Url:  https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects
     Help Description:  To get a valid folder or project id:
```


#### PubSub

For libraries like pubsub, you can also use the wrapper but it will not show error details (the pubsub api does not yet support the details).

Note that the error details before/after are identical:

```bash
$ python main.py --api=pubsub      --projectID=fabled-ray-104117 

403 User not authorized to perform this action.
HTTPStatus.FORBIDDEN
User not authorized to perform this action.
<_InactiveRpcError of RPC that terminated with:
	status = StatusCode.PERMISSION_DENIED
	details = "User not authorized to perform this action."
	debug_error_string = "{"created":"@1617712954.633051421","description":"Error received from peer ipv4:172.217.10.74:443","file":"src/core/lib/surface/call.cc","file_line":1067,"grpc_message":"User not authorized to perform this action.","grpc_status":7}"
>
StatusCode.PERMISSION_DENIED
Details: 
========================================
403 User not authorized to perform this action.
HTTPStatus.FORBIDDEN
User not authorized to perform this action.
<_InactiveRpcError of RPC that terminated with:
	status = StatusCode.PERMISSION_DENIED
	details = "User not authorized to perform this action."
	debug_error_string = "{"created":"@1617712954.633051421","description":"Error received from peer ipv4:172.217.10.74:443","file":"src/core/lib/surface/call.cc","file_line":1067,"grpc_message":"User not authorized to perform this action.","grpc_status":7}"
>
StatusCode.PERMISSION_DENIED
Details: 
```

given

```python
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

```


#### GCS (google.cloud.exceptions.GoogleCloudError)

For libraries like GCS, you can also use the wrapper but it will not show error details since it is REST based API

Note that the error details before/after are identical:

```bash
$ python main.py --api=gcs      --gcsBucket fabled-ray-104117-bucket      --gcsObject foo.txt 

403 GET https://storage.googleapis.com/storage/v1/b/fabled-ray-104117-bucket/o/foo.txt?projection=noAcl&prettyPrint=false: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
HTTPStatus.FORBIDDEN
GET https://storage.googleapis.com/storage/v1/b/fabled-ray-104117-bucket/o/foo.txt?projection=noAcl&prettyPrint=false: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
<Response [403]>
None
Details: 
message vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
domain global
reason forbidden

========================================

403 GET https://storage.googleapis.com/storage/v1/b/fabled-ray-104117-bucket/o/foo.txt?projection=noAcl&prettyPrint=false: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
HTTPStatus.FORBIDDEN
GET https://storage.googleapis.com/storage/v1/b/fabled-ray-104117-bucket/o/foo.txt?projection=noAcl&prettyPrint=false: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
<Response [403]>
None
Details: 
message vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
domain global
reason forbidden
```

given 

```python
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

```

#### Compute (googleapiclient.errors.HttpError)

The compute engine API surfaces `googleapiclient.errors.`.  THis library ONLY catches `googleapiclient.errors.HttpError` error as a POC.

* TODO: handle all the error types in [https://github.com/googleapis/google-api-python-client/blob/master/googleapiclient/errors.py#L35](https://github.com/googleapis/google-api-python-client/blob/master/googleapiclient/errors.py#L35)


Anyway, by default you will see identical responses

```bash
$ python main.py --api=compute     --projectID=fabled-ray-104117 --zone=us-central1-a
{
  "error": {
    "code": 403,
    "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'",
    "errors": [
      {
        "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'",
        "domain": "global",
        "reason": "forbidden"
      }
    ]
  }
}

{'content-type': 'application/json; charset=UTF-8', 'vary': 'Origin, X-Origin, Referer', 'date': 'Tue, 06 Apr 2021 12:52:30 GMT', 'server': 'ESF', 'cache-control': 'private', 'x-xss-protection': '0', 'x-frame-options': 'SAMEORIGIN', 'x-content-type-options': 'nosniff', 'alt-svc': 'h3-29=":443"; ma=2592000,h3-T051=":443"; ma=2592000,h3-Q050=":443"; ma=2592000,h3-Q046=":443"; ma=2592000,h3-Q043=":443"; ma=2592000,quic=":443"; ma=2592000; v="46,43"', 'transfer-encoding': 'chunked', 'status': '403', 'content-length': '330', '-content-encoding': 'gzip'}
https://compute.googleapis.com/compute/v1/projects/fabled-ray-104117/zones/us-central1-a/instances?alt=json
Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'

========================================
{
  "error": {
    "code": 403,
    "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'",
    "errors": [
      {
        "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'",
        "domain": "global",
        "reason": "forbidden"
      }
    ]
  }
}

{'content-type': 'application/json; charset=UTF-8', 'vary': 'Origin, X-Origin, Referer', 'date': 'Tue, 06 Apr 2021 12:52:30 GMT', 'server': 'ESF', 'cache-control': 'private', 'x-xss-protection': '0', 'x-frame-options': 'SAMEORIGIN', 'x-content-type-options': 'nosniff', 'alt-svc': 'h3-29=":443"; ma=2592000,h3-T051=":443"; ma=2592000,h3-Q050=":443"; ma=2592000,h3-Q046=":443"; ma=2592000,h3-Q043=":443"; ma=2592000,quic=":443"; ma=2592000; v="46,43"', 'transfer-encoding': 'chunked', 'status': '403', 'content-length': '330', '-content-encoding': 'gzip'}
https://compute.googleapis.com/compute/v1/projects/fabled-ray-104117/zones/us-central1-a/instances?alt=json
Required 'compute.instances.list' permission for 'projects/fabled-ray-104117
```

given

```python
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
```


### Building gcp-error-handler package

```bash
python3 setup.py sdist
```