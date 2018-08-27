package osgi.software.hsharp.business.core

import aQute.bnd.annotation.component.Component
import software.hsharp.core.models.IDataService

@Component
class DataService : software.hsharp.business.core.DataService(), IDataService