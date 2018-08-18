package software.hsharp.woocommerce.impl

data class MappedOrder(val map: Map<String, Any?>) {
    val id: Int by map

    /* this is left here as a warning why I did not want to go this way and
    used the "strange" way to get Order by ID

    override val number : String by map
    override val status : String by map
    override val currency : String by map
    override val total : String by map

    constructor(
            id : Int,
            number : String,
            status : String,
            currency : String,
            total : String
    ) : this(
            mapOf(
                    "id" to id,
                    "number" to number,
                    "status" to status,
                    "currency" to currency,
                    "total" to total
            )
    )*/
}
