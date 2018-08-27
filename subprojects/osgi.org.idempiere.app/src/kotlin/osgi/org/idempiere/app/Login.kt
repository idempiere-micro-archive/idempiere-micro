package osgi.org.idempiere.app

import org.osgi.service.component.annotations.Component
import software.hsharp.core.services.ILoginUtility
import java.util.Properties

@Component
class Login : org.idempiere.app.Login(), ILoginUtility {
    override fun init(ctx: Properties): ILoginUtility {
        return this
    }
}