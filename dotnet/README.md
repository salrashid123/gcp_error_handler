## GCP Error Details custom handler (dotnat)


The node library contained here wraps both [google-cloud ApiError](https://googleapis.dev/nodejs/common/2.0.0/classes/ApiError.html) and gRPC-based google API Error Details. It also provides convenience methods to extract and print any embedded errors.

The objective of the library is to display by default the _same_ message structure as a developer would see as if the library is not used.  

That is, you should see the same output if you use the wrapper or not but with the distinction that you _can_ extract the details using the wrapper methods provided:

---

###  Usage

### Asset API ()

```
dotnet run --api=asset   --checkResource=//cloudresourcemanager.googleapis.com/projects/fabled-ray-104117   --identity=user:admin@esodemoapp2.com   --scope=projects/fabled-ray-104117
```

### GCS

```
dotnet run --api=gcs --gcsBucket=fabled-ray-104117-bucket --gcsObject=foo.txt
```

### Compute

```
dotnet run --api=compute --projectID=fabled-ray-104117 --zone=us-central1-a
```

### PubSub
```
dotnet run --api=pubsub --projectID=fabled-ray-104117
```