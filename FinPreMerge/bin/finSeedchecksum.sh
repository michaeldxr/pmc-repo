#!/bin/sh

####################################################################################
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/finSeedchecksum.sh /main/5 2013/04/11 13:31:38 sudgupta Exp $
# Purpose: Script to run Seed validation (fpr scheduler) on files part of a transaction and is called as part of fin_premerge.ksh
#
# Revision:
#     Oct-2012   Created                                                 Sudipti
#                                  
####################################################################################
tstart_adt="$(date +%s)"
label=`ade pwv|grep VIEW_LABEL |cut -d: -f2|tr -d [\ ]`
#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
returnCode=0
branchname=`ade desc -l $label | grep AUTO_MAKEBRANCH | cut -d: -f2 | tail -n 1 | awk -F/ '{print $NF}'`
branchproperty=`ade desc -b $branchname | grep FA_PREMERGE_DB | cut -d: -f2`

auditfile="$destdir"/"$txn"_seedvalidation.txt
auditoutfileformail="$destdir"/bugOutputForSeedValidation.txt
returnCode=0

echo -e "" > $auditoutfileformail
echo -e "fpr Patch Output" >> $auditoutfileformail
echo -e "=============" >> $auditoutfileformail
echo -e "Invoking fpr scheduler for the transaction $txn" >> $auditoutfileformail
echo -e "" >> $auditoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.4.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.premerge.PremergeSeedChecksum $label $txn $branchproperty>> $auditfile
returnCode=$?;

tstop_adt="$(date +%s)"
telapsed_adt="$(expr $tstop_adt - $tstart_adt)"

echo -e "" >> $auditoutfileformail
echo -e "Running Seed Validations completed for the transaction $txn" >> $auditoutfileformail
echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_adt sec" +%H:%M:%S)" >> $auditoutfileformail
echo -e "Output written to $auditoutfileformail"
echo -e "" >> $auditoutfileformail
  
exit $returnCode
