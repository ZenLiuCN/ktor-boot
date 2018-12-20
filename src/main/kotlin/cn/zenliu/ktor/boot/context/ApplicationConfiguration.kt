package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.ApplicationConfiguration
import io.ktor.application.Application

/**
 * ApplicationConfiguration  interface
 */
@ApplicationConfiguration
interface ApplicationConfiguration {
    @ApplicationConfiguration
    fun applicationConfiguration(app: Application)
}
