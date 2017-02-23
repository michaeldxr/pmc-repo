#!/bin/sh

tstart_serialization="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

serializationoutfileformail="$destdir"/"$txn"_serialization.out
errCd4=0
serializationoutfile="serialization_scan.csv"

echo -e "" > $serializationoutfileformail
echo -e "Serialization Scan Output" >> $serializationoutfileformail
echo -e "===========" >> $serializationoutfileformail
echo -e "Running Serialization CodeScan for the transaction $txn" >> $serializationoutfileformail

echo -e "" >> $serializationoutfileformail

files=`ade describetrans -nocount | grep fusionapps/ | tr -d "[:blank:]" | sort | uniq`

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar -Xms512m -Xmx1024m oracle.apps.serialization.SerializationPreMergeChecker "$files" $ADE_VIEW_ROOT
errCd4=$?

    if `grep  -q "setActionListener" $serializationoutfile`
    then
	errCd4=1
    elif `grep  -q "pageDef executables" $serializationoutfile`
    then
        errCd4=2
    fi

    echo -e "" >> $serializationoutfileformail
    cat $serializationoutfile >> $serializationoutfileformail 
    echo -e "" >> $serializationoutfileformail

    tstop_serialization="$(date +%s)"
    telapsed_serialization="$(expr $tstop_serialization - $tstart_serialization)"

    echo -e "Running Serialization scan completed for the transaction $txn" >> $serializationoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_serialization sec" +%H:%M:%S)" >> $serializationoutfileformail
    echo -e "" >> $serializationoutfileformail

    exit $errCd4

