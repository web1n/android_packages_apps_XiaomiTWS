package org.lineageos.xiaomi_tws.mma

open class MMARequestBuilder<T>(
    internal var request: MMARequest,
    internal var handler: ((MMAResponse) -> T)
)
