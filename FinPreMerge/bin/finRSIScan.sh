#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_rsi="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

rsioutfileformail="$destdir"/"$txn"_rsi.out
errCd13=0
rsioutfile="RSI_issues.csv"

echo -e "" > $rsioutfileformail
echo -e "RowSetIterator Scan Output" >> $rsioutfileformail
echo -e "==================================================" >> $rsioutfileformail

#echo -e "Running RowSetIterator Scan for the transaction $txn" >> $rsioutfileformail
#echo -e "" >> $rsioutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.rup2scans.RSIPremergeChecker "$txnFiles" $ADE_VIEW_ROOT
errCd13=$?

    if `grep  -q "FileName:" $rsioutfile`
    then
	errCd13=1
    fi

    echo -e "" >> $rsioutfileformail
    cat $rsioutfile >> $rsioutfileformail 
    echo -e "" >> $rsioutfileformail


    tstop_rsi="$(date +%s)"
    telapsed_rsi="$(expr $tstop_rsi - $tstart_rsi)"

    echo -e "Completed for transaction $txn." >> $rsioutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rsi sec" +%H:%M:%S)" >> $rsioutfileformail
    echo -e "" >> $rsioutfileformail

    exit $errCd13

