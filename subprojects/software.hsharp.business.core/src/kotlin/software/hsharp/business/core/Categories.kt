package software.hsharp.business.core

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import software.hsharp.business.models.ICategory

object crm_category : IntIdTable(columnName = "category_id") {
    val ad_client_id = integer("ad_client_id")
    val ad_org_id = integer("ad_org_id")
    val isactive = varchar("isactive", 1)
    val created = datetime("created")
    val createdby = integer("createdby")
    val updated = datetime("updated")
    val updatedby = integer("updatedby")
    val category_uu = varchar("category_uu", 36)

    val name = varchar("name", 60)
    val searchKey = varchar("value", 60)
}

class CategoryModel(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CategoryModel>(crm_category)

    var category_Id by crm_category.id
    var AD_Client_Id by crm_category.ad_client_id
    var AD_Org_Id by crm_category.ad_org_id
    var IsActive by crm_category.isactive
    var Created by crm_category.created
    var CreatedBy by crm_category.createdby
    var Updated by crm_category.updated
    var UpdatedBy by crm_category.updatedby
    var category_Uu by crm_category.category_uu
    var name by crm_category.name
    var searchKey by crm_category.searchKey
}

data class Category(override val Key: Int, override val name: String) : ICategory {
    override val ID: String
        get() = "" + Key
}

class Categories
