## GCP Error Details custom handler (nodejs)


The node library contained here wraps both [google-cloud ApiError](https://googleapis.dev/nodejs/common/2.0.0/classes/ApiError.html) and gRPC-based google API Error Details. It also provides convenience methods to extract and print any embedded errors.

The objective of the library is to display by default the _same_ message structure as a developer would see as if the library is not used.  

That is, you should see the same output if you use the wrapper or not but with the distinction that you _can_ extract the details using the wrapper methods provided:

---

###  Usage

```bash
cd node/
npm i
```

Note the package installs some dependencies as well as a _local_ reference to the actual unwrapping library:


* *`package.json`

```json
{
  "name": "myapp",
  "version": "0.0.0",
  "scripts": {
    "start": "node main.js"
  },
  "dependencies": {
    "@google-cloud/asset": "^3.12.0",
    "@google-cloud/compute": "^2.4.3",
    "@google-cloud/pubsub": "^2.10.0",
    "@google-cloud/storage": "^5.8.1",
    "debug": "~2.2.0",
    "gcp_error_handler": "file:gcp_error_handler",
    "minimist": "^1.2.5"
  }
}
```

* Import the library

```javascript
const  GCPError  = require('gcp_error_handler');
```

Constructor

```javascript
const e = GCPErrorHandler(error, prettyprint=false)
```

where the `error` value is simply the exception returned from any GCP API error (rest or grpc).  The `prettyprint` flag if set will display the error detail in json format to stdout

You can also enable an environment variable `export GOOGLE_ENABLE_ERROR_DETAIL=true`  which is the equivalent of setting `prettyprint=true`

---

To begin using, initialize any Application Default Credentials:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/svc_account.json
```

Then observe the various APIs response formats shown

#### Asset Inventory 

The asset inventory api below will return error detail information.  For example, if you run the following snippet, 

```javascript
function asset(
  checkResource,
  identity,
  scope,
) {

  const { AssetServiceClient } = require('@google-cloud/asset');

  const client = new AssetServiceClient();
  const request = {
    analysisQuery: {
      scope: scope,
      resourceSelector: {
        fullResourceName: checkResource,
      },
      identitySelector: {
        identity: identity,
      }
    }
  };
  const options = {
    autoPaginate: true,
  };

  const result = client.analyzeIamPolicy(request, options).then(function (value) {
    console.log(util.inspect(value, { depth: null }));
  }, function (err) {

    console.log("Error Code: " + err.code);
    console.log("Error Message: " + err.message);
    console.log("Error Errors: " + err.errors);
    console.log("Error Response: " + err.response);

    console.log("================================")

    const e = new GCPErrorHandler(err, false)

    console.log("Error Code: " + e.code);
    console.log("Error Message: " + e.message);
    console.log("Error Errors: " + e.errors);
    console.log("Error Response: " + e.response);

    console.log("Extract google.rpc.*  details")
    var h = e.getHelp();
    console.log("getHelp: " );
    h.links.forEach(element => {
      console.log(element.description);
      console.log(element.url);
    })
    console.log("getBadRequest: " + e.getBadRequest());

  });
}
```

```bash
$ node main.js --api=asset \
   --checkResource=//cloudresourcemanager.googleapis.com/projects/fabled-ray-104117 \
   --identity=user:admin@esodemoapp2.com \
   --scope=projects/fabled-ray-104117

Error Code: 7
Error Message: 7 PERMISSION_DENIED: Request denied by Cloud IAM.
Error Errors: undefined
Error Response: undefined

================================

Error Code: 7
Error Message: 7 PERMISSION_DENIED: Request denied by Cloud IAM.
Error Errors: undefined
Error Response: undefined

Extract google.rpc.*  details
getHelp: 
  To check permissions required for this RPC:
    https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
  To get a valid organization id:
    https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
  To get a valid folder or project id:
    https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects

getBadRequest: undefined
```


First notice that the text before and after the delimiter (`===========`) is identical.  That is intended behavior for the library where by default its a no-op:

Also note that within the thrown exception, we can extract the error details like `com.google.rpc.Help` and other

```javascript
    var h = e.getHelp();
    console.log("getHelp: " );
    h.links.forEach(element => {
      console.log(element.description);
      console.log(element.url);
    })

```

Now, the whole point of the library is so that a user does NOT have to unmarshall using the gRPC metadata...thats where the `GCPErrorHandler()` wrapper comes into the picture and provides convenience methods


also note the `prettyprint` flag which you can also enable with an env-var `$ export GOOGLE_ENABLE_ERROR_DETAIL=true`

The output will show the details in a readable json format

```javascript
    const e = new GCPErrorHandler(err, true)
```

```bash
$ node main.js --api=asset    --checkResource=//cloudresourcemanager.googleapis.com/projects/fabled-ray-104117    --identity=user:admin@esodemoapp2.com    --scope=projects/fabled-ray-104117

Error Code: 7
Error Message: 7 PERMISSION_DENIED: Request denied by Cloud IAM.
Error Errors: undefined
Error Response: undefined

================================

Error Code: 7
Error Message: {
    "message": "7 PERMISSION_DENIED: Request denied by Cloud IAM.",
    "google.rpc.Help": {
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
Error Errors: undefined
Error Response: undefined

Extract google.rpc.*  details
getHelp: 
  To check permissions required for this RPC:
    https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
  To get a valid organization id:
    https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
  To get a valid folder or project id:
    https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects

getBadRequest: undefined
```

#### PubSub

For libraries like pubsub, you can also use the wrapper but it will not show error details (the pubsub api does not yet support the details).

Note that the error details before/after are identical:


```javascript
function pubsub(
  projectID,
) {
  const { PubSub } = require('@google-cloud/pubsub');
  const pubSubClient = new PubSub();
  const topics = pubSubClient.getTopics().then(function (value) {
    console.log(value);
  }, function (err) {
    //console.log(err)
    console.log("Error Code: " + err.code);
    console.log("Error Message: " + err.message);
    console.log("Error Errors: " + err.errors);
    console.log("Error Response: " + err.response);

    console.log("================================")
    const e = new GCPErrorHandler(err)
    //console.log(e)
    console.log("Error Code: " + e.code);
    console.log("Error Message: " + e.message);
    console.log("Error Errors: " + e.errors);
    console.log("Error Response: " + e.response);
  });
}
```

```bash
$ node main.js --api=pubsub -projectID fabled-ray-104117
Error Code: 7
Error Message: 7 PERMISSION_DENIED: User not authorized to perform this action.
Error Errors: undefined
Error Response: undefined

================================

Error Code: 7
Error Message: 7 PERMISSION_DENIED: User not authorized to perform this action.
Error Errors: undefined
Error Response: undefined

```


#### GCS

For libraries like GCS, you can also use the wrapper but it will not show error details since it is REST based API

Note that the error details before/after are identical:

```javascript
function gcs(
  bucketName,
  objectName,
) {
  const { Storage } = require('@google-cloud/storage');
  const storage = new Storage();
  var file = storage.bucket(bucketName).file(objectName);
  file.download(function (err, contents) {
    if (err) 
      console.log("Error Code: " + err.code);
      console.log("Error Message: " + err.message);
      console.log("Error Errors: " + err.errors);
      console.log("Error Response: " + err.response);
      console.log("  Error Response typeof ",  err.response.constructor.name )

      console.log("================================")

      const e = new GCPErrorHandler(err)
      console.log("Error Code: " + e.code);
      console.log("Error Message: " + e.message);
      console.log("Error Errors: " + e.errors);
      console.log("Error Response: " + e.response);
      console.log("  Error Response typeof ", e.response.constructor.name )
    } else {
      console.log("file data: " + contents);
    }
  });
}
```


```bash
$ node main.js --api=gcs --gcsBucket=fabled-ray-104117-bucket --gcsObject=foo.txt
Error Code: 403
Error Message: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
Error Errors: 
Error Response: [object Object]
  Error Response typeof  PassThrough

================================

Error Code: 403
Error Message: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
Error Errors: 
Error Response: [object Object]
  Error Response typeof  PassThrough
```




#### Compute

The compute engine API surfaces google api errors but no details (since it isn't grpc)

Anyway, by default you will see identical responses


```javascript
function gce(
  projectID,
  mzone,
) {
  const Compute = require('@google-cloud/compute');
  const compute = new Compute();

  const vms = compute.getVMs({
    maxResults: 10,
  }).then(function (value) {
    console.log(`Found ${value.length} VMs!`);
    value.forEach(vm => console.log(vm));
  }, function (err) {

    console.log("Error Code: " + err.code);
    console.log("Error Message: " + err.message);
    console.log("Error Errors: " + err.errors);
    console.log("  Error Errors typeof ",  err.errors.constructor.name )    
    console.log("Error Response: " + err.response);
    console.log("  Error Response typeof ",  err.response.constructor.name )

    console.log("================================")
    const e = new GCPErrorHandler(err)

    console.log("Error Code: " + e.code);
    console.log("Error Message: " + e.message);
    console.log("Error Errors: " + e.errors);
    console.log("  Error Errors typeof ",  err.errors.constructor.name )
    console.log("Error Response: " + e.response);
      console.log("  Error Response typeof ",  err.response.constructor.name )
  });
}
```

```bash
$ node main.js --api=compute    --projectID=fabled-ray-104117 --zone=us-central1-a

Error Code: 403
Error Message: Required 'compute.instances.list' permission for 'projects/mineral-minutia-820'
Error Errors: [object Object]
  Error Errors typeof  Array
Error Response: [object Object]
  Error Response typeof  Gunzip

================================

Error Code: 403
Error Message: Required 'compute.instances.list' permission for 'projects/mineral-minutia-820'
Error Errors: [object Object]
  Error Errors typeof  Array
Error Response: [object Object]
  Error Response typeof  Gunzip
```

### Prettyprint

As mentioned, you can enable the string output of an error's `message` property by enabling the env-var
```bash
$ export GOOGLE_ENABLE_ERROR_DETAIL=true
```
or by passing the `prettyprint=true` value into the constructor



```bash
$ node main.js --api=compute    --projectID=fabled-ray-104117 --zone=us-central1-a

Error Code: 403
Error Message: Required 'compute.instances.list' permission for 'projects/mineral-minutia-820'
Error Errors: [object Object]
  Error Errors typeof  Array
Error Response: [object Object]
  Error Response typeof  Gunzip
================================
Error Code: 403
Error Message: {
    "message": "Required 'compute.instances.list' permission for 'projects/mineral-minutia-820'"
}
Error Errors: [object Object]
  Error Errors typeof  Array
Error Response: [object Object]
  Error Response typeof  Gunzip
```

```bash
$ node main.js --api=gcs --gcsBucket=fabled-ray-104117-bucket --gcsObject=foo.txt

Error Code: 403
Error Message: vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object.
Error Errors: 
Error Response: [object Object]
  Error Response typeof  PassThrough
================================
Error Code: 403
Error Message: {
    "message": "vault-seed-account@mineral-minutia-820.iam.gserviceaccount.com does not have storage.objects.get access to the Google Cloud Storage object."
}
Error Errors: 
Error Response: [object Object]
  Error Response typeof  PassThrough
```

```bash
$ node main.js --api=pubsub -projectID fabled-ray-104117
Error Code: 7
Error Message: 7 PERMISSION_DENIED: User not authorized to perform this action.
Error Errors: undefined
Error Response: undefined
================================
Error Code: 7
Error Message: {
    "message": "7 PERMISSION_DENIED: User not authorized to perform this action."
}
Error Errors: undefined
Error Response: undefined
```


```bash
$ node main.js --api=asset    --checkResource=//cloudresourcemanager.googleapis.com/projects/fabled-ray-104117    --identity=user:admin@esodemoapp2.com    --scope=projects/fabled-ray-104117

Error Code: 7
Error Message: 7 PERMISSION_DENIED: Request denied by Cloud IAM.
Error Errors: undefined
Error Response: undefined

================================

Error Code: 7
Error Message: {
    "message": "7 PERMISSION_DENIED: Request denied by Cloud IAM.",
    "google.rpc.Help": {
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
Error Errors: undefined
Error Response: undefined
Extract google.rpc.*  details
getHelp: 
  To check permissions required for this RPC:
    https://cloud.google.com/asset-inventory/docs/access-control#required_permissions
  To get a valid organization id:
    https://cloud.google.com/resource-manager/docs/creating-managing-organization#retrieving_your_organization_id
  To get a valid folder or project id:
    https://cloud.google.com/resource-manager/docs/creating-managing-folders#viewing_or_listing_folders_and_projects

```