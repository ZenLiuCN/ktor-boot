package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.Ignore
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.log
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

//<editor-fold desc="Context">
internal val BootCoroutineContext = GlobalScope.coroutineContext
typealias HandlerContext = PipelineContext<Unit, ApplicationCall>

/**
 * Application Context manage all application dependencies
 */
@Ignore
object Context : CoroutineScope {
    override val coroutineContext: CoroutineContext = BootCoroutineContext
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
        RouteManager.instance(application, ClassManager.getControllers(),ClassManager.getRouteFunctions())
    }

    private fun configuration(app: Application) =
        ClassManager.getConfigurations().filter { !it.clazz.isAbstract }.map {
            it to BeanManager.instanceOf(it)
        }.forEach { (container, instance) ->
            if (container.isConfigurationClass) {
                (instance as ApplicationConfiguration).applicationConfiguration(app)
            } else {
                container.configurationFunctions().forEach {
                    BeanManager.executeKFunction(instance, it, app)
                }
            }
        }






}

//</editor-fold>
