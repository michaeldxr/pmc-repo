#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi


tstart_contentDelivery="$(date +%s)"

contentDeliveryoutfileformail="$destdir"/"$txn"_contentDelivery.out
errCd8=0
contentDeliveryoutfile="immediateContentDelivery.txt"

echo -e "" > $contentDeliveryoutfileformail
echo -e "ContentDelivery Scan Output" >> $contentDeliveryoutfileformail
echo -e "==================================================" >> $contentDeliveryoutfileformail

#echo -e "Running contentDelivery CodeScan for the transaction $txn" >> $contentDeliveryoutfileformail
#echo -e "" >> $contentDeliveryoutfileformail

java -Dmode=premerge -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.ImmediateContentDeliveryFinder "$txnFiles" $ADE_VIEW_ROOT $label
errCd8=$?

    if `grep  -q "fusionapps" $contentDeliveryoutfile`
    then
	errCd8=1
    fi

    cat $contentDeliveryoutfile >> $contentDeliveryoutfileformail 

    tstop_contentDelivery="$(date +%s)"
    telapsed_contentDelivery="$(expr $tstop_contentDelivery - $tstart_contentDelivery)"

    echo -e "Completed for transaction $txn." >> $contentDeliveryoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_rct sec" +%H:%M:%S)" >> $contentDeliveryoutfileformail
    echo -e "" >> $contentDeliveryoutfileformail

    exit $errCd8

