#!/bin/sh
#set -x

if [ $debug -eq 1 ];then
        set -x
fi

tstart_gsc=`date +%s`

exitStatus=0

#For independent script testing purposes
if [ $USER == "hkaniven" -a -z "$destdir" ];
then
  
  set -x
  mkdir -p $ADE_VIEW_ROOT/fusionapps/premerge
  destdir=$ADE_VIEW_ROOT/fusionapps/premerge
  txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
  family=`cat $txnFiles | grep fusionapps | sed 's/ //g' | cut -f 2 -d '/' | sort | uniq`
  #family=fin
  
fi  

#txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#destdir=$ADE_VIEW_ROOT/fusionapps/premerge
#mkdir -p $destdir

gsccfile=$txn"_gscc.txt"
gsccoutfileformail="$destdir"/"$txn"_gscc.out

echo -e "" > $gsccoutfileformail
echo -e "GSCC Output" >> $gsccoutfileformail
echo -e "==================================================" >> $gsccoutfileformail

gscccli=""
test -e "/ade_autofs/ade_infra/ISD_MAIN_LINUX.rdd/LATEST/isd/bin/gscc_run_engine" && gscccli="/ade_autofs/ade_infra/ISD_MAIN_LINUX.rdd/LATEST/isd/bin/gscc_run_engine" || ( test -e "/usr/local/packages/isd/bin/gscccli" && gscccli="/usr/local/packages/isd/bin/gscccli" )

if [[ -z $gscccli ]];then
    echo -e "Your machine cannot access gscccli. Skipping GSCC. You need to set up infrastructure for gscc by following the steps mentioned in this link " >> $gsccoutfileformail
    echo -e "http://ade.oraclecorp.com/twiki/bin/view/ADEFAQ/AdeInfraSetup" >> $gsccoutfileformail
    echo -e "Even after doing that if gscc doesnt work please run GSCC from http://aru.us.oracle.com." >> $gsccoutfileformail
    echo -e "" >> $gsccoutfileformail
	exitStatus=1
	exit $exitStatus
fi

#echo -e "Running GSCC for the transaction $txn" >> $gsccoutfileformail
#echo -e "" >> $gsccoutfileformail


    perl -X $gscccli -txn_name $txn -output_file $gsccfile

    if `grep  -q "GSCC-TEST:FAIL" $gsccfile`
    then
	exitStatus=1
    elif `grep -q "GSCC-TEST:PASS" $gsccfile`;then
	exitStatus=0
    else #Unknown cases like gscc command execution itself failed.
	exitStatus=2
    fi

    echo -e "" >> $gsccoutfileformail

    cat $gsccfile >> $gsccoutfileformail 

    echo -e "" >> $gsccoutfileformail

    tstop_gsc="$(date +%s)"
    telapsed_gsc="$(expr $tstop_gsc - $tstart_gsc)"

    echo -e "Completed for transaction $txn." >> $gsccoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_gsc sec" +%H:%M:%S)" >> $gsccoutfileformail
    echo -e "" >> $gsccoutfileformail

    exit $exitStatus
