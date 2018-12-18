/*
 * Copyright (c) 2018.
 * Program and supplies by Zen.Liu<lcz20@163.com>
 */

package cn.zenliu.ktor.boot


import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.ContentNegotiation
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.ApplicationRequest
import io.ktor.request.path
import io.ktor.request.uri
import io.ktor.request.userAgent
import io.ktor.response.respond
import io.ktor.response.respondTextWriter
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.cio.toByteArray
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.JarURLConnection
import java.net.URL
import java.time.Instant
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KParameter.Kind.EXTENSION_RECEIVER
import kotlin.reflect.KParameter.Kind.INSTANCE
import kotlin.reflect.KType
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.kotlinFunction
import kotlin.streams.toList

//<editor-fold desc="KClassExt">
@Suppress("NOTHING_TO_INLINE")
val String.tryKClass: KClass<*>?
    inline get() = try {
        Thread.currentThread().contextClassLoader.loadClass(this).kotlin
    } catch (e: ClassNotFoundException) {
        null
    }
@Suppress("NOTHING_TO_INLINE")
val String.classExists: Boolean
    inline get() = try {
        Thread.currentThread().contextClassLoader.loadClass(this)
        true
    } catch (e: ClassNotFoundException) {
        false
    }

fun KType.isClass(cls: KClass<*>): Boolean = this.classifier == cls

val KType.isTypeString: Boolean get() = this.isClass(String::class)
val KType.isTypeInt: Boolean get() = this.isClass(Int::class) || this.isClass(java.lang.Integer::class)
val KType.isTypeLong: Boolean get() = this.isClass(Long::class) || this.isClass(java.lang.Long::class)
val KType.isTypeByte: Boolean get() = this.isClass(Byte::class) || this.isClass(java.lang.Byte::class)
val KType.isTypeShort: Boolean get() = this.isClass(Short::class) || this.isClass(java.lang.Short::class)
val KType.isTypeChar: Boolean get() = this.isClass(Char::class) || this.isClass(java.lang.Character::class)
val KType.isTypeBoolean: Boolean get() = this.isClass(Boolean::class) || this.isClass(java.lang.Boolean::class)
val KType.isTypeFloat: Boolean get() = this.isClass(Float::class) || this.isClass(java.lang.Float::class)
val KType.isTypeDouble: Boolean get() = this.isClass(Double::class) || this.isClass(java.lang.Double::class)
val KType.isTypeByteArray: Boolean get() = this.isClass(ByteArray::class)
//val KType.isTypeUInt: Boolean get() = this.isClass(UInt::class)

val KType.kClass: KClass<*>?
    get() = when {
        isTypeString -> String::class
        isTypeInt -> Int::class
        isTypeLong -> Long::class
        isTypeByte -> Byte::class
        isTypeShort -> Short::class
        isTypeChar -> Char::class
        isTypeBoolean -> Boolean::class
        isTypeFloat -> Float::class
        isTypeDouble -> Double::class
        isTypeByteArray -> ByteArray::class
        else -> this.javaType.typeName.tryKClass
    }

val KType.simpleDefaultValue: Any?
    get() = when {
        isTypeString -> ""
        isTypeInt -> 0
        isTypeLong -> 0L
        isTypeByte -> 0.toByte()
        isTypeShort -> 0.toShort()
        isTypeChar -> 0.toChar()
        isTypeBoolean -> false
        isTypeFloat -> 0.toFloat()
        isTypeDouble -> 0.toDouble()
        isTypeByteArray -> "".toByteArray()
        else -> null
    }

inline fun <reified T : Annotation> KClass<*>.findAnnotationSafe() = try {
    this.annotations.firstOrNull { it is T } as T?
} catch (e: UnsupportedOperationException) {
    this.java.annotations.firstOrNull { it is T } as T?
}

@Suppress("NOTHING_TO_INLINE")
inline val KClass<*>.declaredFunctionsSafe
    get() = try {
        this.declaredFunctions
    } catch (e: UnsupportedOperationException) {
        this.java.methods.map { it.kotlinFunction }.filterNotNull()
    }

//</editor-fold>
//<editor-fold desc="Jackson">
object JsonMapper {
    private var mapperObj = ObjectMapper()
    var mapper
        get() = this.mapperObj
        set(value) {
            this.mapperObj = initMapper(value)
        }

    @JvmStatic
    fun initMapper(m: ObjectMapper = this.mapper) =
        m.registerKotlinModule().findAndRegisterModules().registerModule(SimpleModule().apply {
            registJavaTimeInstant()
            registJodaDateTime()
        })

    //<editor-fold desc="JacksonExt">
    @JvmStatic
    inline fun <reified T> SimpleModule.registerDeserializer(crossinline deserializer: (p: JsonParser, dctx: DeserializationContext) -> T) =
        this.addDeserializer(T::class.java, object : JsonDeserializer<T>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T = deserializer(p, ctxt)

        })

    @JvmStatic
    inline fun <reified T> SimpleModule.registerSerializer(crossinline serializer: (value: T, gen: JsonGenerator?, serializers: SerializerProvider?) -> Unit) =
        this.addSerializer(T::class.java, object : JsonSerializer<T>() {
            override fun serialize(value: T, gen: JsonGenerator?, serializers: SerializerProvider?) {
                serializer.invoke(value, gen, serializers)
            }
        })

    @JvmStatic
    fun SimpleModule.registJavaTimeInstant() {
        "java.time.Instant".tryKClass?.let {
            registerDeserializer { p, _ ->
                when {
                    p.text.isNullOrEmpty() -> null
                    p.text == "null" -> null
                    else -> Instant.parse(p.text)
                }/*.apply {
                while (!p.isClosed) {
                    p.nextToken()
                }
            }*/

            }
            registerSerializer<Instant?> { value, gen, _ ->
                gen?.writeRawValue(value?.let {
                    "\"${value.toString()}\""
                }
                    ?: "null")
            }
        }
    }

    @JvmStatic
    fun SimpleModule.registJodaDateTime() {
        "org.joda.time.DateTime".tryKClass?.let {
            registerDeserializer { p, dctx ->
                when {
                    p.text.isNullOrEmpty() -> null
                    p.text == "null" -> null
                    else -> DateTime.parse(p.text)
                }
            }
            registerSerializer<DateTime?> { value, gen, _ ->
                gen?.writeRawValue(value?.let {
                    "\"${value.toString()}\""
                }
                    ?: "null")
            }
        }

    }

    //</editor-fold>
    @JvmStatic
    fun toJsonNode(value: Any?) = mapper.readTree(
        when (value) {
            is String -> value
            is ByteArray -> value.toString()
            else -> mapper.writeValueAsString(value)
        }
    )!!

    @JvmStatic
    fun <T> toClass(value: Any?, clazz: Class<T>) = when (value) {
        is String -> JsonMapper.mapper.readValue(value, clazz)
        is ByteArray -> JsonMapper.mapper.readValue(value, clazz)
        else -> JsonMapper.mapper.readValue(JsonMapper.mapper.writeValueAsString(value), clazz)
    }!!
}

//<editor-fold desc="Extension">

val Any?.toJsonNode: JsonNode
    get() = JsonMapper.toJsonNode(this)

inline fun <reified T> Any?.toClass(): T = JsonMapper.toClass(this, T::class.java)

fun <T> Any?.toClass(clazz: Class<T>): T = JsonMapper.toClass(this, clazz)
//</editor-fold>
//</editor-fold>


//<editor-fold desc="Default">
@Configuration
class DefaultConfiguration : IBootConfiguration {
    override fun applicationConfiguration(app: Application) {
        app.log.info("default configuration running")
        app.apply {
            if ("io.ktor.features.ContentNegotiation".classExists &&
                "com.fasterxml.jackson.databind.ObjectMapper".classExists
            ) {
                app.log.info("default install contentNegotiation of fastxml jackson")
                install(ContentNegotiation) {
                    jackson {
                        JsonMapper.mapper = this
                    }
                }
            }
        }
    }

}
//</editor-fold>

//<editor-fold desc="Exceptions">
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

data class CanNotCreateInstanceByConstructor(val fn: KFunction<*>, val throwable: Throwable? = null) :
    Throwable("can not instance ${fn.returnType.javaType.typeName} by constructor ${fn.parameters}", throwable)

data class CanNotCreateInstanceClass(val fn: KClass<*>, val throwable: Throwable? = null) :
    Throwable("can not instance ${fn.java.typeName}", throwable)
//</editor-fold>

//<editor-fold desc="StatusResponse">
data class StatusResponse(
    val statusCode: HttpStatusCode = HttpStatusCode.OK,
    val body: Any? = null,
    val headers: Map<String, String>? = null,
    val withSafeHeader: Boolean = true
)
//</editor-fold>


//<editor-fold desc="Annotations">
enum class HTTPMETHOD {
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

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Configuration(val order: Int = 0)

@Target(AnnotationTarget.CLASS)
annotation class Controller(val name: String = "", val order: Int = 0)

@Target(AnnotationTarget.CLASS)
annotation class Singleton(val name: String = "")

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
annotation class AutoWire(val name: String)

/**
 * define bean class alias
 * @property name String
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class Name(val name: String)

/**
 * define extra package to scan
 * @property packages Array<String>
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class ScanPackage(val packages: Array<String>)

/**
 * define order
 * @property value Int
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class Order(val value: Int = Int.MAX_VALUE)

/**
 * define router handelr
 * @property path String
 * @property method HTTPMETHOD
 * @constructor
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@MustBeDocumented
annotation class RequestMapping(val path: String, val method: HTTPMETHOD = HTTPMETHOD.GET)

/**
 * define a RawRoute configuration function
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class RawRoute()

/*
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class GET(val path: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class POST(val path: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class PUT(val path: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class PATCH(val path: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class DELETE(val path: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class OPTIONS(val path: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class HEAD(val path: String)*/

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Body

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter(val name: String = "")

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class PathVariable(val name: String = "")
//</editor-fold>

//<editor-fold desc="ServerSentEvent">
data class ServerSentEvent(val data: String?, val event: String? = null, val id: String? = null) {
    override fun toString(): String = buildString {
        id?.let { append("id:$it\n") }
        event?.let { append("event:$event\n") }
        data?.let {
            data.lines().forEach {
                append("data:$it\n")
            }
        }
    }
}
//</editor-fold>

//<editor-fold desc="BootConfig">
@Configuration
interface IBootConfiguration {
    @Configuration
    fun applicationConfiguration(app: Application)
}
//</editor-fold>

//<editor-fold desc="Context">
internal val DiRootCoroutineContext = GlobalScope.coroutineContext
typealias HandlerContext = PipelineContext<Unit, ApplicationCall>

object Context : CoroutineScope {
    override val coroutineContext: CoroutineContext = DiRootCoroutineContext
    /**
     * start application
     * @param clazz KClass<*>
     * @param application Application
     */
    fun start(clazz: KClass<*>, application: Application) {
        ClassManager.register(clazz)
        application.log.debug("find bean classes: ${ClassManager.clazzRegistry}")
        configuration(application)
        application.log.debug("configure application: ${ClassManager.getConfigurations()}")
        RouterManager.instance(application, ClassManager.getEndpoints())
    }

    private fun configuration(app: Application) =
        ClassManager.getConfigurations().filter { !it.clazz.isAbstract }.map {
            it to BeanManager.instanceOf(it)
        }.forEach { (container, instance) ->
            if (container.isConfigurationClass) {
                (instance as IBootConfiguration).applicationConfiguration(app)
            } else {
                container.configurationFunctions().forEach {
                    BeanManager.excuteFunction(instance, it, app)
                }
            }
        }

    data class BeanContainer(
        val pkg: String = "",
        val name: String = "",
        val path: String = "",
        val clazz: KClass<*>
    ) {
        val annotations: List<Annotation> = clazz.java.annotations.toList()
        val isSingleton: Boolean by lazy { clazz.findAnnotationSafe<Singleton>()?.let { true } ?: false }
        val isEndpoint: Boolean  by lazy { clazz.findAnnotationSafe<Controller>()?.let { true } ?: false }
        val alias: String? by lazy { (annotations.find { it is Name } as? Name)?.name }
        val isConfigurationClass: Boolean by lazy {
            clazz.findAnnotationSafe<Configuration>() != null && clazz.isSubclassOf(IBootConfiguration::class)
        }
        val hasConfigurationFunction: Boolean by lazy {
            clazz.declaredFunctionsSafe.any {
                (it.annotations.find { it is Configuration } != null) && it.parameters.find {
                    it.type.isClass(
                        Application::class
                    )
                } != null
            }
        }

        fun configurationFunctions(): Set<KFunction<*>> = clazz.declaredFunctions
            .filter {
                (it.annotations.find { it is Configuration } != null) && it.parameters.find {
                    it.kind == KParameter.Kind.VALUE && it.type.isClass(
                        Application::class
                    )
                } != null
            }.toSet()

    }

    /**
     * ClassManager
     */
    internal object ClassManager : CoroutineScope {
        override val coroutineContext: CoroutineContext = DiRootCoroutineContext
        val clazzRegistry = mutableMapOf<String, BeanContainer>()

        fun beanClass(name: String) = clazzRegistry[name]
        fun beanClass(clazz: KClass<*>) = clazzRegistry[clazz.qualifiedName]
        fun beanClass(clazz: Class<*>) = clazzRegistry[clazz.name]

        init {
            //root package
            scanPackages(setOf("cn.zenliu.ktor.boot"))
        }

        /**
         * regist package from KClass
         * @param clazz KClass<*>
         */
        fun register(clazz: KClass<*>) {
            clazz.annotations
                .filter { it is ScanPackage }
                .map { (it as ScanPackage).packages.toList() }
                .flatten().filter { it.contains(".") }.toMutableSet().let {
                    it.add(clazz.java.`package`.name)
                    scanPackages(it.toSet())
                }
        }

        /**
         * scan package classes
         * @param packages Set<String>
         */
        fun scanPackages(packages: Set<String>) {
            clazzRegistry.putAll(
                packages.map { pkg ->
                    Thread
                        .currentThread()
                        .contextClassLoader
                        .getResource(pkg.replace(".", "/")).let {
                            when (it.protocol) {
                                "jar" -> getJarClasses(it, pkg)
                                "file" -> getFileClasses(it, pkg, it.path.let { pth ->
                                    pkg.replace(".", "/").let { p ->
                                        if (pth.contains(p)) pth.substring(0, pth.lastIndexOf(p))
                                        else pth
                                    }
                                })
                                else -> setOf()
                            }
                        }
                }.flatten().filter { it != null }.map { it!!.pkg + "." + it.name to it }.toMap()
                    .toMutableMap()
            )

        }

        /**
         * fetch all configuration class or class with configuration function
         * @return Map<String, BeanContainer>
         */
        fun getConfigurations() =
            clazzRegistry.filter { it.value.isConfigurationClass || it.value.hasConfigurationFunction }.map { it.value }.sortedBy {
                it.clazz.findAnnotationSafe<Order>()?.value ?: 0
            }

        fun getEndpoints() =
            clazzRegistry
                .filter { it.value.isEndpoint }
                .map { it.value.clazz to BeanManager.instanceOf(it.value) }.sortedBy {
                    it.first.findAnnotationSafe<Order>()?.value ?: 0
                }

        private fun getFileClasses(url: URL, pkg: String, root: String): Set<BeanContainer> =
            File(url.file).listFiles().map {
                when {
                    it.isFile -> it.name.let {
                        when {
                            !it.contains("$") && it.endsWith(".class") -> {
                                try {
                                    setOf(
                                        BeanContainer(
                                            pkg = pkg,
                                            path = url.path + it,
                                            name = it.replace(".class", ""),
                                            clazz = Class.forName(pkg + "." + it.replace(".class", "")).kotlin
                                        )
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    setOf<BeanContainer>()
                                }
                            }
                            else -> setOf()
                        }
                    }
                    it.isDirectory -> getFileClasses(
                        it.toURI().toURL(),
                        it.toURI().toURL().path.let { it.removeSuffix("/") }.removePrefix(root).replace("/", "."), root
                    )
                    else -> setOf()
                }
            }.filter { !it.isEmpty() }.flatten().toSet()

        private fun getJarClasses(url: URL, pkg: String) =
            (url.openConnection() as? JarURLConnection)
                ?.jarFile
                ?.stream()
                ?.map {
                    it.name
                }
                ?.filter {
                    it.replace("/", ".").startsWith(pkg) &&
                            !it.contains("$") &&
                            it.endsWith(".class")
                }
                ?.map {
                    try {
                        BeanContainer(
                            pkg = pkg,
                            path = url.path + it.replace(pkg, ""),
                            name = it.substringAfterLast("/").replace(".class", ""),
                            clazz = Class.forName(it.replace("/", ".").replace(".class", "")).kotlin
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                ?.filter { it != null }
                ?.toList()
                ?.toSet()
                ?: setOf()
    }

    /**
     * BeanManager
     */
    internal object BeanManager : CoroutineScope {
        override val coroutineContext: CoroutineContext = DiRootCoroutineContext
        private val beanRegistry = mutableMapOf<String, Any>()
        /**
         * create instance from container
         * @param container BeanContainer
         * @return Any
         */
        fun instanceOf(container: BeanContainer) = instanceOf(container.alias ?: container.name, container.clazz)

        /**
         * create kClass Instance
         * @param clazz KClass<*>
         * @return Any
         */
        fun instanceOf(name: String, clazz: KClass<*>): Any = when {
            name.isEmpty() -> Instant.now().let { "${it.toEpochMilli()}${it.nano}" }
            else -> name
        }.let { bname ->
            beanRegistry[bname]
                ?: (clazz.objectInstance
                    ?: clazz.constructors.find { it.parameters.isEmpty() }?.call()
                    ?: clazz.constructors.firstOrNull()?.let { fn ->
                        fn.parameters.map { param ->
                            when {
                                param.isOptional || param.type.isMarkedNullable -> null
                                param.kind == INSTANCE || param.kind == EXTENSION_RECEIVER -> throw CanNotCreateInstanceByConstructor(
                                    fn
                                )
                                else -> param.type.simpleDefaultValue ?: instanceOf(
                                    param.annotations.find { it is AutoWire }?.let { it as? AutoWire }?.name.let { if (it.isNullOrBlank()) null else it }
                                        ?: param.name
                                        ?: ""
                                    , param.type.kClass!!
                                )
                            }
                        }.let {
                            fn.call(*it.toTypedArray())
                        }
                    } ?: throw CanNotCreateInstanceClass(clazz)
                        ).apply { beanRegistry[bname] = this }
        }

        fun excuteFunction(target: Any, fn: KFunction<*>, vararg params: Any) = fn.parameters.map { param ->
            when {
                param.kind == INSTANCE || param.kind == EXTENSION_RECEIVER -> target
                else -> params.find { param.type.isClass(it::class) }
                    ?: param.type.simpleDefaultValue
                    ?: instanceOf(
                        param.annotations.find { it is AutoWire }?.let { it as? AutoWire }?.name.let { if (it.isNullOrBlank()) null else it }
                            ?: param.name
                            ?: "", param.type.kClass!!
                    )
            }
        }.let {
            fn.call(*it.toTypedArray())
        }

        suspend fun asyncExcuteFunction(target: Any, fn: KFunction<*>, vararg params: Any) =
            fn.parameters.map { param ->
                when {
                    param.kind == INSTANCE || param.kind == EXTENSION_RECEIVER -> target
                    else -> params.find { param.type.isClass(it::class) }
                        ?: param.type.simpleDefaultValue
                        ?: instanceOf(
                            param.annotations.find { it is AutoWire }?.let { it as? AutoWire }?.name.let { if (it.isNullOrBlank()) null else it }
                                ?: param.name
                                ?: "", param.type.kClass!!
                        )
                }
            }.let {
                fn.callSuspend(*it.toTypedArray())
            }
    }


    class RouterManager(private val application: Application, val clazz: Collection<Pair<KClass<*>, Any>>) :
        CoroutineScope {
        override val coroutineContext: CoroutineContext = application.coroutineContext
        private val log by lazy { application.log }

        init {
            application.routing {
                clazz.forEach { (kclass, instance) ->
                    application.log.debug("regist endpoint: ${kclass.qualifiedName}")
                    kclass.declaredFunctions.filter { it.findAnnotation<RequestMapping>()!=null }.forEach { fn ->
                        annotationEndPointProcessor(
                            kclass.findAnnotationSafe<RequestMapping>()?.path,
                            fn,
                            this,
                            instance
                        )
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
                                    } ?: p.findAnnotation<Parameter>()?.let {
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

            private var instanceClass: RouterManager? = null
            private val emptyList = mutableListOf<Pair<KClass<*>, Any>>()
            fun instance(application: Application? = null, clazz: Collection<Pair<KClass<*>, Any>> = emptyList) = when {
                application == null && instanceClass == null -> null
                instanceClass == null && application != null -> RouterManager(application, clazz)
                else -> instanceClass
            }
        }
    }

}

//</editor-fold>

/**
 * Start embed server
 * @param args Array<String>
 */
inline fun <reified T : Any> Start(args: Array<String>) {
    embeddedServer(
        Netty
    ) {
        Context.start(T::class, this)
    }.start(wait = true)
}
