from google.api_core.exceptions import GoogleAPIError, GoogleAPICallError
from google.rpc import status_pb2, error_details_pb2, code_pb2
import google.api_core.exceptions
import googleapiclient.errors

class GCPError(Exception):

    _err = None
    _is_google_cloud_error = True

    def __init__(self, err):
        super(Exception, self).__init__(err)
        self._err=err
       
        if isinstance(err, google.cloud.exceptions.GoogleCloudError):
            print("Interpreting as google.cloud.exceptions.GoogleCloudError")    
        if isinstance(err, googleapiclient.errors.HttpError):
            print("Interpreting googleapiclient.errors.HttpError")            
            self._is_google_cloud_error = False

    def __str__(self):     
        return str(self._err)

    @property
    def is_google_cloud_error(self):
      return _is_google_cloud_error


    #  For googleapiclient.errors
    @property
    def content(self):
      if self._is_google_cloud_error == False: 
        return self._err.content

    @property
    def uri(self):
      if self._is_google_cloud_error == False: 
        return self._err.uri

    @property
    def error_details(self):
      if self._is_google_cloud_error == False: 
        return self._err.error_details

    @property
    def status_code(self):
        return self._err.resp.status

    #  For google.cloud.exceptions
    @property
    def errors(self):
        if self._is_google_cloud_error:
          return list(self._err._errors)

    @property
    def response(self):
        if self._is_google_cloud_error:
          return self._err._response

    @property
    def code(self):
        if self._is_google_cloud_error:
          return self._err.code

    @property
    def message(self):
        if self._is_google_cloud_error:
          return self._err.message

    @property
    def grpc_status_code(self):
        if self._is_google_cloud_error:
          return self._err.grpc_status_code

    @property
    def get_google_rpc_help(self):
        if self._is_google_cloud_error == False:
          #return None
          raise Exception("not a google cloud error")

        for e in self._err.errors:
          meta = e.trailing_metadata()
          for m in meta:
            if (m.key =='google.rpc.help-bin'):
              info = error_details_pb2.Help()
              info.ParseFromString(m.value)
              return info
        return None

    @property
    def get_google_rpc_errorinfo(self):
        if self._is_google_cloud_error == False:
          #return None
          raise Exception("not a google cloud error")

        for e in self._err.errors:
          meta = e.trailing_metadata()
          for m in meta:
            if (m.key =='google.rpc.errorinfo-bin'):
              info = error_details_pb2.ErrorInfo()
              info.ParseFromString(m.value)
              return info
        return None

    @property
    def get_google_rpc_quotafailure(self):
        if self._is_google_cloud_error == False:
          #return None
          raise Exception("not a google cloud error")

        for e in self._err.errors:
          meta = e.trailing_metadata()
          for m in meta:
            if (m.key =='google.rpc.quotafailure-bin'):
              info = error_details_pb2.QuotaFailure()
              info.ParseFromString(m.value)
              return info
        return None

    @property
    def get_google_rpc_badrequest(self):
        if self._is_google_cloud_error == False:
          #return None
          raise Exception("not a google cloud error")

        for e in self._err.errors:
          meta = e.trailing_metadata()
          for m in meta:
            if (m.key =='google.rpc.badrequest-bin'):
              info = error_details_pb2.BadRequest()
              info.ParseFromString(m.value)
              return info
        return None

    @property
    def get_google_rpc_preconditionfailure(self):
        if self._is_google_cloud_error == False:
          #return None
          raise Exception("not a google cloud error")

        for e in self._err.errors:
          meta = e.trailing_metadata()
          for m in meta:
            if (m.key =='google.rpc.preconditionfailure-bin'):
              info = error_details_pb2.PreconditionFailure()
              info.ParseFromString(m.value)
              return info
        return None