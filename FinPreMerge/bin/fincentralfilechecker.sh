#! /bin/sh
#
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/fincentralfilechecker.sh /main/17 2017/02/14 22:10:04 mengxu Exp $
#
# fincentralfilechecker.sh
#
# Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
#
#    NAME
#      fincentralfilechecker.sh - Checks for any central file changes
#
#    DESCRIPTION
#      <short description of component this file declares/defines>
#
#    NOTES
#      <other useful comments, qualifications, etc.>
#
#    MODIFIED   (MM/DD/YY)
#    mengxu      02/14/17 - Disable SMC checks.
#    mengxu      07/14/16 - Add further checking for supermass consolidation.
#    mengxu      06/13/16 - Bug23569819: Add super consolidation check.
#    vtkrishn    01/21/11 - Creation
#

if [[ $debug -eq 1 ]];then
        set -x
fi

if [[ $USER == "hkaniven" && -z $destdir ]];
then

  set -x
  mkdir -p $ADE_VIEW_ROOT/fusionapps/premerge
  destdir=$ADE_VIEW_ROOT/fusionapps/premerge
  txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
  family=`cat $txnFiles | grep fusionapps | sed 's/ //g' | cut -f 2 -d '/' | sort | uniq`
  #family=fin
  cmd_dir=/ade/hkaniven_fatoolsview/fatools/opensource/jauditFixScripts/FinPreMerge/bin
  
fi  

tstart_bld="$(date +%s)"

dir=$destdir
label=`ade pwv | grep "VIEW_LABEL" | awk '{print $3}'`

#serverDir=$FAToolsMainLatest'/fatools/opensource/jauditFixScripts/FinPreMerge/bin'
centralfilecheckeroutfileformail="$dir"/"$txn"_centralfilechecker.out
tmpCentralFilelist="$dir"/tmpCentralFilelist.txt
tmpConsolidateFilelist="$dir"/tmpConsolidateFilelist.txt
tmpConsolidVioFilelist="$dir"/tmpConsoliVioFilelist.txt
tmpExemConsolidateFilelist="$dir"/tmpExemConsolidateFilelist.txt
tmpFileHashcode="$dir"/tmpFileHashcode.txt
exitstatus=0

if [ -e "$tmpConsolidateFilelist" ];then
	rm $tmpConsolidateFilelist
fi

if [ -e "$tmpConsolidVioFilelist" ];then
	rm $tmpConsolidVioFilelist
fi

if [ -e "$tmpExemConsolidateFilelist" ];then
	rm $tmpExemConsolidateFilelist
fi

echo -e "" > $centralfilecheckeroutfileformail
echo -e "Central File Checker Output" >> $centralfilecheckeroutfileformail
echo -e "==================================================" >> $centralfilecheckeroutfileformail

cat $txnFiles | grep fusionapps/com/config/ | sed 's/ //g' | sort | uniq > $tmpConsolidateFilelist
#cat $txnFiles | grep fusionapps/$family/config/ | sed 's/ //g' | sort | uniq >> $tmpConsolidateFilelist
#cat $txnFiles | grep -f $serverDir/consolidatedfiles.lst  | sed 's/ //g' | sort | uniq >> $tmpConsolidateFilelist

# Temporary Disable the SMC checks
#if [ -e "$tmpConsolidateFilelist" ];
#then
#	comm -23 $txnFiles $tmpConsolidateFilelist > $tmpCentralFilelist
#	descoutput=`cat $tmpCentralFilelist | grep fusionapps/$family | grep -f $cmd_dir/centralfiles.lst  | sed 's/ //g' | sort | uniq`
#	
#	echo > $tmpFileHashcode
#	# Check exemptions
#	java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/lib/JarSpreadsheetHelper.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/modules/javax.mail_1.1.0.0_1-4-1.jar:$ADE_VIEW_ROOT/fmwtools/BUILD_HOME/wlserver_10.3/server/ext/jdbc/oracle/11g/ojdbc5.jar oracle.apps.premerge.PremergeConsolidationCheck $tmpConsolidateFilelist $ADE_VIEW_ROOT $tmpExemConsolidateFilelist $tmpConsolidVioFilelist $label >> $tmpFileHashcode
#	
#	if [ -s "$tmpExemConsolidateFilelist" ];then
#		echo -e "The following files are exempted:" >> $centralfilecheckeroutfileformail
#
#		while read -r line
#		do
#			if [[ ! -z $line ]];then
#				echo $line >> $centralfilecheckeroutfileformail		
#			fi
#		done < $tmpExemConsolidateFilelist
#		echo >> $centralfilecheckeroutfileformail
#	fi
#
#	if [ -s "$tmpConsolidVioFilelist" ];then
#		echo -e "ERROR: found unapproved changed to the following descriptor files" >> $centralfilecheckeroutfileformail
#		exitstatus=1
#
#		while read -r line
#		do
#			if [[ ! -z $line ]];then
#				echo $line >> $centralfilecheckeroutfileformail		
#			fi
#		done < $tmpConsolidVioFilelist
#		echo >> $centralfilecheckeroutfileformail	
#	fi
#else
#	descoutput=`cat $txnFiles | grep fusionapps/$family | grep -f $cmd_dir/centralfiles.lst  | sed 's/ //g' | sort | uniq`
#fi

descoutput=`cat $txnFiles | grep fusionapps/$family | grep -f $cmd_dir/centralfiles.lst | sed 's/ //g' | sort | uniq | cut -f 2- -d'/'`

if [ -n "$descoutput" ];then
	echo -e "WARNING: the following central files are found" >> $centralfilecheckeroutfileformail
	echo -e "" >> $centralfilecheckeroutfileformail	
	for i in $descoutput
	do
		echo -e $ADE_VIEW_ROOT/$i >> $centralfilecheckeroutfileformail
	done
	if [ 1 -ne "$exitstatus" ];then
		exitstatus=2
	fi
fi

if [ 0 -eq "$exitstatus" ];then
	echo -e "$txn does not contain any central files" >> $centralfilecheckeroutfileformail
	echo >> $centralfilecheckeroutfileformail
fi

tstop_bld="$(date +%s)"
telapsed_bld="$(expr $tstop_bld - $tstart_bld)"

echo -e "" >> $centralfilecheckeroutfileformail
echo -e "Completed for transaction $txn." >> $centralfilecheckeroutfileformail
echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_bld sec" +%H:%M:%S)" >> $centralfilecheckeroutfileformail
echo -e "" >> $centralfilecheckeroutfileformail

echo 'exit status = '$exitstatus
exit $exitstatus;
