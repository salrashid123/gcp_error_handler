

function Basic(
  bucketName,
  objectName,
) {
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
}


function Extended(
  checkResource,
  identity,
  scope,
) {
  const util = require('util');
  //const { google } = require('@google-cloud/asset/build/protos/protos');
  const {AssetServiceClient} = require('@google-cloud/asset');

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

const args = require('minimist')(process.argv.slice(2))
mode = args['mode']

if (mode == 'basic') {
  bucketName = args['gcsBucket']
  objectName = args['gcsObject']
  Basic(bucketName,objectName);
}

if (mode == 'extended') {
  checkResource = args['checkResource']
  identity = args['identity']
  scope = args['scope']
  Extended(checkResource,identity,scope);
}
