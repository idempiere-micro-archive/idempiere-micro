package software.hsharp.api.helpers.odata

import javax.ws.rs.core.StreamingOutput
import javax.ws.rs.core.Response

class ResponseCreator(delegate: Response) : ResponseWrapper(delegate) {
    companion object {

        fun OK(entity: StreamingOutput): ResponseCreator {
            var responseBuilder = Response.status(200)
            responseBuilder = responseBuilder.header("Content-Type", "application/json")

            responseBuilder.entity(entity)

            return ResponseCreator(responseBuilder.build())
        }
    }
}