#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_autovc="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

autoexecuteoutfileformail="$destdir"/"$txn"_autoexecute.out
errCd12=0
autoexecuteoutfile="autoexecute_vcscan.csv"    

echo -e "" > $autoexecuteoutfileformail
echo -e "AutoExecuteVC Scan Output" >> $autoexecuteoutfileformail
echo -e "==================================================" >> $autoexecuteoutfileformail

#echo -e "Running AutoExecuteVC Codescan for the transaction $txn" >> $autoexecuteoutfileformail
#echo -e "" >> $autoexecuteoutfileformail


java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar  oracle.apps.sqlanalyzer.AutoExecuteVCPremerge "$txnFiles" $ADE_VIEW_ROOT
errCd12=$?

    if `grep  -q "fusionapps/" $autoexecuteoutfile`
    then
	errCd12=1
    fi

    echo -e "" >> $autoexecuteoutfileformail
    cat $autoexecuteoutfile >> $autoexecuteoutfileformail 
    echo -e "" >> $autoexecuteoutfileformail


    tstop_autovc="$(date +%s)"
    telapsed_autovc="$(expr $tstop_autovc - $tstart_autovc)"

    echo -e "Completed for transaction $txn." >> $autoexecuteoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_autovc sec" +%H:%M:%S)" >> $autoexecuteoutfileformail
    echo -e "" >> $autoexecuteoutfileformail

    exit $errCd12

