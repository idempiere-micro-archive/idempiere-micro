package software.hsharp.db.h2.provider

import software.hsharp.api.icommon.IDatabaseSetup

data class H2DatabaseSetup(
        val url : String,
        val userName : String,
        val password : String
) : IDatabaseSetup