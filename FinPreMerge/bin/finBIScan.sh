#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_bi="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

bioutfileformail="$destdir"/"$txn"_bi.out
errCd13=0
bioutfile="BIExtenderAutomation_scan.csv"

echo -e "" > $bioutfileformail
echo -e "BIExtenderAutomation Scan Output" >> $bioutfileformail
echo -e "==================================================" >> $bioutfileformail

#echo -e "Running BIExtenderAutomation Scan for the transaction $txn" >> $bioutfileformail
#echo -e "" >> $bioutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.bi.BIExtenderPremergeChecker "$txnFiles" $ADE_VIEW_ROOT
errCd13=$?

    if `grep  -q "FileName:" $bioutfile`
    then
	errCd13=1
    fi

    echo -e "" >> $bioutfileformail
    cat $bioutfile >> $bioutfileformail 
    echo -e "" >> $bioutfileformail


    tstop_bi="$(date +%s)"
    telapsed_bi="$(expr $tstop_bi - $tstart_bi)"

    echo -e "Completed for transaction $txn." >> $bioutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_bi sec" +%H:%M:%S)" >> $bioutfileformail
    echo -e "" >> $bioutfileformail

    exit $errCd13

