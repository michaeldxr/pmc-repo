#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_adfdiVersionCheck="$(date +%s)"

adfdiVersionCheckoutfileformail="$destdir"/"$txn"_adfdiVersionCheck.out
errCd8=0
adfdiVersionCheckoutfile="ADFdiVersionCheckMissing.csv"

echo -e "" > $adfdiVersionCheckoutfileformail
echo -e "AdfdiVersionCheck Output" >> $adfdiVersionCheckoutfileformail
echo -e "==================================================" >> $adfdiVersionCheckoutfileformail

#echo -e "Running AdfdiVersionCheck CodeScan for the transaction $txn" >> $adfdiVersionCheckoutfileformail
#echo -e "" >> $adfdiVersionCheckoutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.di.AdfDiVersionCheck "$txnFiles" $ADE_VIEW_ROOT $label
errCd8=$?

    if `grep  -q "fusionapps" $adfdiVersionCheckoutfile`
    then
	errCd8=1
    fi

    cat $adfdiVersionCheckoutfile >> $adfdiVersionCheckoutfileformail 

    tstop_adfdiVersionCheck="$(date +%s)"
    telapsed_adfdiVersionCheck="$(expr $tstop_adfdiVersionCheck - $tstart_adfdiVersionCheck)"

    echo -e "Completed for transaction $txn." >> $adfdiVersionCheckoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $adfdiVersionCheckoutfileformail
    echo -e "" >> $adfdiVersionCheckoutfileformail

    exit $errCd8

