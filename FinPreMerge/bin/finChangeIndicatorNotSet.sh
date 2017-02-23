#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_changeIndicator="$(date +%s)"

changeIndicatoroutfileformail="$destdir"/"$txn"_changeIndicator.out
errCd28=0
changeIndicatoroutfile="changeIndicatorNotSet_scan.csv"

echo -e "" > $changeIndicatoroutfileformail
echo -e "ChangeIndicatorNotSet Scan Output" >> $changeIndicatoroutfileformail
echo -e "==================================================" >> $changeIndicatoroutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.ChangeIndicatorNotSet "$txnFiles" $ADE_VIEW_ROOT $label
errCd28=$?

    if `grep  -q "fusionapps" $changeIndicatoroutfile`
    then
	errCd28=1
    fi

    cat $changeIndicatoroutfile >> $changeIndicatoroutfileformail 

    tstop_changeIndicator="$(date +%s)"
    telapsed_changeIndicator="$(expr $tstop_changeIndicator - $tstart_changeIndicator)"

    echo -e "Completed for transaction $txn." >> $changeIndicatoroutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_changeIndicator sec" +%H:%M:%S)" >> $changeIndicatoroutfileformail
    echo -e "" >> $changeIndicatoroutfileformail

    exit $errCd28

