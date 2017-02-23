#!/bin/sh
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/essStrChkPM.sh /main/2 2017/02/02 01:25:06 cxwang Exp $
################################################
# Purpose: Wrapper for ESS String check in premerge mode
#          
# History:
#    Aug-08-2016   Charmaine M.   Initial Rel to Premerge 
#                                 
################################################
CODEHOME=$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/bin/i18n_scans/essStrChk
#testcode
#CODEHOME=/scratch/cxwang/html/scripts/essStrChk
#destdir=/scratch/cxwang/html/scripts/essStrChk/test

echo "===Run ESS String Check $0 "
echo "===Start Time: `date` "

txnFiles="esstxnFilestemp.txt"
txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
export txnDescFile="$txn"_describe_ess.out
ade describetrans > $txnDescFile 
grep -E 'components\/.+?Ess.+essmeta\/.+\.xml' $txnDescFile | awk '{print $2}' | sort | uniq  > $txnFiles

echo "===Start running Ess String Check on `date` " 
rm "$destdir"/ess_*.csv
rm "$destdir"/ess_*.log

counter=0

for fl in `cat $txnFiles`; do 
#    echo "Processing $fl " 
    oneFile=$ADE_VIEW_ROOT/$fl
    counter=`expr $counter + 1`
    cmd="perl $CODEHOME/essStrchk.pl $oneFile $destdir/ess_$counter.log $destdir/ess_$counter.csv"
    echo $cmd
    $cmd
done
echo "Total Number of Files Found: $counter" 

errCd=0
if `grep  -qm1 "\/" $destdir/ess_*.csv`
then
   errCd=1
   echo "Please review the KIX for more details: https://confluence.oraclecorp.com/confluence/display/fai18n/Other+I18N+String+Checks#OtherI18NStringChecks-ESSStringChecker"
fi
echo "===Program completed with status $errCd: `date` " 
exit $errCd
