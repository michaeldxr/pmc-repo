#!/bin/sh

##############################################################################
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/finmodelstringchecker.sh /main/13 2016/08/01 02:17:44 mengxu Exp $
# Purpose: Script to run Model String Checker on individual files
# involved in a transaction as part of main script fin_premerge.ksh
#
# Revision:
#     Jan-2011   Created         Harikiran
#                                  
##############################################################################

if [ $debug -eq 1 ];then
        set -x
fi


#For independent script testing purposes

if [ $USER == "hkaniven" ]
then
 
  set -x
  export cmd_dir=/ade/hkaniven_fatoolsview/fatools/opensource/jauditFixScripts/FinPreMerge/bin
  txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
  mkdir -p $ADE_VIEW_ROOT/fusionapps/premerge
  destdir=$ADE_VIEW_ROOT/fusionapps/premerge
  family=`cat $txnFiles | grep fusionapps | sed 's/ //g' | cut -f 2 -d '/' | sort | uniq`
  
fi  

stringcheckeroutfileformail="$destdir"/"$txn"_stringchecker.out
logdir=$destdir
exitstatus=0

echo -e "" > $stringcheckeroutfileformail
echo -e "Model String Checker Output" >> $stringcheckeroutfileformail
echo -e "==================================================" >> $stringcheckeroutfileformail

if `cat $txnFiles | grep -iv '/[a-zA-Z]*ui/' | grep -q fusionapps/$family/components`
then

  #echo -e "Running Model String Checker for files involved in the transaction $txn" >> $stringcheckeroutfileformail

  descoutput=`cat $txnFiles | grep -iv '/[a-zA-Z]*ui/' | grep -i fusionapps/$family/components | cut -f 2- -d "/" | sed 's/ //g' | sort | uniq`
  #echo $descoutput

  for i in `echo $descoutput`
  do

    #j=`dirname $i`
    #echo $j;

    echo -e "" >> $stringcheckeroutfileformail
    echo -e "Running Model String Checker for file "$ADE_VIEW_ROOT"/fusionapps/"$i" " >> $stringcheckeroutfileformail

    sleep 5
    runname=`date +%s`
    echo -e "Runname for "$ADE_VIEW_ROOT"/fusionapps/"$i" is $runname" >> $stringcheckeroutfileformail

    $cmd_dir/freModelRBcheck.sh "$ADE_VIEW_ROOT"/fusionapps/"$i" $logdir $runname

    echo -e "" >> $stringcheckeroutfileformail

    if `grep -qw 'Line [0-9][0-9]*' $logdir/msgPlHolder_"$runname".log` 
    then

      echo -e "msgPlHolder Output" >> $stringcheckeroutfileformail
      echo -e "==================" >> $stringcheckeroutfileformail

      cat $logdir/msgPlHolder_"$runname".csv >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      grep -w 'Line [0-9][0-9]*' $logdir/msgPlHolder_"$runname".log >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      echo -e "-------------------------------------------------------------------------------" >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      exitstatus=1

    fi

    if `grep -qw 'Line [0-9][0-9]*' $logdir/noAttr_"$runname".log` 
    then

      echo -e "noAttr Output" >> $stringcheckeroutfileformail
      echo -e "=============" >> $stringcheckeroutfileformail

      cat $logdir/noAttr_"$runname".csv >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      grep -w 'Line [0-9][0-9]*' $logdir/noAttr_"$runname".log >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      echo -e "-------------------------------------------------------------------------------" >> $stringcheckeroutfileformail
 
      echo -e "" >> $stringcheckeroutfileformail

      exitstatus=1

    fi

    if `grep -qw 'Line [0-9][0-9]*' $logdir/nulLOV_"$runname".log` 
    then

      echo -e "nullLOV Output" >> $stringcheckeroutfileformail
      echo -e "=============" >> $stringcheckeroutfileformail

      cat $logdir/nulLOV_"$runname".csv >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      grep -w 'Line [0-9][0-9]*' $logdir/nulLOV_"$runname".log >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      echo -e "-------------------------------------------------------------------------------" >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      exitstatus=1

    fi

    if `grep -qw 'Line [0-9][0-9]*' $logdir/DFFnoAttr_"$runname".log` 
    then

      echo -e "DFFnoAttr Output" >> $stringcheckeroutfileformail
      echo -e "================" >> $stringcheckeroutfileformail

      cat $logdir/DFFnoAttr_"$runname".csv >> $stringcheckeroutfileformail

      echo -e "-------------------------------------------------------------------------------" >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      grep -w 'Line [0-9][0-9]*' $logdir/DFFnoAttr_"$runname".log >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      echo -e "-------------------------------------------------------------------------------" >> $stringcheckeroutfileformail

      echo -e "" >> $stringcheckeroutfileformail

      exitstatus=1

    fi

    echo -e "" >> $stringcheckeroutfileformail

    echo -e "Running String Checker completed for file "$ADE_VIEW_ROOT"/fusionapps/"$i" " >> $stringcheckeroutfileformail
    echo -e "" >> $stringcheckeroutfileformail

    echo -e "===============================================================================" >> $stringcheckeroutfileformail

  done

  echo -e "" >> $stringcheckeroutfileformail
  echo -e "Completed for transaction $txn." >> $stringcheckeroutfileformail
  echo -e "" >> $stringcheckeroutfileformail

else

  echo -e "No files modified in the transaction $txn requires Model String Checker to be run, so skipping it." >> $stringcheckeroutfileformail
  echo -e "" >> $stringcheckeroutfileformail
  exitstatus=3
   
fi

echo -e "Model String Checker Output written to $stringcheckeroutfileformail"

exit $exitstatus;



