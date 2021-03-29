module main

go 1.15

require (
	cloud.google.com/go v0.79.0
	cloud.google.com/go/storage v1.10.0
	github.com/gogo/protobuf v1.3.2 // indirect
	//github.com/salrashid123/gcp_error_handler/golang/errors v0.0.0-20210328155724-0b05656a6901 // indirect
	github.com/salrashid123/gcp_error_handler/golang/errors v0.0.0-00010101000000-000000000000 // indirect

)

replace github.com/salrashid123/gcp_error_handler/golang/errors => ./errors
