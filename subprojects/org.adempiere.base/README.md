# iDempiere Core (org.adempiere.base)
This is the core (or business logic) of iDempiere.

This project contains all [the iDempiere model classes](https://github.com/iDempiere-micro/org.adempiere.base/tree/master/src/java/org/compiere/model) (e.g. MCountry), all the processes etc.
It can be used by a server project (the original iDempiere or any other server).

It does not contain:
- HTML generation
- Callouts
- technical code (database connection etc.)

The current version is based on the [iDempiere 5.1 source code](http://sourceforge.net/projects/idempiere/files/v5.1/source-repo/idempiere_hgrepo_v5.1.zip/download) (rev 12097 commit bef51f50fc7f).
