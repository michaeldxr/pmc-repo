#!/bin/sh

tstart_deptriggers="$(date +%s)"

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps

deptriggersoutfileformail="$destdir"/"$txn"_deprecatedtriggers.out
errCd2=0
deptriggersoutfile="deprecatedTriggers.csv"

echo -e "" > $deptriggersoutfileformail
echo -e "Deprecated Triggers Scan Output" >> $deptriggersoutfileformail
echo -e "==================================================" >> $deptriggersoutfileformail

#echo -e "Running Deprecated Triggers CodeScan for the transaction $txn" >> $deptriggersoutfileformail
#echo -e "" >> $deptriggersoutfileformail

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$destdir/* -Xms512m -Xmx1024m oracle.apps.psr.DeprecatedTriggersPreMergeChecker "$txnFiles" $ADE_VIEW_ROOT

    if `grep  -q "fusionapps" $deptriggersoutfile`
    then
	errCd2=1
    fi

    echo -e "" >> $deptriggersoutfileformail
    cat $deptriggersoutfile >> $deptriggersoutfileformail 
    echo -e "" >> $deptriggersoutfileformail

    tstop_deptriggers="$(date +%s)"
    telapsed_deptriggers="$(expr $tstop_deptriggers - $tstart_deptriggers)"

    echo -e "Completed for transaction $txn." >> $deptriggersoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_deptriggers sec" +%H:%M:%S)" >> $deptriggersoutfileformail
    echo -e "" >> $deptriggersoutfileformail

    exit $errCd2

