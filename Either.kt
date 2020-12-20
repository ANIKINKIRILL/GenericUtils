package online.kruzhok.domain.base.type

sealed class Either<out L, out R> {
    data class Left<out L>(val fail: L) : Either<L, Nothing>()
    data class Right<out R>(val success: R) : Either<Nothing, R>()

    val isLeft get() = this is Left<L>
    val isRight get() = this is Right<R>

    fun <L> left(fail: L) = Left(fail)
    fun <R> right(success: R) = Right(success)

    fun fold(functionLeft: (L) -> Any, functionRight: (R) -> Any): Any =
        when (this) {
            is Left -> functionLeft(fail)
            is Right -> functionRight(success)
        }
}

fun <A, B, C> ((A) -> B).compose(f: (B) -> C): (A) -> C = {
    f(this(it))
}

fun <T, L, R> Either<L, R>.flatMap(fn: (R) -> Either<L, T>): Either<L, T> {
    return when (this) {
        is Either.Left -> Either.Left(fail)
        is Either.Right -> fn(success)
    }
}

fun <T, L, R> Either<L, R>.map(fn: (R) -> (T)): Either<L, T> {
    return this.flatMap(fn.compose(::right))
}

fun <L, R> Either<L, R>.onNext(fn: (R) -> Unit): Either<L, R> {
    this.flatMap(fn.compose(::right))
    return this
}

fun <L, R> Either<L, R>.getOrElse(value: R): R {
    return when (this) {
        is Either.Left -> value
        is Either.Right -> success
    }
}