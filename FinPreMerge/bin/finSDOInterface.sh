#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_sdoInterface="$(date +%s)"

sdoInterfaceoutfileformail="$destdir"/"$txn"_sdoInterface.out
errCd8=0
sdoInterfaceoutfile="sdo_scan.csv"

echo -e "" > $sdoInterfaceoutfileformail
echo -e "SDO Interface Scan Output" >> $sdoInterfaceoutfileformail
echo -e "==================================================" >> $sdoInterfaceoutfileformail

#echo -e "Running SDO Interface CodeScan for the transaction $txn" >> $sdoInterfaceoutfileformail
#echo -e "" >> $sdoInterfaceoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.warnings.SDOPremergeChecker "$txnFiles" $ADE_VIEW_ROOT
errCd8=$?

    if `grep  -q "Serializable" $sdoInterfaceoutfile`
    then
	errCd8=1
    fi

    cat $sdoInterfaceoutfile >> $sdoInterfaceoutfileformail 

    tstop_sdoInterface="$(date +%s)"
    telapsed_sdoInterface="$(expr $tstop_sdoInterface - $tstart_sdoInterface)"

    echo -e "Completed for transaction $txn." >> $sdoInterfaceoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $sdoInterfaceoutfileformail
    echo -e "" >> $sdoInterfaceoutfileformail

    exit $errCd8

