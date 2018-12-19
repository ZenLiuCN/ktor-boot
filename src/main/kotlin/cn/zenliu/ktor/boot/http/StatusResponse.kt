package cn.zenliu.ktor.boot.http

import io.ktor.http.HttpStatusCode

data class StatusResponse(
    val statusCode: HttpStatusCode = HttpStatusCode.OK,
    val body: Any? = null,
    val headers: Map<String, String>? = null,
    val withSafeHeader: Boolean = true
)
