package software.hsharp.core.services

interface ISystem {
    fun startup()
}

interface ISystemImpl : ISystem
interface ISystemEndpoint : ISystem