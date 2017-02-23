#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_unusedTF="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

unusedTFoutfileformail="$destdir"/"$txn"_unusedTF.out
errCd9=0
unusedTFoutfile="unusedTFs_scan.csv"

echo -e "" > $unusedTFoutfileformail
echo -e "Unused TaskFlow Scan Output" >> $unusedTFoutfileformail
echo -e "==================================================" >> $unusedTFoutfileformail

#echo -e "Running Unused TaskFlow CodeScan for the transaction $txn" >> $unusedTFoutfileformail
#echo -e "" >> $unusedTFoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.psr.UnusedTFPremergeChecker "$txnFiles" $ADE_VIEW_ROOT
errCd9=$?

    if `grep  -q "fusionapps/" $unusedTFoutfile`
    then
	errCd9=1
    fi

    echo -e "" >> $unusedTFoutfileformail
    cat $unusedTFoutfile >> $unusedTFoutfileformail 
    echo -e "" >> $unusedTFoutfileformail


    tstop_unusedTF="$(date +%s)"
    telapsed_unusedTF="$(expr $tstop_unusedTF - $tstart_unusedTF)"

    echo -e "Completed for transaction $txn." >> $unusedTFoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_unusedTF sec" +%H:%M:%S)" >> $unusedTFoutfileformail
    echo -e "" >> $unusedTFoutfileformail

    exit $errCd9

