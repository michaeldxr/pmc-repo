#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_vcPerf="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

vcPerfoutfileformail="$destdir"/"$txn"_vcPerf.out
errCd15=0
vcPerfoutfile="vc_perf_scan.txt"

echo -e "" > $vcPerfoutfileformail
echo -e "ViewCriteria Performance Scans Output" >> $vcPerfoutfileformail
echo -e "==================================================" >> $vcPerfoutfileformail

#echo -e "Running View Criteria Performance Scans for the transaction $txn" >> $vcPerfoutfileformail
#echo -e "" >> $vcPerfoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar oracle.apps.sqlanalyzer.VCPerformanceNewPremerge_enhanced "$txnFiles" $ADE_VIEW_ROOT $label
errCd15=$?

    if `grep  -q "Issue:" $vcPerfoutfile`
    then
	errCd15=1
    fi

    echo -e "" >> $vcPerfoutfileformail
    cat $vcPerfoutfile >> $vcPerfoutfileformail 
    echo -e "" >> $vcPerfoutfileformail


    tstop_vcPerf="$(date +%s)"
    telapsed_vcPerf="$(expr $tstop_vcPerf - $tstart_vcPerf)"

    echo -e "Completed for transaction $txn." >> $vcPerfoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_vcperf sec" +%H:%M:%S)" >> $vcPerfoutfileformail
    echo -e "" >> $vcPerfoutfileformail

    exit $errCd15

