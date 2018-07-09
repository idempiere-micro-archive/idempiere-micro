package software.hsharp.api.helpers.jwt

import software.hsharp.core.services.IService
import software.hsharp.core.services.IServiceRegister

interface ILogin {
    val loginName : String
    val password : String
    val clientId : Int?
    val roleId : Int?
    val orgId : Int?
    val warehouseId : Int?
    val language : String?
}

interface ILoginResponse {
    val logged : Boolean
    val token : String?
}

interface ILoginService : IService {
    val uniqueKey : String
    fun login( login : ILogin ) : ILoginResponse
}

interface ILoginServiceRegister : IServiceRegister<ILoginService> {
}
