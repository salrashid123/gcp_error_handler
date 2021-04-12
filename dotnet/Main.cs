using System;
using Google.Cloud.Storage.V1;
using Microsoft.Extensions.CommandLineUtils;

using Google.Apis.Requests;
using Google.Apis.Util;
using Google.Apis.Auth.OAuth2;
using Google.Api.Gax.ResourceNames;
using Google.Cloud.Asset.V1;
using Google.Protobuf.WellKnownTypes;
using System.Collections.Generic;
using Google.Cloud.PubSub.V1;
using static Google.Rpc.Help;
using Google.Apis.Compute.v1;
using Google.Apis.Services;
using Newtonsoft.Json;
using Grpc.Core;
using Data = Google.Apis.Compute.v1.Data;
using System.IO;
using System.Threading.Tasks;

using Github.Salrashid123.GCPErrorHandler;
namespace dotnet
{
    class Program
    {

        static void Main(string[] args)
        {

            var cmd = new CommandLineApplication();
            var argApi = cmd.Option("--api <value>", "api", CommandOptionType.SingleValue);
            var argGcsBucket = cmd.Option("--gcsBucket <value>", "gcsBucket", CommandOptionType.SingleValue);
            var argGcsObject = cmd.Option("--gcsObject <value>", "gcsObject", CommandOptionType.SingleValue);
            var argScope = cmd.Option("--scope <value>", "scope", CommandOptionType.SingleValue);
            var argCheckResource = cmd.Option("--checkResource <value>", "checkResource", CommandOptionType.SingleValue);
            var argIdentity = cmd.Option("--identity <value>", "identity", CommandOptionType.SingleValue);
            var argProjectID = cmd.Option("--projectID <value>", "projectID", CommandOptionType.SingleValue);
            var argZone = cmd.Option("--zone <value>", "zone", CommandOptionType.SingleValue);

            cmd.HelpOption("-? | -h | --help");
            cmd.Execute(args);

            Program p = new Program();
            if (argApi.Value() == "gcs")
            {
                p.GCS(argGcsBucket.Value(), argGcsObject.Value());
            }
            if (argApi.Value() == "asset")
            {
                p.Asset(argScope.Value(), argCheckResource.Value(), argIdentity.Value());
            }
            if (argApi.Value() == "compute")
            {
                p.Compute(argProjectID.Value(), argZone.Value());
            }
            if (argApi.Value() == "pubsub")
            {
                p.PubSub(argProjectID.Value());
            }
        }
        private void Asset(string scope, string checkResource, string identity)
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

                Console.WriteLine("=======================================");
                GCPErrorHandler g = new GCPErrorHandler(e);
                Console.WriteLine("Message: " + g.Message);
                Console.WriteLine("Status: " + g.Status);

                Console.WriteLine("Status.StatusCode: " + g.Status.StatusCode);
                Console.WriteLine("Status.Detail: " + g.Status.Detail);

                if (g.isGoogleCloudError)
                {

                    Google.Rpc.Help h = g.GetGoogleRpcHelp();
                    foreach (Types.Link l in h.Links)
                    {
                        Console.WriteLine("    Description: " + l.Description);
                        Console.WriteLine("    URL: " + l.Url);
                    }
                }
            }
        }
        private void GCS(string bucketName, string objectName)
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

                Console.WriteLine("=======================================");

                GCPErrorHandler g = new GCPErrorHandler(e);

                Console.WriteLine("Message: " + g.Message);
                Console.WriteLine("ServiceName: " + g.ServiceName);
                Console.WriteLine("Source: " + g.Source);
                Console.WriteLine("HttpStatusCode: " + g.HttpStatusCode);
                Console.WriteLine("HelpLink: " + g.HelpLink);
                Console.WriteLine("Error: " + g.Error);
            }
        }
        private void PubSub(string projectID)
        {
            PublisherServiceApiClient publisher = PublisherServiceApiClient.Create();
            try
            {

                ProjectName projectName = ProjectName.FromProject(projectID);
                IEnumerable<Topic> topics = publisher.ListTopics(projectName);
                foreach (Topic t in topics)
                {
                    Console.Write(t.Name);
                }
            }
            catch (Grpc.Core.RpcException e)
            {
                Console.WriteLine("Message: " + e.Message);
                Console.WriteLine("Status: " + e.Status);

                Console.WriteLine("Status.StatusCode: " + e.Status.StatusCode);
                Console.WriteLine("Status.Detail: " + e.Status.Detail);

                Console.WriteLine("=======================================");
                GCPErrorHandler g = new GCPErrorHandler(e);
                Console.WriteLine("Message: " + g.Message);
                Console.WriteLine("Status: " + g.Status);

                Console.WriteLine("Status.StatusCode: " + g.Status.StatusCode);
                Console.WriteLine("Status.Detail: " + g.Status.Detail);
            }
        }

        private void Compute(string projectID, string zone)
        {
            GoogleCredential credential = Task.Run(() => GoogleCredential.GetApplicationDefaultAsync()).Result;
            if (credential.IsCreateScopedRequired)
            {
                credential = credential.CreateScoped("https://www.googleapis.com/auth/cloud-platform");
            }
            ComputeService computeService = new ComputeService(new BaseClientService.Initializer
            {
                HttpClientInitializer = credential,
                ApplicationName = "Google-ComputeSample/0.1",
            });

            try
            {
                InstancesResource.ListRequest request = computeService.Instances.List(projectID, zone);

                Data.InstanceList response;
                do
                {
                    response = request.Execute();
                    if (response.Items == null)
                    {
                        continue;
                    }
                    Console.WriteLine(">>> # Instances " + response.Items.ToString());
                } while (response.NextPageToken != null);

            }
            catch (Google.GoogleApiException e)
            {
                Console.WriteLine("Message: " + e.Message);
                Console.WriteLine("ServiceName: " + e.ServiceName);
                Console.WriteLine("Source: " + e.Source);
                Console.WriteLine("HttpStatusCode: " + e.HttpStatusCode);
                Console.WriteLine("HelpLink: " + e.HelpLink);
                Console.WriteLine("Error: " + e.Error);

                Console.WriteLine("=======================================");

                GCPErrorHandler g = new GCPErrorHandler(e);
                Console.WriteLine("Message: " + g.Message);
                Console.WriteLine("ServiceName: " + g.ServiceName);
                Console.WriteLine("Source: " + g.Source);
                Console.WriteLine("HttpStatusCode: " + g.HttpStatusCode);
                Console.WriteLine("HelpLink: " + g.HelpLink);
                Console.WriteLine("Error: " + g.Error);
            }
        }

    }

}
