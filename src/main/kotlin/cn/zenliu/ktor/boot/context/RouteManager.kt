/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.*
import cn.zenliu.ktor.boot.annotations.request.*
import cn.zenliu.ktor.boot.annotations.routing.*
import cn.zenliu.ktor.boot.context.BeanManager.executeKFunction
import cn.zenliu.ktor.boot.context.RouteManager.Companion.log
import cn.zenliu.ktor.boot.exceptions.*
import cn.zenliu.ktor.boot.http.*
import cn.zenliu.ktor.boot.jackson.toClass
import cn.zenliu.ktor.boot.reflect.*
import io.ktor.application.*
import io.ktor.features.origin
import io.ktor.http.*
import io.ktor.http.content.MultiPartData
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import jdk.nashorn.internal.runtime.regexp.joni.Config.log
import kotlinx.coroutines.*
import kotlinx.coroutines.io.ByteReadChannel
import org.slf4j.LoggerFactory
import reactor.core.publisher.*
import reactor.core.scheduler.Schedulers
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

/**
 * sigleton class for all route handler
 * @property application Application
 * @property clazz Collection<Pair<KClass<*>, Any>> Controller Classes  @see [Controller]
 * @property routeFun Collection<Pair<Collection<KFunction<*>>, Any>> RawRoute Functions @see[RawRoute]
 * @property coroutineContext CoroutineContext @see [BootCoroutineContext]
 * @property log Logger
 * @constructor
 */
@KtorExperimentalAPI
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

    companion object {
        private val outputInnerServiceException: Boolean by lazy {
            PropertiesManager.bool(BOOT_INNER_EXCEPTION, true) ?: true
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
                            log.trace("parameter of ${fn.name} $p ${p.type.jvmErasure}")
                            p to when {
                                p.kind == KParameter.Kind.INSTANCE || p.kind == KParameter.Kind.EXTENSION_RECEIVER -> target
                                p.type.javaType.typeName == ApplicationCall::class.java.name -> call
//                            p.type.jvmErasure ==  -> ctx
                                p.type.javaType.typeName == "io.ktor.util.pipeline.PipelineContext<kotlin.Unit, io.ktor.application.ApplicationCall>" -> ctx
                                p.type.isClass<Attributes>() -> call.attributes
                                p.type.isClass<RequestCookies>() -> call.request.cookies
                                p.type.isClass<Headers>() -> call.request.headers
                                p.type.isClass<ApplicationRequest>() -> call.request
                                p.type.isClass<ResponseCookies>() -> call.response.cookies
                                p.type.isClass<ApplicationResponse>() -> call.response

                                p.findAnnotation<Header>() != null -> p.findAnnotation<Header>()!!.let { anno ->
                                    val name = if (anno.name.isBlank()) p.name!! else anno.name
                                    when {
                                        p.type.isClass<String>() -> when {
                                            p.isOptional || p.type.isMarkedNullable -> call.request.headers.get(name)
                                            else -> call.request.headers.get(name) ?: throw InnerServiceException(
                                                HttpStatusCode.BadRequest,
                                                "request miss header ${name}"
                                            )
                                        }
                                        p.type.isClassContainer1<List<*>, String>() -> when {
                                            p.isOptional || p.type.isMarkedNullable -> call.request.headers.getAll(name)
                                            else -> call.request.headers.getAll(name) ?: listOf()
                                        }
                                        else -> log.error("header parameter is not String or List<String> ${target::class.qualifiedName}::${p.name}")
                                    }
                                }
                                p.findAnnotation<Cookies>() != null && p.type.isMarkedNullable == true -> p.findAnnotation<Cookies>()!!.let {
                                    val name = if (it.name.isBlank()) p.name!! else it.name
                                    when {
                                        p.isOptional || p.type.isMarkedNullable -> call.request.cookies[name]
                                        else -> call.request.cookies[name] ?: throw InnerServiceException(
                                            HttpStatusCode.BadRequest,
                                            "request miss cookie ${name}"
                                        )
                                    }
                                }
                                else -> {
                                    p.findAnnotation<RequestBody>()?.let {
                                        when {
                                            p.type.isTypeString -> call.receiveText()
                                            p.type.isClass<InputStream>() -> call.receiveStream()
                                            p.type.isClass<MultiPartData>() -> call.receiveMultipart()
                                            p.type.isClass<ByteReadChannel>() -> call.receiveChannel()
                                            p.type.isClass<Parameters>() -> call.receiveParameters()
                                            p.isOptional -> try {
                                                call.receiveOrNull(
                                                    p.type.kClass!!
                                                )
                                            } catch (e: Exception) {
                                                null
                                            }
                                            p.type.isMarkedNullable -> try {
                                                call.receiveOrNull(
                                                    p.type.kClass!!
                                                )
                                            } catch (e: Exception) {
                                                null
                                            }
                                            else -> try {
                                                call.receive(
                                                    p.type.apply {
                                                        log.trace("parse class of parameter ${p.type}")
                                                    }.kClass!!
                                                )
                                            } catch (e: Throwable) {
                                                log.trace("error parse body $p ", e)
                                                throw InnerServiceException(
                                                    HttpStatusCode.BadRequest,
                                                    "request not satisfied with query parameter ${p.name} of ${p.type.toString()}",
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
                                                    throw InnerServiceException(
                                                        HttpStatusCode.BadRequest,
                                                        "request not satisfied with query parameter ${p.name}",
                                                        e
                                                    )
                                                }
                                            }
                                                ?: when {
                                                    p.type.isMarkedNullable -> null
                                                    p.isOptional -> null
                                                    else -> throw InnerServiceException(
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
                                                    throw InnerServiceException(
                                                        HttpStatusCode.BadRequest,
                                                        "request not satisfied with query parameter ${p.name}",
                                                        e
                                                    )
                                                }
                                            }
                                                ?: when {
                                                    p.type.isMarkedNullable -> null
                                                    p.isOptional -> null
                                                    else -> throw InnerServiceException(
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
                                                    throw InnerServiceException(
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
                if (this.exceptionHandlers.isEmpty()) {
                    processCallException(e, call)
                } else {
                    run Break@{
                        exceptionHandlers.forEach {(handler,instance) ->
                            if ((handler.callSuspend(instance,e) as Boolean)){
                                return@Break
                            }
                        }
                    }
                }
            }

        }
        private val exceptionHandlers by lazy {
            ClassManager.getExceptionHanders().map { (funcs, instance) ->
                funcs.map {
                    it to instance
                }
            }.flatten().sortedBy { it.first.findAnnotation<ExceptionHandler>()!!.order }
        }
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

        @KtorExperimentalAPI
        private suspend fun processCallException(e: Throwable, call: ApplicationCall) {
            log.trace("${e is InnerServiceException}")
            when {
                e is InvocationTargetException -> processCallException(
                    e.targetException,
                    call
                )
                e is ServiceException -> {
                    call.respondText(ContentType.Application.Json, e.status) {
                        e.toJsonString(call.request.path(), parseRequest(call.request))
                    }
                }
                e is InnerServiceException && outputInnerServiceException -> {
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
