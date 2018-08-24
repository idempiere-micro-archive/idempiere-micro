package org.compiere.bo

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.compiere.crm.SvrProcessBase
import org.compiere.product.MCurrency
import org.idempiere.common.util.DB
import software.hsharp.business.models.IDTOReady
import java.sql.Timestamp

data class EnsureBOResult(val OpportunityId: Int) : IDTOReady

class EnsureBO : SvrProcessBase() {
    var businessPartnerId: Int = 0

    override fun prepare() {
        super.prepare()
        for (para in parameter) {
            if (para.parameterName == "BusinessPartnerId") {
                businessPartnerId = para.parameterAsInt
            }
        }
    }

    private fun getBoId(): Int {
        val sql =
                """
select * from adempiere.C_Opportunity
where (c_bpartner_id = ?) -- params 1.
and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y' -- params 2..4
order by 1 desc
                """.trimIndent()

        @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
        val cnn = DB.getConnectionRO()
        val statement = cnn.prepareStatement(sql)
        try {
            statement.setInt(1, businessPartnerId)
            statement.setInt(2, AD_CLIENT_ID)
            statement.setInt(3, AD_ORG_ID)
            statement.setInt(4, AD_ORG_ID)

            val rs = statement.executeQuery()
            try {
                var oppId = 0
                when {
                    rs.next() -> oppId = rs.getInt("c_opportunity_id")
                }
                return oppId
            } finally {
                rs.close()
            }
        } finally {
            statement.close()
            cnn.close()
        }
    }

    override fun getResult(): IDTOReady {
        var oppId = getBoId()
        if (oppId <= 0) {
            val opp = MOpportunity(ctx, 0, null)
            opp.expectedCloseDate = Timestamp(System.currentTimeMillis())
            opp.c_BPartner_ID = businessPartnerId
            opp.opportunityAmt = 0.toBigDecimal()
            opp.c_Currency_ID = MCurrency.get(ctx, "CZK").c_Currency_ID
            opp.c_SalesStage_ID = 1000000
            opp.probability = 0.toBigDecimal()
            opp.save()

            oppId = getBoId()
        }

        return EnsureBOResult(oppId)
    }
}