#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_fndConvertNumber="$(date +%s)"

fndConvertNumberoutfileformail="$destdir"/"$txn"_fndConvertNumber.out
errCd38=0
fndConvertNumberoutfile="UptakeFndConvertNumber.txt"

echo -e "" > $fndConvertNumberoutfileformail
echo -e "FndConvertNumber Scan Output" >> $fndConvertNumberoutfileformail
echo -e "==================================================" >> $fndConvertNumberoutfileformail

#echo -e "Running FndConvertNumber CodeScan for the transaction $txn" >> $fndConvertNumberoutfileformail
#echo -e "" >> $fndConvertNumberoutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.UICodeScanner.SelfPPRScanner "$txnFiles" $ADE_VIEW_ROOT $label
errCd38=$?

    if `grep  -q "fusionapps" $fndConvertNumberoutfile`
    then
	errCd38=1
    fi

    cat $fndConvertNumberoutfile >> $fndConvertNumberoutfileformail 

    tstop_fndConvertNumber="$(date +%s)"
    telapsed_fndConvertNumber="$(expr $tstop_fndConvertNumber - $tstart_fndConvertNumber)"

    echo -e "Completed for transaction $txn." >> $fndConvertNumberoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_fndConvertNumber sec" +%H:%M:%S)" >> $fndConvertNumberoutfileformail
    echo -e "" >> $fndConvertNumberoutfileformail

    exit $errCd38

