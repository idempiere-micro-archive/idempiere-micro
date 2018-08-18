package software.hsharp.core.services

import java.util.concurrent.ScheduledThreadPoolExecutor

interface ISystem {
    fun startup()
    fun getThreadPoolExecutor(): ScheduledThreadPoolExecutor
}

interface ISystemImpl : ISystem
interface ISystemEndpoint : ISystem