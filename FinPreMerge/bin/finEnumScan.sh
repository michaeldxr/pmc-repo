#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_enum="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

enumoutfileformail="$destdir"/"$txn"_enum.out
errCd12=0
enumoutfile="enum_violations.csv"

echo -e "" > $enumoutfileformail
echo -e "Enum Scan Output" >> $enumoutfileformail
echo -e "==================================================" >> $enumoutfileformail

#echo -e "Running Enum Codescan for the transaction $txn" >> $enumoutfileformail
#echo -e "" >> $enumoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar oracle.apps.fixer.EnumPremergeChecker "$txnFiles" $ADE_VIEW_ROOT
errCd12=$?

    if `grep  -q "fusionapps/" $enumoutfile`
    then
	errCd12=1
    fi

    echo -e "" >> $enumoutfileformail
    cat $enumoutfile >> $enumoutfileformail 
    echo -e "" >> $enumoutfileformail


    tstop_enum="$(date +%s)"
    telapsed_enum="$(expr $tstop_enum - $tstart_enum)"

    echo -e "Completed for transaction $txn." >> $enumoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_enum sec" +%H:%M:%S)" >> $enumoutfileformail
    echo -e "" >> $enumoutfileformail

    exit $errCd12

