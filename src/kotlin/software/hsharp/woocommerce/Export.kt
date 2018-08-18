package software.hsharp.woocommerce

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import org.compiere.model.I_C_BPartner
import org.compiere.model.I_C_BPartner_Location
import org.compiere.model.I_M_Product
import org.compiere.model.I_C_OrderLine
import org.compiere.model.I_C_Order

data class BusinessPartner(private val partner: I_C_BPartner) {
    val key: String get() = partner.value
    val name: String get() = partner.name
}

data class BusinessPartnerLocation(private val location: I_C_BPartner_Location) {
    val address1: String? get() = location.c_Location.address1
    /*val address2 : String? get() = location.c_Location.Address2
    val address3 : String? get() = location.c_Location.Address3
    val address4 : String? get() = location.c_Location.Address4*/
    val city: String? get() = location.c_Location.city
    val country: String? get() = location.c_Location.countryName
    val zip: String? get() = location.c_Location.postal
}

data class ExportProduct(private val product: I_M_Product) {
    val name: String get() = product.name
    val SKU: String? get() = product.sku
    val internalId: Int get() = product._ID
    val externalId: String get() = product.value
}

data class ExportOrderLine(private val line: I_C_OrderLine) {
    val description: String? get() = line.description
    val product: ExportProduct get() = ExportProduct(line.m_Product)
    val priceActual: BigDecimal get() = line.priceActual
    val qtyOrdered: BigDecimal get() = line.qtyOrdered
}

data class ExportOrder(
    private val order: I_C_Order
) {
    val documentNumber: String get() = order.documentNo
    val internalId: Int get() = order._ID
    val billingPartner: BusinessPartner get() = BusinessPartner(order.bill_BPartner)
    val billingAddress: BusinessPartnerLocation get() = BusinessPartnerLocation(order.bill_Location)
    val shippingAddress: BusinessPartnerLocation get() = BusinessPartnerLocation(order.c_BPartner_Location)
    val orderLines: Array<ExportOrderLine> get() = order.lines.map { ExportOrderLine(it) }.toTypedArray()
    val dateOrdered: java.time.LocalDate get() = order.dateOrdered.toLocalDateTime().toLocalDate()
}

fun parseDate(s: String): Timestamp {
    val fmt = DateTimeFormatterBuilder()
            // date/time
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            // optional offset
            .optionalStart().appendOffsetId()
            // create formatter with UTC
            .toFormatter().withZone(ZoneOffset.UTC)

    val date = Date.from(Instant.from(fmt.parse(s)))

    return Timestamp(date.time)
}

/*
 * Convert Object to XML String
 */
fun write2XMLString(obj: Any): String {

    val xmlMapper = XmlMapper()
    // use the line of code for pretty-print XML on console. We should remove it in production.
    xmlMapper.enable(SerializationFeature.INDENT_OUTPUT)
    // xmlMapper.setDefaultUseWrapper(true)

    return xmlMapper.writeValueAsString(obj)
}
