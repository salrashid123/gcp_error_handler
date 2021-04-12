const GCPErrorHandler = require('gcp_error_handler');

function gcs(
  bucketName,
  objectName,
) {
  const { Storage } = require('@google-cloud/storage');
  const storage = new Storage();
  var file = storage.bucket(bucketName).file(objectName);
  file.download(function (err, contents) {
    if (err) {
      //console.log(err)
      console.log("Error Code: " + err.code);
      console.log("Error Message: " + err.message);
      console.log("Error Errors: " + err.errors);
      console.log("Error Response: " + err.response);
      console.log("  Error Response typeof ", err.response.constructor.name)

      console.log("================================")
      const e = new GCPErrorHandler(err)
      //console.log(e)
      console.log("Error Code: " + e.code);
      console.log("Error Message: " + e.message);
      console.log("Error Errors: " + e.errors);
      console.log("Error Response: " + e.response);
      console.log("  Error Response typeof ", e.response.constructor.name)
    } else {
      console.log("file data: " + contents);
    }
  });
}

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
    console.log("  Error Errors typeof ", err.errors.constructor.name)
    console.log("Error Response: " + err.response);
    console.log("  Error Response typeof ", err.response.constructor.name)

    console.log("================================")
    const e = new GCPErrorHandler(err)

    console.log("Error Code: " + e.code);
    console.log("Error Message: " + e.message);
    console.log("Error Errors: " + e.errors);
    console.log("  Error Errors typeof ", err.errors.constructor.name)
    console.log("Error Response: " + e.response);
    console.log("  Error Response typeof ", err.response.constructor.name)
  });
}


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

function asset(
  checkResource,
  identity,
  scope,
) {

  //const { google } = require('@google-cloud/asset/build/protos/protos');
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

    const e = new GCPErrorHandler(err, true)

    console.log("Error Code: " + e.code);
    console.log("Error Message: " + e.message);
    console.log("Error Errors: " + e.errors);
    console.log("Error Response: " + e.response);

    if (e.isGoogleCloudError) {
      console.log("Extract google.rpc.*  details")
      var h = e.getHelp();
      console.log("getHelp: ");
      h.links.forEach(element => {
        console.log(element.description);
        console.log(element.url);
      })
      console.log("getBadRequest: " + e.getBadRequest());
    }
  });
}

const args = require('minimist')(process.argv.slice(2))
api = args['api']

if (api == 'gcs') {
  bucketName = args['gcsBucket']
  objectName = args['gcsObject']
  gcs(bucketName, objectName);
}

if (api == 'asset') {
  checkResource = args['checkResource']
  identity = args['identity']
  scope = args['scope']
  asset(checkResource, identity, scope);
}

if (api == 'pubsub') {
  projectID = args['projectID']
  pubsub(projectID);
}

if (api == 'compute') {
  projectID = args['projectID']
  zone = args['zone']
  gce(projectID, zone);
}