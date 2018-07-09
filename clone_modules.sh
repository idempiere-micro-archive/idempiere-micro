copy_files()
{
  module=$1
  ./clone_idempiere_module $module
}
copy_files_github_root()
{
  module=$1

    git init

    # make the repo clean
    git add .
    git commit -m "Before adding root module $1"

    # download the submodule to /subprojects
    git remote add -f $1 https://github.com/iDempiere-micro/$1
    git subtree add --prefix $1 $1 master --squash
}

copy_files org.adempiere.base
copy_files org.adempiere.exceptions
copy_files org.adempiere.install
copy_files org.adempiere.osgi

copy_files org.compiere.bo
copy_files org.compiere.crm
copy_files org.compiere.db.postgresql.provider
copy_files org.compiere.lookups
copy_files org.compiere.model
copy_files org.compiere.orm
copy_files org.compiere.process
copy_files org.compiere.query
copy_files org.compiere.rule
copy_files org.compiere.util

copy_files org.idempiere.common
copy_files org.idempiere.icommon
copy_files org.idempiere.orm
copy_files org.idempiere.process

copy_files software.hsharp.api.helpers
copy_files software.hsharp.api.icommon

copy_files software.hsharp.business.core
copy_files software.hsharp.business.models

copy_files software.hsharp.core.models

copy_files software.hsharp.db.postgresql.provider
copy_files software.hsharp.db.h2.provider
copy_files software.hsharp.idempiere.api

copy_files software.hsharp.woocommerce
copy_files org.compiere.order
copy_files org.compiere.product
copy_files org.compiere.tax

copy_files org.compiere.conversionrate
copy_files org.compiere.wf
copy_files org.compiere.schedule
copy_files org.compiere.server
copy_files org.compiere.validation

copy_files_github_root integration_tests
