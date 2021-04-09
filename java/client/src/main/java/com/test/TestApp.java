package com.test;

import java.io.IOException;
import java.util.Arrays;

import com.github.salrashid123.gcp_error_handler.ErrorHandler;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceList;
import com.google.auth.http.HttpCredentialsAdapter;
//import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.asset.v1.AnalyzeIamPolicyRequest;
import com.google.cloud.asset.v1.AnalyzeIamPolicyResponse;
import com.google.cloud.asset.v1.AssetServiceClient;
import com.google.cloud.asset.v1.IamPolicyAnalysisQuery;
import com.google.cloud.asset.v1.IamPolicyAnalysisQuery.IdentitySelector;
import com.google.cloud.asset.v1.IamPolicyAnalysisQuery.ResourceSelector;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient.ListTopicsPagedResponse;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.pubsub.v1.ListTopicsRequest;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.Topic;
import com.google.rpc.Help;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class TestApp {

    private static final String constAPI = "api";
    private static final String constGcsBucket = "gcsBucket";
    private static final String constGcsObject = "gcsObject";
    private static final String constCheckResource = "checkResource";
    private static final String constIdentity = "identity";
    private static final String constScope = "scope";
    private static final String constProjectID = "projectID";
    private static final String constZone = "zone";

    public static void main(String[] args) {
        CommandLine commandLine;

        Option optapi = Option.builder(constAPI).required(true).hasArg(true).desc(constAPI).longOpt(constAPI).build();
        Option optgcsBucket = Option.builder(constGcsBucket).required(false).hasArg(true).desc(constGcsBucket)
                .longOpt(constGcsBucket).build();
        Option optgcsObject = Option.builder(constGcsObject).required(false).hasArg(true).desc(constGcsObject)
                .longOpt(constGcsObject).build();
        Option optcheckResource = Option.builder(constCheckResource).required(false).hasArg(true)
                .desc(constCheckResource).longOpt(constCheckResource).build();
        Option optidentity = Option.builder(constIdentity).required(false).hasArg(true).desc(constIdentity)
                .longOpt(constIdentity).build();
        Option optscope = Option.builder(constScope).required(false).hasArg(true).desc(constScope).longOpt(constScope)
                .build();
        Option optprojectID = Option.builder(constProjectID).required(false).hasArg(true).desc(constProjectID)
                .longOpt(constProjectID).build();
        Option optZone = Option.builder(constZone).required(false).hasArg(true).desc(constZone).longOpt(constZone)
                .build();
        Options options = new Options();
        CommandLineParser parser = new DefaultParser();

        options.addOption(optapi);
        options.addOption(optgcsBucket);
        options.addOption(optgcsObject);
        options.addOption(optcheckResource);
        options.addOption(optidentity);
        options.addOption(optscope);
        options.addOption(optprojectID);
        options.addOption(optZone);

        try {
            commandLine = parser.parse(options, args);

            String api = commandLine.getOptionValue(constAPI);

            TestApp tc = new TestApp();

            if (api.equalsIgnoreCase("gcs")) {
                String bucketName = commandLine.getOptionValue(constGcsBucket);
                String objectName = commandLine.getOptionValue(constGcsObject);

                tc.GCS(bucketName, objectName);
            }
            if (api.equalsIgnoreCase("asset")) {
                String checkResource = commandLine.getOptionValue(constCheckResource);
                String identity = commandLine.getOptionValue(constIdentity);
                String scope = commandLine.getOptionValue(constScope);

                tc.Asset(checkResource, identity, scope);
            }
            if (api.equalsIgnoreCase("pubsub")) {
                String projectID = commandLine.getOptionValue(constProjectID);
                tc.PubSub(projectID);
            }
            if (api.equalsIgnoreCase("compute")) {
                String projectID = commandLine.getOptionValue(constProjectID);
                String zone = commandLine.getOptionValue(constZone);
                tc.Compute(projectID, zone);
            }

        } catch (ParseException ex) {
            System.out.println("Could not parse args " + ex);
        }
    }

    public TestApp() {
    }

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
            System.out.println("ApiException: ");
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

    public void Compute(String projectID, String zone) {
        try {

            // String projectId = ServiceOptions.getDefaultProjectId();
            // System.out.println(projectId);
            GoogleCredentials credential = GoogleCredentials.getApplicationDefault();
            if (credential.createScopedRequired()) {
                credential = credential.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
            }

            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            Compute computeService = new Compute.Builder(httpTransport, jsonFactory,
                    new HttpCredentialsAdapter(credential)).setApplicationName("Google-ComputeSample/0.1").build();

            Compute.Instances.List request = computeService.instances().list(projectID, zone);

            InstanceList response;
            do {
                response = request.execute();
                if (response.getItems() == null) {
                    continue;
                }
                for (Instance instance : response.getItems()) {
                    // TODO: Change code below to process each `instance` resource:
                    System.out.println(instance);
                }
                request.setPageToken(response.getNextPageToken());
            } while (response.getNextPageToken() != null);

        } catch (IOException ex) {
            System.out.println(ex);
            System.out.println("getMessage()  " + ex.getMessage());
            System.out.println("===================");
            ErrorHandler e = new ErrorHandler(ex);
            System.out.println(e);
            System.out.println("getMessage()  " + e.getMessage());
        } catch (Exception ex) {
            System.out.println(">>>>>>>>>>>>>>>> " + ex);
        }
    }

    public void GCS(String bucketName, String objectName) {
        try {

            // String projectId = ServiceOptions.getDefaultProjectId();
            // System.out.println(projectId);

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
            System.out.println("Exception:  " + ex.getCause());
        }
    }

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
            System.out.println("ApiException: ");
            System.out.println(" getStatusCode: " + ex.getStatusCode().getCode());
            System.out.println(" Message: " + ex.getMessage());
            System.out.println(" isRetryable: " + ex.isRetryable());

            System.out.println("===================");
            ErrorHandler e = new ErrorHandler(ex, false);
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
            System.out.println(">>>>>>>>>>>>>>>> " + ex);
        }
    }

}
