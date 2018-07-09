package software.hsharp.business.services

import software.hsharp.business.models.ICustomer
import software.hsharp.core.models.IResult

interface ICustomersResult : IResult {
    val customers : Array<ICustomer>
}