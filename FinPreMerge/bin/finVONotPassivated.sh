#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_voNotPassivated="$(date +%s)"

voNotPassivatedoutfileformail="$destdir"/"$txn"_voNotPassivated.out
errCd18=0
voNotPassivatedoutfile="vo_passivation_scan.csv"

echo -e "" > $voNotPassivatedoutfileformail
echo -e "VONotPassivated Scan Output" >> $voNotPassivatedoutfileformail
echo -e "==================================================" >> $voNotPassivatedoutfileformail

#echo -e "Running VONotPassivated CodeScan for the transaction $txn" >> $voNotPassivatedoutfileformail
#echo -e "" >> $voNotPassivatedoutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.psr.VOAnalyzer "$txnFiles" $ADE_VIEW_ROOT $label
errCd18=$?

    if `grep  -q "fusionapps" $voNotPassivatedoutfile`
    then
	errCd18=1
    fi

    cat $voNotPassivatedoutfile >> $voNotPassivatedoutfileformail 

    tstop_voNotPassivated="$(date +%s)"
    telapsed_voNotPassivated="$(expr $tstop_voNotPassivated - $tstart_voNotPassivated)"

    echo -e "Completed for transaction $txn." >> $voNotPassivatedoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_voNotPassivated sec" +%H:%M:%S)" >> $voNotPassivatedoutfileformail
    echo -e "" >> $voNotPassivatedoutfileformail

    exit $errCd18

