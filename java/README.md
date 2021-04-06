## GCP Error Details custom handler (java)


The java library contained here wraps both `com.google.api.gax.rpc.ApiException` and `com.google.cloud.http.BaseHttpServiceException` and provides convenience methods to extract and print any embedded errors.

The objective of the library is to display by default the _same_ message structure as a developer would see as if the library is not used.  

That is, you should see the same output if you use the wrapper or not but with the distinction that you _can_ extract the details using the wrapper methods provided:

* Constructor:

```java

```


Methods for Error Details:

* `getGoogleRpcHelp`
* `getGoogleRpcBadRequest`
* `getGoogleRpcErrorInfo`
* `getGoogleRpcPreconditionFailure`

Usage:

```java

```

In the following examples, the various libraries throw different error types, some of which surface details 

* `google.cloud.exceptions.GoogleCloudError`: Asset API (error details present), PubSub, GCS
* `googleapiclient.errors.HttpError`: Compute Engine

### Usage

```bash
mvn clean install

export GOOGLE_APPLICATION_CREDENTIALS=/path/to/svc_account.json
```

#### Asset Inventory ()

The asset inventory api below will return error detail information.  For example, if you run the following snippet, 

```java
    public void Asset(String checkResource, String identity, String scope) {
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

        } catch (com.google.api.gax.rpc.ApiException ex) {

            System.out.println(" getStatusCode: " + ex.getStatusCode().getCode());
            System.out.println(" Message: " + ex.getMessage());
            System.out.println(" isRetryable: " + ex.isRetryable());
            
            System.out.println("===================");
            ErrorHandler e = new ErrorHandler(ex,false);
            System.out.println(" getStatusCode: " + e.getStatusCode().getCode());
            System.out.println(" Message: " + e.getMessage());
            System.out.println(" isRetryable: " + e.isRetryable());
            if (e.getGoogleRPCHelp() != null) {
                com.google.rpc.Help h = e.getGoogleRPCHelp();
                for (Help.Link l : h.getLinksList()) {
                    System.out.println("     Exception Link getDescription:  " + l.getDescription());
                    System.out.println("     Exception Link getUrl:  " + l.getUrl());
                }
            }
        
        } catch (IOException ex) {
            System.out.println(">>>>>>>>>>>>>>>> "+ ex);
        }
    }
```
```bash
mvn clean install exec:java -q  \
    -Dexec.args="-api asset -checkResource //cloudresourcemanager.googleapis.com/projects/fabled-ray-104117 -identity user:admin@esodemoapp2.com -scope projectsvv/fabled-ray-104117"


 getStatusCode: INVALID_ARGUMENT
 Message: io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Invalid parent in request.
 isRetryable: false

===================

 getStatusCode: INVALID_ARGUMENT
 Message: io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Invalid parent in request.
 isRetryable: false

     Exception Link getDescription:  To check permissions required for this RPC:
     Exception Link getUrl:  https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
     Exception Link getDescription:  To get a valid organization id:
     Exception Link getUrl:  https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
     Exception Link getDescription:  To get a valid folder or project id:
     Exception Link getUrl:  https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects    
```


First notice that the tet before and after the delimiter (`===========`) is identical.  That is intended behavior for the library where by default its a no-op:

Also note that within the thrown exception, we can extract the error details like `com.google.rpc.Help` and other
```java


```

Now, the whole point of the library is so that a user does NOT have to unmarshall using the gRPC metadata...thats where the `ErrorHandler()` wrapper comes into the picture and provides convenience methods


also note the `prettyprint` flag which you can also enable with an env-var `$ export GOOGLE_ENABLE_ERROR_DETAIL=true`

The output will show the details in a readable json format

```bash
$ mvn clean install exec:java -q    -Dexec.args="-api asset -checkResource //cloudresourcemanager.googleapis.com/projects/fabled-ray-104117 -identity user:admin@esodemoapp2.com -scope projectssdfasdf/fabled-ray-104117"

 getStatusCode: INVALID_ARGUMENT
 Message: io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Invalid parent in request.
 isRetryable: false

===================

 getStatusCode: INVALID_ARGUMENT
 Message: {
  "getMessage": "io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Invalid parent in request.",
  "getStatusCode.getCode.name": "INVALID_ARGUMENT",
  "com.google.rpc.Help": "links {  description: \"To check permissions required for this RPC:\"  url: \"https://cloud.google.com/asset-inventory/docs/access-control#required_permissions\"}links {  description: \"To get a valid organization id:\"  url: \"https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id\"}links {  description: \"To get a valid folder or project id:\"  url: \"https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects\"}",
  "com.google.rpc.BadRequest": "field_violations {  field: \"parent\"  description: \"Invalid parent in request.\"}"
}
 isRetryable: false
     Exception Link getDescription:  To check permissions required for this RPC:
     Exception Link getUrl:  https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
     Exception Link getDescription:  To get a valid organization id:
     Exception Link getUrl:  https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
     Exception Link getDescription:  To get a valid folder or project id:
     Exception Link getUrl:  https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects

```


#### PubSub

For libraries like pubsub, you can also use the wrapper but it will not show error details (the pubsub api does not yet support the details).

Note that the error details before/after are identical:


given

```java
    public void PubSub(String projectID) {
        try {
            TopicAdminClient topicClient = TopicAdminClient.create(TopicAdminSettings.newBuilder().build());
            ListTopicsRequest listTopicsRequest = ListTopicsRequest.newBuilder()
                    .setProject(ProjectName.format(projectID)).build();
            ListTopicsPagedResponse response = topicClient.listTopics(listTopicsRequest);
            Iterable<Topic> topics = response.iterateAll();
            for (Topic topic : topics)
                System.out.println(topic);
        } catch (com.google.api.gax.rpc.ApiException ex) {
            System.out.println(" getStatusCode: " + ex.getStatusCode().getCode());
            System.out.println(" Message: " + ex.getMessage());
            System.out.println("===================");
            ErrorHandler e = new ErrorHandler(ex);
            System.out.println(" getStatusCode: " + e.getStatusCode().getCode());
            System.out.println(" Message: " + e.getMessage());   

        } catch (Exception ex) {
            System.out.println("PubSub Excetion " + ex);
        }
    }
```

gives

```bash
$ mvn clean install exec:java -q    -Dexec.args="-api pubsub -projectID fabled-ray-104117"

 getStatusCode: PERMISSION_DENIED
 Message: io.grpc.StatusRuntimeException: PERMISSION_DENIED: User not authorized to perform this action.

===================

 getStatusCode: PERMISSION_DENIED
 Message: io.grpc.StatusRuntimeException: PERMISSION_DENIED: User not authorized to perform this act
```



#### GCS

For libraries like GCS, you can also use the wrapper but it will not show error details since it is REST based API

Note that the error details before/after are identical:
 

```java
    public void GCS(String bucketName, String objectName) {
        try {
            Storage storage = StorageOptions.newBuilder().build().getService();

            Blob blob = storage.get(BlobId.of(bucketName, objectName));
            String value = new String(blob.getContent());
            System.out.println(value);

        } catch (StorageException ex) {
            System.out.println(ex);
            System.out.println("  Code: " + ex.getCode());
            System.out.println("  Message: " + ex.getMessage());
            System.out.println("  DebugInfo: " + ex.getDebugInfo());
            System.out.println("  Location: " + ex.getLocation());
            System.out.println("  Reason: " + ex.getReason());
            System.out.println("  Cause: " + ex.getCause());

            System.out.println("=============================");
            ErrorHandler e = new ErrorHandler(ex);
            System.out.println(e);
            System.out.println("  Code: " + e.getCode());
            System.out.println("  Message: " + e.getMessage());
            System.out.println("  DebugInfo: " + e.getDebugInfo());
            System.out.println("  Location: " + e.getLocation());
            System.out.println("  Reason: " + e.getReason());
            System.out.println("  Cause: " + e.getCause());

        } catch (Exception ex) {
            System.out.println("Exception:  " + ex);
        }
    }
```

gives

```bash
mvn clean install exec:java -q    -Dexec.args="-api gcs -gcsBucket fabled-ray-104117-bucket -gcsObject foo.txt"

com.google.cloud.storage.StorageException: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
  Code: 403
  Message: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
  DebugInfo: null
  Location: null
  Reason: forbidden
  Cause: com.google.api.client.googleapis.json.GoogleJsonResponseException: 403 Forbidden
GET https://storage.googleapis.com/storage/v1/b/fabled-ray-104117-bucket/o/foo.txt?projection=full
{
  "code" : 403,
  "errors" : [ {
    "domain" : "global",
    "message" : "vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.",
    "reason" : "forbidden"
  } ],
  "message" : "vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object."
}

=============================

com.google.cloud.storage.StorageException: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
  Code: 403
  Message: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
  DebugInfo: null
  Location: null
  Reason: forbidden
  Cause: com.google.api.client.googleapis.json.GoogleJsonResponseException: 403 Forbidden
GET https://storage.googleapis.com/storage/v1/b/fabled-ray-104117-bucket/o/foo.txt?projection=full
{
  "code" : 403,
  "errors" : [ {
    "domain" : "global",
    "message" : "vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.",
    "reason" : "forbidden"
  } ],
  "message" : "vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object."
}
```

#### Compute

The compute engine API surfaces 

Anyway, by default you will see identical responses


```java
    public void Compute(String projectID, String zone) {
        try {
            GoogleCredentials credential = GoogleCredentials.getApplicationDefault();
            if (credential.createScopedRequired()) {
                credential = credential.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
            }

            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            Compute computeService = new Compute.Builder(httpTransport, jsonFactory,
                    new HttpCredentialsAdapter(credential)).setApplicationName("Google-ComputeSample/0.1").build();

            Compute.Instances.List request = computeService.instances().list(projectID, zone);
            ...

        } catch (IOException ex) {
            System.out.println(ex);
            System.out.println("getMessage()  " + ex.getMessage());
            System.out.println("===================");

            ErrorHandler e = new ErrorHandler(ex);
            System.out.println(e);
            System.out.println("getMessage()  " + e.getMessage());

        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
```

gives

```bash
$ mvn clean install exec:java -q    -Dexec.args="-api compute -projectID fabled-ray-104117 -zone us-central1-a"

com.google.api.client.googleapis.json.GoogleJsonResponseException: 403 Forbidden
GET https://compute.googleapis.com/compute/v1/projects/fabled-ray-104117/zones/us-central1-a/instances
{
  "code": 403,
  "errors": [
    {
      "domain": "global",
      "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'",
      "reason": "forbidden"
    }
  ],
  "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'"
}
getMessage()  403 Forbidden
GET https://compute.googleapis.com/compute/v1/projects/fabled-ray-104117/zones/us-central1-a/instances
{
  "code": 403,
  "errors": [
    {
      "domain": "global",
      "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'",
      "reason": "forbidden"
    }
  ],
  "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'"
}

===================

com.google.api.client.googleapis.json.GoogleJsonResponseException: 403 Forbidden
GET https://compute.googleapis.com/compute/v1/projects/fabled-ray-104117/zones/us-central1-a/instances
{
  "code": 403,
  "errors": [
    {
      "domain": "global",
      "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'",
      "reason": "forbidden"
    }
  ],
  "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'"
}
getMessage()  403 Forbidden
GET https://compute.googleapis.com/compute/v1/projects/fabled-ray-104117/zones/us-central1-a/instances
{
  "code": 403,
  "errors": [
    {
      "domain": "global",
      "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'",
      "reason": "forbidden"
    }
  ],
  "message": "Required 'compute.instances.list' permission for 'projects/fabled-ray-104117'"
}
```

