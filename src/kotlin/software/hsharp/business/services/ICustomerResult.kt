package software.hsharp.business.services

import software.hsharp.business.models.ICustomer
import software.hsharp.core.models.IResult

interface ICustomerResult : IResult {
    val customer: ICustomer?
}