package com.kkstream.imademo.data

sealed class ApiResponse {
    data class Success<ResultType>(val data: ResultType) : ApiResponse()
    data class Error(val errorEvent: String) : ApiResponse()
}