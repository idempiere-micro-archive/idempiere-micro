/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.idempiere.org/license.html           *
 *****************************************************************************/

package org.compiere.bo

import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.Properties

import org.compiere.model.I_C_Opportunity
import org.compiere.orm.MTable
import org.compiere.orm.PO
import org.idempiere.orm.I_Persistent
import org.idempiere.common.util.Env
import org.idempiere.common.util.KeyNamePair
import org.idempiere.orm.POInfo

open class X_C_Opportunity : PO, I_C_Opportunity, I_Persistent {
    constructor(ctx: Properties, C_Opportunity_ID: Int, trxName: String?) : super(ctx, C_Opportunity_ID, trxName)
    constructor (ctx: Properties, rs: ResultSet, trxName: String?) : super(ctx, rs, trxName)

    /** AccessLevel
     * @return 3 - Client - Org
     */
    override fun get_AccessLevel(): Int {
        return I_C_Opportunity.accessLevel.toInt()
    }

    /** Load Meta Data  */
    override fun initPO(ctx: Properties): POInfo {
        return POInfo.getPOInfo(ctx, I_C_Opportunity.Table_ID, _TrxName)
    }

    override fun toString(): String {
        val sb = StringBuffer("X_C_Opportunity[")
                .append(_ID).append("]")
        return sb.toString()
    }

    @Throws(RuntimeException::class)
    override fun getAD_User(): org.compiere.model.I_AD_User? {
        if (aD_User_ID == 0) return null
        return MTable.get(ctx, org.compiere.model.I_AD_User.Table_Name)
                .getPO(aD_User_ID, _TrxName) as org.compiere.model.I_AD_User?
    }

    /** Set User/Contact.
     * @param AD_User_ID
     * User within the system - Internal or Business Partner Contact
     */
    override fun setAD_User_ID(AD_User_ID: Int) {
        if (AD_User_ID < 1)
            set_Value(I_C_Opportunity.COLUMNNAME_AD_User_ID, null)
        else
            set_Value(I_C_Opportunity.COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID))
    }

    /** Get User/Contact.
     * @return User within the system - Internal or Business Partner Contact
     */
    override fun getAD_User_ID(): Int {
        return get_Value(I_C_Opportunity.COLUMNNAME_AD_User_ID) as Int? ?: return 0
    }

    @Throws(RuntimeException::class)
    override fun getC_BPartner(): org.compiere.model.I_C_BPartner {
        return MTable.get(ctx, org.compiere.model.I_C_BPartner.Table_Name)
                .getPO(c_BPartner_ID, _TrxName) as org.compiere.model.I_C_BPartner
    }

    /** Set Business Partner .
     * @param C_BPartner_ID
     * Identifies a Business Partner
     */
    override fun setC_BPartner_ID(C_BPartner_ID: Int) {
        if (C_BPartner_ID < 1)
            set_Value(I_C_Opportunity.COLUMNNAME_C_BPartner_ID, null)
        else
            set_Value(I_C_Opportunity.COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID))
    }

    /** Get Business Partner .
     * @return Identifies a Business Partner
     */
    override fun getC_BPartner_ID(): Int {
        return get_Value(I_C_Opportunity.COLUMNNAME_C_BPartner_ID) as Int? ?: return 0
    }

    @Throws(RuntimeException::class)
    override fun getC_Campaign(): org.compiere.model.I_C_Campaign? {
        if (c_Campaign_ID == 0) return null
        return MTable.get(ctx, org.compiere.model.I_C_Campaign.Table_Name)
                .getPO(c_Campaign_ID, _TrxName) as org.compiere.model.I_C_Campaign?
    }

    /** Set Campaign.
     * @param C_Campaign_ID
     * Marketing Campaign
     */
    override fun setC_Campaign_ID(C_Campaign_ID: Int) {
        if (C_Campaign_ID < 1)
            set_Value(I_C_Opportunity.COLUMNNAME_C_Campaign_ID, null)
        else
            set_Value(I_C_Opportunity.COLUMNNAME_C_Campaign_ID, Integer.valueOf(C_Campaign_ID))
    }

    /** Get Campaign.
     * @return Marketing Campaign
     */
    override fun getC_Campaign_ID(): Int {
        return get_Value(I_C_Opportunity.COLUMNNAME_C_Campaign_ID) as Int? ?: return 0
    }

    @Throws(RuntimeException::class)
    override fun getC_Currency(): org.compiere.model.I_C_Currency {
        return MTable.get(ctx, org.compiere.model.I_C_Currency.Table_Name)
                .getPO(c_Currency_ID, _TrxName) as org.compiere.model.I_C_Currency
    }

    /** Set Currency.
     * @param C_Currency_ID
     * The Currency for this record
     */
    override fun setC_Currency_ID(C_Currency_ID: Int) {
        if (C_Currency_ID < 1)
            set_Value(I_C_Opportunity.COLUMNNAME_C_Currency_ID, null)
        else
            set_Value(I_C_Opportunity.COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID))
    }

    /** Get Currency.
     * @return The Currency for this record
     */
    override fun getC_Currency_ID(): Int {
        return get_Value(I_C_Opportunity.COLUMNNAME_C_Currency_ID) as Int? ?: return 0
    }

    /** Set Close Date.
     * @param CloseDate
     * Close Date
     */
    override fun setCloseDate(CloseDate: Timestamp) {
        set_Value(I_C_Opportunity.COLUMNNAME_CloseDate, CloseDate)
    }

    /** Get Close Date.
     * @return Close Date
     */
    override fun getCloseDate(): Timestamp? {
        return get_Value(I_C_Opportunity.COLUMNNAME_CloseDate) as Timestamp?
    }

    /** Set Comments.
     * @param Comments
     * Comments or additional information
     */
    override fun setComments(Comments: String) {
        set_Value(I_C_Opportunity.COLUMNNAME_Comments, Comments)
    }

    /** Get Comments.
     * @return Comments or additional information
     */
    override fun getComments(): String? {
        return get_Value(I_C_Opportunity.COLUMNNAME_Comments) as String?
    }

    /** Set Sales Opportunity.
     * @param C_Opportunity_ID Sales Opportunity
     */
    override fun setC_Opportunity_ID(C_Opportunity_ID: Int) {
        if (C_Opportunity_ID < 1)
            set_ValueNoCheck(I_C_Opportunity.COLUMNNAME_C_Opportunity_ID, null)
        else
            set_ValueNoCheck(I_C_Opportunity.COLUMNNAME_C_Opportunity_ID, Integer.valueOf(C_Opportunity_ID))
    }

    /** Get Sales Opportunity.
     * @return Sales Opportunity
     */
    override fun getC_Opportunity_ID(): Int {
        return get_Value(I_C_Opportunity.COLUMNNAME_C_Opportunity_ID) as Int? ?: return 0
    }

    /** Set C_Opportunity_UU.
     * @param C_Opportunity_UU C_Opportunity_UU
     */
    override fun setC_Opportunity_UU(C_Opportunity_UU: String) {
        set_Value(I_C_Opportunity.COLUMNNAME_C_Opportunity_UU, C_Opportunity_UU)
    }

    /** Get C_Opportunity_UU.
     * @return C_Opportunity_UU
     */
    override fun getC_Opportunity_UU(): String {
        return get_Value(I_C_Opportunity.COLUMNNAME_C_Opportunity_UU) as String
    }

    @Throws(RuntimeException::class)
    override fun getC_Order(): org.compiere.model.I_C_Order? {
        if (c_Order_ID == 0) return null
        return MTable.get(ctx, org.compiere.model.I_C_Order.Table_Name)
                .getPO(c_Order_ID, _TrxName) as org.compiere.model.I_C_Order
    }

    /** Set Order.
     * @param C_Order_ID
     * Order
     */
    override fun setC_Order_ID(C_Order_ID: Int) {
        if (C_Order_ID < 1)
            set_Value(I_C_Opportunity.COLUMNNAME_C_Order_ID, null)
        else
            set_Value(I_C_Opportunity.COLUMNNAME_C_Order_ID, Integer.valueOf(C_Order_ID))
    }

    /** Get Order.
     * @return Order
     */
    override fun getC_Order_ID(): Int {
        return (get_Value(I_C_Opportunity.COLUMNNAME_C_Order_ID) as Int?) ?: return 0
    }

    /** Set Cost.
     * @param Cost
     * Cost information
     */
    override fun setCost(Cost: BigDecimal) {
        set_Value(I_C_Opportunity.COLUMNNAME_Cost, Cost)
    }

    /** Get Cost.
     * @return Cost information
     */
    override fun getCost(): BigDecimal {
        return get_Value(I_C_Opportunity.COLUMNNAME_Cost) as BigDecimal? ?: return Env.ZERO
    }

    @Throws(RuntimeException::class)
    override fun getC_SalesStage(): org.compiere.model.I_C_SalesStage {
        return MTable.get(ctx, org.compiere.model.I_C_SalesStage.Table_Name)
                .getPO(c_SalesStage_ID, _TrxName) as org.compiere.model.I_C_SalesStage
    }

    /** Set Sales Stage.
     * @param C_SalesStage_ID
     * Stages of the sales process
     */
    override fun setC_SalesStage_ID(C_SalesStage_ID: Int) {
        if (C_SalesStage_ID < 1)
            set_Value(I_C_Opportunity.COLUMNNAME_C_SalesStage_ID, null)
        else
            set_Value(I_C_Opportunity.COLUMNNAME_C_SalesStage_ID, Integer.valueOf(C_SalesStage_ID))
    }

    /** Get Sales Stage.
     * @return Stages of the sales process
     */
    override fun getC_SalesStage_ID(): Int {
        return get_Value(I_C_Opportunity.COLUMNNAME_C_SalesStage_ID) as Int? ?: return 0
    }

    /** Set Description.
     * @param Description
     * Optional short description of the record
     */
    override fun setDescription(Description: String) {
        set_Value(I_C_Opportunity.COLUMNNAME_Description, Description)
    }

    /** Get Description.
     * @return Optional short description of the record
     */
    override fun getDescription(): String? {
        return get_Value(I_C_Opportunity.COLUMNNAME_Description) as String?
    }

    /** Set Document No.
     * @param DocumentNo
     * Document sequence number of the document
     */
    override fun setDocumentNo(DocumentNo: String) {
        set_Value(I_C_Opportunity.COLUMNNAME_DocumentNo, DocumentNo)
    }

    /** Get Document No.
     * @return Document sequence number of the document
     */
    override fun getDocumentNo(): String {
        return get_Value(I_C_Opportunity.COLUMNNAME_DocumentNo) as String
    }

    /** Get Record ID/ColumnName
     * @return ID/ColumnName pair
     */
    fun getKeyNamePair(): KeyNamePair {
        return KeyNamePair(_ID, documentNo)
    }

    /** Set Expected Close Date.
     * @param ExpectedCloseDate
     * Expected Close Date
     */
    override fun setExpectedCloseDate(ExpectedCloseDate: Timestamp) {
        set_Value(I_C_Opportunity.COLUMNNAME_ExpectedCloseDate, ExpectedCloseDate)
    }

    /** Get Expected Close Date.
     * @return Expected Close Date
     */
    override fun getExpectedCloseDate(): Timestamp {
        return get_Value(I_C_Opportunity.COLUMNNAME_ExpectedCloseDate) as Timestamp
    }

    /** Set Opportunity Amount.
     * @param OpportunityAmt
     * The estimated value of this opportunity.
     */
    override fun setOpportunityAmt(OpportunityAmt: BigDecimal) {
        set_Value(I_C_Opportunity.COLUMNNAME_OpportunityAmt, OpportunityAmt)
    }

    /** Get Opportunity Amount.
     * @return The estimated value of this opportunity.
     */
    override fun getOpportunityAmt(): BigDecimal {
        return get_Value(I_C_Opportunity.COLUMNNAME_OpportunityAmt) as BigDecimal? ?: return Env.ZERO
    }

    /** Set Probability.
     * @param Probability Probability
     */
    override fun setProbability(Probability: BigDecimal) {
        set_Value(I_C_Opportunity.COLUMNNAME_Probability, Probability)
    }

    /** Get Probability.
     * @return Probability
     */
    override fun getProbability(): BigDecimal {
        return get_Value(I_C_Opportunity.COLUMNNAME_Probability) as BigDecimal? ?: return Env.ZERO
    }

    @Throws(RuntimeException::class)
    override fun getSalesRep(): org.compiere.model.I_AD_User? {
        if (salesRep_ID == 0) return null
        return MTable.get(ctx, org.compiere.model.I_AD_User.Table_Name)
                .getPO(salesRep_ID, _TrxName) as org.compiere.model.I_AD_User
    }

    /** Set Sales Representative.
     * @param SalesRep_ID
     * Sales Representative or Company Agent
     */
    override fun setSalesRep_ID(SalesRep_ID: Int) {
        if (SalesRep_ID < 1)
            set_Value(I_C_Opportunity.COLUMNNAME_SalesRep_ID, null)
        else
            set_Value(I_C_Opportunity.COLUMNNAME_SalesRep_ID, Integer.valueOf(SalesRep_ID))
    }

    /** Get Sales Representative.
     * @return Sales Representative or Company Agent
     */
    override fun getSalesRep_ID(): Int {
        return get_Value(I_C_Opportunity.COLUMNNAME_SalesRep_ID) as Int? ?: return 0
    }

    /** Set Weighted Amount.
     * @param WeightedAmt
     * The amount adjusted by the probability.
     */
    override fun setWeightedAmt(WeightedAmt: BigDecimal) {
        throw IllegalArgumentException("WeightedAmt is virtual column")
    }

    /** Get Weighted Amount.
     * @return The amount adjusted by the probability.
     */
    override fun getWeightedAmt(): BigDecimal {
        return get_Value(I_C_Opportunity.COLUMNNAME_WeightedAmt) as BigDecimal? ?: return Env.ZERO
    }
}
