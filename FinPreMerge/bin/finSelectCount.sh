#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_selectCount="$(date +%s)"

selectCountoutfileformail="$destdir"/"$txn"_selectCount.out
errCd7=0
selectCountoutfile="SelectCountScan.csv"

echo -e "" > $selectCountoutfileformail
echo -e "SelectCount Scan Output" >> $selectCountoutfileformail
echo -e "==================================================" >> $selectCountoutfileformail

#echo -e "Running SelectCount CodeScan for the transaction $txn" >> $selectCountoutfileformail
#echo -e "" >> $selectCountoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.rowCountAnalyzer.SelectCountPreMergeChecker "$txnFiles" $ADE_VIEW_ROOT
errCd7=$?

    if `grep  -q "fusionapps/" $selectCountoutfile`
    then
	errCd7=1
    fi

    cat $selectCountoutfile >> $selectCountoutfileformail 

    tstop_selectCount="$(date +%s)"
    telapsed_selectCount="$(expr $tstop_selectCount - $tstart_selectCount)"

    echo -e "Completed for transaction $txn." >> $selectCountoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $selectCountoutfileformail
    echo -e "" >> $selectCountoutfileformail

    exit $errCd7

