#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_vo="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

vooutfileformail="$destdir"/"$txn"_vo.out
errCd12=0
vooutfile="vo_scan.csv"

echo -e "" > $vooutfileformail
echo -e "Table and LOV Tuning Scan Output" >> $vooutfileformail
echo -e "===========" >> $vooutfileformail
echo -e "Running Table and LOV Tuning Codescan for the transaction $txn" >> $vooutfileformail

echo -e "" >> $vooutfileformail

files=`cat $txnFiles`

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.votuning.LOVAnalysisPreMergeChecker "$files" $ADE_VIEW_ROOT $label
errCd12=$?

    if `grep  -q "fusionapps/" $vooutfile`
    then
	errCd12=1
    fi

    echo -e "" >> $vooutfileformail
    cat $vooutfile >> $vooutfileformail 
    echo -e "" >> $vooutfileformail


    tstop_vo="$(date +%s)"
    telapsed_vo="$(expr $tstop_vo - $tstart_vo)"

    echo -e "Running Table and LOV Tuning scan completed for the transaction $txn" >> $vooutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_vo sec" +%H:%M:%S)" >> $vooutfileformail
    echo -e "" >> $vooutfileformail

    exit $errCd12

