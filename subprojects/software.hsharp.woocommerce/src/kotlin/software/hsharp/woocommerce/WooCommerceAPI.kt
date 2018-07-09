package software.hsharp.woocommerce

import software.hsharp.woocommerce.impl.*

class WooCommerceAPI(config : IConfig, apiVersion : ApiVersionType) : IWooCommerce, WooCommerceBase(config, apiVersion) {
    override fun getOrder(id: Int): SingleOrder {
        val order : SingleOrder =
                get<SingleOrder>(
                        EndpointBaseType.ORDERS.value,
                        id,
                        {it as SingleOrder }
                )
        return order
    }

    override fun getOrders(): Array<SingleOrder> {
        val orders : Array<SingleOrder> =
                getAll(
                        EndpointBaseType.ORDERS.value,
                        mapOf(),
                        { MappedOrder(it as Map<String, Any?>) }
                ).map { getOrder(it.id) }.toTypedArray()
        return orders
    }

    override fun getProducts(): Array<IProduct> {
        val products : Array<IProduct> =
                getAll(
                        EndpointBaseType.PRODUCTS.value,
                        mapOf(),
                        { MappedProduct(it as Map<String, Any?>) }
                ).map { it as IProduct }.toTypedArray()
        return products
    }
}