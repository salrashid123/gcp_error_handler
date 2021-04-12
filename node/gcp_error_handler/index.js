const { ApiError } = require('@google-cloud/common');
const protobuf = require("protobufjs/minimal");
const util = require('util');
const protos = require('google-proto-files');
const root = protos.loadSync('./node_modules/google-proto-files/google/rpc/error_details.proto');


class GCPErrorHandler {

  constructor(error, prettyPrint = false) {
    this.name = this.constructor.name

    const env_pp = process.env.GOOGLE_ENABLE_ERROR_DETAIL || 'false';
    if (env_pp == 'true') {
      prettyPrint = true
    }
    this._prettyPrint = prettyPrint
    Error.captureStackTrace(this, this.constructor)

    //console.log(util.inspect(error, { depth: null }))

    this.err = error
    this.isGoogleCloudError = false;
    this.errors = error.errors
    this.code = error.code
    this._message = error.message
    this.response = error.response

    if (error.hasOwnProperty('code') && error.hasOwnProperty('details') && error.hasOwnProperty('metadata')) {
      this.isGoogleCloudError = true;
      this.metadata = error.metadata
      this.details = error.details
    }
  }

  get message() {
    if (this._prettyPrint) {

      let r = {
        "message": this._message,
        "google.rpc.Help": this.getHelp(),
        "google.rpc.ErrorInfo": this.getErrorInfo(),
        "google.rpc.BadRequest": this.getBadRequest()
      }

      return JSON.stringify(r, null, 4);

    }
    return this._message
  }


  getHelp() {
    if (this.err.metadata) {
      const error_bytes = this.err.metadata.get('google.rpc.help-bin');
      if (error_bytes.length > 0) {
        const errordef = root.lookup("google.rpc.Help");
        return errordef.decode(error_bytes[0])
      }
    }
  }

  getErrorInfo() {
    if (this.err.metadata) {
      const error_bytes = this.err.metadata.get('google.rpc.errorinfo-bin');
      if (error_bytes.length > 0) {
        const errordef = root.lookup("google.rpc.ErrorInfo");
        return errordef.decode(error_bytes[0])
      }
    }
  }

  getBadRequest() {
    if (this.err.metadata) {
      const error_bytes = this.err.metadata.get('google.rpc.badrequest-bin');
      if (error_bytes.length > 0) {
        const errordef = root.lookup("google.rpc.BadRequest");
        return errordef.decode(error_bytes[0])
      }
    }
  }

}

module.exports = GCPErrorHandler
