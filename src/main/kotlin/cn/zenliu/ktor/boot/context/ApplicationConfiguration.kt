/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

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
