#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_eoHistory="$(date +%s)"

eoHistoryoutfileformail="$destdir"/"$txn"_eoHistory.out
errCd29=0
eoHistoryoutfile="eo_historyCol_scan.csv"

echo -e "" > $eoHistoryoutfileformail
echo -e "EOHistoryColumns Scan Output" >> $eoHistoryoutfileformail
echo -e "==================================================" >> $eoHistoryoutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.ScanEOHistoryColumns "$txnFiles" $ADE_VIEW_ROOT $label
errCd29=$?

    if `grep  -q "fusionapps" $eoHistoryoutfile`
    then
	errCd29=1
    fi

    cat $eoHistoryoutfile >> $eoHistoryoutfileformail 

    tstop_eoHistory="$(date +%s)"
    telapsed_eoHistory="$(expr $tstop_eoHistory - $tstart_eoHistory)"

    echo -e "Completed for transaction $txn." >> $eoHistoryoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_eoHistory sec" +%H:%M:%S)" >> $eoHistoryoutfileformail
    echo -e "" >> $eoHistoryoutfileformail

    exit $errCd29

