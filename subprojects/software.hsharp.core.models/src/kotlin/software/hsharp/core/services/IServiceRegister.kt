package software.hsharp.core.services

interface IServiceRegister<T : IService> : IService {
    fun registerService(service: T)
    val services: Array<T>
}