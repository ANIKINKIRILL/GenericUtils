package com.anikinkirill.mviplayground.util

data class DataState<T>(
    var error: Event<StateError>? = null,       // Event and StateError from StateResource helper class
    var loading: Loading = Loading(false),      // Loading from StateResource helper class
    var data: Data<T>? = null                   // Data from StateResource helper class
) {

    companion object {

        fun <T> error(response: Response) : DataState<T> {
            return DataState(error = Event(StateError(response)))
        }

        fun <T> loading(isLoading: Boolean, cachedData: T? = null) : DataState<T> {
            return DataState(null, Loading(isLoading), Data(Event.dataEvent(cachedData), null))
        }

        fun <T> data(data: T? = null, response: Response? = null) : DataState<T> {
            return DataState(null, Loading(false), Data(Event.dataEvent(data), Event.responseEvent(response)))
        }

    }

}
