#!/bin/sh
#set -x

####################################################################################
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/finjaudit.sh /main/29 2017/01/16 22:35:00 tedong Exp $
# Purpose: Script to run JAudit on files part of a transaction and is called as part of fin_premerge.ksh
#
# Revision:
#     Nov-2010   Created                                                 Harikiran
#                                  
####################################################################################

if [[ $debug -eq 1 ]];then
        set -x
fi

tstart_adt="$(date +%s)"

#For independent script testing purposes
if [[ $USER = "mideng" || $USER = "mengxu" ||$USER = "tedong" ]]; then
  if [ -a "$destdir" ] || [ -z "$destdir" ]; then
    echo "JAudit_Test mode"
    set -x
    destdir=$ADE_VIEW_ROOT/fusionapps/premerge
    mkdir -p $destdir
    txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
    label=`ade pwv|grep VIEW_LABEL |cut -d: -f2|tr -d [\ ]`
    cd "$destdir"

    export txnDescFile="$txn"_describe.out
    echo >$txnDescFile
    echo "Transaction Description" >>$txnDescFile
    echo "=======================" >>$txnDescFile
    ade describetrans >>$txnDescFile 
    echo "=======================" >>$txnDescFile
    echo >>$txnDescFile

    export txnFiles="$txn"_files.lst
    cat $txnDescFile |grep fusionapps/ | awk '{print $2}' | sort | uniq > $txnFiles
    export family=`cat $txnFiles | grep fusionapps | sed 's/ //g' | cut -f 2 -d '/' | sort | uniq`
  fi
fi
 
###############################################################

auditfile="$destdir"/"$txn"_audit.txt
auditoutfileformail="$destdir"/"$txn"_jaudit.out
exitStatus=0

echo -e "" > $auditoutfileformail
echo -e "JAudit Output" >> $auditoutfileformail
echo -e "==================================================" >> $auditoutfileformail

#echo -e "Running JAudit for the transaction $txn" >> $auditoutfileformail
#echo -e "" >> $auditoutfileformail


java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.4.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.premerge.PremergeAuditChecker "$txnFiles" $ADE_VIEW_ROOT $label $auditoutfileformail >> $auditfile
returnCode=$?;

auditOutFile=`grep "Results are saved in the file" "$auditoutfileformail" | cut -d\> -f2`

if [ "$auditOutFile" ];
then
  exitStatus=$returnCode  
else 

  echo -e "" >> $auditoutfileformail
  echo -e "The tool was not able to run JAudit for files part of $txn as the command running JAudit is not working correctly. Please run JAudit from within JDeveloper." >> $auditoutfileformail
  echo -e "" >> $auditoutfileformail
  
  if [[ $exitStatus -eq 0 ]];then
    exitStatus=2
  fi

fi

#Check for duplicate component ids
grep -i "ade WARNING: Lock not acquired by this process" $auditfile
if [[ $? = 0 ]];then
	exitStatus=2
fi	

adeviewroot=`echo $ADE_VIEW_ROOT | sed 's/\//\\\\\//g'`
desctranscmd="cat $txnFiles | grep fusionapps/fin/components | sed 's/fusionapps/$adeviewroot\/fusionapps/'"
echo $desctranscmd
desctransoutput=`eval $desctranscmd`
echo $desctransoutput
duplicatecomponentidflag=0

for filename in $desctransoutput
do

  if `awk '/<\!--/,/-->/{next}1' $filename | grep -v 'parameter id=' | grep -o '[a-zA-Z]*id="[a-zA-Z0-9]*"' | sort | uniq -d | grep -q '[a-zA-Z]*id="[a-zA-Z0-9]*"'`
  then


    if [[ $duplicatecomponentidflag -eq 0 ]];then

      echo -e "" >> $auditoutfileformail
      echo -e "Duplicate Component Id Check Output" >> $auditoutfileformail
      echo -e "" >> $auditoutfileformail
      echo -e "Duplicate component ids found in the files part of the transaction. Please correct them." >> $auditoutfileformail
      echo -e "" >> $auditoutfileformail  
      echo -e "FileName:LineNo:Id" >> $auditoutfileformail
      echo -e "-------- ------ --" >> $auditoutfileformail  
  
      duplicatecomponentidflag=1
      exitStatus=`expr $exitStatus + 4`
  
    fi
  
#  grep -on `grep -o '[a-zA-Z]*id="[a-zA-Z0-9]*"' $desctransoutput | sort | uniq -d | cut -f 2 -d ":" ` $desctransoutput >> $auditoutfileformail
#  for filename in `grep '[a-zA-Z]*id="[a-zA-Z0-9]*"' $desctransoutput | grep -v 'parameter id=' | sort | uniq -d | cut -f 1 -d ":" `
#  do
  
    echo -e "Filename is $filename"
    for pattern in `awk '/<\!--/,/-->/{next}1' $filename | grep -v 'parameter id=' | grep -o '[a-zA-Z]*id="[a-zA-Z0-9]*"' | grep -o '[a-zA-Z]*id="[a-zA-Z0-9]*"' | sort | uniq -d | cut -f 2 -d ":" | sort | uniq`
    do
    
       echo -e "Pattern is $pattern"
       grep -onH $pattern $filename >> $auditoutfileformail
       echo -e "" >> $auditoutfileformail
    
    done

    echo -e "" >> $auditoutfileformail    
    echo -e "" >> $auditoutfileformail    
    
  fi  
  
done
if [[ `cat $txnFiles | grep -iv .sql` ]];then
	echo -e "" >> $auditoutfileformail 
else
	echo -e "Only sql script files found in the current transation. The JAudit Checker will be skipped.">> $auditoutfileformail
	exitStatus=3
fi
  
#if `grep '[a-zA-Z]*id="[a-zA-Z0-9]*"' $desctransoutput | grep -v 'parameter id=' | sort | uniq -d | grep -q '[a-zA-Z]*id="[a-zA-Z0-9]*"'`
#then

tstop_adt="$(date +%s)"
telapsed_adt="$(expr $tstop_adt - $tstart_adt)"

echo -e "Completed for transaction $txn." >> $auditoutfileformail
echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_adt sec" +%H:%M:%S)" >> $auditoutfileformail
echo -e "" >> $auditoutfileformail
echo -e "Output written to $auditoutfileformail"
  
exit $exitStatus
