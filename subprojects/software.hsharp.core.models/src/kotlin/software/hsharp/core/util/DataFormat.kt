package software.hsharp.core.util

import software.hsharp.core.models.IDataFormat

data class DataFormat(
    override val Key: Int,
    override val name: String
) : IDataFormat {
    override val ID: String
        get() = "" + Key
}
