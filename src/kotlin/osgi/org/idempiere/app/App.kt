package osgi.org.idempiere.app

import org.osgi.service.component.annotations.Component
import software.hsharp.core.services.ISystemImpl

@Component
class App : org.idempiere.app.Micro(), ISystemImpl