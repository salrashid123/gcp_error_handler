package com.github.salrashid123.gcp_error_handler;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.BadRequest;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Help;
import com.google.rpc.PreconditionFailure;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.grpc.Metadata;
import io.grpc.Status;

public class ErrorHandler extends Exception {

    private static final long serialVersionUID = -2133257318957488431L;
    private static final String GOOGLE_ENABLE_ERROR_DETAIL = "GOOGLE_ENABLE_ERROR_DETAIL";

    private Throwable cause;
    private com.google.cloud.http.BaseHttpServiceException apiError;
    private com.google.api.gax.rpc.ApiException cloudError; // com.google.api.gax.rpc.PermissionDeniedException
    // private com.google.api.client.googleapis.json.GoogleJsonResponseException
    // googleAPIError;
    private com.google.api.client.http.HttpResponseException googleAPIError;
    private boolean prettyPrint = false;
    private boolean isGoogleCloudError = true;

    private com.google.rpc.Help googleRPCHelp;
    private com.google.rpc.BadRequest googleRPCBadRequest;
    private com.google.rpc.ErrorInfo googleRPCErrorInfo;
    private com.google.rpc.PreconditionFailure googleRPCPreconditionFailure;

    public ErrorHandler(Throwable cause, boolean prettyPrint) {
        this.handler(cause, prettyPrint);
    }

    public ErrorHandler(Throwable cause) {
        this.handler(cause, false);
    }

    public boolean isGoogleCloudError() {
        return this.isGoogleCloudError;
    }

    private void handler(Throwable cause, boolean prettyPrint) {
        this.cause = cause;
        String verbosity = System.getenv(GOOGLE_ENABLE_ERROR_DETAIL);
        if (verbosity != null && verbosity.equalsIgnoreCase("true")) {
            this.prettyPrint = true;
        } else {
            this.prettyPrint = prettyPrint;
        }

        // System.out.println("Class --> " +
        // cause.getClass().getSuperclass().getName());
        if (cause.getClass().getSuperclass() == com.google.cloud.http.BaseHttpServiceException.class) {
            isGoogleCloudError = false;
            this.apiError = (com.google.cloud.http.BaseHttpServiceException) cause;
        } else if (cause.getClass().getSuperclass() == com.google.api.gax.rpc.ApiException.class) {
            isGoogleCloudError = true;
            this.cloudError = (com.google.api.gax.rpc.ApiException) cause;

            // Status ss = Status.fromThrowable(cause);
            Metadata m = Status.trailersFromThrowable(cause);
            // grpc-status-details-bin grpc-server-stats-bin
            for (String k : m.keys()) {
                if (k.equals("google.rpc.help-bin")) {
                    byte[] byt_help = m.get(Metadata.Key.of("google.rpc.help-bin", Metadata.BINARY_BYTE_MARSHALLER));

                    try {
                        this.googleRPCHelp = Help.parseFrom(byt_help);
                    } catch (InvalidProtocolBufferException ioex) {
                        System.out.println("err" + ioex);
                        return;
                    }
                }
                if (k.equals("google.rpc.badrequest-bin")) {
                    byte[] byt_help = m
                            .get(Metadata.Key.of("google.rpc.badrequest-bin", Metadata.BINARY_BYTE_MARSHALLER));
                    BadRequest h = null;
                    try {
                        this.googleRPCBadRequest = BadRequest.parseFrom(byt_help);
                    } catch (InvalidProtocolBufferException ioex) {
                        System.out.println("err" + ioex);
                        return;
                    }
                }
            }
        } else if (cause.getClass().getSuperclass() == com.google.api.client.http.HttpResponseException.class) {
            isGoogleCloudError = false;
            this.googleAPIError = (com.google.api.client.http.HttpResponseException) cause;
        } else {
            // TODO: throw exception somehow
            System.out.println("ERROR: unable to parse exception type");
            return;
        }

        if (isGoogleCloudError) {
            Metadata m = Status.trailersFromThrowable(cause);
            // grpc-status-details-bin grpc-server-stats-bin
            for (String k : m.keys()) {
                if (k.equals("google.rpc.help-bin")) {
                    byte[] byt_help = m.get(Metadata.Key.of("google.rpc.help-bin", Metadata.BINARY_BYTE_MARSHALLER));
                    Help h = null;
                    try {
                        h = Help.parseFrom(byt_help);
                        this.googleRPCHelp = h;
                    } catch (InvalidProtocolBufferException ioex) {
                        System.out.println("err" + ioex);
                        return;
                    }
                }
                if (k.equals("google.rpc.badrequest-bin")) {
                    byte[] byt_help = m
                            .get(Metadata.Key.of("google.rpc.badrequest-bin", Metadata.BINARY_BYTE_MARSHALLER));
                    BadRequest h = null;
                    try {
                        h = BadRequest.parseFrom(byt_help);
                        this.googleRPCBadRequest = h;
                    } catch (InvalidProtocolBufferException ioex) {
                        System.out.println("err" + ioex);
                        return;
                    }
                }
                if (k.equals("google.rpc.errorinfo-bin")) {
                    byte[] byt_help = m
                            .get(Metadata.Key.of("google.rpc.errorinfo-bin", Metadata.BINARY_BYTE_MARSHALLER));
                    ErrorInfo h = null;
                    try {
                        h = ErrorInfo.parseFrom(byt_help);
                        this.googleRPCErrorInfo = h;
                    } catch (InvalidProtocolBufferException ioex) {
                        System.out.println("err" + ioex);
                        return;
                    }
                }
                if (k.equals("google.rpc.preconditionfailure-bin")) {
                    byte[] byt_help = m.get(
                            Metadata.Key.of("google.rpc.preconditionfailure-bin", Metadata.BINARY_BYTE_MARSHALLER));
                    PreconditionFailure h = null;
                    try {
                        h = PreconditionFailure.parseFrom(byt_help);
                        this.googleRPCPreconditionFailure = h;
                    } catch (InvalidProtocolBufferException ioex) {
                        System.out.println("err" + ioex);
                        return;
                    }
                }
            }
        } else {

        }
    }

    // https://googleapis.dev/java/google-http-client/latest/index.html?com/google/api/client/http/HttpResponseException.html
    // https://googleapis.dev/java/gax/latest/com/google/api/gax/rpc/ApiException.html

    public String getContent() {
        if (this.googleAPIError != null)
            return this.googleAPIError.getContent();
        return null;
    }

    public com.google.api.client.http.HttpHeaders getHeaders() {
        if (this.googleAPIError != null)
            return this.googleAPIError.getHeaders();
        return null;
    }

    public String getStatusMessage() {
        if (this.googleAPIError != null)
            return this.googleAPIError.getStatusMessage();
        return null;
    }

    public boolean isSuccessStatusCode() {
        if (this.googleAPIError != null)
            return this.googleAPIError.isSuccessStatusCode();
        return false;
    }

    public com.google.api.gax.rpc.StatusCode getStatusCode() {
        if (this.cloudError != null)
            return this.cloudError.getStatusCode();
        return null;
    }

    public com.google.rpc.Help getGoogleRPCHelp() {
        if (this.cloudError != null)
            return this.googleRPCHelp;
        return null;
    }

    public com.google.rpc.ErrorInfo getGoogleRPCErrorInfo() {
        if (this.cloudError != null)
            return this.googleRPCErrorInfo;
        return null;
    }

    public com.google.rpc.PreconditionFailure getGoogleRPCPreconditionFailure() {
        if (this.cloudError != null)
            return this.googleRPCPreconditionFailure;
        return null;
    }

    public com.google.rpc.BadRequest getGoogleRPCBadRequest() {
        if (this.cloudError != null)
            return this.googleRPCBadRequest;
        return null;
    }

    public boolean isRetryable() {
        if (this.cloudError != null)
            return this.cloudError.isRetryable();
        return false;
    }

    public int getCode() {
        if (this.apiError != null)
            return this.apiError.getCode();
        return -1;
    }

    public String getDebugInfo() {
        if (this.apiError != null)
            return this.apiError.getDebugInfo();
        return null;
    }

    public String getLocation() {
        if (this.apiError != null)
            return this.apiError.getLocation();
        return null;
    }

    public String getReason() {
        if (this.apiError != null)
            return this.apiError.getReason();
        return null;
    }

    @Override
    public Throwable getCause() {
        return this.cause.getCause();
    }

    @Override
    public String getMessage() {
        if (this.isGoogleCloudError && this.prettyPrint) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            JsonObject jo = new com.google.gson.JsonObject();
            jo.addProperty("getMessage", this.cloudError.getMessage());

            // // TODO: make formatted json....
            
            jo.addProperty("getStatusCode.getCode.name", this.cloudError.getStatusCode().getCode().name());
            if (this.getGoogleRPCHelp() != null) {
                jo.addProperty("com.google.rpc.Help", this.getGoogleRPCHelp().toString().replace("\n", ""));
            }
            if (this.getGoogleRPCErrorInfo() != null) {
                jo.addProperty("com.google.rpc.ErrorInfo", this.getGoogleRPCErrorInfo().toString().replace("\n", ""));
            }
            if (this.getGoogleRPCPreconditionFailure() != null) {
                jo.addProperty("com.google.rpc.PreconditionFailure",
                        this.getGoogleRPCPreconditionFailure().toString().replace("\n", ""));
            }
            if (this.getGoogleRPCBadRequest() != null) {
                jo.addProperty("com.google.rpc.BadRequest", this.getGoogleRPCBadRequest().toString().replace("\n", ""));
            }
            return gson.toJson(jo);
        }
        return this.cause.getMessage();
    }

    @Override
    public String toString() {
        if (this.isGoogleCloudError && this.prettyPrint) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            JsonObject jo = new com.google.gson.JsonObject();
            jo.addProperty("getMessage", this.cloudError.getMessage());

            // // TODO: make formatted json....
            
            jo.addProperty("getStatusCode.getCode.name", this.cloudError.getStatusCode().getCode().name());
            if (this.getGoogleRPCHelp() != null) {
                jo.addProperty("com.google.rpc.Help", this.getGoogleRPCHelp().toString().replace("\n", ""));
            }
            if (this.getGoogleRPCErrorInfo() != null) {
                jo.addProperty("com.google.rpc.ErrorInfo", this.getGoogleRPCErrorInfo().toString().replace("\n", ""));
            }
            if (this.getGoogleRPCPreconditionFailure() != null) {
                jo.addProperty("com.google.rpc.PreconditionFailure",
                        this.getGoogleRPCPreconditionFailure().toString().replace("\n", ""));
            }
            if (this.getGoogleRPCBadRequest() != null) {
                jo.addProperty("com.google.rpc.BadRequest", this.getGoogleRPCBadRequest().toString().replace("\n", ""));
            }
            return gson.toJson(jo);
        }        
        return this.cause.toString();
    }

}