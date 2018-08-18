package software.hsharp.idempiere.api.servlets

data class Result(val error: Error)

data class Error(val content: String)
