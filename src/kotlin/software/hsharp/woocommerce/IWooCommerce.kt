package software.hsharp.woocommerce

interface IWooCommerce {
    /**
     * Creates WooCommerce entity
     *
     * @param endpointBase API endpoint base @see EndpointBaseType
     * @param object       Map with entity properties and values
     * @return Map with created entity
     */
    fun create(endpointBase: String, context: Map<String, Any>): Map<*, *>

    /**
     * Retrieves on WooCommerce entity
     *
     * @param endpointBase API endpoint base @see EndpointBaseType
     * @param id           id of WooCommerce entity
     * @return Retrieved WooCommerce entity
     */
    //inline fun <reified T: Any> get(endpointBase: String, id: Int): T
    fun getOrder(id:Int):SingleOrder

    /**
     * Retrieves all WooCommerce entities with request parameters
     *
     * @param endpointBase API endpoint base @see EndpointBaseType
     * @param params additional request params
     * @return List of retrieved entities
     */
    //fun getAll(endpointBase: String, params: Map<String, String>): List<*>
    fun getOrders():Array<SingleOrder>
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

    /**
     * Updates WooCommerce entity
     *
     * @param endpointBase API endpoint base @see EndpointBaseType
     * @param id           id of the entity to update
     * @param object       Map with updated properties
     * @return updated WooCommerce entity
     */
    fun update(endpointBase: String, id: Int, `object`: Map<String, Any>): Map<*, *>

    /**
     * Deletes WooCommerce entity
     *
     * @param endpointBase API endpoint base @see EndpointBaseType
     * @param id           id of the entity to update
     * @return deleted WooCommerce entity
     */
    fun delete(endpointBase: String, id: Int): Map<*, *>
}