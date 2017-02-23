#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_adfdiRowLevelBindingCheck="$(date +%s)"

adfdiRowLevelBindingCheckoutfileformail="$destdir"/"$txn"_adfdiRowLevelBindingCheck.out
errCd8=0
adfdiRowLevelBindingCheckoutfile="ADFdiRowLevelBindings.csv"

echo -e "" > $adfdiRowLevelBindingCheckoutfileformail
echo -e "Adfdi RowLevelBindingCheck Scan Output" >> $adfdiRowLevelBindingCheckoutfileformail
echo -e "==================================================" >> $adfdiRowLevelBindingCheckoutfileformail

#echo -e "Running AdfdiRowLevelBindingCheck CodeScan for the transaction $txn" >> $adfdiRowLevelBindingCheckoutfileformail
#echo -e "" >> $adfdiRowLevelBindingCheckoutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.di.RowLevelBindingCheck "$txnFiles" $ADE_VIEW_ROOT $label
errCd8=$?

    if `grep  -q "fusionapps" $adfdiRowLevelBindingCheckoutfile`
    then
	errCd8=1
    fi

    cat $adfdiRowLevelBindingCheckoutfile >> $adfdiRowLevelBindingCheckoutfileformail 

    tstop_adfdiRowLevelBindingCheck="$(date +%s)"
    telapsed_adfdiRowLevelBindingCheck="$(expr $tstop_adfdiRowLevelBindingCheck - $tstart_adfdiRowLevelBindingCheck)"

    echo -e "Completed for transaction $txn." >> $adfdiRowLevelBindingCheckoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $adfdiRowLevelBindingCheckoutfileformail
    echo -e "" >> $adfdiRowLevelBindingCheckoutfileformail

    exit $errCd8

