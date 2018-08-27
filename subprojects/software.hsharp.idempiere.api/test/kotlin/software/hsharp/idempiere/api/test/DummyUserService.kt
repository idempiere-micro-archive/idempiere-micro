package software.hsharp.idempiere.api.test

import org.compiere.crm.MUser
import software.hsharp.core.models.IUser
import software.hsharp.core.services.IUserServiceImpl
import java.util.*

class DummyUserService: IUserServiceImpl {
    override fun getUser(ctx: Properties, loginName: String): IUser? {
        return MUser.get(ctx, loginName)
    }
}