#!/bin/sh

####################################################################################
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/finincrbuild.sh /main/20 2016/08/01 02:17:44 mengxu Exp $
# Purpose: Script to run Incremental Build for the files involved in a transaction as part of fin_premerge.ksh
#
# Revision:
#     Nov-2010   Created                                                 Harikiran
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
  export destdir=$ADE_VIEW_ROOT/fusionapps/premerge
  export txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
  export family=`cat $txnFiles | grep fusionapps | sed 's/ //g' | cut -f 2 -d '/' | sort | uniq`
  #family=fin
  export cmd_dir=/ade/hkaniven_fatoolsview/fatools/opensource/jauditFixScripts/FinPreMerge/bin
  
fi  

tstart_bld="$(date +%s)"

incrbldoutfileformail="$destdir"/"$txn"_incrbld.out
exitStatus=0
incrbldlogfile="$destdir"/"$txn"_incrbld.log
esscheckerlogfileformail="$destdir"/"$txn"_esscheck.out
essdirpatterntocheck="financialsEss/"

if [ $family == "crm" ];
then

  essdirpatterntocheck="contractManagementEss/\|crmEss/"
  
elif [ $family == "scm" ]; 
then

  essdirpatterntocheck="scmEss/"

fi


echo -e "" > $incrbldoutfileformail
echo -e "Incremental Build Output" >> $incrbldoutfileformail
echo -e "==================================================" >> $incrbldoutfileformail

if `cat $txnFiles | grep fusionapps/$family | grep -qv $essdirpatterntocheck`
then

#  echo -e "Running Incremental Build for the transaction $txn" >> $incrbldoutfileformail
#  echo -e "" >> $incrbldoutfileformail

  ant -f $ADE_VIEW_ROOT/fusionapps/$family/build.xml incrementalBuild -Dtransactions=$txn -Dpatchcore.excludedListeners=oracle.apps.ateam.patch.service.OHSDiffListener > $incrbldlogfile 2>&1
  
  if `grep  -q "FAILED" $incrbldlogfile >/dev/null`
  then

    #TODO Get the relevant portion from log
    exitStatus=1
  elif `grep  -q "ERROR: PATCH BUILD FAILURE" $incrbldlogfile >/dev/null`;then
    exitStatus=2
  fi

  grep "SUCCESS" $incrbldlogfile >> $incrbldoutfileformail
  grep "FAILED\|ERROR: PATCH BUILD FAILURE" $incrbldlogfile >> $incrbldoutfileformail

  echo -e "" >> $incrbldoutfileformail
	
else

  echo -e "No files involved in the transaction $txn requires incremental build to be run, so skipping it" >> $incrbldoutfileformail
  echo -e "" >> $incrbldoutfileformail
  exitStatus=3


fi	

tstop_bld="$(date +%s)"
telapsed_bld="$(expr $tstop_bld - $tstart_bld)"

echo -e "Completed for transaction $txn." >> $incrbldoutfileformail
echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_bld sec" +%H:%M:%S)" >> $incrbldoutfileformail
echo -e "" >> $incrbldoutfileformail

#if [[ $exitStatus -eq 1 ]];then
  
#  echo -e "" > $esscheckerlogfileformail
#  echo -e "ESS Checker Output" >> $esscheckerlogfileformail
#  echo -e "==================" >> $esscheckerlogfileformail
#  echo -e "Incremental Build failed, so skipping ESS Checker" >> $esscheckerlogfileformail
#  echo -e "" >> $esscheckerlogfileformail

#else 

#  $cmd_dir/finEssCheck.sh
#  finessexitStatus=$?
  
#fi  

#if [[ $exitStatus -eq 0 ]];then
#  exitStatus=$finessexitStatus
#fi

exit $exitStatus

