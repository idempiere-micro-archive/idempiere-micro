package software.hsharp.idempiere.api.test

import software.hsharp.core.services.ILoginUtility
import java.util.*

class DummyLogin : org.idempiere.app.Login(), ILoginUtility {
    override fun init(ctx: Properties): ILoginUtility {
        return this
    }

}