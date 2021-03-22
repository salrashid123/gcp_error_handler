using System;
using Google.Cloud.Storage.V1;
using Microsoft.Extensions.CommandLineUtils;

using Google.Apis.Requests;
using Google.Apis.Util;

using Google.Api.Gax.ResourceNames;
using Google.Cloud.Asset.V1;
using Google.Protobuf.WellKnownTypes;

using System.IO;
namespace dotnet
{
    class Program
    {
        static void Main(string[] args)
        {

            var cmd = new CommandLineApplication();
            var argMode = cmd.Option("--mode <value>", "mode", CommandOptionType.SingleValue);
            var argGcsBucket = cmd.Option("--gcsBucket <value>", "gcsBucket", CommandOptionType.SingleValue);
            var argGcsObject = cmd.Option("--gcsObject <value>", "gcsObject", CommandOptionType.SingleValue);
            var argScope = cmd.Option("--scope <value>", "scope", CommandOptionType.SingleValue);
            var argCheckResource = cmd.Option("--checkResource <value>", "checkResource", CommandOptionType.SingleValue);
            var argIdentity = cmd.Option("--identity <value>", "identity", CommandOptionType.SingleValue);

            cmd.HelpOption("-? | -h | --help");
            cmd.Execute(args);

            Program p = new Program();
            if (argMode.Value() == "basic")
            {
                p.Basic(argGcsBucket.Value(), argGcsObject.Value());
            }
            if (argMode.Value() == "extended")
            {
                p.Extended(argScope.Value(), argCheckResource.Value(), argIdentity.Value());
            }
        }
        private void Extended(string scope, string checkResource, string identity)
        {
            var client = AssetServiceClient.Create();
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
                foreach (Grpc.Core.Metadata.Entry m in e.Trailers ){
                    Console.WriteLine(" metadata " + m);
                }
            }
        }

        private void Basic(string bucketName, string objectName)
        {
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
        }
    }


}
