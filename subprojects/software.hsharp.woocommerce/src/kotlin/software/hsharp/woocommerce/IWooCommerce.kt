package software.hsharp.woocommerce

interface IWooCommerce {
    /**
     * Retrieves on WooCommerce entity
     *
     * @param endpointBase API endpoint base @see EndpointBaseType
     * @param id id of WooCommerce entity
     * @return Retrieved WooCommerce entity
     */
    // inline fun <reified T: Any> get(endpointBase: String, id: Int): T
    fun getOrder(id: Int): SingleOrder

    /**
     * Retrieves all WooCommerce entities with request parameters
     *
     * @param endpointBase API endpoint base @see EndpointBaseType
     * @param params additional request params
     * @return List of retrieved entities
     */
    // fun getAll(endpointBase: String, params: Map<String, String>): List<*>
    fun getOrders(): Array<SingleOrder>
    fun getProducts(): Array<IProduct>

    /**
     * Retrieves all WooCommerce entities
     *
     * @param endpointBase API endpoint base @see EndpointBaseType
     * @return List of retrieved entities
     */
    /*fun getAll(endpointBase: String): List<*> {
        return getAll(endpointBase, mapOf())
    }*/
}