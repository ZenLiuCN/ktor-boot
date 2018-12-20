package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.Ignore
import cn.zenliu.ktor.boot.annotations.request.Body
import cn.zenliu.ktor.boot.annotations.request.QueryParam
import cn.zenliu.ktor.boot.annotations.request.PathVariable
import cn.zenliu.ktor.boot.annotations.routing.RawRoute
import cn.zenliu.ktor.boot.annotations.routing.RequestMapping
import cn.zenliu.ktor.boot.context.BeanManager.executeKFunction
import cn.zenliu.ktor.boot.exceptions.NotValidRawRouteFunction
import cn.zenliu.ktor.boot.exceptions.ServiceException
import cn.zenliu.ktor.boot.http.ServerSentEvent
import cn.zenliu.ktor.boot.http.StatusResponse
import cn.zenliu.ktor.boot.jackson.toClass
import cn.zenliu.ktor.boot.reflect.*
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.log
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.path
import io.ktor.request.uri
import io.ktor.request.userAgent
import io.ktor.response.respond
import io.ktor.response.respondTextWriter
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.cio.toByteArray
import io.ktor.util.pipeline.PipelineContext

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
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
    private val log by lazy { application.log }

    init {
        application.routing {
            routeFun.forEach { (fns, instance) ->
                fns.forEach { fn ->
                    when {
                        fn.parameters.size != 2 -> throw NotValidRawRouteFunction(fn, instance::class)
                        (fn.parameters.first().kind == KParameter.Kind.EXTENSION_RECEIVER || fn.parameters.first().kind == KParameter.Kind.INSTANCE) && fn.parameters[1].type.isClass(
                            Routing::class
                        )
                        -> executeKFunction(instance, fn, this@routing)
                        else -> throw NotValidRawRouteFunction(fn, instance::class)
                    }
                }
            }
            clazz.forEach { (kclass, instance) ->
                application.log.debug("regist endpoint: ${kclass.qualifiedName}")
                kclass.declaredFunctions.apply {
                    filter { it.findAnnotation<RawRoute>() != null }.forEach { fn ->
                        when {
                            fn.parameters.size != 2 -> throw NotValidRawRouteFunction(fn, instance::class)
                            (fn.parameters.first().kind == KParameter.Kind.EXTENSION_RECEIVER || fn.parameters.first().kind == KParameter.Kind.INSTANCE) && fn.parameters[1].type.isClass(
                                Routing::class
                            )
                            -> executeKFunction(instance, fn, this@routing)
                            else -> throw NotValidRawRouteFunction(fn, instance::class)
                        }
                    }
                    filter { it.findAnnotation<RequestMapping>() != null }.forEach { fn ->
                        annotationEndPointProcessor(
                            kclass.findAnnotationSafe<RequestMapping>()?.path,
                            fn,
                            this@routing,
                            instance
                        )
                    }
                }
            }

        }
    }

    private fun annotationEndPointProcessor(
        clazzPath: String? = null,
        fn: KFunction<*>,
        routing: Routing,
        fnInstance: Any
    ) {
        routing {
            log.debug("${fn.name}=>${fn.annotations}")
            fn.findAnnotation<RequestMapping>()?.let { anno ->
                (clazzPath?.let { it + anno.path }?.replace("//", "/") ?: anno.path).let { path ->
                    log.info("regist [${anno.method.name}]($path) handler: ${fnInstance::class.simpleName}::${fn.name}")
                    route(path, anno.method.toHttpMethod) {
                        handle {
                            annotationParameterProcessor(fnInstance, this, fn)
                        }
                    }
                }
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
                else -> fn.callSuspend(
                    *fn.parameters.map { p ->
                        when {
                            p.kind == KParameter.Kind.INSTANCE || p.kind == KParameter.Kind.EXTENSION_RECEIVER -> target
                            p.type.javaType.typeName == ApplicationCall::class.java.name -> call
                            p.type.javaType.typeName == PipelineContext::class.java.name -> ctx
                            else -> {
                                p.findAnnotation<Body>()?.let {
                                    call.request.receiveChannel().toByteArray().let { bytes ->
                                        when {
                                            bytes.isEmpty() && (p.isOptional || p.type.isMarkedNullable) -> null
                                            bytes.isEmpty() -> throw ServiceException(
                                                HttpStatusCode.BadRequest,
                                                "request not satisfied"
                                            )
                                            else -> try {
                                                bytes.toClass(Class.forName(p.type.javaType.typeName))
                                            } catch (e: Throwable) {
                                                throw ServiceException(
                                                    HttpStatusCode.BadRequest,
                                                    "request not satisfied"
                                                )
                                            }
                                        }
                                    }
                                } ?: p.findAnnotation<QueryParam>()?.let {
                                    call.parameters[if (it.name.isNotBlank()) it.name else p.name!!]?.let {
                                        parseParameter(
                                            p.type,
                                            it
                                        )
                                    } ?: if (p.type.isMarkedNullable) {
                                        null
                                    } else {
                                        throw ServiceException(
                                            HttpStatusCode.BadRequest,
                                            "request not satisfied"
                                        )
                                    }
                                } ?: p.findAnnotation<PathVariable>()?.let {
                                    call.parameters[if (it.name.isNotBlank()) it.name else p.name!!]?.let {
                                        parseParameter(
                                            p.type,
                                            it
                                        )
                                    } ?: if (p.type.isMarkedNullable) {
                                        null
                                    } else {
                                        throw ServiceException(
                                            HttpStatusCode.BadRequest,
                                            "request not satisfied"
                                        )
                                    }
                                }
                            }
                        }
                    }.toTypedArray()
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
            when (e) {
                is InvocationTargetException -> processCallException(
                    e.targetException,
                    call,
                    log
                )
                is ServiceException -> call.respond(e.status, e.toJson().apply {
                    if (e.withPath) {
                        put("path", call.request.path())
                    }
                    if (e.withRequest) {
                        put(
                            "request", parseRequest(call.request)
                        )
                    }
                })
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
