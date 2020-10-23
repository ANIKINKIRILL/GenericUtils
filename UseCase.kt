abstract class UseCase<out Type, in Params> {
    private var backgroundContext: CoroutineContext = Dispatchers.IO
    private var foregroundContext: CoroutineContext = Dispatchers.Main
    private var parentJob: Job = Job()

    abstract suspend fun run(params: Params): Either<Failure, Type>

    operator fun invoke(params: Params, onResult: (Either<Failure, Type>) -> Unit = {}) {
        unsubscribe()
        parentJob = Job()
        CoroutineScope(foregroundContext + parentJob).launch {
            val result = withContext(backgroundContext) { run(params) }
            onResult(result)
        }
    }

    fun unsubscribe() {
        parentJob.cancelChildren()
        parentJob.cancel()
    }
}
