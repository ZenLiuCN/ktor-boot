package cn.zenliu.ktor.boot.http

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
