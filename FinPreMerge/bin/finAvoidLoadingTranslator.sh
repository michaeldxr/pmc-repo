#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_avoidTranslator="$(date +%s)"

avoidTranslatoroutfileformail="$destdir"/"$txn"_avoidTranslator.out
errCd8=0
avoidTranslatoroutfile="ADFConfigFlagCheckMissing.csv"

echo -e "" > $avoidTranslatoroutfileformail
echo -e "AvoidLoadingTranslatorNotes Scan Output" >> $avoidTranslatoroutfileformail
echo -e "==================================================" >> $avoidTranslatoroutfileformail

#echo -e "Running AvoidLoadingTranslatorNotes Scan for the transaction $txn" >> $avoidTranslatoroutfileformail
#echo -e "" >> $avoidTranslatoroutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar:$destdir/* oracle.apps.ADFConfigFlagCheck "$txnFiles" $ADE_VIEW_ROOT $label
errCd8=$?

    if `grep  -q "fusionapps" $avoidTranslatoroutfile`
    then
	errCd8=1
    fi

    cat $avoidTranslatoroutfile >> $avoidTranslatoroutfileformail 

    tstop_avoidTranslator="$(date +%s)"
    telapsed_avoidTranslator="$(expr $tstop_avoidTranslator - $tstart_avoidTranslator)"

    echo -e "Completed for transaction $txn." >> $avoidTranslatoroutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $avoidTranslatoroutfileformail
    echo -e "" >> $avoidTranslatoroutfileformail

    exit $errCd8

