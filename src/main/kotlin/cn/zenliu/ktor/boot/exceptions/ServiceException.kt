/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.exceptions

import cn.zenliu.ktor.boot.annotations.context.Ignore
import cn.zenliu.ktor.boot.jackson.toJsonNode
import io.ktor.http.HttpStatusCode
import java.time.Instant

/**
 * Throwable for response with self defined error messages
 * @property status HttpStatusCode
 * @property content String?
 * @property causes Exception?
 * @property withPath Boolean
 * @property withRequest Boolean
 * @property withStack Boolean
 * @constructor
 */
@Ignore
open class ServiceException(
    val status: HttpStatusCode = HttpStatusCode.InternalServerError,
    val content: String? = null,
    val causes: Throwable? = null,
    val withPath: Boolean = false,
    val withRequest: Boolean = false,
    val withStack: Boolean = false
) :
    Throwable(content, causes) {
    /**
     * function to generate Map for json response
     * @param path String?
     * @param request Map<String, Any?>?
     * @param stack Boolean
     * @return String
     */
    fun toJsonString(path: String? = null, request: Map<String, Any?>? = null, stack: Boolean = false) =
        mutableMapOf<String, Any>().apply {
            put("timestamp", Instant.now().toEpochMilli())
            put("status", status.value)
            if (path != null && withPath) {
                put("path", path)
            }
            if (content != null) {
                put("message", content)
            }
            if ((stack || withStack) && causes != null) {
                put("stack", causes.stackTrace.toJsonNode.toString())
            }
            if (request != null && withRequest) {
                put("request", request)
            }
        }.toJsonNode.toString()
}

/**
 *  Internal Service Exception
 * @property status HttpStatusCode
 * @property content String?
 * @property causes Throwable?
 * @property withPath Boolean
 * @property withRequest Boolean
 * @property withStack Boolean
 * @constructor
 */
@Ignore
open class InnerServiceException(
    val status: HttpStatusCode = HttpStatusCode.InternalServerError,
    val content: String? = null,
    val causes: Throwable? = null,
    val withPath: Boolean = false,
    val withRequest: Boolean = false,
    val withStack: Boolean = false
) :
    Throwable(content, causes) {
    /**
     * function to generate Map for json response
     * @param path String?
     * @param request Map<String, Any?>?
     * @param stack Boolean
     * @return String
     */
    fun toJsonString(path: String? = null, request: Map<String, Any?>? = null, stack: Boolean = false) =
        mutableMapOf<String, Any>().apply {
            put("timestamp", Instant.now().toEpochMilli())
            put("status", status.value)
            if (path != null && withPath) {
                put("path", path)
            }
            if (content != null) {
                put("message", content)
            }
            if ((stack || withStack) && causes != null) {
                put("stack", causes.stackTrace.toJsonNode.toString())
            }
            if (request != null && withRequest) {
                put("request", request)
            }
        }.toJsonNode.toString()
}
