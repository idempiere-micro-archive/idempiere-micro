package org.compiere.crm

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.compiere.model.I_C_BPartner
import org.compiere.model.I_C_ContactActivity
import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import software.hsharp.business.core.BusinessPartner
import software.hsharp.business.core.BusinessPartnerLocation
import software.hsharp.business.models.IDTOReady
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement

abstract class BaseBPartnerSearch : SvrProcessBaseSql() {
    override val isRO: Boolean
    get() = true
    var search: String = ""
    var full: Boolean = false

    override fun prepare() {
        super.prepare()
        for (para in parameter) {
            if (para.parameterName == "Search") {
                search = para.parameterAsString
            } else if (para.parameterName == "Full") {
                full = para.parameterAsBoolean
            }
        }
    }

    abstract fun getSql(): String
    abstract fun setStatementParams(statement: PreparedStatement)

    @SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION")
    private fun convertBusinessPartner(bpartner: I_C_BPartner): BusinessPartner {
        return BusinessPartner(
                bpartner.c_BPartner_ID, bpartner.name, bpartner.value, bpartner.locations.map { BusinessPartnerLocation(it) }.toTypedArray(),
                bpartner.flatDiscount
        )
    }

    override fun getSqlResult(cnn: Connection): IDTOReady {
        val sql = getSql()

        val statement = cnn.prepareStatement(sql)
        setStatementParams(statement)
        val rs = statement.executeQuery()

        val modelFactory: IModelFactory = DefaultModelFactory()
        val result: List<Any> = generateSequence {
            if (rs.next()) {
                if (full) {
                    val bpartner: I_C_BPartner = modelFactory.getPO(I_C_BPartner.Table_Name, rs, null) as I_C_BPartner
                    val c_contactactivity_id = rs.getObject("c_contactactivity_id") as BigDecimal?
                    BPartnerWithActivity(
                            convertBusinessPartner(bpartner),
                            if (c_contactactivity_id == null) { null } else {
                                ContactActivity(
                                        modelFactory.getPO(I_C_ContactActivity.Table_Name, rs, null, "activity_") as I_C_ContactActivity
                                )
                            },
                            rs.getString("category_name")
                    )
                } else {
                    val name = rs.getString("name")
                    val foundIdx = name.toLowerCase().indexOf(search.toLowerCase())
                    val subName = if (foundIdx > 0) { name.substring(foundIdx) } else { name }
                    BPartnerFindResult(rs.getInt("c_bpartner_id"), name, subName, rs.getString("taxid"))
                }
            } else null
        }.toList()

        return FindResult(result)
    }
}