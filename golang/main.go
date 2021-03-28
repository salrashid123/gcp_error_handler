package main

import (
	"context"
	"flag"
	"fmt"
	"io"
	"os"

	asset "cloud.google.com/go/asset/apiv1"
	"cloud.google.com/go/pubsub"
	"cloud.google.com/go/storage"
	"golang.org/x/oauth2/google"
	compute "google.golang.org/api/compute/v1"
	"google.golang.org/api/iterator"

	"log"

	gcperrors "github.com/salrashid123/gcp_error_handler/golang/errors"
	assetpb "google.golang.org/genproto/googleapis/cloud/asset/v1"
)

const (
	resourceManagerProjectsRegex = "//cloudresourcemanager.googleapis.com/projects/(.+)"
)

var (
	scope         = flag.String("scope", "", "Scope to check")
	checkResource = flag.String("checkResource", "", "Resource to check")
	identity      = flag.String("identity", "", "Permission to check")

	api         = flag.String("api", "", "gcs|compute|pubsub|asset")
	gcsBucket   = flag.String("gcsBucket", "fabled-ray-104117-bucket", "GCS Bucket to access")
	gcsObject   = flag.String("gcsObject", "foo.txt", "GCS object to access")
	computeZone = flag.String("computeZone", "us-central1-a", "Compute Engine Zone")
	projectID   = flag.String("projectID", "", "ProjectID")
)

func init() {
}

func runTestCases(err error) {
	fmt.Printf("Default:\n%v\n", err)
	fmt.Println("------------------------------------")
	gerr := gcperrors.New(gcperrors.Error{
		Err: err,
	})
	fmt.Printf("Default Proposed:\n%v\n", gerr)
	fmt.Println("------------------------------------")

	os.Setenv("GOOGLE_ENABLE_ERROR_DETAIL", "true")

	defEnv := gcperrors.New(gcperrors.Error{
		//Err: gerr.Err,
		Err: err,
	})
	fmt.Printf("Default Proposed with env-var:\n %v\n", defEnv)
	fmt.Println("------------------------------------")

	os.Setenv("GOOGLE_ENABLE_ERROR_DETAIL", "false")
	prettyErrors := gcperrors.New(gcperrors.Error{
		//Err: gerr.Err,
		Err:         err,
		PrettyPrint: true,
	})
	fmt.Printf("Proposed PrettyPrint:\n %v\n", prettyErrors)
	fmt.Println("------------------------------------")

	if gerr.IsGoogleAPIError {
		fmt.Printf("Proposed googleapi.Error:\n %v\n", gerr)
		prettyErrors := gcperrors.New(gcperrors.Error{
			//Err: gerr.Err,
			Err:         err,
			PrettyPrint: true,
		})
		fmt.Println("------------------------------------")
		fmt.Printf("Proposed PrettyPrint:\n %v\n", prettyErrors)
	}

	if gerr.IsStatusError {
		fmt.Printf("Proposed google.rpc.Help:\n")
		h, err := gerr.GetGoogleRPCHelp()
		if err != nil {
			fmt.Printf("%v\n", err)
		} else {

			for _, v := range h.Links {
				fmt.Printf("  google.rpc.Help.Description: %s\n", v.Description)
				fmt.Printf("  google.rpc.Help.Url: %s\n", v.Url)
			}
		}
		fmt.Printf("Proposed google.rpc.BadRequest:\n")
		b, err := gerr.GetGoogleRPCBadRequest()
		if err != nil {
			fmt.Printf("%v\n", err)
		} else {

			for _, v := range b.FieldViolations {
				fmt.Printf("  google.rpc.BadRequest.FieldViolations.Field: %s\n", v.Field)
				fmt.Printf("  google.rpc.BadRequest.FieldViolations.Description: %s\n", v.Description)
			}
		}
	}
	return
}

func main() {
	flag.Parse()

	if *api != "gcs" && *api != "compute" && *api != "pubsub" && *api != "asset" {
		log.Fatal("api must be either gcs,compute, pubsub or asset")
	}

	ctx := context.Background()

	if *api == "compute" {

		log.Println("================ Using (Compute Engine) ======================")
		if *computeZone == "" || *projectID == "" {
			log.Fatal("ComputeZone and ProjectID must be set")
		}
		c, err := google.DefaultClient(ctx, compute.CloudPlatformScope)
		if err != nil {
			log.Fatal(err)
		}

		computeService, err := compute.New(c)
		if err != nil {
			log.Fatal(err)
		}

		req := computeService.Instances.List(*projectID, *computeZone)
		if err := req.Pages(ctx, func(page *compute.InstanceList) error {
			for _, instance := range page.Items {
				fmt.Printf("%#v\n", instance)
			}
			return nil
		}); err != nil {
			runTestCases(err)
			return

		}
	} else if *api == "gcs" {
		log.Println("================ Using (GCS) ======================")
		if *gcsBucket == "" || *gcsObject == "" {
			log.Fatal("Must specify scope,checkResource,identity")
		}

		storageClient, err := storage.NewClient(ctx)
		if err != nil {
			log.Fatalf("%v", err)
		}

		bkt := storageClient.Bucket(*gcsBucket)
		obj := bkt.Object(*gcsObject)
		r, err := obj.NewReader(ctx)
		if err != nil {
			runTestCases(err)
			return
		}
		defer r.Close()

		if _, err := io.Copy(os.Stdout, r); err != nil {
			log.Fatal(err)
		}
	} else if *api == "pubsub" {

		var topics []*pubsub.Topic
		client, err := pubsub.NewClient(ctx, *projectID)
		if err != nil {
			log.Printf("%v", err)
			return
		}
		it := client.Topics(ctx)
		for {
			topic, err := it.Next()
			if err == iterator.Done {
				break
			}
			if err != nil {
				runTestCases(err)
				return
			}
			topics = append(topics, topic)
		}

	} else if *api == "asset" {
		log.Printf("================ Using  (AssetManager) ======================\n")
		if *scope == "" || *checkResource == "" || *identity == "" {
			log.Fatal("Must specify scope,checkResource,identity")
		}

		assetClient, err := asset.NewClient(ctx)
		if err != nil {
			err := gcperrors.New(gcperrors.Error{
				Err:         err,
				PrettyPrint: false,
			})

			fmt.Printf("%v\n", err)
			return
		}

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
			runTestCases(err)
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
