/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.*
import cn.zenliu.ktor.boot.annotations.request.*
import cn.zenliu.ktor.boot.annotations.routing.Interceptor
import cn.zenliu.ktor.boot.annotations.routing.RawRoute
import cn.zenliu.ktor.boot.annotations.routing.RequestMapping
import cn.zenliu.ktor.boot.context.BeanManager.executeKFunction
import cn.zenliu.ktor.boot.context.RouteManager.Companion.log
import cn.zenliu.ktor.boot.exceptions.NotValidRawRouteFunction
import cn.zenliu.ktor.boot.exceptions.ServiceException
import cn.zenliu.ktor.boot.http.ServerSentEvent
import cn.zenliu.ktor.boot.http.StatusResponse
import cn.zenliu.ktor.boot.jackson.toClass
import cn.zenliu.ktor.boot.reflect.*
import io.ktor.application.*
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.PipelineContext
import jdk.nashorn.internal.runtime.regexp.joni.Config.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaType

/**
 * sigleton class for all route handler
 * @property application Application
 * @property clazz Collection<Pair<KClass<*>, Any>> Controller Classes  @see [Controller]
 * @property routeFun Collection<Pair<Collection<KFunction<*>>, Any>> RawRoute Functions @see[RawRoute]
 * @property coroutineContext CoroutineContext @see [BootCoroutineContext]
 * @property log Logger
 * @constructor
 */
@Ignore
class RouteManager(
    private val application: Application,
    val clazz: Collection<Pair<KClass<*>, Any>>,
    val routeFun: Collection<Pair<Collection<KFunction<*>>, Any>>
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = application.coroutineContext


    init {
        application.routing {
            routeFun.forEach { (fns, instance) ->
                fns.forEach { fn ->
                    log.info("register raw route function ${instance::class.java.name}::${fn.name}")
                    when {
                        fn.parameters.size != 2 -> throw NotValidRawRouteFunction(fn, instance::class)
                        (fn.parameters.first().kind == KParameter.Kind.EXTENSION_RECEIVER || fn.parameters.first().kind == KParameter.Kind.INSTANCE) &&
                                fn.parameters[1].type.isClass(Routing::class)
                        -> executeKFunction(instance, fn, this@routing)
                        else -> throw NotValidRawRouteFunction(fn, instance::class)
                    }
                }
            }
            buildRoute(this)

        }
    }

    internal data class RouteInterceptor(val fn: KFunction<*>, var path: String, val order: Int, val instance: Any)
    internal data class RouteDefine(
        val instance: Any,
        val requestMapping: RequestMapping,
        val clazzRequestMapping: RequestMapping?,
        val fn: KFunction<*>,
        var interceptorFn: KFunction<*>? = null
    ) {
        fun toRoutePath() = RoutePath(
            path = (("/" + (clazzRequestMapping?.path ?: "") + "/") + requestMapping.path)
                .replace("\\", "/").cleanUrl,
            handler = this
        )/*.apply {
            log.trace("genrated routePath $this from ${this@RouteDefine}")
        }*/

        companion object {
            fun toTree(routes: MutableList<RouteDefine>) = routes.map {
                it.toRoutePath().toRoutePaths()
            }.flatten().sortedBy { it.path.count { it == '/' } }.toSet().fold(
                mutableListOf<RoutePath>()
            ) { tree, ele ->
                tree.apply {
                    when {
                        ele.path.count { it == '/' } == 1 -> find { it.path == ele.path }?.let {
                            if (it.handler == null && ele.handler != null) add(ele)
                        } ?: add(ele)
                        ele.path.count { it == '/' } != 1 -> addChild(tree, ele)
                        else -> log.trace("error calc tree of $ele")
                    }
                }
            }

            private fun addChild(lst: MutableList<RoutePath>, ele: RoutePath) {
                lst.forEach { e ->
                    when {
                        e.path == ele.path.replaceAfterLast("/", "").removeSuffix("/") -> e.sub.add(ele)
                        e.sub.isNotEmpty() -> addChild(e.sub, ele)

                        //else->log.debug("Not found parent routePath of $e=>$ele ,${ele.path.replaceAfterLast("/","").removeSuffix("/")}")
                    }
                }
            }
        }
    }

    internal data class RoutePath(
        val path: String,
        val variable: MutableList<String> = mutableListOf(),
        var handler: RouteDefine? = null,
        val sub: MutableList<RoutePath> = mutableListOf()
    ) {
        fun toRoutePaths() = path.split("/").let { lst ->
            lst.filter { it.isNotBlank() }.let { lsts ->
                lsts.mapIndexed { idx, _ ->
                    when (idx) {
                        lsts.lastIndex -> RoutePath(
                            lst.subList(0, idx + 2).joinToString("/"),
                            handler = handler
                        ).apply { variable.addAll(lst.filter { it.contains("{") }) }
                        else -> RoutePath(lst.subList(0, idx + 2).joinToString("/"))
                    }
                }
            }
        }
    }

    private fun buildRoute(routing: Routing) {
        val interceptors = clazz.fold(mutableMapOf<String, MutableList<RouteInterceptor>>()) { map, (klazz, instance) ->
            klazz.declaredFunctionsSafe.filter {
                it.isInterceptor
            }.map {
                RouteInterceptor(
                    it,
                    (("/" + (klazz.findAnnotationSafe<RequestMapping>()?.path
                        ?: "")) + "/" + it.findAnnotation<Interceptor>()!!.path).cleanUrl,
                    it.findAnnotation<Interceptor>()!!.order,
                    instance
                )
            }.toSet().toMutableList().forEach { e ->

                map[e.path]?.add(e) ?: run { map[e.path] = mutableListOf(e) }
            }
            map
        }.apply {
            forEach { it.value.sortBy { it.order } }
        }.apply {
            log.trace("find interceptors $this")
        }
        routing {
            interceptors["/"]?.toSet()?.forEach { ri ->
                log.info("register interceptor [${ri.path}](${ri.instance::class.java.canonicalName}::${ri.fn.name} )<${ri.order}>")
                intercept(ApplicationCallPipeline.Features) {
                    ri.fn.callSuspend(ri.instance, this)
                }
            }
            fun parseRoute(r: RoutePath) {
                route(r.path) {
                    interceptors[r.path]?.forEach { fn ->
                        log.info("register interceptor ${fn.instance::class.qualifiedName}::${fn.fn.name} on ${fn.path}<${fn.order}>[${fn.fn.findAnnotation<Interceptor>()!!.method}]")
                        intercept(ApplicationCallPipeline.Features) {
                            fn.fn.findAnnotation<Interceptor>()!!.method.let {
                                when {
                                    it.isNotEmpty() && it.contains(call.request.httpMethod.toMETHOD()) ->
                                        fn.fn.callSuspend(fn.instance, this)
                                    it.isEmpty() -> fn.fn.callSuspend(fn.instance, this)
                                    else -> Unit

                                }
                            }

                        }
                    }
                    if (r.handler != null) {
                        r.handler!!.let { handler ->
                            if (handler.requestMapping.accept != RequestMapping.CONTENT.NULL) {
                                accept(handler.requestMapping.accept.toContentType()) {
                                    method(handler.requestMapping.method.toHttpMethod) {
                                        log.info("register route  on ${r.path} with ${handler.instance::class.qualifiedName}::${handler.fn.name} accept:${handler.requestMapping.accept.toContentType()} method:${handler.requestMapping.method.toHttpMethod.value} ")
                                        handle {
                                            annotationParameterProcessor(handler.instance, this, handler.fn)
                                        }
                                    }
                                }
                            } else {
                                log.info("register route <${handler.requestMapping.method.toHttpMethod.value}>[${r.path}] (${handler.instance::class.qualifiedName}::${handler.fn.name})")
                                method(handler.requestMapping.method.toHttpMethod) {
                                    handle {
                                        annotationParameterProcessor(handler.instance, this, handler.fn)
                                    }
                                }
                            }
                        }
                    } else {
                        log.info("register route [${r.path}]")
                    }
                    if (r.sub.isNotEmpty()) {
                        r.sub.forEach {
                            parseRoute(it)
                        }
                    }

                }
            }
            clazz.fold(mutableListOf<RouteDefine>()) { def, (klazz, instance) ->
                def.apply {
                    addAll(klazz.functionsSafe.filter { it.findAnnotation<RequestMapping>() != null }.map { fn ->
                        RouteDefine(
                            instance,
                            fn.findAnnotation()!!,
                            klazz.findAnnotationSafe(),
                            fn
                        )
                    })
                }
            }.let { RouteDefine.toTree(it) }.forEach {
                parseRoute(it)
            }
        }
    }


    private suspend fun annotationParameterProcessor(
        target: Any,
        ctx: HandlerContext,
        fn: KFunction<*>
    ) {
        val call = ctx.context
        try {
            when (fn.parameters.size) {
                1 -> fn.callSuspend(target)
                else -> fn.callSuspendBy(
                    fn.parameters.map { p ->
                        p to when {
                            p.kind == KParameter.Kind.INSTANCE || p.kind == KParameter.Kind.EXTENSION_RECEIVER -> target
                            p.type.javaType.typeName == ApplicationCall::class.java.name -> call
                            p.type.javaType.typeName == PipelineContext::class.java.name -> ctx
                            p.type.javaType.typeName == PipelineContext::class.java.name -> ctx
                            p.type.javaType.typeName == Attributes::class.java.name -> call.attributes
                            else -> {
                                p.findAnnotation<RequestBody>()?.let {
                                    when {
                                        p.type.isTypeString -> call.receiveText()
                                        p.isOptional -> call.receiveOrNull(p.type.kClass!!)
                                        else -> try {
                                            call.receive(p.type.kClass!!)
                                        } catch (e: Throwable) {
                                            throw ServiceException(
                                                HttpStatusCode.BadRequest,
                                                "request not satisfied with query parameter ${p.name}",
                                                e
                                            )
                                        }
                                    }
                                }
                                    ?: p.findAnnotation<QueryParameter>()?.let {
                                        call.request.queryParameters[if (it.name.isNotBlank()) it.name else p.name!!]?.let {
                                            try {
                                                parseParameter(
                                                    p.type,
                                                    it
                                                )
                                            } catch (e: Throwable) {
                                                throw ServiceException(
                                                    HttpStatusCode.BadRequest,
                                                    "request not satisfied with query parameter ${p.name}",
                                                    e
                                                )
                                            }
                                        }
                                            ?: when {
                                                p.type.isMarkedNullable -> null
                                                p.isOptional -> null
                                                else -> throw ServiceException(
                                                    HttpStatusCode.BadRequest,
                                                    "request not satisfied with query parameter ${p.name}"
                                                )
                                            }
                                    }
                                    ?: p.findAnnotation<PathVariable>()?.let {
                                        call.parameters[if (it.name.isNotBlank()) it.name else p.name!!]?.let {
                                            try {
                                                parseParameter(
                                                    p.type,
                                                    it
                                                )
                                            } catch (e: Throwable) {
                                                throw ServiceException(
                                                    HttpStatusCode.BadRequest,
                                                    "request not satisfied with query parameter ${p.name}",
                                                    e
                                                )
                                            }
                                        }
                                            ?: when {
                                                p.type.isMarkedNullable -> null
                                                p.isOptional -> null
                                                else -> throw ServiceException(
                                                    HttpStatusCode.BadRequest,
                                                    "request not satisfied with path parameter ${p.name}"
                                                )
                                            }
                                    }
                                    ?: when {
                                        p.type.isMarkedNullable -> null
                                        p.isOptional -> null
                                        else -> p.findAnnotation<AutoWire>().let {
                                            try {
                                                BeanManager
                                                    .instanceOf(it?.name ?: p.name ?: "", p.type.kClass!!)
                                            } catch (e: Throwable) {
                                                throw ServiceException(
                                                    HttpStatusCode.InternalServerError,
                                                    "request process failed"
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }.filter { !(it.first.isOptional && it.second == null) }.toMap()
                )
            }.apply {
                if ("reactor.core.publisher.Mono".classExists) {
                    when (this) {
                        is Unit -> Unit
                        null -> Unit
                        is Mono<*> -> doOnError {
                            this.cancelOn(Schedulers.immediate())
                            throw it
                        }.subscribe {
                            ctx.launch {
                                parseResponse(it, ctx)
                            }
                        }
                        is Flux<*> -> parseResponseReactor(this, ctx)
                        else -> parseResponse(this, ctx)
                    }
                } else {
                    when (this) {
                        is Unit -> Unit
                        null -> Unit
                        else -> parseResponse(this, ctx)
                    }
                }
            }
        } catch (e: Throwable) {
            log.debug("error when process ${fn.name}")
            processCallException(e, call, log)
        }

    }

    companion object {


        @JvmStatic
        val String.cleanUrl: String
            get() = kotlin.run {
                var t = this
                while (t.contains("//")) {
                    t = t.replace("//", "/")
                }
                t
            }
        private val log = LoggerFactory.getLogger(RouteManager::class.java)
        private suspend fun parseResponse(res: Any?, ctx: HandlerContext) {
            when {
                res == null -> ctx.finish()
                res is StatusResponse -> {
                    res.headers?.forEach { k, v -> ctx.context.response.headers.append(k, v, res.withSafeHeader) }
                    res.body?.apply {
                        when (this) {//TODO::for diffierent type process

                            else -> ctx.context.respond(res.statusCode, this)
                        }
                    }


                }
                else -> ctx.context.respond(res)
            }
        }

        private suspend fun parseResponseReactor(res: Flux<*>, ctx: HandlerContext) {
            ctx.context.respondTextWriter(contentType = ContentType.parse("text/event-stream")) {
                res.doOnCancel(this::close).doOnComplete(this::close).subscribe {
                    when (it) {
                        is ServerSentEvent -> write(it.toString())
                    }
                    flush()
                }
            }

        }

        private fun parseParameter(type: KType, value: String) = when {
            type.isTypeString -> value
            type.isTypeInt -> value.toInt()
            type.isTypeFloat -> value.toFloat()
            type.isTypeLong -> value.toLong()
            type.isTypeDouble -> value.toDouble()
            type.isTypeShort -> value.toShort()
            type.isTypeBoolean -> value.toBoolean()
            type.isTypeChar -> value[0]
            type.isTypeByte -> value.toByte()
            type.isTypeByteArray -> value.toByteArray()
            else -> value.toClass(Class.forName(type.javaType.typeName))
        }

        private suspend fun processCallException(e: Throwable, call: ApplicationCall, log: org.slf4j.Logger) {
            log.trace("${e is ServiceException}")
            when (e) {
                is InvocationTargetException -> processCallException(
                    e.targetException,
                    call,
                    log
                )
                is ServiceException -> {
                    call.respondText(ContentType.Application.Json, e.status) {
                        e.toJsonString(call.request.path(), parseRequest(call.request))
                    }
                }
                else -> {
                    log.error("error when handle ${call.request.path()} ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }

        @KtorExperimentalAPI
        private fun parseRequest(request: ApplicationRequest) = mapOf(
            "uri" to "[${request.origin.method.value}]${request.origin.scheme}://${request.origin.host}${request.origin.port.let { if (it == 80) "" else it.toString() }}${request.uri}",
            "userAgent" to request.userAgent(),
            "origin" to request.origin.let
            {
                mapOf(
                    "host" to it.host,
                    "remoteHost" to it.remoteHost,
                    "version" to it.version,
                    "scheme" to it.scheme,
                    "port" to it.port
                )
            },
            "headers" to request.headers.entries().map
            { it.key to it.value.joinToString("\n") }.toMap(),
            "cookies" to request.cookies.rawCookies,
            "parameters" to request.queryParameters.entries()
        )

        private var instanceClass: RouteManager? = null
        private val emptyClassList = mutableListOf<Pair<KClass<*>, Any>>()
        private val emptyFunList = mutableListOf<Pair<Collection<KFunction<*>>, Any>>()
        fun instance(
            application: Application? = null,
            clazz: Collection<Pair<KClass<*>, Any>> = emptyClassList,
            fn: Collection<Pair<Collection<KFunction<*>>, Any>> = emptyFunList
        ) = when {
            application == null && instanceClass == null -> null
            instanceClass == null && application != null -> RouteManager(application, clazz, fn)
            else -> instanceClass
        }
    }
}

fun HttpMethod.toMETHOD() = RequestMapping.METHOD.values().find { it.value == this.value }
