package software.hsharp.business.models

interface IClientBound {
    companion object {
    /** Column name
 	*/
        const val COLUMNNAME_AD_Client_ID = "AD_Client_ID"
    }

        /** Get Client.
	 * 	  * Client/Tenant for this installation.
	 * 	  	  */
        val AD_Client_ID: Int
}
