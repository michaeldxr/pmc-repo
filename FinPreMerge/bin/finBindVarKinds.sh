#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_bindVarKindMismatch="$(date +%s)"

bindVarKindMismatchoutfileformail="$destdir"/"$txn"_bindVarKindMismatch.out
errCd8=0
bindVarKindMismatchoutfile="BindVarKindMismatch.csv"

echo -e "" > $bindVarKindMismatchoutfileformail
echo -e "BindVarKindMismatch Scan Output" >> $bindVarKindMismatchoutfileformail
echo -e "==================================================" >> $bindVarKindMismatchoutfileformail

#echo -e "Running bindVarKindMismatch CodeScan for the transaction $txn" >> $bindVarKindMismatchoutfileformail
#echo -e "" >> $bindVarKindMismatchoutfileformail

java -Dmode=premerge -Djava.secutiry.egd=file:/dev/./urandom -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.sqlanalyzer.BindVariableKinds "$txnFiles" $ADE_VIEW_ROOT $label
errCd8=$?

    if `grep  -q "fusionapps" $bindVarKindMismatchoutfile`
    then
	errCd8=1
    fi

    cat $bindVarKindMismatchoutfile >> $bindVarKindMismatchoutfileformail 

    tstop_bindVarKindMismatch="$(date +%s)"
    telapsed_bindVarKindMismatch="$(expr $tstop_bindVarKindMismatch - $tstart_bindVarKindMismatch)"

    echo -e "Completed for transaction $txn." >> $bindVarKindMismatchoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $bindVarKindMismatchoutfileformail
    echo -e "" >> $bindVarKindMismatchoutfileformail

    exit $errCd8

