### Decoding GCP Errors Details

>> **NOTE:**  this repo is just a placeholder...once i figure out how to actually catch and unmarshall the exceptions properly, i'll update this as v1 and wrap the errors in a library (if possible)

Several GCP Services now return descriptive errors embedded within the top-level exception. For example,

A) if you list pubsub topics and do not have permissions, you'd see

```json
curl -s -H "Authorization: Bearer `gcloud auth print-access-token`" https://pubsub.googleapis.com/v1/projects/fabled-ray-104117/topics
{
  "error": {
    "code": 403,
    "message": "User not authorized to perform this action.",
    "status": "PERMISSION_DENIED"
  }
}
```

* **`remark:`** great...who is the user and what permissions do i need?


B) if you try to list a GCS bucket that doesn't exist, you'd see a `NOT FOUND`

```json
$  curl -H "Authorization: Bearer `gcloud auth print-access-token`"  https://storage.googleapis.com/storage/v1/b/mineral-minutia-820-fooo
{
  "error": {
    "code": 404,
    "message": "Not Found",
    "errors": [
      {
        "message": "Not Found",
        "domain": "global",
        "reason": "notFound"
      }
    ]
  }
}
```

* **`remake`**:  whats not found? the bucket..how do i resolve it?


C) if you try to access an object in GCS

```json
$ curl -H "Authorization: Bearer `gcloud auth print-access-token`"  https://storage.googleapis.com/storage/v1/b/fabled-ray-104117-bucket/o/foo.txt
{
  "error": {
    "code": 403,
    "message": "vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.",
    "errors": [
      {
        "message": "vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.",
        "domain": "global",
        "reason": "forbidden"
      }
    ]
  }
}
```

* **`remark`**: Thats really good!...now i see who the user is and the permission!

D) if you try to check access permissions using [Cloud Asset API](https://cloud.google.com/asset-inventory/docs/reference/rest)

```json
 curl  "https://cloudasset.googleapis.com/v1/projects/fabled-ray-104117:analyzeIamPolicy?analysisQuery.identitySelector.identity=user%3Auser4%40esodemoapp2.com&analysisQuery.resourceSelector.fullResourceName=%2F%2Fcompute.googleapis.com%2Fprojects%2Ffabled-ray-104117%2Fzones%2Fus-central1-a%2Finstances%2Fexternal"   --header "Authorization: Bearer $TOKEN"   --header 'Accept: application/json'

{
  "error": {
    "code": 403,
    "message": "Request denied by Cloud IAM.",
    "status": "PERMISSION_DENIED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
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
    ]
  }
}
```

* **`remark:`**  Thats awesome!...look at all the embedded *details*, links in the error response!

---

### Google API Error Design

Now, what are these details?

These are just standard and extended error responses Google APIs return (or should return).  The expectation is that all GCP APIs return meaningful errors in the format and structure described in [API Error Design](https://cloud.google.com/apis/design/errors) and  [google.rpc](https://github.com/googleapis/googleapis/tree/master/google/rpc).

In golang, this translates to the [googleapis.Error](https://pkg.go.dev/google.golang.org/api/googleapi#Error) struct where the really good information is inside the `Details []interface{}` which itself is a struct of various types (described below)


```golang
type Error struct {
	// Code is the HTTP response status code and will always be populated.
	Code int `json:"code"`
	// Message is the server response message and is only populated when
	// explicitly referenced by the JSON server response.
	Message string `json:"message"`
	// Details provide more context to an error.
	Details []interface{} `json:"details"`
	// Body is the raw response returned by the server.
	// It is often but not always JSON, depending on how the request fails.
	Body string
	// Header contains the response header fields from the server.
	Header http.Header

	Errors []ErrorItem
}

type ErrorItem struct {
	// Reason is the typed error code. For example: "some_example".
	Reason string `json:"reason"`
	// Message is the human-readable description of the error.
	Message string `json:"message"`
}
```

We'll use golang as a base language for concrete examples.  At the top level, when you catch a generic authentication or api error, you can inspect it for more base top level descriptions by casting to `googleapis.Error`.  For details, you need to go one step further and convert the returned proto to `googleapi.Error.Details` (see [error_details.proto](https://github.com/googleapis/googleapis/blob/master/google/rpc/error_details.proto)).  There are several types of details that could be returned:

* `ErrorInfo`: Provides structured error information that is both stable and extensible.
* `RetryInfo`: Describes when clients can retry a failed request, may be returned on `Code.UNAVAILABLE` or `Code.ABORTED`
* `QuotaFailure`: Describes how a quota check failed, may be returned on `Code.RESOURCE_EXHAUSTED`
* `BadRequest`: Describes violations in a client request, may be returned on `Code.INVALID_ARGUMENT`
* `Help`: Provides links to documentation or for performing an out of band action.

What this repo seeks to do is to provide a prescriptive way in various languages for your application to catch, log and handle the descriptive errors as well as to provide a wrapper library that encapsulates all this.  By the latter it would be a wrapper Error handler that your application can delegate to that does the internal proto unmarshalling

We will start off with `gcloud`, then have bindings in golang, java, python, nodejs and dotnet and will be using `GOOGLE_APPLICATION_CREDENTIALS` environment variable and service accounts primarily.  

---

* [gcloud](#gcloud)
    - [gcloud basic](#gcloud-basic)
    - [gcloud detail](#gcloud-detail)
* [golang](#golang)
    - [golang basic](#golang-basic)
    - [golang detail](#golang-detail)
* [python](#python)
    - [python basic](#python-basic)
    - [python detail](#python-detail)
* [java](#java)
    - [java basic](#java-basic)
    - [java detail](#java-detail)
* [nodejs](#nodejs)
    - [nodejs basic](#nodejs-basic)
    - [nodejs detail](#nodejs-detail)
* [dotnet](#dotnet)
    - [dotnet basic](#dotnet-basic)
    - [dotnet detail](#dotnet-detail)
---


First download a service account and export the variables for application default credentials

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/svc_account.json
export PROJECT_ID=`gcloud config get-value core/project`
export PROJECT_NUMBER=`gcloud projects describe $PROJECT_ID --format='value(projectNumber)'`
```

### gcloud

`gcloud` cli by default does not use the environment variables for Application default credentials so we have to manually instruct it to use the service account

```bash
$ gcloud auth activate-service-account --key-file=/path/to/svc_account.json
```

Use gcloud to access GCS and Cloud Asset Inventory.  Notice the two formats for errors returned:


#### gcloud Basic

The GCS api does not return detailed messages such as the `type.googleapis.com/google.rpc.Help` structure so we see just basic data

```bash
$ gcloud alpha storage cp gs://fabled-ray-104117-bucket/foo.txt .

ERROR: gcloud crashed (GcsApiError): HTTPError 403: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
```

#### gcloud Detail

The cloud asset api on the other hand does...and we see the error rendered in detail:

```bash
$ gcloud  asset analyze-iam-policy \
   --project fabled-ray-104117 \
   --identity=user:admin@esodemoapp2.com \
   --full-resource-name="//cloudresourcemanager.googleapis.com/projects/fabled-ray-104117"

ERROR: (gcloud.asset.analyze-iam-policy) User [vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com] does not have permission to access projects instance [fabled-ray-104117:analyzeIamPolicy] (or it may not exist): Request denied by Cloud IAM.
- '@type': type.googleapis.com/google.rpc.Help
  links:
  - description: 'To check permissions required for this RPC:'
    url: https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
  - description: 'To get a valid organization id:'
    url: https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
  - description: 'To get a valid folder or project id:'
    url: https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects
```

### Golang

#### golang Basic

Basic errors types are reurned  [googleapis.Error](https://pkg.go.dev/google.golang.org/api/googleapi#Error) is parsed using casting

eg
```golang
if ee, ok := err.(*googleapi.Error); ok { ...
```

You can see this using the provided sample and run:

```log
$ go run main.go \
    --mode=basic  \
    --gcsBucket fabled-ray-104117-bucket  \
    --gcsObject foo.txt


2021/03/16 19:56:42 ================ Using REST (GCS) ======================
2021/03/16 19:56:42 Error Code: 403
2021/03/16 19:56:42 Error Message:
2021/03/16 19:56:42 Error Details: []
2021/03/16 19:56:42 Error Body: <?xml version='1.0' encoding='UTF-8'?><Error><Code>AccessDenied</Code><Message>Access denied.</Message><Details>vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.</Details></Error>
2021/03/16 19:56:42 Errors:
```

#### golang Detail

Details data can be extracted using [status.FromError()](https://pkg.go.dev/google.golang.org/grpc/status#FromError) and unmarshalled appropriately

```golang
if s, ok := status.FromError(err); ok {
		for _, d := range s.Proto().Details {
			switch d.TypeUrl {
      case "type.googleapis.com/google.rpc.Help":
				h := &errdetails.Help{}
				err = ptypes.UnmarshalAny(d, h)
				if err != nil {
					return err
				}
				for _, l := range h.Links {
					log.Printf("     ErrorHelp Description %s\n", l.Description)
					log.Printf("     ErrorHelp Url %s\n", l.Url)
				}
...      
```

You can see this using the provided sample and run:

```log
go run main.go \
   --mode=extended \
   --gcsBucket=fabled-ray-104117-bucket \
   --gcsObject=foo.txt \
   --checkResource="//cloudresourcemanager.googleapis.com/projects/fabled-ray-104117"   \
   --identity="user:admin@esodemoapp2.com"  \
   --scope="projects/fabled-ray-104117"

I0316 18:30:46.867344 3353123 main.go:50] ================ QueryTestablePermissions with Resource ======================
I0316 18:30:46.867557 3353123 main.go:52] Getting AnalyzeIamPolicyRequest
I0316 18:30:47.239546 3353123 main.go:121] type.googleapis.com/google.rpc.Help
E0316 18:30:47.239914 3353123 main.go:131]    ErrorHelp Description To check permissions required for this RPC:
E0316 18:30:47.241062 3353123 main.go:131]    ErrorHelp Description To get a valid organization id:
E0316 18:30:47.241093 3353123 main.go:131]    ErrorHelp Description To get a valid folder or project id:
```

### Python


#### python Basic
Basic top-level errors can be caught by directly using [google.cloud.exceptions.GoogleCloudError](https://gcloud.readthedocs.io/en/latest/_modules/google/cloud/exceptions.html)

```python
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
```

```bash
$ python main.py --mode=basic      --gcsBucket fabled-ray-104117-bucket      --gcsObject foo.txt

HTTPStatus.FORBIDDEN
[Forbidden('GET https://storage.googleapis.com/storage/v1/b/fabled-ray-104117-bucket/o/foo.txt?projection=noAcl&prettyPrint=false: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.')]
Details:
  message vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
  domain global
  reason forbidden
```

#### python Detail

Detail errors should be casted using `GoogleCloudError` methods then extract from the trailing metadata

- [grpc.Status](https://grpc.github.io/grpc/python/grpc_status.html)
```python
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
```


```bash
python main.py --mode=extended \
   --checkResource="//cloudresourcemanager.googleapis.com/projects/fabled-ray-104117"   \
   --identity=user:admin@esodemoapp2.com  \
   --scope=projects/fabled-ray-104117
```

gives output

```
403 Request denied by Cloud IAM.
HTTPStatus.FORBIDDEN
Request denied by Cloud IAM.
StatusCode.PERMISSION_DENIED
........................
     Help Url:  https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
     Help Description:  To check permissions required for this RPC:
     Help Url:  https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
     Help Description:  To get a valid organization id:
     Help Url:  https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects
     Help Description:  To get a valid folder or project id:
```

### Java

#### java Basic

Catch basic exceptions using service-specific handlers.  For example for GCS, use [com.google.cloud.storage.StorageException](https://googleapis.dev/java/google-cloud-storage/latest/com/google/cloud/storage/StorageException.html)

```java
        try {
            Storage storage = StorageOptions.newBuilder().build().getService();
            Blob blob = storage.get(BlobId.of(bucketName, objectName));
            String value = new String(blob.getContent());
            System.out.println(value);

        } catch (StorageException ex) {
            System.out.println("StorageException: ");
            System.out.println("  Code: " + ex.getCode());
            System.out.println("  Message: " + ex.getMessage());
            System.out.println("  DebugInfo: " + ex.getDebugInfo());
            System.out.println("  Location: " + ex.getLocation());
            System.out.println("  Reason: " + ex.getReason());
            // System.out.println(" Cause: " + ex.getCause());
        } catch (Exception ex) {
            System.out.println("Exception:  " + ex);
        }
```

```bash
mvn clean install exec:java -q \
   -Dexec.args="-mode basic -gcsBucket fabled-ray-104117-bucket -gcsObject foo.txt"

StorageException:
  Code: 403
  Message: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
  DebugInfo: null
  Location: null
  Reason: forbidden
```

#### java Detail

Detail messages must be marshalled out of generic `Exception` from within that the various Metadata types.  The type Keys within the metadata shows the types

* `grpc-status-details-bin`

See [io.grpc.Status](https://grpc.github.io/grpc-java/javadoc/io/grpc/Status.html)

For example

```java
        try {
            AssetServiceClient client = AssetServiceClient.create();
            AnalyzeIamPolicyRequest request = AnalyzeIamPolicyRequest.newBuilder()
                    .setAnalysisQuery(IamPolicyAnalysisQuery.newBuilder().setScope(scope)
                            .setIdentitySelector(IdentitySelector.newBuilder().setIdentity(identity).build())
                            .setResourceSelector(
                                    ResourceSelector.newBuilder().setFullResourceName(checkResource).build()))
                    .build();

            AnalyzeIamPolicyResponse response = client.analyzeIamPolicy(request);
            System.out.println(response);
        } catch (Exception ex) {
            System.out.println("Exception:");
            Status ss = Status.fromThrowable(ex);
            System.out.println("  Status.getCode:  " + ss.getCode().value());
            System.out.println("  Status.getDescription:  " + ss.getDescription());
            Metadata m = Status.trailersFromThrowable(ex);

            for (String k : m.keys()) {
                if (k.equals("google.rpc.help-bin")) {
                    byte[] byt_help = m.get(Metadata.Key.of("google.rpc.help-bin", Metadata.BINARY_BYTE_MARSHALLER));
                    Help h = null;
                    try {
                      h = Help.parseFrom(byt_help);
                    } catch (InvalidProtocolBufferException ioex) {
                        System.out.println("err" + ioex);
                        return;
                    }
                    for (Help.Link l : h.getLinksList()){
                        System.out.println("     Exception Link getDescription:  " + l.getDescription());
                        System.out.println("     Exception Link getUrl:  " + l.getUrl());                        
                    }                    
                }
            }
        }
```

Sample output is:

```bash
mvn clean install exec:java -q \
   -Dexec.args="-mode extended -checkResource //cloudresourcemanager.googleapis.com/projects/fabled-ray-104117 -identity user:admin@esodemoapp2.com -scope projects/fabled-ray-104117"


Exception:
  Status.getCode:  7
  Status.getDescription:  Request denied by Cloud IAM.
   Parsing: grpc-server-stats-bin
   Parsing: google.rpc.help-bin
     Exception Link getDescription:  To check permissions required for this RPC:
     Exception Link getUrl:  https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
     Exception Link getDescription:  To get a valid organization id:
     Exception Link getUrl:  https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
     Exception Link getDescription:  To get a valid folder or project id:
     Exception Link getUrl:  https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects
   Parsing: grpc-status-details-bin
```

### NodeJS

#### nodejs Basic

Catch Basic errors as node exceptions without marshalling.  The `err` is actually a googleapi Error

```javascript
  const {Storage} = require('@google-cloud/storage');
  const storage = new Storage();
  var file =  storage.bucket(bucketName).file(objectName);
  file.download(function(err, contents) {

      if (err) {
        // err is ApiError
        console.log("Error Code: " + err.code);
        console.log("Error Message: " + err.message);
        console.log("Error Errors: " + err.errors);
      }  else {
        console.log("file data: " + contents);   
      }
  });
```

```bash
$ node main.js --mode=basic --gcsBucket=fabled-ray-104117-bucket --gcsObject=foo.txt

Error Code: 403
Error Message: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
Error Errors:
```

#### nodejs Detail

>>> Note..while the following works...this is certainly not righ

Detail Messages are embedded inside the metadata fields of the RPC.  For example,

```javascript
const result =  client.analyzeIamPolicy(request, options).then(function(value) {
  console.log(util.inspect(value, {depth: null}));
}, function(err) {
  console.log('Code: ' + err.code);
  console.log('Details: ' + err.details);
  console.log('Message: ' + err.message);  

  if (err.metadata) {
    // TODO: deserialize to approprate @type:      
    const help_bytes = err.metadata.get('google.rpc.help-bin');
    const protos = require('google-proto-files');
    protos.load('./node_modules/google-proto-files/google/rpc/error_details.proto').then(function(root) {
      const helpdef = root.lookup("google.rpc.Help");
      const help = helpdef.decode(help_bytes[0])
      help.links.forEach(element => {
        console.log(element.description);
        console.log(element.url);
      })
    }, function(err){
      console.log(err)
    }) ;
  }

});  
}
```

gives the error details but I'm not sure how to unmarshall in node


```bash
$ node main.js --mode=extended \
  --checkResource=//cloudresourcemanager.googleapis.com/projects/fabled-ray-104117 \
  --identity=user:admin@esodemoapp2.com \
  --scope=projects/fabled-ray-104117
```

gives output

```
Code: 7
Details: Request denied by Cloud IAM.
Message: 7 PERMISSION_DENIED: Request denied by Cloud IAM.
To check permissions required for this RPC:
https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
To get a valid organization id:
https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
To get a valid folder or project id:
https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects
```


### dotnet

#### dotnet Basic

Catching basic errors in GCS is pretty straightforward as [Google.GoogleApiException](https://googleapis.dev/dotnet/Google.Apis.Core/latest/api/Google.GoogleApiException.html)

```csharp
            var storage = StorageClient.Create();
            try
            {
                storage.DownloadObject(bucketName, objectName, Console.OpenStandardOutput());
            }
            catch (Google.GoogleApiException e)
            {
                Console.WriteLine("Message: " + e.Message);
                Console.WriteLine("ServiceName: " + e.ServiceName);
                Console.WriteLine("Source: " + e.Source);
                Console.WriteLine("HttpStatusCode: " + e.HttpStatusCode);
                Console.WriteLine("HelpLink: " + e.HelpLink);
                Console.WriteLine("Error: " + e.Error);
            }
```

```bash
$ dotnet --version
3.1.302

$ dotnet run --mode=basic --gcsBucket=fabled-ray-104117-bucket --gcsObject=foo.txt

Message: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
ServiceName: storage
Source: Google.Cloud.Storage.V1
HttpStatusCode: Forbidden
HelpLink:
Error:
```

#### dotnet Detail

```bash
$ dotnet run --mode=extended \
  --checkResource=//cloudresourcemanager.googleapis.com/projects/fabled-ray-104117 \
  --identity=user:admin@esodemoapp2.com \
  --scope=projects/fabled-ray-104117
```

>> TODO: i'm not sure how to do this...the following works but is pretty ineffecient

```csharp
            try
            {
                AnalyzeIamPolicyRequest request = new AnalyzeIamPolicyRequest
                {
                    AnalysisQuery = new IamPolicyAnalysisQuery
                    {
                        Scope = scope,
                        IdentitySelector = new IamPolicyAnalysisQuery.Types.IdentitySelector
                        {
                            Identity = identity,
                        },
                        ResourceSelector = new IamPolicyAnalysisQuery.Types.ResourceSelector
                        {
                            FullResourceName = checkResource,
                        }

                    },
                };
                var response = client.AnalyzeIamPolicy(request);
                Console.WriteLine(response);
            }
            catch (Grpc.Core.RpcException e)
            {
                Console.WriteLine("Message: " + e.Message);
                Console.WriteLine("Status: " + e.Status);

                Console.WriteLine("Status.StatusCode: " + e.Status.StatusCode);
                Console.WriteLine("Status.Detail: " + e.Status.Detail);

                PrintRpcExceptionDetails(e);
            }
        }
        // https://github.com/chwarr/grpc-dotnet-google-rpc-status/blob/master/client/Program.cs
        private static void PrintRpcExceptionDetails(RpcException ex)
        {
            byte[]? statusBytes = null;

            foreach (Metadata.Entry me in ex.Trailers)
            {

                if (me.Key == StatusDetailsTrailerName)
                {
                    statusBytes = me.ValueBytes;
                }
            }

            if (statusBytes is null)
            {
                return;
            }

            var status = Google.Rpc.Status.Parser.ParseFrom(statusBytes);

            foreach (Any any in status.Details)
            {
                PrintRPCDetails(any);
            }
        }

        private static void PrintRPCDetails(Any any)
        {
            if (any.TryUnpack(out Google.Rpc.BadRequest br))
            {
                Console.WriteLine($"  BadRequest {br}");                
            }
            else if (any.TryUnpack(out Google.Rpc.PreconditionFailure pf))
            {
                Console.WriteLine($"  PreconditionFailure {pf}");
            } else if (any.TryUnpack(out Google.Rpc.Help h))
            {                
                foreach (Types.Link l in h.Links)
                {
                    Console.WriteLine("    Description: " + l.Description);
                    Console.WriteLine("    URL: " + l.Url);                    
                }
            }
        }
```

```text
Status.StatusCode: PermissionDenied
Status.Detail: Request denied by Cloud IAM.
    Description: To check permissions required for this RPC:
    URL: https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
    Description: To get a valid organization id:
    URL: https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
    Description: To get a valid folder or project id:
    URL: https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects
```
