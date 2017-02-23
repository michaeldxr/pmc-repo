#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_rct="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

rctoutfileformail="$destdir"/"$txn"_rct.out
errCd6=0
rctoutfile="rowCountThreshold_scan.csv"

echo -e "" > $rctoutfileformail
echo -e "Row Count Threshold Scan Output" >> $rctoutfileformail
echo -e "==================================================" >> $rctoutfileformail

#echo -e "Running RowCount Threshold CodeScan for the transaction $txn" >> $rctoutfileformail
#echo -e "" >> $rctoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar oracle.apps.rowCountAnalyzer.RCTPremergeChecker "$txnFiles" $ADE_VIEW_ROOT
errCd6=$?

    if `grep  -q "fusionapps/" $rctoutfile`
    then
	errCd6=1
    fi

    echo -e "" >> $rctoutfileformail
    cat $rctoutfile >> $rctoutfileformail 
    echo -e "" >> $rctoutfileformail


    tstop_rct="$(date +%s)"
    telapsed_rct="$(expr $tstop_rct - $tstart_rct)"

    echo -e "Completed for transaction $txn." >> $rctoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $rctoutfileformail
    echo -e "" >> $rctoutfileformail

    exit $errCd6

