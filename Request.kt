package online.kruzhok.remote.base

import online.kruzhok.domain.base.type.Either
import online.kruzhok.domain.base.type.Failure
import online.kruzhok.domain.base.type.None
import online.kruzhok.remote.base.service.ServiceFactory
import retrofit2.Call
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Request @Inject constructor(private val networkHandler: NetworkHandler) {

    fun <T : BaseResponse, R> make(call: Call<T>, transform: (T) -> R): Either<Failure, R> {
        return when (networkHandler.isConnected) {
            true -> execute(call, transform)
            false -> Either.Left(Failure.NetworkConnectionError)
        }
    }

    fun <T : BaseResponse> make(call: Call<T>): Either<Failure, None> {
        return when (networkHandler.isConnected) {
            true -> execute(call)
            false -> Either.Left(Failure.NetworkConnectionError)
        }
    }

    private fun <T : BaseResponse, R> execute(
        call: Call<T>,
        transform: (T) -> R
    ): Either<Failure, R> {
        return try {
            val response = call.execute()
            when (response.isSuccessful) {
                true -> Either.Right(transform(response.body()!!))
                false -> Either.Left(response.parseError())
            }
        } catch (exception: Throwable) {
            Either.Left(Failure.ServerError)
        }
    }

    private fun <T : BaseResponse> execute(call: Call<T>): Either<Failure, None> {
        return try {
            val response = call.execute()
            when (response.isSuccessful) {
                true -> Either.Right(None())
                false -> Either.Left(response.parseError())
            }
        } catch (exception: Throwable) {
            Either.Left(Failure.ServerError)
        }
    }
}

fun <T : BaseResponse> Response<T>.parseError(): Failure {
    if (code() == 401) return Failure.TokenError
    val baseResponse = ServiceFactory.gson.fromJson(errorBody()?.string(), BaseResponse::class.java)
    baseResponse.message?.let { message ->
        return when (message) {
            "there is a user has this email",
            "email already exists" -> Failure.EmailAlreadyExistError
            "error in email or password" -> Failure.AuthError
            "invalid_token" -> Failure.TokenError
            "this contact is already in your friends list" -> Failure.AlreadyFriendError
            "already found in your friend requests",
            "you requested adding this friend before" -> Failure.AlreadyRequestedFriendError
            "No Contact has this email" -> Failure.ContactNotFoundError
            else -> Failure.ServerError
        }
    }

    baseResponse.errors?.let { errors ->
        errors.email?.let { errorsList ->
            for (error in errorsList) {
                when (error) {
                    "Данный email уже занят" -> return Failure.EmailAlreadyExistError
                }
            }
        }
        errors.nickname?.let { errorsList ->
            for (error in errorsList) {
                when (error) {
                    "Данный Никнейм уже занят" -> return Failure.NicknameAlreadyExists
                }
            }
        }
        errors.id?.let { errorsList ->
            for (error in errorsList) {
                when (error) {
                    "Жалоба на данный комментарий уже отправлена" -> return Failure.AlreadyReportCommentError
                    "Данный Никнейм уже занят" -> return Failure.NicknameAlreadyExists
                    "Жалоба на данный проект уже отправлена" -> return Failure.AlreadyReportProjectError
                }
            }
        }
        errors.userId?.let { errorsList ->
            for (error in errorsList) {
                when (error) {
                    "Email пользователя уже подтверждён" -> return Failure.EmailAlreadyConfirmedError
                }
            }
        }
        errors.code?.let { errorsList ->
            for (error in errorsList) {
                when (error) {
                    "Ошибка подтверждения email" -> return Failure.EmailConfirmationError
                }
            }
        }
    }
    return Failure.ServerError
}