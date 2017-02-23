#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_ui="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

uioutfileformail="$destdir"/"$txn"_ui.out
errCd10=0
uioutfile="ui_scan.csv"

echo -e "" > $uioutfileformail
echo -e "UI Scans Scan Output" >> $uioutfileformail
echo -e "===========" >> $uioutfileformail
echo -e "Running UI Scans CodeScan for the transaction $txn" >> $uioutfileformail

echo -e "" >> $uioutfileformail

files=`cat $txnFiles`

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.UICodeScanner.UIScansPremergeChecker "$files" $ADE_VIEW_ROOT
errCd10=$?

    if `grep  -q "fusionapps/" $uioutfile`
    then
	errCd10=1
    fi

    echo -e "" >> $uioutfileformail
    cat $uioutfile >> $uioutfileformail 
    echo -e "" >> $uioutfileformail


    tstop_ui="$(date +%s)"
    telapsed_ui="$(expr $tstop_ui - $tstart_ui)"

    echo -e "Running UI Scans scan completed for the transaction $txn" >> $uioutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_ui sec" +%H:%M:%S)" >> $uioutfileformail
    echo -e "" >> $uioutfileformail

    exit $errCd10

