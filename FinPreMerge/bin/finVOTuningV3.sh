#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_VOTuning="$(date +%s)"

VOTuningoutfileformail="$destdir"/"$txn"_VOTuning.out
errCd8=0
VOTuningoutfile="votuning.txt"

echo -e "" > $VOTuningoutfileformail
echo -e "VOTuning Scan Output" >> $VOTuningoutfileformail
echo -e "==================================================" >> $VOTuningoutfileformail
#echo -e "Running VOTuning CodeScan for the transaction $txn" >> $VOTuningoutfileformail
#echo -e "Family: $family"
#echo -e "" >> $VOTuningoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.VOTuningPMC "$txnFiles" $ADE_VIEW_ROOT $label $family
errCd8=$?

    if `grep  -q "fusionapps" $VOTuningoutfile`
    then
	errCd8=1
    fi

    cat $VOTuningoutfile >> $VOTuningoutfileformail 

    tstop_VOTuning="$(date +%s)"
    telapsed_VOTuning="$(expr $tstop_VOTuning - $tstart_VOTuning)"

    echo -e "Completed for transaction $txn." >> $VOTuningoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $VOTuningoutfileformail
    echo -e "" >> $VOTuningoutfileformail

    exit $errCd8

