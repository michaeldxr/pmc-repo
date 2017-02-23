#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_vc="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

vcoutfileformail="$destdir"/"$txn"_vc.out
errCd11=0
vcoutfile="vc_scan.csv"

echo -e "" > $vcoutfileformail
echo -e "ViewCriteria Scan Output" >> $vcoutfileformail
echo -e "==================================================" >> $vcoutfileformail

#echo -e "Running ViewCriteria Codescan for the transaction $txn" >> $vcoutfileformail
#echo -e "" >> $vcoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.sqlanalyzer.VCPremergeChecker "$txnFiles" $ADE_VIEW_ROOT $family
errCd11=$?

    if `grep  -q "fusionapps/" $vcoutfile`
    then
	errCd11=1
    fi

    echo -e "" >> $vcoutfileformail
    cat $vcoutfile >> $vcoutfileformail 
    echo -e "" >> $vcoutfileformail


    tstop_vc="$(date +%s)"
    telapsed_vc="$(expr $tstop_vc - $tstart_vc)"

    echo -e "Completed for transaction $txn." >> $vcoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_vc sec" +%H:%M:%S)" >> $vcoutfileformail
    echo -e "" >> $vcoutfileformail

    exit $errCd11

