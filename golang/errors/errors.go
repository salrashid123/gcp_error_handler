package errors

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"strconv"

	"github.com/golang/protobuf/jsonpb"
	"github.com/golang/protobuf/ptypes"

	"google.golang.org/api/googleapi"
	"google.golang.org/genproto/googleapis/rpc/errdetails"
	"google.golang.org/grpc/status"
)

// Error contains an error response from the server.
type Error struct {
	Err              error
	PrettyPrint      bool
	IsGoogleAPIError bool
	IsStatusError    bool
	//ReInterpret      bool
	//rootCtx          context.Context
	//client           interface{}
}

const (
	// env-var variable that automatically enables returning the embedded details
	GOOGLE_ENABLE_ERROR_DETAIL = "GOOGLE_ENABLE_ERROR_DETAIL"
)

func (r *Error) Error() string {

	if !r.enableErrorDetails() {
		return r.Err.Error()
	}

	if r.IsStatusError {

		s, err := r.GetStatus()
		if err != nil {
			return errors.New("Error is not a grpc/status.Status").Error()
		}

		m := jsonpb.Marshaler{}
		result, err := m.MarshalToString(s.Proto())
		if err != nil {
			return fmt.Errorf("Could not Unmarshal google.rpc.Error Status [%v]: [%v]", err.Error(), r.Err).Error()
		}

		if r.PrettyPrint {
			var prettyJSON bytes.Buffer
			error := json.Indent(&prettyJSON, []byte(result), "", "\t")
			if error != nil {
				return fmt.Errorf("google.rpc.Error: PrettyPrint(%s)", result).Error()
			}
			return fmt.Errorf("google.rpc.Error: PrettyPrint(%s)", &prettyJSON).Error()
		}
		return fmt.Errorf("google.rpc.Error: %s", result).Error()

	} else if r.IsGoogleAPIError {

		gerror, err := r.GetGoogleAPIError()
		if err != nil {
			return fmt.Errorf("Could not Unmarshal googleapi.Error [%v]: [%v]", err.Error(), r.Err).Error()
		}
		result, _ := json.Marshal(gerror)
		if r.PrettyPrint {
			var prettyJSON bytes.Buffer
			error := json.Indent(&prettyJSON, []byte(result), "", "\t")
			if error != nil {
				return fmt.Errorf("googleapi.Error: PrettyPrint(%s)", result).Error()
			}
			r := prettyJSON.String() + "\n" + "googleapi.Error.Body:\n" + gerror.Body
			return fmt.Errorf("googleapi.Error: PrettyPrint(%s)", r).Error()
		}
		return fmt.Errorf("googleapi.Error: %s", result).Error()
	}

	return r.Err.Error()
}

// GetGoogleAPIError returns base googleauis.Error
func (r *Error) GetGoogleAPIError() (*googleapi.Error, error) {
	if ee, ok := r.Err.(*googleapi.Error); ok {
		return ee, nil
	}
	return nil, errors.New("Error is not a googleapi.Error")
}

// GetStatus returns base google.rpc.Status
func (r *Error) GetStatus() (*status.Status, error) {
	if s, ok := status.FromError(r.Err); ok {
		return s, nil
	}
	return nil, errors.New("Error is not a grpc/status.Status")
}

func (r *Error) enableErrorDetails() bool {
	if r.PrettyPrint {
		return true
	}
	if value, ok := os.LookupEnv(GOOGLE_ENABLE_ERROR_DETAIL); ok {
		v, err := strconv.ParseBool(value)
		if err != nil {
			return false
		}
		return v
	}
	return false

}

// GetGoogleRPCHelp returns google.rpc.Help
func (r *Error) GetGoogleRPCHelp() (*errdetails.Help, error) {
	if s, ok := status.FromError(r.Err); ok {
		for _, d := range s.Proto().Details {
			if d.TypeUrl == "type.googleapis.com/google.rpc.Help" {
				h := &errdetails.Help{}
				err := ptypes.UnmarshalAny(d, h)
				if err != nil {
					return nil, err
				}
				return h, nil
			}
		}
		return nil, errors.New("grpc/status.Status does not include type.googleapis.com/google.rpc.Help")
	}
	return nil, errors.New("Error is not a grpc/status.Status")
}

// GetGoogleRPCErrorInfo returns google.rpc.ErrorInfo
func (r *Error) GetGoogleRPCErrorInfo() (*errdetails.ErrorInfo, error) {
	if s, ok := status.FromError(r.Err); ok {
		for _, d := range s.Proto().Details {
			if d.TypeUrl == "type.googleapis.com/google.rpc.ErrorInfo" {
				h := &errdetails.ErrorInfo{}
				err := ptypes.UnmarshalAny(d, h)
				if err != nil {
					return nil, err
				}
				return h, nil
			}
		}
		return nil, errors.New("grpc/status.Status does not include type.googleapis.com/google.rpc.ErrorInfo")
	}
	return nil, errors.New("Error is not a grpc/status.Status")
}

// GetGoogleRPCQuotaFailure returns google.rpc.QuotaFailure
func (r *Error) GetGoogleRPCQuotaFailure() (*errdetails.QuotaFailure, error) {
	if s, ok := status.FromError(r.Err); ok {
		for _, d := range s.Proto().Details {
			if d.TypeUrl == "type.googleapis.com/google.rpc.QuotaFailure" {
				h := &errdetails.QuotaFailure{}
				err := ptypes.UnmarshalAny(d, h)
				if err != nil {
					return nil, err
				}
				return h, nil
			}
		}
		return nil, errors.New("grpc/status.Status does not include type.googleapis.com/google.rpc.QuotaFailure")
	}
	return nil, errors.New("Error is not a grpc/status.Status")
}

// GetGoogleRPCDebugInfo returns google.rpc.DebugInfo
func (r *Error) GetGoogleRPCDebugInfo() (*errdetails.DebugInfo, error) {
	if s, ok := status.FromError(r.Err); ok {
		for _, d := range s.Proto().Details {
			if d.TypeUrl == "type.googleapis.com/google.rpc.DebugInfo" {
				h := &errdetails.DebugInfo{}
				err := ptypes.UnmarshalAny(d, h)
				if err != nil {
					return nil, err
				}
				return h, nil
			}
		}
		return nil, errors.New("grpc/status.Status does not include type.googleapis.com/google.rpc.DebugInfo")
	}
	return nil, errors.New("Error is not a grpc/status.Status")
}

// GetGoogleRPCRetryInfo returns google.rpc.RetryInfo
func (r *Error) GetGoogleRPCRetryInfo() (*errdetails.RetryInfo, error) {
	if s, ok := status.FromError(r.Err); ok {
		for _, d := range s.Proto().Details {
			if d.TypeUrl == "type.googleapis.com/google.rpc.RetryInfo" {
				h := &errdetails.RetryInfo{}
				err := ptypes.UnmarshalAny(d, h)
				if err != nil {
					return nil, err
				}
				return h, nil
			}
		}
		return nil, errors.New("grpc/status.Status does not include type.googleapis.com/google.rpc.RetryInfo")
	}
	return nil, errors.New("Error is not a grpc/status.Status")
}

// GetGoogleRPCPreconditionFailure returns google.rpc.PreconditionFailure
func (r *Error) GetGoogleRPCPreconditionFailure() (*errdetails.PreconditionFailure, error) {
	if s, ok := status.FromError(r.Err); ok {
		for _, d := range s.Proto().Details {
			if d.TypeUrl == "type.googleapis.com/google.rpc.PreconditionFailure" {
				h := &errdetails.PreconditionFailure{}
				err := ptypes.UnmarshalAny(d, h)
				if err != nil {
					return nil, err
				}
				return h, nil
			}
		}
		return nil, errors.New("grpc/status.Status does not include type.googleapis.com/google.rpc.PreconditionFailure")
	}
	return nil, errors.New("Error is not a grpc/status.Status")
}

// GetGoogleRPCBadRequest returns google.rpc.BadRequest
func (r *Error) GetGoogleRPCBadRequest() (*errdetails.BadRequest, error) {
	if s, ok := status.FromError(r.Err); ok {
		for _, d := range s.Proto().Details {
			if d.TypeUrl == "type.googleapis.com/google.rpc.BadRequest" {
				h := &errdetails.BadRequest{}
				err := ptypes.UnmarshalAny(d, h)
				if err != nil {
					return nil, err
				}
				return h, nil
			}
		}
		return nil, errors.New("grpc/status.Status does not include type.googleapis.com/google.rpc.BadRequest")
	}
	return nil, errors.New("Error is not a grpc/status.Status")
}

// New creates structured Error object of either googleapis.Error or google.rpc.Status
func New(err Error) *Error {

	_, isGoogleAPIError := err.Err.(*googleapi.Error)
	_, isStatusError := status.FromError(err.Err)

	return &Error{
		Err:              err.Err,
		IsGoogleAPIError: isGoogleAPIError,
		IsStatusError:    isStatusError,
		PrettyPrint:      err.PrettyPrint,
	}
}

// *****************************************************************

// The following is not used at the moment.  It is intended to use client-side interpretation of the error using
// the regex match of the current error string coupled with additional data.  It will not replace the existing error details
// but provide additional information, help links to the user

//  The idea is to fill up the errorList with a number (few) ReinterpretedError messages that can be set back to the user as a debug message

// var errorList = []ReinterpretedError{
// 	{
// 		Description: "This error likely means you are doing someing wrong with VPC-SC",
// 		URL:         "https://cloud.google.com/vpc-service-controls/docs/service-perimeters",
// 		errorMatch:  "Request is prohibited by organization's policy. vpcServiceControlsUniqueIdentifier",
// 	},
// 	{
// 		Description: "This error likely means you are doing someing wrong with Access Context",
// 		URL:         "https://cloud.google.com/beyondcorp-enterprise/docs/securing-console-and-apis",
// 		errorMatch:  "https://accounts.google.com/info/servicerestricted",
// 	},
// }

// // ReinterpretedError represents an error added in locally
// type ReinterpretedError struct {
// 	Description string `json:"description,omitempty"`
// 	URL         string `json:"url,omitempty"`
// 	errorMatch  string
// }

// // ReInterpretedErrors represents list of errors added in locally
// type ReInterpretedErrors struct {
// 	Client             string               `json:"client"`
// 	Context            string               `json:"context,omitempty"`
// 	ReinterpretedError []ReinterpretedError `json:"reinterpreted_errors,omitempty"`
// }

// func (r *Error) reInterpret(err string) string {
// 	if !(r.ReInterpret) {
// 		return err
// 	}

// 	fmt.Printf("Reinterpreting Error for client type %s", reflect.TypeOf(r.client))

// 	var rmsg []ReinterpretedError
// 	for _, v := range errorList {
// 		if strings.Contains(err, v.errorMatch) {
// 			rmsg = append(rmsg, v)
// 		}
// 	}

// 	rmsgs := &ReInterpretedErrors{
// 		Client:             reflect.TypeOf(r.client).String(),
// 		ReinterpretedError: rmsg,
// 	}

// 	e, jerr := json.Marshal(rmsgs)
// 	if jerr != nil {
// 		return err
// 	}

// 	return err + "\nReInterpreted Errors:\n" + string(e)
// }

// // New creates structured Error object and enables Reinterpreted Errors
// func NewWithClient(ctx context.Context, client interface{}, err Error) *Error {

// 	_, isGoogleAPIError := err.Err.(*googleapi.Error)
// 	_, isStatusError := status.FromError(err.Err)

// 	return &Error{
// 		Err:              err.Err,
// 		IsGoogleAPIError: isGoogleAPIError,
// 		IsStatusError:    isStatusError,
// 		PrettyPrint:      err.PrettyPrint,
// 		ReInterpret:      err.ReInterpret,
// 		rootCtx:          ctx,
// 		client:           client,
// 	}
// }
