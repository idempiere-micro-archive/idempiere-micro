package org.compiere.crm

import org.compiere.model.I_C_ContactActivity
import software.hsharp.business.models.IContactActivity

data class ContactActivity(
    override val Key: Int,
    override val name: String,
    override val start: Long, // represent the value of java.sql.Timestamp
    override val bpartnerName: String,
    override val completed: Boolean,
    override val activityOwnerName: String
) : IContactActivity {
    override val ID: String
        get() = "$Key"

    constructor (a: I_C_ContactActivity) :
        this(a.c_ContactActivity_ID, a.description, a.startDate.time, a.c_Opportunity.c_BPartner.name, a.isComplete, a.salesRep.name)
}
