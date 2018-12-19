package cn.zenliu.ktor.boot.exceptions

import cn.zenliu.ktor.boot.jackson.toJsonNode
import io.ktor.http.HttpStatusCode
import java.time.Instant

data class ServiceException(
    val status: HttpStatusCode = HttpStatusCode.InternalServerError,
    val content: String? = null,
    val causes: Exception? = null,
    val withPath: Boolean = false,
    val withRequest: Boolean = false,
    val withStack: Boolean = false
) :
    Throwable(content, causes) {
    fun toJson(stack: Boolean = false) = mutableMapOf(
        "timestamp" to Instant.now().toEpochMilli(),
        "status" to status.value,
        "message" to content
    ).apply {
        if ((stack || withStack) && causes != null) {
            put("stack", causes.stackTrace.toJsonNode.toString())
        }
    }
}
