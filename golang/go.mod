module main

go 1.15

require (
	cloud.google.com/go v0.79.0
	cloud.google.com/go/storage v1.10.0
	github.com/salrashid123/gcp_error_handler/golang/errors v0.0.0-00010101000000-000000000000 // indirect

)

replace github.com/salrashid123/gcp_error_handler/golang/errors => ./errors
