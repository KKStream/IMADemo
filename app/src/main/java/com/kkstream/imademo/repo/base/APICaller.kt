package com.kkstream.imademo.repo.base

import android.util.Log
import com.kkstream.android.ottfs.module.api.API
import com.kkstream.android.ottfs.module.api.APIError
import com.kkstream.imademo.data.ApiResponse
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class APICaller {
    private val serviceMap = mutableMapOf<Int, Any>()

    companion object {
        val TAG: String = APICaller::class.java.simpleName
    }

    fun cancel() {
        for (service in serviceMap.values) {
            when (service) {
                is APIBase<*> -> service.cancel()
            }
        }
    }

    /**
     * Callback will pass ApiResponse back.
     * Result of Success(with data) or Error(with error event) is wrapped into ApiResponse
     */
    suspend fun <ResultType> execute(
        service: APIBase<ResultType>
    ): ApiResponse {

        return suspendCoroutine { continuation ->
            serviceMap[service.hashCode()] = service
            Log.d(TAG, "url: ${service.url}")
            service.apply {
                addListener(object : API.Listener<ResultType> {
                    override fun onComplete(result: ResultType?) {
                        continuation.resume(ApiResponse.Success(result ?: ""))
                        Log.d(TAG, "res: $result")
                        serviceMap.remove(service.hashCode())
                    }

                    override fun onError(error: APIError) {
                        continuation.resume(ApiResponse.Error(error.response))
                        serviceMap.remove(service.hashCode())
                    }
                })
                start()
            }
        }
    }
}