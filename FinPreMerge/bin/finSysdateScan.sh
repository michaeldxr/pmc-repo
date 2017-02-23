#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_sysdate="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

sysdateoutfileformail="$destdir"/"$txn"_sysdate.out
errCd12=0
sysdateoutfile="sql_date_scan.txt"

echo -e "" > $sysdateoutfileformail
echo -e "Sysdate Scan Output" >> $sysdateoutfileformail
echo -e "==================================================" >> $sysdateoutfileformail

#echo -e "Running Sysdate Codescan for the transaction $txn" >> $sysdateoutfileformail
#echo -e "" >> $sysdateoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.sqlanalyzer.SQLDatePredicatePremerge_new "$txnFiles" $ADE_VIEW_ROOT
errCd12=$?

    if `grep  -q "fusionapps/" $sysdateoutfile`
    then
	errCd12=1
    fi

    echo -e "" >> $sysdateoutfileformail
    cat $sysdateoutfile >> $sysdateoutfileformail 
    echo -e "" >> $sysdateoutfileformail


    tstop_sysdate="$(date +%s)"
    telapsed_sysdate="$(expr $tstop_sysdate - $tstart_sysdate)"

    echo -e "Completed for transaction $txn." >> $sysdateoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_sysdate sec" +%H:%M:%S)" >> $sysdateoutfileformail
    echo -e "" >> $sysdateoutfileformail

    exit $errCd12

