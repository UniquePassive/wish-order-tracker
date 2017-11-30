package com.wishordertracker.site

import com.google.gson.Gson
import com.wishordertracker.site.models.WishLoginResponseModel
import com.wishordertracker.site.models.WishOrderHistoryModel
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.core.body.form
import org.http4k.lens.Header
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.CookieStore
import java.net.URI

class WishSite {
    companion object {
        private const val URL_WISH_INDEX = "https://www.wish.com"
        private const val URL_WISH_EMAIL_LOGIN = "https://www.wish.com/api/email-login"
        private const val URL_WISH_ORDER_HISTORY = "https://www.wish.com/transaction"
    }

    private var http: OkHttp
    private val cookies: CookieStore

    private val gson = Gson()

    init {
        /* Initialize http client with cookie manager */
        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)

        cookies = cookieManager.cookieStore

        val builder = OkHttpClient.Builder()
                .cookieJar(JavaNetCookieJar(cookieManager))

        http = OkHttp(builder.build())
    }

    fun login(email: String, pass: String): String? {
        run {
            val request = Request(Method.GET, URL_WISH_INDEX)
                    .appendXSRFHeader()
            val response = http(request)

            if (response.status != Status.OK && response.status != Status.BAD_REQUEST) {
                return "Could not visit index page (response code: " + response.status + ")"
            }
        }

        run {
            val request = Request(Method.POST, URL_WISH_EMAIL_LOGIN)
                    .with(Header.Common.CONTENT_TYPE of ContentType.APPLICATION_FORM_URLENCODED)
                    .form("email", email)
                    .form("password", pass)
                    .form("_buckets", "")
                    .form("_experiments", "")
                    .appendXSRFHeader()
            val response = http(request)

            val responseModel = gson.fromJson(response.bodyString(), WishLoginResponseModel::class.java)

            if (responseModel != null && !responseModel.msg.isEmpty()) {
                return "Could not login (message: " + responseModel.msg + ")"
            }

            if (response.status != Status.OK && response.status != Status.BAD_REQUEST) {
                return "Could not login (response code: " + response.status + ")"
            }
        }

        return null
    }

    fun orderHistory(): Pair<WishOrderHistoryModel?, String?> {
        val request = Request(Method.GET, URL_WISH_ORDER_HISTORY)
                .appendXSRFHeader()
        val response = http(request)

        if (response.status != Status.OK) {
            return Pair(null, "Could not get order history (response code: " + response.status + ")")
        }

        /* Find the order history json from the page source */
        var responseBody = response.bodyString()

        val startPos = responseBody.indexOf("pageParams['transactions'] = ") + "pageParams['transactions'] = ".length
        val endPos = responseBody.indexOf(";", startPos)
        responseBody = responseBody.substring(startPos, endPos)

        responseBody = "{\"orders\":$responseBody}"

        val responseModel = gson.fromJson(responseBody, WishOrderHistoryModel::class.java)
                ?: return Pair(null, "Serialized order history is null")

        return Pair(responseModel, null)
    }

    private fun Request.appendXSRFHeader(): Request {
        cookies.get(URI(uri.toString())).forEach {
            if (it.name == "_xsrf") {
                return header("X-XSRFToken", it.value)
            }
        }
        return this
    }
}
