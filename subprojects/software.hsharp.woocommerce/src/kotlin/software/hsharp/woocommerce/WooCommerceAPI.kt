package software.hsharp.woocommerce

import software.hsharp.woocommerce.impl.EndpointBaseType
import software.hsharp.woocommerce.impl.MappedOrder
import software.hsharp.woocommerce.impl.WooCommerceBase
import software.hsharp.woocommerce.impl.IConfig
import software.hsharp.woocommerce.impl.ApiVersionType
import software.hsharp.woocommerce.impl.MappedProduct

class WooCommerceAPI(config: IConfig, apiVersion: ApiVersionType) : IWooCommerce, WooCommerceBase(config, apiVersion) {
    override fun getOrder(id: Int): SingleOrder {
        val order: SingleOrder =
                get<SingleOrder>(
                        EndpointBaseType.ORDERS.value,
                        id,
                        { it as SingleOrder }
                )
        return order
    }

    override fun getOrders(): Array<SingleOrder> {
        val orders: Array<SingleOrder> =
                getAll<Map<String, Any?>, MappedOrder>(
                        EndpointBaseType.ORDERS.value
                ) { MappedOrder(it) }.map { getOrder(it.id) }.toTypedArray()
        return orders
    }

    override fun getProducts(): Array<IProduct> {
        val products: Array<IProduct> =
                getAll<Map<String, Any?>, MappedProduct>(
                        EndpointBaseType.PRODUCTS.value
                ) { MappedProduct(it) }.map { it as IProduct }.toTypedArray()
        return products
    }
}