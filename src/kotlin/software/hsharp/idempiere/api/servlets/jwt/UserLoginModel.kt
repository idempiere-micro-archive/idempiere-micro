package software.hsharp.idempiere.api.servlets.jwt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.idempiere.common.util.KeyNamePair
import software.hsharp.api.helpers.jwt.ILogin
import software.hsharp.api.helpers.jwt.ILoginResponse
import software.hsharp.api.helpers.jwt.IUserLoginModel
import software.hsharp.core.models.INameKeyPair

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserLoginModel(
    override val loginName: String,
    override val password: String,
    override val clientId: Int?,
    override val roleId: Int?,
    override val orgId: Int?,
    override val warehouseId: Int?,
    override val language: String?
)
    : IUserLoginModel, ILogin {
    constructor() : this("",
    "",
    null, // System ClientId = 0
    null,
    null,
    null,
    "en_US")

    constructor(userName: String, password: String) : this(userName,
        password,
        null, // System ClientId = 0
        null,
        null,
        null,
        "en_US"
    )
}

data class UserLoginModelResponse(
    override val logged: Boolean,
    val clients: Array<INameKeyPair>,
    val roles: Array<INameKeyPair>?,
    val orgs: Array<INameKeyPair>?,
    val warehouses: Array<INameKeyPair>?,
    override val token: String?
) : ILoginResponse {
    constructor() : this(
            false,
            Array<INameKeyPair>(0, { _ -> KeyNamePair(0, "dummy") }),
            null,
            null,
            null,
            null
    )
}
