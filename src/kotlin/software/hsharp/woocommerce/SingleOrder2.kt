package software.hsharp.woocommerce

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties("taxes", "meta_data")
data class ShippingLinesItem(
    @JsonProperty("total")
    val total: String = "",
    @JsonProperty("method_id")
    val methodId: String = "",
    @JsonProperty("meta_data")
    val metaData: List<MetaDataItem>?,
    @JsonProperty("id")
    val id: Int = 0,
    @JsonProperty("total_tax")
    val totalTax: String = "",
    @JsonProperty("method_title")
    val methodTitle: String = "",
    @JsonProperty("instance_id")
    val instanceId: String = ""
)

data class MetaDataItem(
    @JsonProperty("id")
    val id: Int = 0,
    @JsonProperty("value")
    val value: String = "",
    @JsonProperty("key")
    val key: String = ""
)

@JsonIgnoreProperties("taxes", "meta_data")
data class LineItemsItem(
    @JsonProperty("quantity")
    val quantity: Int = 0,
    @JsonProperty("tax_class")
    val taxClass: String = "",
    @JsonProperty("total_tax")
    val totalTax: String = "",
    @JsonProperty("total")
    val total: String = "",
    @JsonProperty("variation_id")
    val variationId: Int = 0,
    @JsonProperty("subtotal")
    val subtotal: String = "",
    @JsonProperty("price")
    val price: Int = 0,
    @JsonProperty("product_id")
    val productId: Int = 0,
    @JsonProperty("name")
    val name: String = "",
    @JsonProperty("id")
    val id: Int = 0,
    @JsonProperty("subtotal_tax")
    val subtotalTax: String = "",
    @JsonProperty("sku")
    val sku: String = ""
)

data class Billing(
    @JsonProperty("country")
    val country: String = "",
    @JsonProperty("city")
    val city: String = "",
    @JsonProperty("phone")
    val phone: String = "",
    @JsonProperty("address_1")
    val address: String = "",
    @JsonProperty("address_2")
    val address2: String = "",
    @JsonProperty("postcode")
    val postcode: String = "",
    @JsonProperty("last_name")
    val lastName: String = "",
    @JsonProperty("company")
    val company: String = "",
    @JsonProperty("state")
    val state: String = "",
    @JsonProperty("first_name")
    val firstName: String = "",
    @JsonProperty("email")
    val email: String = ""
)

data class Links(
    @JsonProperty("self")
    val self: List<SelfItem>?,
    @JsonProperty("collection")
    val collection: List<CollectionItem>?,
    @JsonProperty("customer")
    val customer: List<SelfItem>?
)

@JsonIgnoreProperties("tax_lines", "fee_lines", "coupon_lines", "refunds", "meta_data")
data class SingleOrder(
    @JsonProperty("discount_total")
    val discountTotal: String = "",
    @JsonProperty("order_key")
    val orderKey: String = "",
    @JsonProperty("prices_include_tax")
    val pricesIncludeTax: Boolean = false,
    @JsonProperty("_links")
    val Links: Links,
    @JsonProperty("customer_note")
    val customerNote: String = "",
    @JsonProperty("line_items")
    val lineItems: List<LineItemsItem>?,
    @JsonProperty("billing")
    val billing: Billing,
    @JsonProperty("number")
    val number: String = "",
    @JsonProperty("total")
    val total: String = "",
    @JsonProperty("shipping")
    val shipping: Shipping,
    @JsonProperty("date_paid_gmt")
    val datePaidGmt: String? = null,
    @JsonProperty("date_paid")
    val datePaid: String? = null,
    @JsonProperty("customer_user_agent")
    val customerUserAgent: String = "",
    @JsonProperty("payment_method_title")
    val paymentMethodTitle: String = "",
                       /*@JsonProperty("meta_data")
                       val metaData: List<MetaDataItem>?,*/
    @JsonProperty("date_completed")
    val dateCompleted: String? = null,
    @JsonProperty("currency")
    val currency: String = "",
    @JsonProperty("id")
    val id: Int = 0,
    @JsonProperty("date_completed_gmt")
    val dateCompletedGmt: String? = null,
    @JsonProperty("payment_method")
    val paymentMethod: String = "",
    @JsonProperty("shipping_tax")
    val shippingTax: String = "",
    @JsonProperty("transaction_id")
    val transactionId: String = "",
    @JsonProperty("date_modified_gmt")
    val dateModifiedGmt: String = "",
    @JsonProperty("cart_hash")
    val cartHash: String = "",
    @JsonProperty("shipping_total")
    val shippingTotal: String = "",
    @JsonProperty("cart_tax")
    val cartTax: String = "",
    @JsonProperty("created_via")
    val createdVia: String = "",
    @JsonProperty("date_created")
    val dateCreated: String = "",
    @JsonProperty("date_created_gmt")
    val dateCreatedGmt: String = "",
    @JsonProperty("discount_tax")
    val discountTax: String = "",
    @JsonProperty("total_tax")
    val totalTax: String = "",
    @JsonProperty("version")
    val version: String = "",
    @JsonProperty("customer_ip_address")
    val customerIpAddress: String = "",
    @JsonProperty("shipping_lines")
    val shippingLines: List<ShippingLinesItem>?,
    @JsonProperty("date_modified")
    val dateModified: String = "",
    @JsonProperty("parent_id")
    val parentId: Int = 0,
    @JsonProperty("customer_id")
    val customerId: Int = 0,
    @JsonProperty("status")
    val status: String = ""
)

data class CollectionItem(
    @JsonProperty("href")
    val href: String = ""
)

data class Shipping(
    @JsonProperty("country")
    val country: String = "",
    @JsonProperty("city")
    val city: String = "",
    @JsonProperty("address_1")
    val address: String = "",
    @JsonProperty("address_2")
    val address2: String = "",
    @JsonProperty("postcode")
    val postcode: String = "",
    @JsonProperty("last_name")
    val lastName: String = "",
    @JsonProperty("company")
    val company: String = "",
    @JsonProperty("state")
    val state: String = "",
    @JsonProperty("first_name")
    val firstName: String = ""
)

data class SelfItem(
    @JsonProperty("href")
    val href: String = ""
)
