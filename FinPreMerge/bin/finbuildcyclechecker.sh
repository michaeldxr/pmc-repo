#!/bin/sh
#set -x

####################################################################################
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/finbuildcyclechecker.sh /main/9 2016/08/01 02:17:43 mengxu Exp $
# Purpose: Script to check Build Cycles which will be called as part of fin_premerge.ksh
#
# Revision:
#     Feb-2011   Created                                                 Harikiran
#                                  
####################################################################################

if [ $debug -eq 1 ];then
        set -x
fi

tstart_jut="$(date +%s)"

#For independent script testing purposes
if [ $USER == "hkaniven" -a -z "$destdir" ];
then

  set -x
  mkdir -p $ADE_VIEW_ROOT/fusionapps/premerge
  destdir=$ADE_VIEW_ROOT/fusionapps/premerge
  txn=`ade pwv | grep VIEW_TXN_NAME | cut -d : -f 2 | tr -d [\ ]`
  family=`cat $txnFiles | grep fusionapps | sed 's/ //g' | cut -f 2 -d '/' | sort | uniq`
  
fi  

buildcyclecheckeroutfileformail="$destdir"/"$txn"_bcc.out
buildcyclecheckerlog="$destdir"/"$txn"_bcc.log
exitStatus=0

echo -e "" > $buildcyclecheckerlog
echo -e "" > $buildcyclecheckeroutfileformail

echo -e "Build Cycle Checker Output" >> $buildcyclecheckeroutfileformail
echo -e "==================================================" >> $buildcyclecheckeroutfileformail

if `cat $txnFiles | grep fusionapps/$family/components | grep -q ".jws\|.jpr"`
then

#  echo -e "Running Build Cycle Checker for jpr/jws involved in the transaction $txn" >> $buildcyclecheckeroutfileformail

  for i in `cat $txnFiles | grep fusionapps/$family/components | grep ".jws\|.jpr" | cut -f 1-4 -d "/" | sort | uniq`

  do

    echo -e "" >> $buildcyclecheckeroutfileformail
    echo -e "Running Build Cycle Checker now for jpr/jws part of $i" >> $buildcyclecheckeroutfileformail
    echo -e "" >> $buildcyclecheckeroutfileformail

#    buildapps=`echo $i | cut -f 4 -d "/"`
#    buildfile="build-"$buildapps".xml" 
         
#    echo -e "Build file used is $buildfile" >> $buildcyclecheckeroutfileformail
    
    ant -f $ADE_VIEW_ROOT/fusionapps/$family/build.xml build-testrun >> $buildcyclecheckerlog
      
    if `grep -q 'cycle ' $buildcyclecheckerlog`
    then

      echo -e "" >> $buildcyclecheckeroutfileformail
      echo -e "Build cycles have been detected for jpr/jws part of $i" >> $buildcyclecheckeroutfileformail
      echo -e `grep 'cycle ' $buildcyclecheckerlog | cut -f 2- -d " " ` >> $buildcyclecheckeroutfileformail
      echo -e "" >> $buildcyclecheckeroutfileformail
        
      exitStatus=1
      
    else  

      echo -e "" >> $buildcyclecheckeroutfileformail
      echo -e "No Build cycles detected for jpr/jws part of $i" >> $buildcyclecheckeroutfileformail
      echo -e "" >> $buildcyclecheckeroutfileformail
    
    fi
  
  done

  echo -e "Running Build Cycle Checker completed for jpr/jws involved in the transaction $txn" >> $buildcyclecheckeroutfileformail
  
else

  echo -e "" >> $buildcyclecheckeroutfileformail
  echo -e "No jpr/jws involved in the transaction $txn, so skipping running Build Cycle Checker" >> $buildcyclecheckeroutfileformail
  echo -e "" >> $buildcyclecheckeroutfileformail
  exitStatus=3
  
  
fi

tstop_jut="$(date +%s)"
telapsed_jut="$(expr $tstop_jut - $tstart_jut)"
echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_jut sec" +%H:%M:%S)" >> $buildcyclecheckeroutfileformail

exit $exitStatus
