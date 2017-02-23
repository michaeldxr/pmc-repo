#!/bin/sh

####################################################################################
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/runFreValidate.sh /main/5 2016/08/01 02:17:45 mengxu Exp $
# Purpose: Script to run FreValidation on files part of a transaction and is called as part of fin_premerge.ksh
#
# Revision:
#     Aug-2013   Created                                                 Sudipti
#                                  
####################################################################################
tstart_adt="$(date +%s)"

#mkdir -p $ADE_VIEW_ROOT/fusionapps/premerge
#destdir=$ADE_VIEW_ROOT/fusionapps/premerge
#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#cd "$destdir"

label=`ade pwv|grep VIEW_LABEL |cut -d: -f2|tr -d [\ ]`
auditfile="$destdir"/"$txn"_frevalidation.txt
auditoutfileformail="$destdir"/bugOutputForFreValidation.txt
returnCode=0

echo -e "" > $auditoutfileformail
echo -e "FRE Validation Output" >> $auditoutfileformail
echo -e "==================================================" >> $auditoutfileformail
#echo -e "Invoking FRE Validations for the transaction $txn" >> $auditoutfileformail
#echo -e "" >> $auditoutfileformail

rm -f /tmp/validationFiles.txt
fpr -s currenttransaction -r createmanifest -m /tmp/validationFiles.txt -f F_PREMERGE > $auditfile
if [ -s /tmp/validationFiles.txt ]
then

  java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.4.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.premerge.PremergeFreValidation $label $freDB >> $auditfile
returnCode=$?;

else
  echo "Transaction has no files to validate using FRE." >> $auditoutfileformail
  returnCode=4
  exit $returnCode
fi

tstop_adt="$(date +%s)"
telapsed_adt="$(expr $tstop_adt - $tstart_adt)"

echo -e "" >> $auditoutfileformail
echo -e "Completed for transaction $txn." >> $auditoutfileformail
echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_adt sec" +%H:%M:%S)" >> $auditoutfileformail
echo -e "Output written to $auditoutfileformail"
echo -e "" >> $auditoutfileformail

#echo -e "Return code: " >> $auditoutfileformail
#echo -e "$returnCode" >> $auditoutfileformail
  
exit $returnCode
