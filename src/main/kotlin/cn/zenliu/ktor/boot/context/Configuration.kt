package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.Configuration
import io.ktor.application.Application

@Configuration
interface Configuration {
    @Configuration
    fun applicationConfiguration(app: Application)
}
