package cn.zenliu.ktor.boot.annotations.routing

import io.ktor.http.HttpMethod


/**
 * define router handelr
 * @property path String
 * @property method HTTPMETHOD
 * @constructor
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@MustBeDocumented
annotation class RequestMapping(val path: String, val method: METHOD = METHOD.GET){
    enum class METHOD {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS;

        val toHttpMethod
            get() = when (this) {
                GET -> HttpMethod.Get
                POST -> HttpMethod.Post
                PUT -> HttpMethod.Put
                DELETE -> HttpMethod.Delete
                PATCH -> HttpMethod.Patch
                HEAD -> HttpMethod.Head
                OPTIONS -> HttpMethod.Options

            }
    }
}
