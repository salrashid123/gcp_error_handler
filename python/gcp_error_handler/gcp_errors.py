from google.api_core.exceptions import GoogleAPIError, GoogleAPICallError
from google.rpc import status_pb2, error_details_pb2, code_pb2
import google.api_core.exceptions
import googleapiclient.errors
from google.protobuf.json_format import MessageToDict,MessageToJson

import os, json

class GCPError(Exception):

    _err = None
    _is_google_cloud_error = True
    _pretty_print = False

    def __init__(self, err, prettyprint=False):
        super(Exception, self).__init__(err)
        self._err=err
        self._pretty_print=prettyprint
  
        # TODO, account for https://github.com/googleapis/google-api-python-client/blob/master/googleapiclient/errors.py
        if isinstance(err, googleapiclient.errors.HttpError):  # or isinstance(err, googleapiclient.errors.UnexpectedBodyError) or ...:
            #print("Interpreting googleapiclient.errors.HttpError")            
            self._is_google_cloud_error = False

    def __str__(self): 
      if 'GOOGLE_ENABLE_ERROR_DETAIL' in os.environ or self._pretty_print:
        if (os.environ.get('GOOGLE_ENABLE_ERROR_DETAIL') == "true" or self._pretty_print) and self._is_google_cloud_error and self.grpc_status_code != None:
          resp = {'GoogleCloudError': str(self._err)}
          for e in self._err.errors:
            info = self.get_google_rpc_help
            if info!=None:
              resp['google.rpc.help-bin'] = MessageToDict(info)
            info = self.get_google_rpc_badrequest
            if info!=None:
              resp['google.rpc.badrequest-bin'] = MessageToDict(info)
          return (json.dumps(resp, indent=4, sort_keys=False))

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
    def resp(self):
        if self._is_google_cloud_error == False:
          return self._err.resp

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