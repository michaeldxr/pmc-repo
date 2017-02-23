#!/bin/sh
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/taskMenuChkPM.sh /main/3 2017/02/02 01:25:06 cxwang Exp $
################################################
# Purpose: Wrapper for taskmenu check in premerge mode
#          Taskmenu Check scans *taskmenu.xml files to identify
# cases where the labels for the tasks/taskgroups are 
# referencing strings in the old resource bundles or are hardcoded strings.
#          
# History:
#    aug-08-2016   Charmaine M.   Initial Rel to Premerge 
#                                 
################################################
CODEHOME=$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/bin/i18n_scans/taskMenuChk
#test code
#destdir=/scratch/cxwang/html/scripts/essStrChk/test

echo "===Run Taskmenu Check $0 "
echo "===Start Time: `date` "

#added to direct exception to FATOOLS dev
#fatools_label=`ade showlabels -series FATOOLS_MAIN_LINUX -latest | tail -n 1`
#fatools_labelserver=`ade desc -l $fatools_label -labelserver`
EXDIR=$FAToolsMainLatest/fatools/opensource/jauditFixScripts/bin/i18n_scans/taskMenuChk

#EXDIR=""

txnFiles="taskmenuTxnFilestemp.txt"
txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
export txnDescFile="$txn"_describe_taskmenu.out
ade describetrans > $txnDescFile 
grep 'taskmenu.xml' $txnDescFile | awk '{print $2}' | sort | uniq  > $txnFiles


echo "===Start running taskmenu check `date` " 
rm "$destdir"/taskmenu_*.csv
rm "$destdir"/taskmenu_*.log

#### Loop through each taskmenu.xml file  ####

counter=0

for fl in `cat $txnFiles`; do 
    #echo "Processing $fl " 
    oneFile=$ADE_VIEW_ROOT/$fl
    counter=`expr $counter + 1`
    #echo "oneFile=$oneFile"
    cmd="perl $CODEHOME/taskmenu.pl $oneFile $destdir/taskmenu_$counter.log $destdir/taskmenu_$counter.csv $EXDIR"
    echo $cmd
    $cmd
done
echo "Total Number of Files Found: $counter" 

errCd=0
if `grep  -qm1 "\/" $destdir/taskmenu_*.csv`
then
   errCd=1
   echo "Please review the KIX for more details:https://confluence.oraclecorp.com/confluence/display/fai18n/Taskmenu+Check"
fi
echo "===Program completed with status $errCd: `date` " 
exit $errCd
