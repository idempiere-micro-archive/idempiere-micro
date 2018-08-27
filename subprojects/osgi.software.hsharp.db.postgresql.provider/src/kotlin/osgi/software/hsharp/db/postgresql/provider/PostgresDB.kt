package osgi.software.hsharp.db.postgresql.provider

import org.osgi.service.component.annotations.Component
import software.hsharp.api.icommon.IDatabase
import software.hsharp.db.postgresql.provider.PgDB

@Component
class PostgresDB : PgDB(), IDatabase