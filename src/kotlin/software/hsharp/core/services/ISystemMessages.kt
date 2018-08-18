package software.hsharp.core.services

import software.hsharp.core.models.IResult
import software.hsharp.core.models.ISystemMessage
import java.sql.Connection

interface ISystemMessagesResult : IResult {
    val messages: Array<ISystemMessage>
}

interface ISystemMessages {
    fun getActiveMessages(connection: Connection): ISystemMessagesResult
}

interface ISystemMessagesImpl : ISystemMessages
interface ISystemMessagesEndpoint : ISystemMessages
