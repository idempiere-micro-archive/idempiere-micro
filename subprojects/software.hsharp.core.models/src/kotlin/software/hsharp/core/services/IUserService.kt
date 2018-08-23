package software.hsharp.core.services

import software.hsharp.core.models.IUser
import java.util.Properties

interface IUserService {
    fun getUser(ctx: Properties, loginName: String): IUser?
}

interface IUserServiceImpl : IUserService
interface IUserServiceEndpoint : IUserService
