#!/bin/sh

# Setting destination Directory and txn variables incase they are not set. This will allow for local testing of the script
if [ $destdir ]; then
	echo ""
else
	destdir="$ADE_VIEW_ROOT"
fi

if [ $txn ]; then
	echo ""
else
	txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
fi

# For each scan these are the set of variables that change. 
tstart="$(date +%s)"
outFile="$destdir"/"$txn"_methodActionVc.out
resultFile="vcResults.csv"
errCode=0
header="Method Action has ViewCriteria Scan"
classToRun="oracle.apps.serialization.ViewCriteriaSerializationScanner"

echo -e "" > $outFile
echo -e "$header" >> $outFile
echo -e "==================================================" >> $outFile

#echo -e "Running $header for the transaction $txn" >> $outFile
#echo -e "" >> $outFile

listOfFiles="transactionFiles.txt"
files=`ade describetrans -nocount | grep fusionapps/ | tr -d "[:blank:]" | sort | uniq>$listOfFiles`

java -DfilterByList=$listOfFiles -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar $classToRun $ADE_VIEW_ROOT/fusionapps/

errCode=$?

    if `grep  -q "fusionapps/" $resultFile`
    then
	errCode=1
    fi

    cat $resultFile >> $outFile

    tstop="$(date +%s)"
    telapsed="$(expr $tstop - $tstart)"

    echo -e "$header completed for transaction $txn." >> $outFile
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed sec" +%H:%M:%S)" >> $outFile
    echo -e "" >> $outFile

    exit $errCode

