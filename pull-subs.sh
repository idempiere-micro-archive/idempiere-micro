#!/usr/bin/env bash
pull()
{
  module=$1
  git subtree pull -P subprojects/$module $module master --squash
}


pull org.idempiere.app
pull org.adempiere.exceptions
pull org.adempiere.install
pull org.adempiere.osgi

pull org.compiere.bo
pull org.compiere.conversionrate
pull org.compiere.crm
pull org.compiere.db.postgresql.provider
pull org.compiere.lookups
pull org.compiere.model
pull org.compiere.order
pull org.compiere.orm
pull org.compiere.process
pull org.compiere.product
pull org.compiere.query
pull org.compiere.rule
pull org.compiere.schedule
pull org.compiere.server
pull org.compiere.tax
pull org.compiere.util
pull org.compiere.validation
pull org.compiere.wf

pull org.idempiere.common
pull org.idempiere.icommon
pull org.idempiere.orm

pull software.hsharp.api.helpers
pull software.hsharp.api.icommon

pull software.hsharp.business.core
pull software.hsharp.business.models

pull software.hsharp.core.models

pull software.hsharp.db.postgresql.provider
pull software.hsharp.idempiere.api

pull software.hsharp.woocommerce
