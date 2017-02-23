#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_altaIssues="$(date +%s)"

altaIssuesoutfileformail="$destdir"/"$txn"_altaIssues.out
errCd28=0
altaIssuesoutfile="AltaUIViolations.csv"

echo -e "" > $altaIssuesoutfileformail
echo -e "Alta Icon Issues Scan Output" >> $altaIssuesoutfileformail
echo -e "==================================================" >> $altaIssuesoutfileformail

#echo -e "Running altaIssues CodeScan for the transaction $txn" >> $altaIssuesoutfileformail
#echo -e "" >> $altaIssuesoutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.UICodeScanner.AltaIconIssues "$txnFiles" $ADE_VIEW_ROOT $label
errCd28=$?

    if `grep  -q "fusionapps" $altaIssuesoutfile`
    then
	errCd28=1
    fi

    cat $altaIssuesoutfile >> $altaIssuesoutfileformail 

    tstop_altaIssues="$(date +%s)"
    telapsed_altaIssues="$(expr $tstop_altaIssues - $tstart_altaIssues)"

    echo -e "Completed for transaction $txn." >> $altaIssuesoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_altaIssues sec" +%H:%M:%S)" >> $altaIssuesoutfileformail
    echo -e "" >> $altaIssuesoutfileformail

    exit $errCd28

