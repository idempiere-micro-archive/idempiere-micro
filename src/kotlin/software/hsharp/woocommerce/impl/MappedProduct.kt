package software.hsharp.woocommerce.impl

import software.hsharp.woocommerce.IProduct

data class MappedProduct(val map: Map<String, Any?>) : IProduct {
    override val id: Int by map
    override val name: String by map
}
