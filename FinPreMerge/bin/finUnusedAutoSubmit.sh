#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_unusedAutoSubmit="$(date +%s)"

unusedAutoSubmitoutfileformail="$destdir"/"$txn"_unusedAutoSubmit.out
errCd8=0
unusedAutoSubmitoutfile="UnusedAutoSubmit.csv"
nonExistentPartialTriggeroutfile="NonExistantPartialTriggers.csv"

echo -e "" > $unusedAutoSubmitoutfileformail
echo -e "UnusedAutoSubmit & NonExistentPartialTrigger Scan Output" >> $unusedAutoSubmitoutfileformail
echo -e "==================================================" >> $unusedAutoSubmitoutfileformail

#echo -e "Running UnusedAutoSubmit & NonExistentPartialTrigger CodeScan for the transaction $txn" >> $unusedAutoSubmitoutfileformail
#echo -e "" >> $unusedAutoSubmitoutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.UICodeScanner.PartialTriggerScanner "$txnFiles" $ADE_VIEW_ROOT $label
errCd8=$?

    if `grep  -q "fusionapps" $unusedAutoSubmitoutfile`
    then
	errCd8=1
    fi

    if `grep  -q "fusionapps" $nonExistentPartialTriggeroutfile`
    then
	errCd8=1
    fi

    echo -e "Scan Results for UnusedAutoSubmit:" >> $unusedAutoSubmitoutfileformail
    cat $unusedAutoSubmitoutfile >> $unusedAutoSubmitoutfileformail 
    echo -e "" >> $unusedAutoSubmitoutfileformail

    echo -e "Scan Results for NonExistentPartialTrigger:" >> $unusedAutoSubmitoutfileformail
    cat $nonExistentPartialTriggeroutfile >> $unusedAutoSubmitoutfileformail 
    echo -e "" >> $unusedAutoSubmitoutfileformail

    tstop_unusedAutoSubmit="$(date +%s)"
    telapsed_unusedAutoSubmit="$(expr $tstop_unusedAutoSubmit - $tstart_unusedAutoSubmit)"

    echo -e "Completed for transaction $txn." >> $unusedAutoSubmitoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $unusedAutoSubmitoutfileformail
    echo -e "" >> $unusedAutoSubmitoutfileformail

    exit $errCd8

