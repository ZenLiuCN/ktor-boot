package cn.zenliu.ktor.boot.context

import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.config.ApplicationConfig
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.util.KtorExperimentalAPI
import kotlinx.io.charsets.Charset
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

@KtorExperimentalAPI
object PropertiesManager {

    private val applicationIdPath = "ktor.application.id"
    private val hostConfigPath = "ktor.deployment.host"
    private val hostPortPath = "ktor.deployment.port"
    private val hostWatchPaths = "ktor.deployment.watch"
    private val hostSslPortPath = "ktor.deployment.sslPort"
    private val hostSslKeyStore = "ktor.security.ssl.keyStore"
    private val hostSslKeyAlias = "ktor.security.ssl.keyAlias"
    private val hostSslKeyStorePassword = "ktor.security.ssl.keyStorePassword"
    private val hostSslPrivateKeyPassword = "ktor.security.ssl.privateKeyPassword"
    
    private var env:ApplicationEngineEnvironment?=null
    private val properties = mutableMapOf<String, KClass<*>>()
    private var conf: ApplicationConfig? = null
    val config: ApplicationConfig
        get() = if (conf == null) {
            throw Throwable("configuration not init")
        } else {
            conf!!
        }
    val configuration by lazy { ConfigFactory.load() }
    val application: Application
        get() = if (env == null) {
            throw Throwable("application not init")
        } else {
            env.application!!
        }

    fun setConfiguration(args: Array<String>) = commandLineEnvironment(args).apply {
        this@PropertiesManager.env = this
        this@PropertiesManager.conf = this.config
    }
    fun port(default:Int=80)=config.propertyOrNull(hostPortPath)?.getString()?.toInt()?:default
    fun host(default:String="0.0.0.0")=config.propertyOrNull(hostConfigPath)?.getString()?:default
    fun watchPaths(default:List<String> =emptyList())=config.propertyOrNull(hostWatchPaths)?.getList()?:default
    fun getProperties(kClass: KClass<*>)=null //TODO

    fun string(path:String,default:String?=null)=config.propertyOrNull(path)?.getString()?:default
    fun int(path:String,default:Int?=null)=config.propertyOrNull(path)?.getString()?.toIntOrNull()?:default
    fun bool(path:String,default:Boolean?=null)=config.propertyOrNull(path)?.getString()?.toBoolean()?:default
    fun long(path:String,default:Long?=null)=config.propertyOrNull(path)?.getString()?.toLongOrNull()?:default
    fun short(path:String,default:Short?=null)=config.propertyOrNull(path)?.getString()?.toShortOrNull()?:default
    fun float(path:String,default:Float?=null)=config.propertyOrNull(path)?.getString()?.toFloatOrNull()?:default
    fun bigDecimal(path:String,default:BigDecimal?=null)=config.propertyOrNull(path)?.getString()?.toBigDecimalOrNull()?:default
    fun byteArray(path:String,charset:Charset=Charsets.UTF_8,default:ByteArray?=null)=config.propertyOrNull(path)?.getString()?.toByteArray(charset)?:default
    fun byte(path:String,default:Byte?=null)=config.propertyOrNull(path)?.getString()?.toByteOrNull()?:default
    fun bigInteger(path:String,default:BigInteger?=null)=config.propertyOrNull(path)?.getString()?.toBigIntegerOrNull()?:default
    fun double(path:String,default:Double?=null)=config.propertyOrNull(path)?.getString()?.toDoubleOrNull()?:default

    fun raw(path:String)=config.propertyOrNull(path)
    fun configurate(path:String)=config.config(path)
}
