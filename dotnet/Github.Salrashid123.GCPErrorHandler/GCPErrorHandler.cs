using System;
using Google.Protobuf.WellKnownTypes;
using Grpc.Core;
using Newtonsoft.Json;

namespace Github.Salrashid123.GCPErrorHandler
{
    [Serializable]
    public class GCPErrorHandler : Exception
    {
        private const string StatusDetailsTrailerName = "grpc-status-details-bin";
        private Exception ex;
        private bool prettyPrint = false;

        public bool isGoogleCloudError = false;

        private string prettyMessage;
        public GCPErrorHandler(Exception ex, bool prettyPrint = false) : base(ex.Message, ex.InnerException)
        {
            this.ex = ex;
            if (ex.GetType().Name == typeof(Google.GoogleApiException).Name)
            {
                this.isGoogleCloudError = false;
            }
            if (ex.GetType().Name == typeof(Grpc.Core.RpcException).Name)
            {
                this.isGoogleCloudError = true;
            }
            var value = Environment.GetEnvironmentVariable("GOOGLE_ENABLE_ERROR_DETAIL");
            if (value != null && value == "true")
            {
                this.prettyPrint = true;


                this.prettyMessage = JsonConvert.SerializeObject(new
                {
                    Message = this.ex.Message,  //TODO: don't know how to prettyprint this
                    GoogleRpcHelp = GetGoogleRpcHelp(),
                    GoogleRpcErrorInfo = GetGoogleRpcErrorInfo(),
                    GoogleRpcBadRequest = GetGoogleRpcBadRequest()

                }, Formatting.Indented);

            }
        }

        public override string Message
        {
            get
            {
                if (this.isGoogleCloudError == false)
                {
                    Google.GoogleApiException re = (Google.GoogleApiException)ex;
                    return re.Message;
                }
                else
                {
                    if (this.prettyPrint)
                        return prettyMessage;
                    else
                        return this.ex.Message;
                }

            }
        }

        public string ServiceName
        {
            get
            {
                if (this.isGoogleCloudError == false)
                {
                    Google.GoogleApiException re = (Google.GoogleApiException)ex;
                    return re.ServiceName;
                }
                else
                    return "";
            }
        }

        public System.Net.HttpStatusCode HttpStatusCode
        {
            get
            {
                if (this.isGoogleCloudError == false)
                {
                    Google.GoogleApiException re = (Google.GoogleApiException)ex;
                    return re.HttpStatusCode;
                }
                else
                    return new System.Net.HttpStatusCode();
            }
        }

        public Google.Apis.Requests.RequestError Error
        {
            get
            {
                if (this.isGoogleCloudError == false)
                {
                    Google.GoogleApiException re = (Google.GoogleApiException)ex;
                    return re.Error;
                }
                else
                    return new Google.Apis.Requests.RequestError();
            }
        }

        public override string ToString()
        {
            if (this.prettyPrint)
            {
                return "Foooo";
            }
            return this.ex.ToString();
        }

        // https://github.com/chwarr/grpc-dotnet-google-rpc-status/blob/master/client/Program.cs
        public Google.Rpc.Help GetGoogleRpcHelp()
        {
            if (this.isGoogleCloudError == false)
            {
                throw new Exception("Error Type is not RpcException");
            }

            byte[]? statusBytes = null;

            RpcException re = (RpcException)ex;
            foreach (Metadata.Entry me in re.Trailers)
            {
                if (me.Key == StatusDetailsTrailerName)
                {
                    statusBytes = me.ValueBytes;
                }
            }

            if (statusBytes is null)
            {
                return new Google.Rpc.Help();
            }

            var status = Google.Rpc.Status.Parser.ParseFrom(statusBytes);

            foreach (Any any in status.Details)
            {
                if (any.TryUnpack(out Google.Rpc.Help h))
                {
                    return h;
                }
            }
            return new Google.Rpc.Help();
        }

        public Google.Rpc.ErrorInfo GetGoogleRpcErrorInfo()
        {
            if (this.isGoogleCloudError == false)
            {
                throw new Exception("Error Type is not RpcException");
            }

            byte[]? statusBytes = null;

            RpcException re = (RpcException)ex;
            foreach (Metadata.Entry me in re.Trailers)
            {
                if (me.Key == StatusDetailsTrailerName)
                {
                    statusBytes = me.ValueBytes;
                }
            }

            if (statusBytes is null)
            {
                return new Google.Rpc.ErrorInfo();
            }

            var status = Google.Rpc.Status.Parser.ParseFrom(statusBytes);

            foreach (Any any in status.Details)
            {
                if (any.TryUnpack(out Google.Rpc.ErrorInfo h))
                {
                    return h;
                }
            }
            return new Google.Rpc.ErrorInfo();
        }

        public Google.Rpc.PreconditionFailure GetGoogleRpcPreconditionFailuer()
        {
            if (this.isGoogleCloudError == false)
            {
                throw new Exception("Error Type is not RpcException");
            }

            byte[]? statusBytes = null;

            RpcException re = (RpcException)ex;
            foreach (Metadata.Entry me in re.Trailers)
            {
                if (me.Key == StatusDetailsTrailerName)
                {
                    statusBytes = me.ValueBytes;
                }
            }

            if (statusBytes is null)
            {
                return new Google.Rpc.PreconditionFailure();
            }

            var status = Google.Rpc.Status.Parser.ParseFrom(statusBytes);

            foreach (Any any in status.Details)
            {
                if (any.TryUnpack(out Google.Rpc.PreconditionFailure h))
                {
                    return h;
                }
            }
            return new Google.Rpc.PreconditionFailure();
        }

        public Google.Rpc.BadRequest GetGoogleRpcBadRequest()
        {
            if (this.isGoogleCloudError == false)
            {
                throw new Exception("Error Type is not RpcException");
            }

            byte[]? statusBytes = null;

            RpcException re = (RpcException)ex;
            foreach (Metadata.Entry me in re.Trailers)
            {
                if (me.Key == StatusDetailsTrailerName)
                {
                    statusBytes = me.ValueBytes;
                }
            }

            if (statusBytes is null)
            {
                return new Google.Rpc.BadRequest();
            }

            var status = Google.Rpc.Status.Parser.ParseFrom(statusBytes);

            foreach (Any any in status.Details)
            {
                if (any.TryUnpack(out Google.Rpc.BadRequest h))
                {
                    return h;
                }
            }
            return new Google.Rpc.BadRequest();
        }
        public Status Status
        {
            get
            {
                if (this.isGoogleCloudError)
                {
                    RpcException re = (RpcException)ex;
                    return re.Status;
                }
                else
                    return new Grpc.Core.Status();
            }
        }

        public StatusCode StatusCode
        {
            get
            {
                if (this.isGoogleCloudError)
                {
                    RpcException re = (RpcException)ex;
                    return re.Status.StatusCode;
                }
                else
                    return new Grpc.Core.StatusCode();
            }
        }
        public Grpc.Core.Metadata Trailers
        {
            get
            {
                if (this.isGoogleCloudError)
                {
                    RpcException re = (RpcException)ex;
                    return re.Trailers;
                }
                else
                    return new Grpc.Core.Metadata();
            }
        }

    }
}