package com.mongodb.quickpr.core

// https://kotlin.christmas/2019/17

sealed class SafeResult<out T, out E> {
    abstract fun isOk(): Boolean
    abstract fun isError(): Boolean
}

class Ok<out T>(val value: T) : SafeResult<T, Nothing>() {
    override fun isOk(): Boolean {
        return true
    }

    override fun isError(): Boolean {
        return false
    }
}

class Err<out E>(val error: E) : SafeResult<Nothing, E>() {
    override fun isOk(): Boolean {
        return false
    }

    override fun isError(): Boolean {
        return true
    }
}

fun <U, T, E> SafeResult<T, E>.map(transform: (T) -> U): SafeResult<U, E> =
    when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

fun <U, T, E> SafeResult<T, E>.mapError(transform: (E) -> U): SafeResult<T, U> =
    when (this) {
        is Ok -> this
        is Err -> Err(transform(error))
    }

fun <U, T, E> SafeResult<T, E>.andThen(transform: (T) -> SafeResult<U, E>): SafeResult<U, E> =
    when (this) {
        is Ok -> transform(value)
        is Err -> this
    }

fun <T, E> runResultTry(block: RunResultTryContext<E>.() -> SafeResult<T, E>): SafeResult<T, E> =
    try {
        RunResultTryContext<E>().block()
    } catch (ex: RunResultTryAbortion) {
        @Suppress("UNCHECKED_CAST")
        Err(ex.err as E)
    }

class RunResultTryContext<E> {
    fun <T> SafeResult<T, E>.abortOnError(): T =
        when (this) {
            is Ok -> value
            is Err -> throw RunResultTryAbortion(error as Any)
        }

    fun abortWithError(error: E) {
        throw RunResultTryAbortion(error as Any)
    }
}

private class RunResultTryAbortion(val err: Any) : Exception()
