#!/bin/sh

if [ $debug -eq 1 ];then
        set -x
fi

tstart_sqlheaders="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

sqlheadersoutfileformail="$destdir"/"$txn"_sqlheaders.out
errCd1=0
sqlheadersoutfile="headers_violations.txt"

echo -e "" > $sqlheadersoutfileformail
echo -e "Bad PL/SQL Headers Scan Output" >> $sqlheadersoutfileformail
echo -e "==================================================" >> $sqlheadersoutfileformail

#echo -e "Running BadSQLHeaders CodeScan for the transaction $txn" >> $sqlheadersoutfileformail
#echo -e "" >> $sqlheadersoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar -Xms512m -Xmx1024m oracle.apps.headers.BadHeaderPremergeChecker "$txnFiles" $ADE_VIEW_ROOT
errCd1=$1

    if `grep  -q "Filename" $sqlheadersoutfile`
    then
	errCd1=1
    fi

    cat $sqlheadersoutfile >> $sqlheadersoutfileformail 
    echo -e "" >> $sqlheadersoutfileformail

    tstop_sqlheaders="$(date +%s)"
    telapsed_sqlheaders="$(expr $tstop_sqlheaders - $tstart_sqlheaders)"

    echo -e "Completed for transaction $txn." >> $sqlheadersoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_sqlheaders sec" +%H:%M:%S)" >> $sqlheadersoutfileformail
    echo -e "" >> $sqlheadersoutfileformail

    exit $errCd1

