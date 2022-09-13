package com.example.alan.sdkdemo.contact

import android.text.TextUtils
import android.util.Log
import com.vcrtc.utils.OkHttpUtil
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.lang.IllegalStateException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HttpUtil {
    companion object{
        suspend fun doPostAsync(url: String, params: String, header: Map<String, String>?): Deferred<String> {

            return suspendCancellableCoroutine {

                val deferred = CompletableDeferred<String>()
                Log.d("HttpUtil", "url: $url")
                OkHttpUtil.doPost(url, params, header, object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.d("HttpUtil", "reason : ${e.message}")
                        it.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val resp = response.body()?.string()
                        Log.d("HttpUtil", "response body: $resp")
                        val responseBody = JSONObject(resp)
                        val result = responseBody.optString("result")
                        if (!TextUtils.isEmpty(result)) {
                            it.resumeWithException(IllegalStateException(result))
                        } else {
                            val code = responseBody.optString("code")
                            when {
                                TextUtils.isEmpty(code) -> {
                                    if (resp != null) {
                                        deferred.complete(resp)
                                    }
                                    it.resume(deferred)
                                }
                                code == "200" -> {
                                    resp?.let { it1 -> deferred.complete(it1) }
                                    it.resume(deferred)
                                }
                                else -> {
                                    it.resumeWithException(IllegalStateException(responseBody.toString()))
                                }
                            }

                        }
                    }
                })
            }
        }
    }
}
