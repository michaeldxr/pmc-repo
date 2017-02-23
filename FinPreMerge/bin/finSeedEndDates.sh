#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_seedEndDate="$(date +%s)"

seedEndDateoutfileformail="$destdir"/"$txn"_seedEndDate.out
errCd8=0
seedEndDateoutfile="SeedEndDateIncorrect.csv"

echo -e "" > $seedEndDateoutfileformail
echo -e "SeedEndDates Scan Output" >> $seedEndDateoutfileformail
echo -e "==================================================" >> $seedEndDateoutfileformail

#echo -e "Running seedEndDate CodeScan for the transaction $txn" >> $seedEndDateoutfileformail
#echo -e "" >> $seedEndDateoutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.seed.SeedEndDates "$txnFiles" $ADE_VIEW_ROOT $label
errCd8=$?

    if `grep  -q "fusionapps" $seedEndDateoutfile`
    then
	errCd8=1
    fi

    cat $seedEndDateoutfile >> $seedEndDateoutfileformail 

    tstop_seedEndDate="$(date +%s)"
    telapsed_seedEndDate="$(expr $tstop_seedEndDate - $tstart_seedEndDate)"

    echo -e "Completed for transaction $txn." >> $seedEndDateoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $seedEndDateoutfileformail
    echo -e "" >> $seedEndDateoutfileformail

    exit $errCd8

