package main

import (
	"context"
	"flag"
	"io"
	"os"

	asset "cloud.google.com/go/asset/apiv1"
	"cloud.google.com/go/storage"

	"log"

	"github.com/golang/protobuf/ptypes"
	"google.golang.org/api/googleapi"
	errdetails "google.golang.org/genproto/googleapis/rpc/errdetails"

	"google.golang.org/grpc/status"

	assetpb "google.golang.org/genproto/googleapis/cloud/asset/v1"
)

const (
	resourceManagerProjectsRegex = "//cloudresourcemanager.googleapis.com/projects/(.+)"
)

var (
	scope         = flag.String("scope", "", "Scope to check")
	checkResource = flag.String("checkResource", "", "Resource to check")
	identity      = flag.String("identity", "", "Permission to check")
	gcsBucket     = flag.String("gcsBucket", "fabled-ray-104117-bucket", "GCS Bucket to access")
	gcsObject     = flag.String("gcsObject", "foo.txt", "GCS object to access")

	mode = flag.String("mode", "rest", "Test basic|extended")
)

func init() {
}

func main() {
	flag.Parse()

	if *mode != "basic" && *mode != "extended" {
		log.Fatal("Mode must be either basic or extended")
	}

	ctx := context.Background()

	if *mode == "basic" {

		log.Println("================ Using (GCS) ======================\n")
		if *gcsBucket == "" || *gcsObject == "" {
			log.Fatal("Must specify scope,checkResource,identity")
		}

		storageClient, err := storage.NewClient(ctx)
		if err != nil {
			log.Fatal("%v", err)
		}

		bkt := storageClient.Bucket(*gcsBucket)
		obj := bkt.Object(*gcsObject)
		r, err := obj.NewReader(ctx)
		if err != nil {
			err := handleError(err)
			if err != nil {
				//log.Printf("%v", err)
			}
			return
		}
		defer r.Close()

		if _, err := io.Copy(os.Stdout, r); err != nil {
			log.Fatal(err)
		}
	} else {
		log.Printf("================ Using  (AssetManager) ======================\n")
		if *scope == "" || *checkResource == "" || *identity == "" {
			log.Fatal("Must specify scope,checkResource,identity")
		}

		assetClient, err := asset.NewClient(ctx)
		if err != nil {
			err := handleError(err)
			if err != nil {
				log.Printf("%v", err)
			}
			return
		}

		// https://cloud.google.com/asset-inventory/docs/resource-name-format
		// https://cloud.google.com/asset-inventory/docs/supported-asset-types#analyzable_asset_types

		req := &assetpb.AnalyzeIamPolicyRequest{
			AnalysisQuery: &assetpb.IamPolicyAnalysisQuery{
				Scope: *scope,
				ResourceSelector: &assetpb.IamPolicyAnalysisQuery_ResourceSelector{
					FullResourceName: *checkResource,
				},
				IdentitySelector: &assetpb.IamPolicyAnalysisQuery_IdentitySelector{
					Identity: *identity,
				},
			},
		}
		resp, err := assetClient.AnalyzeIamPolicy(ctx, req)
		if err != nil {
			err := handleError(err)
			if err != nil {
				log.Fatal(err)
			}
			return
		}

		for _, result := range resp.MainAnalysis.AnalysisResults {
			for _, acl := range result.AccessControlLists {
				log.Printf("    AnalysisResults.Resources %s", acl.Resources)
				log.Printf("    AnalysisResults.Accesses %s", acl.Accesses)
			}
		}
	}

}

func handleError(err error) error {

	if ee, ok := err.(*googleapi.Error); ok {
		log.Printf("Error Code: %d\n", ee.Code)
		log.Printf("Error Message: %s\n", ee.Message)
		log.Printf("Error Details: %d\n", ee.Details)
		log.Printf("Error Body: %s\n", ee.Body)
		log.Println("Errors: ")
		for _, e := range ee.Errors {
			log.Println("  Error Reason: %s\n", e.Reason)
			log.Println("  Error Message: %s\n", e.Message)
		}
	}

	if s, ok := status.FromError(err); ok {
		for _, d := range s.Proto().Details {

			log.Printf("Error Type %s\n", d.TypeUrl)
			// https://github.com/googleapis/googleapis/blob/master/google/rpc/error_details.proto
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
			case "type.googleapis.com/google.rpc.ErrorInfo":
				h := &errdetails.ErrorInfo{}
				err = ptypes.UnmarshalAny(d, h)
				if err != nil {
					return err
				}
				log.Printf("    ErrorInfo: Reason %s\n", h.Reason)
				log.Printf("    ErrorInfo: Domain %s\n", h.Domain)
				log.Printf("    ErrorInfo: Metadata %s\n", h.Metadata)

			case "type.googleapis.com/google.rpc.QuotaFailure":
				h := &errdetails.QuotaFailure{}
				err = ptypes.UnmarshalAny(d, h)
				if err != nil {
					return err
				}
				log.Printf("    QuotaFailure.Violations: %v\n", h.Violations)
			case "type.googleapis.com/google.rpc.DebugInfo":
				h := &errdetails.DebugInfo{}
				err = ptypes.UnmarshalAny(d, h)
				if err != nil {
					return err
				}
				log.Printf("  DebugInfo: %v\n", h.Detail)

			default:
				log.Printf("Don't know type %T\n", d.TypeUrl)
			}
		}
	}
	return err
}
