package software.hsharp.business.services

import software.hsharp.business.models.ICustomer
import software.hsharp.business.models.ICategory
import software.hsharp.core.services.IService
import software.hsharp.core.services.IServiceRegister

interface ICustomers : IService {
    fun getAllCustomers() : ICustomersResult
    fun getCustomerById( id : Int ) : ICustomerResult
    fun getCustomersByAnyCategory( categories : Array<ICategory> ) : ICustomersResult
}

interface ICustomersImpl : ICustomers
interface ICustomersEndpoint : ICustomers

interface ICustomersServiceRegister : IServiceRegister<ICustomers> {
}
