#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_invalidref="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

invalidrefoutfileformail="$destdir"/"$txn"_invalidref.out
errCd3=0
invalidrefoutfile="invalidJarReferences.csv"

echo -e "" > $invalidrefoutfileformail
echo -e "Invalid Jar References Output" >> $invalidrefoutfileformail
echo -e "==================================================" >> $invalidrefoutfileformail

#echo -e "Running Invalid Jar References CodeScan for the transaction $txn" >> $invalidrefoutfileformail
#echo -e "" >> $invalidrefoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar -Xms512m -Xmx1024m oracle.apps.references.InvalidJarRefPreMergeChecker "$txnFiles" $ADE_VIEW_ROOT
errCd3=$?

    if `grep  -q "InvalidRef" $invalidrefoutfile`
    then
	errCd3=1
    fi

    echo -e "" >> $invalidrefoutfileformail
    cat $invalidrefoutfile >> $invalidrefoutfileformail 
    echo -e "" >> $invalidrefoutfileformail

    tstop_invalidref="$(date +%s)"
    telapsed_invalidref="$(expr $tstop_invalidref - $tstart_invalidref)"

    echo -e "Completed for transaction $txn." >> $invalidrefoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_invalidref sec" +%H:%M:%S)" >> $invalidrefoutfileformail
    echo -e "" >> $invalidrefoutfileformail

    exit $errCd3

