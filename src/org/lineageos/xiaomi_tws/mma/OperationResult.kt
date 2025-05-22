package org.lineageos.xiaomi_tws.mma

sealed class OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>()
    data class Error(val exception: Throwable) : OperationResult<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }

    inline fun getOrElse(onError: (exception: Throwable) -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> onError(exception)
    }

    inline fun onSuccess(action: (value: T) -> Unit): OperationResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (exception: Throwable) -> Unit): OperationResult<T> {
        if (this is Error) action(exception)
        return this
    }
}
