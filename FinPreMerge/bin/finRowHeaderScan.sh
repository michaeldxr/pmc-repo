#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_rowheader="$(date +%s)"
rowheaderoutfileformail="$destdir"/"$txn"_rowheader.out
errCd14=0
rowheaderoutfile="rowheader_scan.csv"

echo -e "" > $rowheaderoutfileformail
echo -e "Accessibility RowHeader Scan Output" >> $rowheaderoutfileformail
echo -e "===========" >> $rowheaderoutfileformail
echo -e "Running Accessibility RowHeader CodeScan for the transaction $txn" >> $rowheaderoutfileformail

echo -e "" >> $rowheaderoutfileformail

files=`cat $txnFiles`

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/RowHeader.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar RowHeaderPremergeChecker "$files" $ADE_VIEW_ROOT 

errCd14=$?

    if `grep  -q "fusionapps/" $rowheaderoutfile`
    then
        errCd14=1
    fi

    echo -e "" >> $rowheaderoutfileformail
    cat $rowheaderoutfile >> $rowheaderoutfileformail
    echo -e "" >> $rowheaderoutfileformail


    tstop_rowheader="$(date +%s)"
    telapsed_rowheader="$(expr $tstop_rowheader - $tstart_rowheader)"

    echo -e "Running Accessibility - Rowheader scan completed for the transaction $txn" >> $rowheaderoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rowheader sec" +%H:%M:%S)" >> $rowheaderoutfileformail
    echo -e "" >> $rowheaderoutfileformail

    exit $errCd14

