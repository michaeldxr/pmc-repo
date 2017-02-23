#!/bin/sh

####################################################################################
# Purpose: Script to create patch on files involved in the transaction as part of fin_premerge.ksh
#
# Revision:
#     Jun-2011   Created                                                 Harikiran
#                                  
####################################################################################


if [ $debug -eq 1 ];then
        set -x
fi
#For independent script testing purposes
if [ $USER == "hkaniven" -a -z "$destdir" ];
then

  set -x
  mkdir -p $ADE_VIEW_ROOT/fusionapps/premerge
  destdir=$ADE_VIEW_ROOT/fusionapps/premerge
  txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
  family=`cat $txnFiles| grep fusionapps | awk '{print $2}' | cut -f 2 -d '/' | sort | uniq`
  #family=fin
  bugNo=`echo $txn | cut -f 2 -d "_" | cut -f 2 -d "-"`
  
fi  

tstart_arupatch="$(date +%s)"
arupatchoutfileformail="$destdir"/"$txn"_arupatch.out
arupatchlogfile="$destdir"/"$txn"_arupatch.log
exitStatus=0

echo -e "" > $arupatchoutfileformail
echo -e "" > $arupatchoutfileformail

echo -e "" > $arupatchoutfileformail
echo -e "Patch Creation Output" >> $arupatchoutfileformail
echo -e "==================================================" >> $arupatchoutfileformail

if `cat $txnFiles | grep -q fusionapps/$family`
then

  echo -e "Creating patch for files involved in the transaction $txn" >> $arupatchoutfileformail
  echo -e "" >> $arupatchoutfileformail

  ant patch -f $ADE_VIEW_ROOT/fabuildtools/lib/build-patch.xml -DgraphFile=build_metadata/graph.xml -Dtransaction=$txn -Dbug=$bugNo -Dbaseproductfamily=$family -Dmode=dev > $arupatchlogfile 2>&1
  
  if `grep  -q "FAILED" $arupatchlogfile >/dev/null`
  then

    grep "FAILED\|[Ee]xception\|Error\|ERROR\|ARU:\|TA validating\|error:" $arupatchlogfile >> $arupatchoutfileformail
    exitStatus=1
   
  elif `grep  -q "WARNING:\|INFO:" $arupatchlogfile >/dev/null`
  then
     
        grep "WARNING:\|INFO:" $arupatchlogfile >> $arupatchoutfileformail
        exitStatus=2
    
  else
  
    grep "SUCCESS\|ARU:\|Building zip:\|created patch\|TA validating" $arupatchlogfile >> $arupatchoutfileformail

  fi

  echo -e "" >> $arupatchoutfileformail
	
else

  echo -e "No files involved in the transaction $txn, so skipping patch creation" >> $arupatchoutfileformail
  echo -e "" >> $arupatchoutfileformail
  exitStatus=3


fi	

tstop_arupatch="$(date +%s)"
telapsed_arupatch="$(expr $tstop_arupatch - $tstart_arupatch)"

echo -e "Patch Creation Completed for transaction $txn" >> $arupatchoutfileformail
echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_arupatch sec" +%H:%M:%S)" >> $arupatchoutfileformail
echo -e "" >> $arupatchoutfileformail
  
exit $exitStatus


  
