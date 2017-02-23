#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_uisearch="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

uioutfileformail="$destdir"/"$txn"_uisearch.out
errCd12=0
uioutfile="ui_searches_scan.txt"

viewlabel=`ade pwv|grep VIEW_LABEL |cut -d: -f2`

echo -e "" > $uioutfileformail
echo -e "UI Search Scan Output" >> $uioutfileformail
echo -e "==================================================" >> $uioutfileformail

#echo -e "Running UI Search Codescan for the transaction $txn" >> $uioutfileformail
#echo -e "" >> $uioutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.sqlanalyzer.UISearchesPremergeChecker_new "$txnFiles" $ADE_VIEW_ROOT
errCd12=$?

    if `grep  -q "fusionapps/" $uioutfile`
    then
	errCd12=1
    fi

    echo -e "" >> $uioutfileformail
    cat $uioutfile >> $uioutfileformail 
    echo -e "" >> $uioutfileformail


    tstop_uisearch="$(date +%s)"
    telapsed_uisearch="$(expr $tstop_uisearch - $tstart_uisearch)"

    echo -e "Completed for transaction $txn." >> $uioutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_uisearch sec" +%H:%M:%S)" >> $uioutfileformail
    echo -e "" >> $uioutfileformail

    exit $errCd12

