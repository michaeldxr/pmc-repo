#!/bin/sh
#
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/finEssCheck.sh /main/17 2016/08/01 02:17:45 mengxu Exp $
#
# finEssCheck.sh
#
# Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
#
#    NAME
#      finEssCheck.sh - Check for any ESS file changes
#
#    DESCRIPTION
#      <short description of component this file declares/defines>
#
#    NOTES
#      <other useful comments, qualifications, etc.>
#
#    MODIFIED   (MM/DD/YY)
#    vtkrishn    01/31/11 - Creation		    
#
#!/bin/sh

if [ $debug -eq 1 ];then
	set -x
fi

if [ $USER == "hkaniven" -a -z "$destdir" ];
then
  
  set -x
  mkdir -p $ADE_VIEW_ROOT/fusionapps/premerge
  export destdir=$ADE_VIEW_ROOT/fusionapps/premerge
  export txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
  export family=`cat $txnFiles| cut -f 2 -d '/' | sort | uniq`
  #family=fin
  export cmd_dir=/ade/hkaniven_fatoolsview/fatools/opensource/jauditFixScripts/FinPreMerge/bin
  
fi  

tstart_bld="$(date +%s)"

dir=$destdir
esscheckerlogfileformail="$dir"/"$txn"_esscheck.out
exitstatus=0
earFlag=0
logfile="$dir"/"$txn"_esscheck_buildoutput.log
logEarfile="$dir"/"$txn"_esscheck_earoutput.log
essdirpatterntocheck="financialsEss/"
buildfile=build-financialsEss.xml

if [ $family == "crm" ];
then

  essdirpatterntocheck="contractManagementEss/\|crmEss/"
  buildfile=build-crmEss.xml
  
elif [ $family == "scm" ]; 
then

  essdirpatterntocheck="scmEss/"
  buildfile=build-scmEss.xml

fi

echo -e "" > $esscheckerlogfileformail
echo -e "ESS Checker Output" >> $esscheckerlogfileformail
echo -e "==================================================" >> $esscheckerlogfileformail
echo -e "Checking for ESS related files" >> $esscheckerlogfileformail
echo -e "" >> $esscheckerlogfileformail

essOutput=`cat $txnFiles | grep $essdirpatterntocheck | cut -f 2- -d'/'`

	if [ -z "$essOutput" ];
	then
		echo -e "$txn does not contain any ess files" >> $esscheckerlogfileformail
		exitstatus=3
	else
		echo -e "Found these files" >> $esscheckerlogfileformail

		for i in $essOutput
		do
			echo -e "" `echo $ADE_VIEW_ROOT/fusionapps/$i` >> $esscheckerlogfileformail
		done

			echo -e "" >> $esscheckerlogfileformail

			echo -e "Running the $family ESS full build as ESS files are found in the transaction" >> $esscheckerlogfileformail
			echo -e "" >> $esscheckerlogfileformail
			
			ade lsexpand > adelsexp.tmp 
			expOK=1
			grep "fusionapps/\*" adelsexp.tmp >/dev/null 2>&1 &&  expOK=0
			if [[ $expOK -ne 0 ]];then
				grep "fusionapps/$family/\*" adelsexp.tmp >/dev/null 2>&1
				expOK=$?
				grep "fusionapps/jlib/\*" adelsexp.tmp >/dev/null 2>&1
				expOK=`expr $? + $expOK`
				grep "fusionapps/build_metadata/\*" adelsexp.tmp >/dev/null 2>&1
				expOK=`expr $? + $expOK`
			fi
			expBuildOK=1
			grep "fmwtools/\*" adelsexp.tmp >/dev/null 2>&1 &&  expBuildOK=0 || { grep "fmwtools/BUILD_HOME/\*" adelsexp.tmp >/dev/null 2>&1 &&  expBuildOK=0 ;}
			rm adelsexp.tmp >/dev/null 2>&1

			earFlag=0
			if [[ `expr $expOK + $expBuildOK` -gt 0 ]];then #Not expanded, Not OK to build.
				echo -e "Warning - Required directories are not expanded. Please expand fusionapps/* and fmwtools/BUILD_HOME/* before trying again." |tee -a $esscheckerlogfileformail
				echo -e 'Command: cd $ADE_VIEW_ROOT; ade expand -recurse fusionapps fmwtools/BUILD_HOME' |tee -a $esscheckerlogfileformail
				exitstatus=2
			else #Expanded fine, OK to build

	
				ant -f $ADE_VIEW_ROOT/fusionapps/$family/$buildfile > $logfile
			
				if `grep  -q "FAILED\|P1 violation" "$logfile" >/dev/null`
				then	
					exitstatus=1
					echo -e "" >> $esscheckerlogfileformail
					echo -e "$family ESS full build is not sucessful so skipping $family ESS EAR creation" >> $esscheckerlogfileformail
					
					grep  "FAILED\|P1 violation" "$logfile" >> $esscheckerlogfileformail
	
				elif `grep  -q "SUCCESS" "$logfile" >/dev/null`
				then	
					earFlag=1
	#	                        grep "SUCCESS" "$logfile" >> $esscheckerlogfileformail
					echo -e "" >> $esscheckerlogfileformail
				else
					exitstatus=2				
					echo -e "The ant command running the ESS full build is not working correctly" >> $esscheckerlogfileformail
				fi
	
				echo -e "" >> $esscheckerlogfileformail
			fi #Expanded fine, OK to build - End.


		if [ $earFlag == 1 ] ; then

			echo -e "ESS full build is successful, so building the $family ESS EAR" >> $esscheckerlogfileformail
			echo -e "" >> $esscheckerlogfileformail
			ant ear -f $ADE_VIEW_ROOT/fusionapps/$family/$buildfile > $logEarfile

			if `grep  -q "FAILED\|P1 violation" "$logEarfile" >/dev/null`
			then	
				exitstatus=1
				echo -e "$family ESS EAR creation failed" >> $esscheckerlogfileformail
				echo -e "" >> $esscheckerlogfileformail
				grep "FAILED\|P1 violation" "$logEarfile" >> $esscheckerlogfileformail
			
			elif `grep  -q "SUCCESS" "$logEarfile" >/dev/null`
			then	
				echo -e "$family ESS EAR creation succeeded" >> $esscheckerlogfileformail
				echo -e "" >> $esscheckerlogfileformail
				grep "SUCCESS" "$logEarfile" >> $esscheckerlogfileformail

			else	
				exitstatus=2		
				echo -e "The ant command running the ESS EAR creation is not working correctly" >> $esscheckerlogfileformail
			fi
			

		fi

		
	fi

tstop_bld="$(date +%s)"
telapsed_bld="$(expr $tstop_bld - $tstart_bld)"

echo -e "" >> $esscheckerlogfileformail
echo -e "Completed for transaction $txn." >> $esscheckerlogfileformail
echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_bld sec" +%H:%M:%S)" >> $esscheckerlogfileformail
echo -e "" >> $esscheckerlogfileformail

exit $exitstatus;
