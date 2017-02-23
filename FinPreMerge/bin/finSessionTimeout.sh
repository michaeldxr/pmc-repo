#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_sessionTimeout="$(date +%s)"

sessionTimeoutoutfileformail="$destdir"/"$txn"_sessionTimeout.out
errCd8=0
sessionTimeoutoutfile="SessionTimeoutSetting.csv"

echo -e "" > $sessionTimeoutoutfileformail
echo -e "SessionTimeout Scan Output" >> $sessionTimeoutoutfileformail
echo -e "==================================================" >> $sessionTimeoutoutfileformail

#echo -e "Running sessionTimeout CodeScan for the transaction $txn" >> $sessionTimeoutoutfileformail
#echo -e "" >> $sessionTimeoutoutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.ScanSessionTimeout "$txnFiles" $ADE_VIEW_ROOT $label
errCd8=$?

    if `grep  -q "fusionapps" $sessionTimeoutoutfile`
    then
	errCd8=1
    fi

    cat $sessionTimeoutoutfile >> $sessionTimeoutoutfileformail 

    tstop_sessionTimeout="$(date +%s)"
    telapsed_sessionTimeout="$(expr $tstop_sessionTimeout - $tstart_sessionTimeout)"

    echo -e "Completed for transaction $txn." >> $sessionTimeoutoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $sessionTimeoutoutfileformail
    echo -e "" >> $sessionTimeoutoutfileformail

    exit $errCd8

