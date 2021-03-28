package com.test;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import com.google.rpc.Help;
import com.google.rpc.ErrorInfo;
import com.google.rpc.BadRequest;

import com.google.cloud.storage.StorageException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import io.grpc.Metadata;

import io.grpc.Status;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.cloud.asset.v1.AssetServiceClient;
import com.google.cloud.asset.v1.AnalyzeIamPolicyRequest;
import com.google.cloud.asset.v1.AnalyzeIamPolicyResponse;
import com.google.cloud.asset.v1.IamPolicyAnalysisQuery;
import com.google.cloud.asset.v1.IamPolicyAnalysisQuery.IdentitySelector;
import com.google.cloud.asset.v1.IamPolicyAnalysisQuery.ResourceSelector;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.List;

public class TestApp {

    private static final String constMode = "mode";
    private static final String constGcsBucket = "gcsBucket";
    private static final String constGcsObject = "gcsObject";
    private static final String constCheckResource = "checkResource";
    private static final String constIdentity = "identity";
    private static final String constScope = "scope";

    public static void main(String[] args) {
        CommandLine commandLine;

        Option optmode = Option.builder(constMode).required(true).hasArg(true).desc(constMode).longOpt(constMode)
                .build();
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
        Options options = new Options();
        CommandLineParser parser = new DefaultParser();

        options.addOption(optmode);
        options.addOption(optgcsBucket);
        options.addOption(optgcsObject);
        options.addOption(optcheckResource);
        options.addOption(optidentity);
        options.addOption(optscope);

        try {
            commandLine = parser.parse(options, args);

            String mode = commandLine.getOptionValue(constMode);

            TestApp tc = new TestApp();

            if (mode.equalsIgnoreCase("basic")) {
                String bucketName = commandLine.getOptionValue(constGcsBucket);
                String objectName = commandLine.getOptionValue(constGcsObject);

                tc.Basic(bucketName, objectName);
            }
            if (mode.equalsIgnoreCase("extended")) {
                String checkResource = commandLine.getOptionValue(constCheckResource);
                String identity = commandLine.getOptionValue(constIdentity);
                String scope = commandLine.getOptionValue(constScope);

                tc.Extended(checkResource, identity, scope);
            }

        } catch (ParseException ex) {
            System.out.println("Could not parse args " + ex);
        }
    }

    public TestApp() {
    }

    public void Basic(String bucketName, String objectName) {
        try {

            // String projectId = ServiceOptions.getDefaultProjectId();
            // System.out.println(projectId);

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
    }

    public void Extended(String checkResource, String identity, String scope) {
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

            // } catch (com.google.api.gax.rpc.ApiException ex) {
            // System.out.println("ApiException: ");
            // System.out.println(" getStatusCode: " + ex.getStatusCode().getCode());
            // System.out.println(" Message: " + ex.getMessage());
        } catch (Exception ex) {
            // https://grpc.github.io/grpc-java/javadoc/io/grpc/Status.html
            System.out.println("Exception:");
            Status ss = Status.fromThrowable(ex);
            System.out.println("  Status.getCode:  " + ss.getCode().value());
            System.out.println("  Status.getDescription:  " + ss.getDescription());
            Metadata m = Status.trailersFromThrowable(ex);
            // grpc-status-details-bin grpc-server-stats-bin
            for (String k : m.keys()) {
                System.out.println("   Parsing: " + k);
                if (k.equals("google.rpc.help-bin")) {
                    byte[] byt_help = m.get(Metadata.Key.of("google.rpc.help-bin", Metadata.BINARY_BYTE_MARSHALLER));
                    Help h = null;
                    try {
                        h = Help.parseFrom(byt_help);
                    } catch (InvalidProtocolBufferException ioex) {
                        System.out.println("err" + ioex);
                        return;
                    }
                    for (Help.Link l : h.getLinksList()) {
                        System.out.println("     Exception Link getDescription:  " + l.getDescription());
                        System.out.println("     Exception Link getUrl:  " + l.getUrl());
                    }
                }
                if (k.equals("google.rpc.badrequest-bin")) {
                    byte[] byt_help = m.get(Metadata.Key.of("google.rpc.badrequest-bin", Metadata.BINARY_BYTE_MARSHALLER));
                    BadRequest h = null;
                    try {
                        h = BadRequest.parseFrom(byt_help);
                    } catch (InvalidProtocolBufferException ioex) {
                        System.out.println("err" + ioex);
                        return;
                    }
                    System.out.println("BadRequest:" + h);
                }                
            }
            
        }
    }

}
