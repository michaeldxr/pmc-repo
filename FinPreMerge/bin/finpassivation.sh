#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_passivation="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

passivationoutfileformail="$destdir"/"$txn"_passivation.out
errCd5=0
passivationoutfile="passivation_violations.txt"

echo -e "" > $passivationoutfileformail
echo -e "Excessive Passivation Output" >> $passivationoutfileformail
echo -e "==================================================" >> $passivationoutfileformail

#echo -e "Running Passivation CodeScan for the transaction $txn" >> $passivationoutfileformail
#echo -e "" >> $passivationoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar -Xms512m -Xmx1024m oracle.apps.psr.PassivationPreMergeViolations "$txnFiles" $ADE_VIEW_ROOT
errCd5=$?

    if `grep  -q "File Name" $passivationoutfile`
    then
	errCd5=1
    fi

    echo -e "" >> $passivationoutfileformail

    cat $passivationoutfile >> $passivationoutfileformail 

    echo -e "" >> $passivationoutfileformail

    tstop_passivation="$(date +%s)"
    telapsed_pasivation="$(expr $tstop_passivation - $tstart_passivation)"

    echo -e "Completed for transaction $txn." >> $passivationoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_passivation sec" +%H:%M:%S)" >> $passivationoutfileformail
    echo -e "" >> $passivationoutfileformail

    exit $errCd5

