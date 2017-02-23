#! /bin/ksh

# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/premerge.ksh /main/10 2017/02/07 19:11:00 mengxu Exp $
#
# premerge.ksh
#
# Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
#
#    NAME
#      premerge.ksh - Central script that invokes various validations mandated in Financials division.
#
#    DESCRIPTION
#      <short description of component this file declares/defines>
#
#    NOTES
#      <other useful comments, qualifications, etc.>
#
#    MODIFIED   (MM/DD/YY)
#       bomathew    2016/12/21 - Creation

#set -x


#start_time=$(date +%s)
#cfgFile=~/.Premerge.cfg

#cmd_this=$0

#profileFile=$cmd_dir/timeProfiling.cfg

#Wrapping the command setters within a function. $time_cmd is set after transaction information is read.
SetCommands()
{

	cmd_incrbld=$time_cmd$cmd_dir/finincrbuild.sh
	cmd_junit=$time_cmd$cmd_dir/finjunit.sh
	cmd_jaudit=$time_cmd$cmd_dir/finjaudit.sh
	cmd_gscc=$time_cmd$cmd_dir/fingscccli.sh
	cmd_strchk=$time_cmd$cmd_dir/finmodelstringchecker.sh
	cmd_cntrlfiles=$time_cmd$cmd_dir/fincentralfilechecker.sh
	cmd_showprogress=$cmd_dir/ShowProgress.ksh
	cmd_essbuild=$time_cmd$cmd_dir/finEssCheck.sh
	cmd_bihdrchk=$time_cmd$cmd_dir/finHdrCheck.pl
	cmd_bicptnchk=$time_cmd$cmd_dir/finCaptionsCheck.pl
	cmd_passivation=$time_cmd$cmd_dir/finpassivation.sh
	cmd_invalidref=$time_cmd$cmd_dir/fininvalidjarrefs.sh
	cmd_depTrig=$time_cmd$cmd_dir/findeprecatedtriggers.sh
	cmd_sqlheaders=$time_cmd$cmd_dir/finbadheaders.sh
	cmd_rct=$time_cmd$cmd_dir/finrct.sh
	cmd_selectCount=$time_cmd$cmd_dir/finSelectCount.sh
	cmd_sdoInterface=$time_cmd$cmd_dir/finSDOInterface.sh
	cmd_unusedTF=$time_cmd$cmd_dir/finUnusedTF.sh
	cmd_vcScan=$time_cmd$cmd_dir/finVCScan.sh
	cmd_vcPerfScan=$time_cmd$cmd_dir/finVCNewScans.sh
	cmd_rsiScan=$time_cmd$cmd_dir/finRSIScan.sh
	cmd_biScan=$time_cmd$cmd_dir/finBIScan.sh
	cmd_uiSearchScan=$time_cmd$cmd_dir/finUISearchScan.sh
        cmd_sysdateScan=$time_cmd$cmd_dir/finSysdateScan.sh
        cmd_enumScan=$time_cmd$cmd_dir/finEnumScan.sh
        cmd_autoExecuteVCScan=$time_cmd$cmd_dir/finAutoExecuteVCScan.sh
	cmd_buildcycle=$time_cmd$cmd_dir/finbuildcyclechecker.sh
	cmd_bifolderchk="$time_cmd$cmd_dir/faBipFolderChk.pl mode=trans"
	cmd_arupatch=$time_cmd$cmd_dir/finarupatch.sh
	cmd_utplsql=$time_cmd$cmd_dir/finrunutplsql.sh
	cmd_vcMethodAction=$time_cmd$cmd_dir/finMethodActionVC.sh
	cmd_adfdi=$time_cmd$cmd_dir/finDiScans.sh
        cmd_rowlevelbinding=$time_cmd$cmd_dir/finADFdiRowLevelBindingCheck.sh
        cmd_plsqlauto=$time_cmd$cmd_dir/finPlsqlAutonomous.sh
        cmd_seedvalidation=$time_cmd$cmd_dir/finSeedchecksum.sh
        cmd_contentDelivery=$time_cmd$cmd_dir/finContentDelivery.sh
        cmd_adfdiVersionCheck=$time_cmd$cmd_dir/finADFdiVersionCheck.sh
        cmd_bindVarCheck=$time_cmd$cmd_dir/finBindVarKinds.sh
        cmd_seedEndDates=$time_cmd$cmd_dir/finSeedEndDates.sh
        cmd_votuningV3=$time_cmd$cmd_dir/finVOTuningV3.sh
        cmd_sessiontimeout=$time_cmd$cmd_dir/finSessionTimeout.sh
        cmd_avoidTranslator=$time_cmd$cmd_dir/finAvoidLoadingTranslator.sh
        cmd_frevalidation=$time_cmd$cmd_dir/runFreValidate.sh
        cmd_psuedotranslation=$time_cmd$cmd_dir/finTranslation.sh
        cmd_hrdCodeStrCheck=$time_cmd$cmd_dir/hardcodeStrChkPM.sh
	cmd_unusedAutoSubmit=$time_cmd$cmd_dir/finUnusedAutoSubmit.sh
	cmd_delMRInfo=$time_cmd$cmd_dir/deleteMrgreqInfo.sh
	cmd_insMRInfo=$time_cmd$cmd_dir/createMrgreqInfo.sh
	cmd_oatsIAT=$time_cmd$cmd_dir/finOatsVerify.sh
        cmd_voNotPassivateCheck=$time_cmd$cmd_dir/finVONotPassivated.sh
        cmd_altaIssuesCheck=$time_cmd$cmd_dir/finAltaIssues.sh
	cmd_fndConvertNumber=$time_cmd$cmd_dir/finFndConvertNumber.sh
        cmd_taskMenuChk=$time_cmd$cmd_dir/taskMenuChkPM.sh
        cmd_essStrChk=$time_cmd$cmd_dir/essStrChkPM.sh
	cmd_eoHistoryCols=$time_cmd$cmd_dir/finScanEOHistoryCols.sh
	cmd_changeIndicator=$time_cmd$cmd_dir/finChangeIndicatorNotSet.sh
}

identifyLabels()
{
        while read l;do
        	echo $l|grep "Primary Product" >/dev/null
        	if [[ $? -eq 0 ]];then
                	export PrimaryView=`echo $l|cut -d\  -f1`
        	else
        		echo $l|grep "FATOOLS_" >/dev/null
        		if [[ $? -eq 0 ]];then
                		export FAView=`echo $l|cut -d\  -f1`
        		fi
		fi
        done <<-EOF
                `ade showdepprods | egrep "Primary Product|FATOOLS_"|cut -d: -f2`
	EOF
	
	if [[ -z $FAView ]];then #Primary view is on FATOOLS
		export FAView=$PrimaryView
	fi
	resetFAlabel=`ls -l $ADE_VIEW_ROOT/fatools|grep "FATOOLS[A-Z0-9\_\.]*LINUX.rdd"`
	if [[ -n $resetFAlabel ]];then
		FAReset=$( basename $( dirname $( echo $resetFAlabel |cut -d\> -f2 ) | sed 's/.rdd\//_/' ) )
	fi

	export label=$PrimaryView
	export labelServer=`ade desc -l $label -labelserver`
	export labelServerTop=`echo $labelServer|cut -d/ -f1-3`
	export FAToolsLatest="$( dirname $( ade desc -l $FAView -labelserver ) )/LATEST"
	FatoolsLatestLabel=`ade showlabels -series FATOOLS_MAIN_LINUX -latest | tail -n 1`
        if [[ -z $FAToolsMainLatest ]]; then 
        	export FAToolsMainLatest=`ade desc -l $FatoolsLatestLabel -labelserver`
        fi
	
	MsgFile="$FAToolsLatest/fatools/opensource/jauditFixScripts/FinPreMerge/bin/Message.txt"
}


showBanner()
{
	echo
	echo "		Fusion Pre-merge Validation Tool (version 1.0.0.8)"
	echo "		--------------------------------------------------"
	echo
	echo "	Read more about the tool at https://confluence.oraclecorp.com/confluence/pages/viewpage.action?pageId=82378292"
	echo "	Issues, ERs, Suggestions? Please follow the FAQ link at the bottom of the above confluence page."
	echo "	Note: Some Angrybird issues will only be audited in central runs, details are at"
	echo "	https://stbeehive.oracle.com/teamcollab/wiki/Fintech+Standards:All+Angrybirds+Scans"
	echo
	tail -n +2 $MsgFile 2>/dev/null
	if [[ -z $1 ]];then
		head -1 $MsgFile 2>/dev/null|grep "^Critical=1" >/dev/null 
		if [[ $? -eq 0 ]];then
			Ans=`head -1 $MsgFile 2>/dev/null|grep "Answer="|cut -d: -f2|cut -d= -f2`
			if [[ -z $Ans ]];then
				echo "Press Enter to continue." && read -t$timeOut
			else
				UsrIp=`grep UserAns $ADE_VIEW_ROOT/Ans.txt 2>/dev/null |cut -d: -f2`
				while [[ $Ans != $UsrIp ]];do
					echo "Please read the above message carefully and type $Ans to continue."
					read UsrIp
				done
				echo "UserAns:$Ans" > $ADE_VIEW_ROOT/Ans.txt 
			fi
		fi
		
	fi
}

showUsageAndExit()
{
        echo
        echo "##############################################################"
        echo "Usage $0 [-v |-f] [-d <db connect string>] [-n] [-j <Junit Test Projects>] [-h] [-r] -D<Key>=<Value>"
	echo "-h  - Show this help."
        echo "-v  - Validate Only. No mergereq."
        echo "-r  - FRE DB connection details. Required when running premerge with the option -DPerformFreValidation=1."
        echo "      Format -r dbhost:port:sid/systempass:system_password/fusionpass:fusion_password"
        echo "      eg -r abc.us.oracle.com:1521:abc/systempass:manager/fusionpass:fusion"
        echo "-f  - Force mergereq even if validations fail."
        echo "-d  - Connection information for the db to run JUnits against. Mandatory for the first time."
        echo "      The information will be stored in $junitdb_file for subsequent runs when -d option is not provided."
        echo "      e.g. -d fusion_runtime/fusion_runtime@indl144xxx.idc.oracle.com:1522/in144xxx"
        echo "-j  - Do not automatically identify the jUnit test project to run. Run the one passed instead."
        echo "      Path of the test project must start with fusionapps."
	echo "      Multiple projects can be passed as a comma separated list or by repeating -j."
        echo "      e.g. -j fusionapps/fin/components/payables/ap/invoices/transactions/modelTest/FinApInvTransactionsModelTest.jpr"
        echo "-n  - Do not update bug with validation results. (This option is not honored anymore! Please use -DupdateBug=N instead.)"
        echo "##############################################################"
        echo
        echo "  Read more about the tool at https://confluence.oraclecorp.com/confluence/pages/viewpage.action?pageId=82378292"
        echo "  Issues, ERs, Suggestions? Please follow the FAQ link at the bottom of the above confluence page."
        echo

        exit 99
}

getEmailID()
{

 email=`grep -i "^EMail:$curUser:" $cfgFile 2>/dev/null |cut -d: -f3|grep -i "@oracle.com$"`

 if [[ -n $email ]];then
	echo "Reading email id from $cfgFile" | tee -a $outfile
	return
 fi
 tmp=$curUser".ldap.tmp"
 /usr/local/bin/phone $curUser >$tmp 2>/dev/null

 nEntries=`cat $tmp 2>/dev/null|grep "numEntries" | cut -d: -f2`
 if [[ $nEntries -eq 1 ]];then
  email=`grep "^mail:" $tmp| cut -d: -f2`
  echo "Obtained email id from LDAP." | tee -a $outfile
 else
  validId=0
  while [[ $validId -eq 0 ]];do
	if [[ $nEntries -gt 1 ]];then #Multiple entries returned, just list them.
		grep "^mail:" $tmp| cut -d: -f2
	fi
	while [[ -z $email ]];do
		echo "Please enter your email id (firstname.lastname@oracle.com):"
		read email
		emailOrignial=$email
	done

	#ensure email domain is always oracle.com
  	flname=`echo $email|cut -d@ -f1`
	email=$flname"@oracle.com"
	if [[ $email != $emailOrignial ]];then
		echo "*Warning - E-mail id is set to $email."
	fi

	Ask=0
  	/usr/local/bin/phone $flname|grep -i $curUser >/dev/null || { echo "*Warning - $email doesn't appear to be $curUser's e-mail id." | tee -a $outfile; Ask=1 ;}
	echo $email|egrep -i "[[:alpha:]]+\.[[:alpha:]]+@oracle.com$" >/dev/null && validId=1 || { echo "*Warning - The id $email doesn't appear to be a valid oracle email id." | tee -a $outfile; Ask=1 ;} 
	if [[ $Ask -eq 1 ]];then
		validId=0
		echo "Do you want to save it? (y/n)"
		Ans=n
		read -t$timeOut Ans
		echo $Ans|cut -c1|grep -i "y" >/dev/null && validId=1 || email=""
		echo "Storing user specified email id." | tee -a $outfile
	fi
  done
  
 fi
 rm $tmp
 
 echo "EMail:$curUser:$email" >>$cfgFile
 echo "$curUser's email id $email is stored into $cfgFile" | tee -a $outfile
}


SendMail()
{
	mail_start=$(date +%s)
	bugLink=1
	if [[ $mailSent -eq 0 ]];then
		if [[ $1 -eq 0 ]];then
			status=Successful
			bugLink=0
		elif [[ $1 -eq $ABORT ]];then
			status=Aborted
		elif [[ $1 -lt $ERR ]];then
			status=Warning
		else
			status=Failed
		fi
		echo "Exit Status - $1" >>$outfile
		echo "Fusion pre-merge validations end at "`date "+%d-%h-%y %H:%M"`>>$outfile
		echo  |tee -a $outfile
		echo "*** Overall Validation Status - $status ***" >>$outfile
		echo >>$outfile
		if [[ $bugLink -eq 1 ]];then
			echo "If you encounter any problem when using pre-merge, please raise your questions to our forum:" >>$outfile
			echo "http://myforums.oracle.com/jive3/forum.jspa?forumID=6187" >>$outfile
			echo >>$outfile
			echo "If you want to log a bug against the Pre-merge tool, please click https://bug.oraclecorp.com/pls/bug/webbug_create.enter_info?product_suite_id=&product_suite_id_desc=&product_id=&product_id_desc=&comp=&component_desc=&subcomponent=&subcomponent_desc=&bugno=12532440" >>$outfile
			echo >>$outfile
		fi
		echo >>$outfile	
		mailSubject="[Fusion-Pre-mergereq] - Processing $status for $txn in $ADE_VIEW_NAME"	
		if [[ $1 -eq $ABORT ]];then
			cat $outfile "$destdir"/$txnDescFile 2>/dev/null|mutt -c "$idsToCopyMail" -s "$mailSubject" $email 
		else
			#cat $outfile "$destdir"/"$txn"_*.out 2>/dev/null|mailx -s"[Fusion-Pre-mergereq] - Processing $status for $txn in $ADE_VIEW_NAME" -c "$idsToCopyMail" $email
			#Compatible with Linux6+
			cat $outfile "$destdir"/"$txn"_*.out 2>/dev/null|mutt -c "$idsToCopyMail" -s "$mailSubject" $email 
		fi
		mailSent=1
	fi
	mail_end=$(date +%s)
	mail_elapsed=$(($mail_end - $mail_start))	
}

#Time Stat
ParseAndUploadTimeStat()
{

	total_time=$(($end_time  - $start_time));
	time_invoked=$(date -d@$start_time +"%Y-%m-%d %k:%M:%S")
	if [[ -n "$input_elapsed_time" ]];then
		echo "User_Input,0,$input_elapsed_time,0,0,0%">>$TIME_LOG
	fi
	if [[ -n "$vp_elapsed_time" ]];then
		echo "View_Private,0,$vp_elapsed_time,0,0,0%">>$TIME_LOG
	fi
	if [[ -n "$bug_elapsed_time" ]];then
		echo "Set_Bug_Contents,0,$bug_elapsed_time,0,0,0%">>$TIME_LOG
	fi
	if [[ -n "$txn_elapsed_time" ]];then
		echo "Check_Txn_Info,0,$txn_elapsed_time,0,0,0%">>$TIME_LOG
	fi
	if [[ -n "$telapsed_bug" ]];then
		echo "Bug_FTP,0,$telapsed_bug,0,0,0%">>$TIME_LOG
	fi
	if [[ -n "$process_elapsed" ]];then
		echo "Running_Commands,0,$process_elapsed,0,0,0%">>$TIME_LOG
	fi
	if [[ -n "$tupdatebug_elapsed" ]];then
		echo "Update_Bug_Overall,0,$tupdatebug_elapsed,0,0,0%">>$TIME_LOG
	fi
	if [[ -n "$mail_elapsed" ]];then
		echo "Send_Mail,0,$mail_elapsed,0,0,0%">>$TIME_LOG
	fi
	if [[ -n "$startup_elapsed" ]];then
		echo "ADE_Checks,0,$startup_elapsed,0,0,0%">>$TIME_LOG
	fi
#Time Log Parsing 
#Awk 1. Gets rid of the "Command exited with X status" lines,removes the percent signs found in CPU% and only grabs the last part of the command path.
#Awk 2. Removes parameters passed into the command (Easier to group commands in db, remove if you want to start grouping commands by their input parameters also) 
#tr replaces the newline char with a  ~ character, multiline procedure calls are not supported in SQLPLUS, 
#    Note, if a command in the future contains a tilde(~) character, change the tr and the sql proc to use some other character to delineate separate lines (NOT THE NEWLINE CHARACTER). 

	parsed_time_log=$(awk -F/ '!/^Command exited*/ {gsub(/%/, "",$NF); print $NF}' $TIME_LOG | awk '{if(NF == 1) {print $NF} else{print $1substr($NF, index($NF, ","))}}' | tr '\n' '~')
	
	hostname=$(hostname -f);
	username=$(echo "$txn" | awk -F_ '{print $1;}');
if [[ "x$angrybirdsDBStatus" = "xok" ]];then
	sqlplus -s -l  fintech/fintech@angrybirds.us.oracle.com:1521/codescan > /dev/null 2>&1 <<-EOF
	whenever sqlerror exit
	SET SQLBLANKLINES ON
	exec upload_time_stat('$txn', '$bugNo', '$username', '$family','$label', '$hostname', $total_time, '$time_invoked','$parsed_time_log');
	commit;
	exit;
	EOF
fi
if [[ $runProfile = "1" ]];then
	mv $TIME_LOG $fn
else
	rm "$TIME_LOG"
fi

exit
}



#CleanAndExit ErrorCode Message 
#Also generate the file to be looked up by ADE
CleanAndExit()
{
	echo $2 |tee -a $outfile
	if [[ $1 -ne $ABORT ]];then
		echo "Validation output files `basename $outfile`* are available at $destdir" |tee -a $outfile
		echo "*** FA Pre-merge Validation Status of $txn - $status ***" > $fileForAdeMR
		echo "Current time is `date`" >> $fileForAdeMR
	fi
	SendMail $1
	end_time=$(date +%s)
	if [[ -n  "$TIME_LOG" ]]; then
		fn="$outdir/TimeProfile.`date +%s`.txt"
		ParseAndUploadTimeStat >/dev/null 2>&1 &
		if [[ $runProfile = "1" ]];then
			echo "Time profile is $fn"
		fi
	fi

	if [[ $is32Bit -eq 1 ]];then
		exit 1
	else
		exit $1
	fi

}

getJutDBInfo()
{
        which tnsping >/dev/null 2>&1
        if [[ $? -ne 0 ]];then
                echo
                echo "*** Can't find tnsping. Please make sure you are in a view, ORACLE_HOME is valid and PATH includes ORACLE_HOME/bin."
                errCode=4
                return
        fi

	if [[ -f $junitdb_file ]];then
		echo 
		echo "*** Reading db information from $junitdb_file for running JUnits"
		echo
		export uid=`grep "^User" $junitdb_file |cut -d= -f2`
		export pswd=`grep "^Password" $junitdb_file |cut -d= -f2`
		export host=`grep "^Host" $junitdb_file |cut -d= -f2`
		export port=`grep "^Port" $junitdb_file |cut -d= -f2`
		export sid=`grep "^SID" $junitdb_file |cut -d= -f2`
		
		db=$host:$port/$sid
	        tnsping $db >/dev/null 2>&1
       	 	if [[ $? -eq 0 ]];then
			echo "tnsping to $db succeeded"	
			echo
		else
       	       	  	echo "*** tnsping to $db failed."
			echo "Please make sure you are in a view, $junitdb_file is not corrupt and the db is not down. Cannot proceed."
       	       	  	echo
       	       	  	errCode=4
       	       	  	return
	        fi
		jutDb=1
	fi
}

CallWait()
{
	waitPid=$1
	if [[ -z $waitPid ]];then #CallWait must be called with a pid. Return 255 otherwise.
		return 255
	fi
	if [[ $(( BreakOnError + isErr )) -ge 2 ]];then
		retNow=0
		jobsToKill=`ps -o pid,ppid|grep $waitPid|awk '{print $1}'`
                kill -0 $waitPid 2>/dev/null && echo Killing $waitPid && kill $jobsToKill 2>/dev/null && KilledPids=$KilledPids$waitPid":" && retNow=1
		kill -0 $waitPid 2>/dev/null && kill -9 $jobsToKill 2>/dev/null
		if [[ $retNow -eq 1 ]];then
			return
		fi
	fi
        wait $waitPid
        ret=$?
	kill -0 $waitPid 2>/dev/null
        if [[ $? -eq 0 ]];then
		CallWait $waitPid  
		return $? 
		ret=$?
	fi
        return $ret
}

ProcessBreak()
{

        trap "" 2

        PidGrepList='^'`echo $PidList|sed 's/\ /$|^/g'`'$'
        PidList=`ps -f|awk '{print $2}'|egrep "$PidGrepList" `
	if [[ -z "$PidList" ]];then
		echo "No running jobs." >> "$outfile.jobs"
		trap "ProcessBreak" 2
		return
	fi
        kill -19 `ps -f|awk '{print $2}'|egrep "$PidGrepList" ` $pidPrgs 2>/dev/null #Suspend all running jobs
        jobs  >/dev/null 2>&1
        runningJobs=""

        if [[ "$runProfile"  != "0" ]];then
		runProf="On"
		PidProfGrepList='^'`echo $PidList|sed 's/\ /|^/g'`
		runningJobs=`ps -o pid,cmd |egrep "$PidProfGrepList"|awk -v str=$cmd_dir '{for (i=1;i<=NF;i++) {if ($i ~ str){ n=split($i,a,"/");print $1 "-" a[n]; break} }}'|grep -v "str="`

	else

		runProf="Off"
		jobs >/dev/null 2>&1
		ps -o pid,cmd >/dev/null 2>&1
	        for i in $PidList;do
        	        runningJobs=$runningJobs" "`ps -o pid,cmd |awk '$1 == '$i' {n=split($3,a,"/");print $1 "-" a[n]}'`
	        done
	fi

        echo "Running Jobs - Profiling $runProf" >> "$outfile.jobs"
        echo "------------------------" >>"$outfile.jobs"
        print "$runningJobs" >> "$outfile.jobs"
        echo "------------------------" >>"$outfile.jobs"


        echo
        killPid=-9
        PS3="Your choice to terminate :"
        select choice in All $runningJobs Done
        do
                if [[ $choice = "Done" ]];then
                        break
                elif [[ $choice = "All" ]];then
                        for killPid in $PidList;do
                                jobsToKill=`ps -o pid,ppid|grep $killPid|awk '{print $1}'`
                                kill -0 $killPid 2>/dev/null && echo Killing $killPid && kill $jobsToKill 2>/dev/null && KilledPids=$KilledPids$killPid":"
                                kill -0 $killPid 2>/dev/null && kill -9 $jobsToKill 2>/dev/null
                        done
                        break
                fi

                killPid=`echo $choice|cut -d- -f1`
                if [[ -n $killPid ]];then
                        jobsToKill=`ps -o pid,ppid|grep $killPid|awk '{print $1}'`
                        kill -0 $killPid 2>/dev/null && echo Killing $killPid && kill $jobsToKill 2>/dev/null && KilledPids=$KilledPids$killPid":"
                        kill -0 $killPid 2>/dev/null && kill -9 $jobsToKill 2>/dev/null
                        break
                fi
        done
        trap "ProcessBreak" 2
        PidList=`ps -f 2>/dev/null|awk '{print $2}'|egrep "$PidGrepList" 2>/dev/null`
        kill -18 $PidList $pidPrgs 2>/dev/null #Resume suspended jobs
}


getBugUserInfo()
{
        if [[ -f $bugdb_file ]];then
                echo
                echo "*** Reading bug user information from $bugdb_file"
                echo
                export bugUid=`grep "^User" $bugdb_file |cut -d= -f2`
                export bugPswd=`grep "^Password" $bugdb_file |cut -d= -f2`

                bugUser=1
        fi
}

getVerInfo()
{
	echo "Product view on $PrimaryView" >> $outfile
	echo "Dependent FA label - $FAView" >> $outfile
	if [[ -n $FAReset ]];then
		echo "FA label reset to - $FAReset" >> $outfile
	fi

	vTemp=`cat $VersionFile 2>/dev/null`
	l=${#vTemp}
	if [[ $is32Bit -eq 1 ]];then
		Ver2=`echo $vTemp|cut -c $(( ( ( $l + 1 ) / 2 + 1 ) ))-`
	else
		Ver2=${vTemp: -( l / 2 )}
	fi
	bugUser=1
	firstChar=`echo $cmd_this|cut -c 1`
	if [[ $firstChar = "/" ]];then
        	Param=1011
	else
		echo '**Possibly incorrect execution of the tool. The tool must be run providing absolute path starting with $ADE_VIEW_ROOT.'|tee -a $outfile
        	Param=1012
	fi
}

askAndSaveBugUserInfo()
{
	echo "Please enter BugDB user name [$curUser]: "
	read -t$timeOut bugId
	if [[ -z $bugId ]];then
		bugId=$curUser
	fi
	echo "Password:"
	stty -echo
	read  -t$timeOut bugPwd
	stty echo
	if [[ -z $bugPwd ]];then
		echo "Bug password cannot be empty."
		return
	fi
	echo "User=$bugId">$bugdb_file
	echo "Password=$bugPwd">>$bugdb_file
	chmod 400 $bugdb_file
	export bugUid=$bugId
	export bugPswd=$bugPwd
	bugUser=1
	echo "Stored bug user info to $bugdb_file (readable only by you)."
}

setBugContext()
{

        which sqlplus >/dev/null 2>&1
        if [[ $? -ne 0 ]];then
                echo
                echo "*** Can't find sqlplus. Please make sure you are in a view, ORACLE_HOME is pointing to a valid location and PATH includes ORACLE_HOME/bin. Bug update is turned off."
                updateBug="N"
                return
        fi

	set +x

	Ver1=`$VersionCmd $Param $StartDir`


        SFtp1=`$VersionCmd 2011 $StartDir`
        vTemp=`cat $BugVersionFile 2>/dev/null`
        l=${#vTemp}
        SFtp2=`echo $vTemp|cut -c $(( ( ( $l + 1 ) / 2 + 1 ) ))-`

        bugVer=$( $VersionCmd 2100 "$SFtp1$SFtp2" 2>/dev/null )

        oldCon='(DESCRIPTION= (ADDRESS_LIST= (LOAD_BALANCE=ON) (ADDRESS=(PROTOCOL=tcp)(HOST=apd211-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd211-crs.us.oracle.com)(PORT=1522)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd212-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd212-crs.us.oracle.com)(PORT=1522)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd213-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd213-crs.us.oracle.com)(PORT=1522)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd214-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd214-crs.us.oracle.com)(PORT=1522)) ) (CONNECT_DATA=(SERVICE_NAME=bugap.us.oracle.com)))'

        if [[ $dev -eq 1 ]];then
            newCon='(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=umogridxt08-scan.us.oracle.com)(PORT = 1529))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME = LDAP_BUGAU.us.oracle.com)))'
            export ADE_USE_BUGAU=1
        else
            newCon='(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=amogridxp09-scan.us.oracle.com)(PORT = 1529))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME = bugap_adx.us.oracle.com)))'
        fi 

	myConVer=$( $VersionCmd 1100 "$Ver1$Ver2" 2>/dev/null )

connectStr=""

if [[ -z $bugTns ]];then #Try to use the TNS passed by the user using -DbugTns
	sqlplus -L -S $bugUid/$myConVer@$bugTns >/dev/null 2>&1 >/dev/null 2>&1 << EOF && connectStr=$bugTns
	exit
EOF
fi

#OK, now check if tns entry for new Exadata is available in the env.
test -z $connectStr && sqlplus -L -S $bugUid/$myConVer@bugap_adx.us.oracle.com >/dev/null 2>&1 << EOF && connectStr=bugap_adx.us.oracle.com
exit
EOF

#Still no luck, try the new Exadata with the known conn info
test -z $connectStr && sqlplus -L -S $bugUid/$myConVer@"$newCon" >/dev/null 2>&1 <<EOF && connectStr=$newCon && echo "bugap_adx.us.oracle.com=$connectStr" >> ~/.tnsnames.ora
exit
EOF

#May be the old one is still around?
test -z $connectStr && sqlplus -L -S $bugUid/$myConVer@bugap >/dev/null 2>&1 << EOF && connectStr=bugap
exit
EOF

#OK, last try, use the known conn info for the old machine.
test -z $connectStr && sqlplus -L -S $bugUid/$myConVer@"$oldCon" >/dev/null 2>&1 <<EOF && connectStr=$oldCon && echo "bugap=$connectStr" >> ~/.tnsnames.ora
exit
EOF

        if [[ -z $connectStr ]];then
                echo
                echo "*** Could not connect to bugDB. Bug update is turned off."
		updateBug="N"
	else
		echo Bug Connection is: $connectStr |tee -a $outfile
		export connectStr
        fi

        if [[ $debug -eq 1 ]];then
         set -x
        fi

}


checkIfFusionBug()
{

	fusionSuiteId=23

	echo "Verifying if bug $bugNo belongs to Fusion family..."

	set +x

	ret=$(sqlplus  -s  /nolog <<-EOF
	connect $bugUid/$myConVer@"$connectStr"
	set serveroutput on
	set head off
	SET FEEDBACK OFF

	declare
        	pgmr rpthead.programmer%TYPE;
	begin
		select lower(programmer) into pgmr from rpthead where rptno=$bugNo and product_id in (Select product_id from PRODUCT_SUITES, product_groups, product where PRODUCT_SUITES.ID=$fusionSuiteId and product_groups.PRODUCT_SUITE_ID = PRODUCT_SUITES.ID and product.PRODUCT_GROUP_ID=product_groups.PRODGROUP_ID);
		dbms_output.put_line('BugUser:'||pgmr );

	exception
        	WHEN NO_DATA_FOUND then
                	dbms_output.put_line('BugUser:NonFusion');
        	when others then
                	dbms_output.put_line('BugUser:Unknown');
	end;
	/
	EOF
	)

        if [[ $debug -eq 1 ]];then
         set -x
        fi

	programmer=`echo $ret|grep "BugUser:"|cut -d: -f2`
	if [[ $programmer = $curUser ]];then
		echo "Bug $bugNo belongs to Fusion family and assigned to $curUser."|tee -a $outfile
	elif [[ $programmer = 'NonFusion' ]];then
		CleanAndExit $ABORT "Bug $bugNo does not belong to Fusion family or the tool cannot access the bug (e.g. Security Vulnerability bug). Can't proceed. Please remove it from the BUG_NUM property to proceed."
	elif [[ -n $programmer ]];then
		echo "*Warning - Bug $bugNo is not assigned to $curUser."|tee -a $outfile
	else
		CleanAndExit $ABORT "Cannot verify if bug $bugNo belongs to Fusion family. Can't proceed."
	fi

}

#Must set 'bugTxt' and $bugNo variables before calling this proc.
updateBugDb()
{
	if [[ $bugUser -eq 0 ]];then
		return
	fi
        echo "Updating bug $bugNo."
	set +x

        sqlplus -s  /nolog <<-EOF
	connect $bugUid/$myConVer@"$connectStr" 
        SET SERVEROUTPUT ON
        SET SCAN OFF
	SET FEEDBACK OFF
        DECLARE
        bugNumber rpthead.RPTNO%TYPE:=$bugNo;
	bugType rpthead.bug_type%TYPE;
        bugText VARCHAR2($MAXLEN):='$bugTxt';
        BugErrorCode Number;
        bugErrorMessage VARCHAR2(10000);
	lineType VARCHAR2(1):='N';
        BEGIN
        select bug_type into bugType from rpthead where rptno=bugNumber;

        if bugType = 'E' then
                dbms_output.put_line('This is an ER bug');
                lineType := 'D';
        end if;

      	bug.bug_api.create_bug_text
        (p_rptno                  => bugNumber
        ,p_text                   => bugText
        ,p_line_type              => lineType
        ,p_error_code              => BugErrorCode
        ,p_error_mesg              => bugErrorMessage
        ,p_hide                    =>'Y'
        );

        dbms_output.put_line('Return code for CREATE_BUG_TEXT: ' || BugErrorCode); 
        dbms_output.put_line('Return message for CREATE_BUG_TEXT: ' || bugErrorMessage);
        commit;
        END;
        /
	EOF
	if [[ $debug -eq 1 ]];then
       	 set -x
	fi


}

#uploadToBugFTP <Source Dir>
#Assumes that bugNo is already set and it is a valid bug number.
#Assumes that filesForBugFtp is already set to the files to be uploaded.
uploadToBugFTP()
{

	lcd=$1
	if [[ -z $filesForBugFtp || $bugUser -eq 0 ]];then
        	echo "Nothing to upload."
        	return 1
	fi

	cd $lcd
	zipAll="FAPremerge-$curRun.All.tar.gz"
	zipOut="FAPremerge-$curRun.Out.tar.gz"
	ftpLog="FAPremerge-$curRun.ftp.log"

	if [[ $addJUnitsFolder -eq 1 ]];then
		tar czf $zipAll $txn* $NDE_PRODUCT_ROOT/patchReport.xml -c _files/ >/dev/null 2>&1
	else
		tar czf $zipAll $txn* $NDE_PRODUCT_ROOT/patchReport.xml >/dev/null 2>&1
	fi

	if [[ -f $zipAll ]];then
		chmod -w $zipAll
	fi

	tar czf $zipOut $filesForBugFtp >/dev/null 2>&1
	if [[ -f $zipOut ]];then
		chmod -w $zipOut
	fi

        set +x

	echo "Uploading files to BugSFTP..." > $ftpLog
        $FTPCmd  $BugFTPServer <<-EOF >>$ftpLog 2>&1
                user $FTPUser $bugVer
                cd /$bugNo
                lcd $lcd
                mput $zipAll $zipOut
		bye
		EOF
	
	echo "Uploading ftp.log" >> $ftpLog
        $FTPCmd $BugFTPServer <<-EOF >>$ftpLog 2>&1
                        user $FTPUser $bugVer
                        lcd $lcd
			cd /$bugNo
                        mput $ftpLog
                        bye
	EOF

        if [[ $debug -eq 1 ]];then
         set -x
        fi

	return 0
}

startJUnit()
{
        if [[ $runJUnits -ne 1 ]];then
		echo "Skipping JUnits. Please pass -DrunJUnits=1 if you would like run JUnits." |tee -a $outfile
                return
        fi
 
	if [[ $customJUnits -eq 1 ]];then
		echo "*Warning - Overriding automatic detection of JUnit test projects and running the ones passed by the user."|tee -a $outfile
		echo "Please ensure they are the right ones for the transaction."|tee -a $outfile
		errCode=1
	else
		echo "Running JUnits for the relevant projects."
	fi
	$cmd_junit >"$outfile".jut 2>&1 &
	pidJut=$!
}

processJUnit()
{

        if [[ $runJUnits -ne 1 ]];then
		echo "Skipped - JUnits. Please pass -DrunJUnits=1 if you would like run JUnits." |tee -a $outfile
                return
        fi
 
	CallWait $pidJut
	retJut=$?
	retJut=`echo $KilledPids|grep ":$pidJut:" >/dev/null && echo -2||echo $retJut`
	if [[ $retJut -eq -2 ]];then
        	echo "***Terminated - JUnits execution."|tee -a $outfile
        	errCode=`expr $errCode + 4`
        	isErr=1
		filesForBugFtp="$filesForBugFtp"' '"$txn"_junit.out
		if [[ -n  "$TIME_LOG" ]]; then
			echo "finjunit.sh,999,0,0,0,0%">>$TIME_LOG
		fi
	elif [[ $retJut -eq 0 ]];then
		echo "Success - JUnits." |tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' '"$txn"_junit.out' '"$txn"_*_testreport.html' '"$txn"_*_codecoveragereport.html
	elif [[ $retJut -eq 1 ]];then
		echo "***Failure - JUnits." |tee -a $outfile
		errCode=`expr $errCode + 4`
		isErr=1
		filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_junit.out
		filesForBugFtp="$filesForBugFtp"' '"$txn"_*_testreport.html' '"$txn"_*_codecoveragereport.html
	elif [[ $retJut -eq 2 ]];then
		echo "*Warning - JUnits did not complete successfully. Please check manually."|tee -a $outfile
		filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_junit.out
		errCode=`expr $errCode + 4`
	elif [[ $retJut -eq 3 ]];then
		echo "Skipped - JUnits."|tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' '"$txn"_junit.out
	elif [[ $retJut -eq 4 ]];then
		echo "**Timeout - JUnits execution timed out(exceeded 60min) and was killed. Please check manually."|tee -a $outfile
		filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_junit.out
		errCode=`expr $errCode + 4`
	elif [[ $retJut -eq 5 ]];then
		echo "*Warning - JUnits completed successfully but Code Coverage is not meeting industry standard."|tee -a $outfile
		filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_junit.out
		filesForBugFtp="$filesForBugFtp"' '"$txn"_*_testreport.html' '"$txn"_*_codecoveragereport.html
		errCode=`expr $errCode + 4`
	elif [[ $retJut -eq 6 ]];then
		echo "***Failure - JUnits and Code Coverage."|tee -a $outfile
		errCode=`expr $errCode + 4`
		isErr=1
		filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_junit.out
		filesForBugFtp="$filesForBugFtp"' '"$txn"_*_testreport.html' '"$txn"_*_codecoveragereport.html
	else
		echo "*Warning - JUnits execution returned unexpected error - $retJut. Please check the output manually."|tee -a $outfile
		errCode=`expr $errCode + 4`
		filesForBugFtp="$filesForBugFtp"' '"$txn"_junit.out
	fi
	
}

#Main starts from here..

#bugUser=0

# This tool will not raise mergereq. This is still under discussion.
#validateOnly=1

#test -n $breakOnError && export BreakOnError=$breakOnError

#if [[ $debug -eq 1 ]];then
#	set -x
#fi

#identifyLabels
#showBanner

#uname -a | grep -v x86_64|grep "i[3-6]86" >/dev/null 2>&1  && is32Bit=1

#if [[ `expr $errCode + $validateOnly + $forceMergeRq` -gt 1 ]];then
#	showUsageAndExit
#fi

#errCode=0
#export curUser=`whoami`
#export txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
#export outdir=$ADE_VIEW_ROOT
#if [[ -d $outdir/fusionapps ]];then
#	export destdir=$outdir/fusionapps
#else
#	export destdir=$outdir
#fi
#mkdir "$destdir/premerge" >/dev/null 2>&1
#cd "$destdir/premerge" 2>/dev/null && destdir=`pwd`

#outdir=$destdir

#if [[ "X$txn" = "XNONE" || -z $txn ]];then
#	txn="NONE"
#	errCode=1
#else #Check if backport txn first. Else, check normal bug fix.

 #       bugNo=`ade describetrans -properties_only|grep BACKPORT_BUG_NUM |awk '{print $3}'`
  #      if [[ -z $bugNo ]];then
   #             bugNo=`ade describetrans -properties_only|grep "^ *BUG_NUM"|awk '{print $3}'`
    #    fi
     #   if [[ -z $bugNo ]];then
	#	badBug=1
#		errCode=2
#		runARUValid=0
#	else
#		bugList=$bugNo
#		export bugNo=`echo $bugList|cut -d, -f1`
#		prelimBug=$bugNo
#	fi
#fi


#export outfile=$outdir/$txn.txt 
#rm -f $destdir/$txn* 2>/dev/null

#Always run the tools from fusionapps/premerge directory.
#StartDir=`pwd`
#cd $destdir

#showBanner skip >$outfile

#echo "Fusion pre-merge validations of $txn start at "`date "+%d-%h-%y %H:%M"` >>$outfile
#echo "Command - $cmdLine" >>$outfile
#echo "ORACLE_HOME is $ORACLE_HOME" >>$outfile
#if [[ $is32Bit -eq 1 ]];then
#	VersionCmd=$cmd_dir/GetVer32
#	echo "Running 32 bit version." |tee -a $outfile
#fi

#getEmailID
#fileForAdeMR=$ADE_VIEW_ROOT/fa_premerge_"$txn".sts 	#File to be looked up by mergereq. Deleting if any exists from previous run.
#rm -f $fileForAdeMR 2>/dev/null				#Always represent only the last execution status.

#trap "CleanAndExit $ABORT 'User Break'" 2

#if [[ $errCode -eq 0 ]];then
#	if [[ $badBug -eq 0 ]];then
#		bugurl="https://bug.oraclecorp.com/pls/bug/webbug_print.show?c_rptno=$bugNo#Bottomofthepage$bugNo"
#		echo "$bugurl" >>$outfile
#	fi

#	echo $cmd_this |grep "/$ADE_VIEW_NAME/fatools/" >/dev/null 2>&1 
#	if [[ $? -ne 0 ]];then
#		errCode=2
#		echo "*** The tool location is not matching your current view. Please run it starting with $ADE_VIEW_ROOT."|tee -a $outfile
#	fi

#	curLabel=`ade pwv | grep "VIEW_LABEL" | awk '{print $3}'`
#	latestLabel=`ade showlabels|tail -1`
#	if [[ $curLabel != $latestLabel ]];then
#		echo "*** There is a new label available. Current view must be refreshed to the latest before proceeding."|tee -a $outfile
#		errCode=2
#	else
#		ade lsco |grep "No files checked-out in view" >/dev/null 2>/dev/null
#		if [[ $? -ne 0 ]];then
#       		 	echo "There are checked-out files. Please check-in all files and save the transaction."|tee -a $outfile
#			errCode=2
#		fi
#	fi
#elif [[ $errCode -eq 1 ]];then
#
#	echo "*************************************************************"
#	echo "Invalid or no transaction. Cannot proceed."
#	echo "*************************************************************"
#	rm $outfile
#	exit 99
#
#else #Bug update impossible, badBug
#	echo "BUG_NUM/BACKPORT_BUG_NUM property not set."|tee -a $outfile
#	updateBug="N"
#	bugUser=0 
#fi

# - Not forcing an exit here as there could be some irrelevant file checked-out, which will be unchecked-out later. Leaving it to the user.
#if [[ $errCode -ne 0 ]];then
#	echo "Some validations will be skipped and overall status of the validations will be marked as failed. Do you want to continue? "
#	Ans='N'
#	read -t$timeOut Ans
 #       Ans=`echo $Ans|cut -c1|sed 's/[a-z]*/\U&/g'`
#        if [[ $Ans = "Y" ]];then
#		print "Continuing with validations.";errCode=1;BreakOnError=0 # Setting errCode so that mail says "Failure"
#	else
#		CleanAndExit $ABORT
#	fi
#	isErr=1
#fi
#
#if [[ $runJUnits -eq 1 && $jutDb -eq 0 ]];then
#	getJutDBInfo
#	if [[ $jutDb -eq 0 ]];then
#		if [[ $errCode -eq 4 ]];then
#			exit $errCode
#		else
#			echo
#			echo "Looks like you are running this tool first time. Please provide the connection information using -d option."
#			echo "e.g. -d fusion_runtime/fusion_runtime@indl144xxx.idc.oracle.com:1522/in144xxx"
#			echo
#			exit 99
#		fi
#	fi
#fi
#
#export txnDescFile="$txn"_describe.out
#echo >$txnDescFile
#echo "Transaction Description" >>$txnDescFile
#echo "==================================================" >>$txnDescFile
#ade describetrans >>$txnDescFile 
#echo "==================================================" >>$txnDescFile
#echo >>$txnDescFile

#grep "ade WARNING: Initial Kerberos ticket required" $txnDescFile >/dev/null 2>&1
#if [[ $? -eq 0 ]];then
#	CleanAndExit $ABORT "Initial Kerberos ticket required to proceed. Please see http://ade.oraclecorp.com/twiki/bin/view/ADEFAQ/GetKerberosTicket"
#fi


#check whether the angrybirds DB is up
#sqlplus -L -S fintech/fintech@angrybirds.oracle.com:1521/codescan >/dev/null 2>&1 << EOF && angrybirdsDBStatus=ok
#exit
#EOF

#exitstatus=$?
#echo $exitstatus

#if [[ $exitstatus -eq 0 ]]&&[[ "x$angrybirdsDBStatus" = "xok" ]];then
#	echo "AngryBirds DB is ok."
#elif [[ $exitstatus -eq 127 ]];then
#	echo "*** Can't find sqlplus. Please make sure you are in a view, ORACLE_HOME is pointing to a valid location and PATH includes ORACLE_HOME/bin. Bug update is turned off."
#else
#	echo "AngryBirds DB is down. Unused auto submit and nonexistent partial trigger scan will not report correct result. Please start it up."|mutt -s "Angrybirds DB is Down"  -c #yao.yao@oracle.com fusion_premerge_support_ww_grp@oracle.com
#	echo "AngryBirds DB is down. Unused auto submit and nonexistent partial trigger scan will not report correct result. "|tee -a $outfile
	#exit 99
#fi

#export txnFiles="$txn"_files.lst
#start=$(( $( cat $txnDescFile |grep -n "ALL AFFECTED FILES:"|cut -d: -f1 ) + 1 ))
#end=$(( $( tail -n+$start $txnDescFile |grep "^[A-Z]" -n |head -1|cut -d: -f1 ) - 1 ))
#if [[ $end -le 0 ]];then
#	 end=$( cat $txnDescFile |wc -l )
#fi
#tail -n+$start $txnDescFile|head -$end |grep fusionapps/ |grep -v '/noship/'| awk '{print $2}' | sort | uniq > $txnFiles

#if [[ -z $family ]];then
#	export family=`cat $txnFiles| cut -d/ -f2 | sort | uniq`
#	if [[ $( echo $family |wc -w ) -ne 1 ]];then
#		CleanAndExit $ABORT "Cannot ascertain to which Fusion family this transaction belongs based on the files in it. Can't proceed. Please run the tool specifying it using -#Dfamily=<family>"
#	else
#		echo "Product family is identified as $family." |tee -a $outfile
#	fi
#else
#	ls $ADE_VIEW_ROOT/fusionapps/$family/build.xml >/dev/null 2>&1 ||family=`echo $family | tr "A-Z" "a-z"`
#	echo "Product family is explicitly set to $family." |tee -a $outfile
#	export family
#fi

#Profile this run? Giving the user a choice. 0-Do not run, 1-run
#if [[ -z $runProfile ]];then
#	series=$(echo $label | awk -F_ '{print substr($0, 1, index($0, $NF)-2)}')
#
#	runProfile=$(egrep "^[[:space:]]*$series[[:space:]]*:[[:space:]]*($family|all)[[:space:]]*$" $profileFile)
#fi
#
#if [[ "$runProfile"  != "0" ]];then 
#	TIME_LOG=$(mktemp)
#	time_cmd="time -o $TIME_LOG -a -f %C,%x,%e,%S,%U,%P -- "
#else
#	time_cmd=""
#fi



#ls $ADE_VIEW_ROOT/fusionapps/$family/build.xml >/dev/null 2>&1 || echo "*Warning - Product family set to suspicious value - $family" |tee -a $outfile

#The following code checks for private files.

#vp_start_time=$(date +%s)
#startup_elapsed=$(($vp_start_time -$start_time));
#echo "Checking for view-private files..." | tee -a $outfile

#cd $ADE_VIEW_ROOT        #This is to ensure that the find command returns file names starting with fusionapps/
#for i in `cat $ADE_VIEW_ROOT/fusionapps/premerge/$txnFiles|cut -d/ -f1-4|sort|uniq`;do
# echo "Checking $i" | tee -a $outfile
# files=$files" "`find $i -type f -print|grep -v "/classes/"|grep -v "\.class$"|grep -v "~$"|grep -v "#[0-9]*$"|grep -v "\.jar$"|grep -v "/jlib/"|grep -v "\.properties$"|grep -v "\.war$"|#grep -v "\.lck$"|grep -v "GRABTRANS"|grep -v "\/\.\#"`
#done

#if all files are from transaction, then there's no view private files and vpFiles should be empty
#vpFiles=""
#for i in `echo $files`;do
#        grep $i $ADE_VIEW_ROOT/fusionapps/premerge/$txnFiles >/dev/null || vpFiles=$vpFiles" "$i
#done
#
#cd - >/dev/null         #Move back to the old directory
#
#echo $vpFiles|grep "fusionapps/" >/dev/null
#if [[ $? -eq 0 ]];then
#    input_start_time=$(date +%s);
#    echo ""
#    echo "*Warning - The following files are not part of the transaction, but exist as view-private. They will not be merged to ADE:" | tee -a $outfile
#    print $vpFiles|sed 's/ /\n/g' | tee -a $outfile
#    echo ""
#    echo "Press Control-C to exit (Oh! I forgot to add them to the transaction.), or Enter to proceed (I don't need them.)."
#    read -t$timeOut 
#    input_end_time=$(date +%s);
#    input_elapsed_time="$(expr $input_end_time - $input_start_time)";
#else
#
#    echo "There are no view-private files under the checked LBAs." |tee -a $outfile
#
#fi
#vp_end_time=$(date +%s);
#vp_elapsed_time=$(($vp_end_time - $vp_start_time));
#
#getVerInfo
#if [[ $updateBug != "N" ]];then
#	bug_start_time=$(date +%s);
#	setBugContext
#	for bugNo in $( echo $bugList |tr ',' ' ' );do
#		checkIfFusionBug
#	done
#	bugNo=$prelimBug
#
#	bug_end_time=$(date +%s);
#	bug_elapsed_time=$(($bug_end_time - $bug_start_time))
#fi # updateBug != "N"
#
#stop=1
#if [[ $badBug -eq 0 ]];then #Don't use updateBug, let this validation happen.
#	txn_start_time=$(date +%s);
#	parentDir="bug_"`echo $bugNo|cut -c 1-$(( ${#bugNo} - 6 ))`
#	bugDir="bug"$bugNo
#	curRun=`date +%Y%m%d-%H%M%S`
#	export bugftpdir="$parentDir/$bugDir/$curRun"
#
#	echo "Checking if the transaction is saved..." 
#
#	savedTxns=$(ade sqlplus <<-EOF |grep "$bugNo"|sed "s/^.* //"
#	set sqlprompt ' '
#	set sqlnumber off
#	set serveroutput on
#	set feedback off
#	
#	variable bugNo varchar2(25);
#	
#	exec :bugNo :='$bugNo';
#	declare
#	    transactions jr_common.rm.string_coll;
#            status pls_integer;
#	begin
#            status := jr_common.rm_common_task.get_tasks_for_bug(:bugNo,transactions);
#            FOR i IN transactions.FIRST..transactions.LAST LOOP
#                dbms_output.put_line(transactions(i));
#            END LOOP;
#	end;
#	/
#	EOF)
#
#	txnCount=`echo $savedTxns |wc -w`
#	if [[ $txnCount -eq 1 ]];then
#		if [[ $txn = $savedTxns ]];then
#			echo "$txn is saved and there are no other transactions saved for $bugNo. Safe to proceed." |tee -a $outfile
#			stop=0
#		else
#			echo "*Warning - $savedTxns already saved for this bug and this transaction ($txn) is not saved (or BUG_NUM property is not set correctly)." |tee -a $outfile
#		fi
#	elif [[ $txnCount -gt 1 ]];then
#        	echo "*Warning - Multiple transactions saved for this bug ($savedTxns). It is not safe to proceed if you are on an ARU transaction patching enabled branch." |tee -a #######$outfile
#	else
#		echo "*Warning - This transaction is not saved. Please exit, save the transaction and run the tool again." |tee -a $outfile
#	fi
#	txn_end_time=$(date +%s);
#	txn_elapsed_time=$(($txn_end_time - $txn_start_time))
#else
#	echo "*BUG_NUM/BACKPORT_BUG_NUM property is not set correctly. Will not check if the transaction is saved and the bug will not be updated." |tee -a $outfile
#fi # badBug = 0
#
#if [[ $stop -eq 1 ]];then
#        input_start_time="$(date +%s)";
#	echo "Press Control-C to exit, Enter to proceed."
#	read -t$timeOut 
#	input_end_time="$(date +%s)";
#	test -z $input_elapsed_time && input_elapsed_time=0
#	input_elapsed_time=$(( $input_end_time - $input_start_time + $input_elapsed_time));
#fi
#
#if [[ ! -e $FAToolsMainLatest/fatools/opensource/lib/JarJAuditFixer.jar ]];then
#	jAuditJar=`ls -rt $labelServerTop/FATOOLS_MAIN_LINUX.rdd/*/fatools/opensource/lib/JarJAuditFixer.jar | tail -1|grep JarJAuditFixer.jar`
#	if [[ -n $jAuditJar ]];then
#		echo "Using JAudit Jar - $jAuditJar" |tee -a $outfile
#	else
#		if [[ $ADE_SITE = "ade_fusion_idc" ]];then
#			echo "JAudit jar not found. Using from /net/indl144121.idc.oracle.com/scratch/bomathew/JAuditJar/JarJAuditFixer.jar"  |tee -a $outfile
#			jAuditJar=/net/indl144121.idc.oracle.com/scratch/bomathew/JAuditJar/JarJAuditFixer.jar
#		else
#			echo "JAudit jar not found. Using from /net/adc00las.us.oracle.com/scratch/bomathew/JAuditJar/JarJAuditFixer.jar"  |tee -a $outfile
#			jAuditJar=/net/adc00las.us.oracle.com/scratch/bomathew/JAuditJar/JarJAuditFixer.jar
#			runARUValid=0
#		fi
#	fi
#	if [[ -f $jAuditJar ]];then
#		cp $jAuditJar $outdir
#	else
#		echo "Unable to copy JAudit jar. Please find and copy JarJAuditFixer.jar to $outdir before proceeding." |tee -a $outfile
#        	echo "Press Control-C to exit, Enter to proceed."
#        	read -t$timeOut 
#	fi
#fi


SetCommands
# ARU transaction validation added for ER 21512175
if [[ $runARUValid -eq 1 ]];then
     if [[ $family = "fin" || $family = "prj" ]];then
		ARUValidation=$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/premerge/ARUValidation.java

		if [[ -f $ARUValidation ]];then
			echo "Checking ARU transaction for "$family"..."
		
			backportBugNo=`ade describetrans -properties_only|grep BACKPORT_BUG_NUM |awk '{print $3}'`
			baseBugNo=`ade describetrans -properties_only|grep "^ *BUG_NUM"|awk '{print $3}'`

			java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar:$ADE_VIEW_ROOT/fatools/opensource/utilities/MessageUtilities/MessageUtilities/libraries/ojdbc6.jar oracle.apps.premerge.ARUValidation $txn $series $family $outdir $baseBugNo $backportBugNo
			
			cat $outdir"/ARUValidresult.txt" |tee -a $outfile
			arustatusCode=$(tail -1 $outfile".ARUValid.log")

			if [[ $arustatusCode -eq 1 ]];then
				exit 99
			fi
		fi
     fi
fi

echo;echo;echo "Ensuring that BUILD_HOME has the latest patches.." |tee -a $outfile
jdev repatchBuildHome |tee -a $outfile
echo|tee -a $outfile

#ER 19943867 2nd Level Mergereq Approval - Delete Marker file.
if [[ -z $skipMRInfo ]];then #Do mergereq info processing by defualt. Pass -DskipMRInfo=1 to skip MR info processing.
	$cmd_delMRInfo
fi
	
process_start=$(date +%s);
trap "echo Starting background processes. Cannot stop now." 2

echo "Running Incremental Build."
$cmd_incrbld >"$outfile".bld 2>&1 &
pidBld=$!

if [[ $runJUnitsAfterBuild -ne 1 ]];then
	startJUnit
else
	jut="JUnits, "
fi

if [[ $PerformSeedValidation -eq 1 ]];then
  echo "Invoking fpr for Seed Data Validation."
  $cmd_seedvalidation >"$outfile".seed 2>&1 &
  pidSeed=$!
fi  

echo "Running JAudits."
$cmd_jaudit >"$outfile".adt 2>&1 &
pidAdt=$!
echo "Running GSCC Checker."
$cmd_gscc >"$outfile".gsc 2>&1 &
pidGsc=$!
echo "Running Model String Checker."
$cmd_strchk >"$outfile".str 2>&1 &
pidStr=$!
echo "Running Central Files Inclusion Checker."
$cmd_cntrlfiles >"$outfile".ctl 2>&1 &
pidCntrl=$!
echo "Running BIP Files Header Checker."
$cmd_bihdrchk >"$outfile".hdr 2>&1 &
pidHdr=$!
echo "Running BIP Captions Checker."
$cmd_bicptnchk >"$outfile".cptn 2>&1 &
pidCptn=$!

echo "Running Excessive Passivation Scan."
$cmd_passivation >"$outfile".pasv 2>&1 &
pidPasv=$!

echo "Running Deprecated Triggers Scan."
$cmd_depTrig >"$outfile".depTrig 2>&1 &
pidDepTrig=$!

echo "Running Bad PL/SQL Headers Scan."
$cmd_sqlheaders >"$outfile".sqlhdr 2>&1 &
pidSqlhdr=$!

echo "Running Row Count Threshold Scan."
$cmd_rct >"$outfile".rct 2>&1 &
pidRct=$!

echo "Running SelectCount Scan."
$cmd_selectCount >"$outfile".selectCount 2>&1 &
pidSelectCnt=$!

echo "Running SDO Interface Scan."
$cmd_sdoInterface >"$outfile".sdoInterface 2>&1 &
pidSdoInterface=$!

echo "Running Unused TaskFlow Scan."
$cmd_unusedTF >"$outfile".unusedTF 2>&1 &
pidUnusedTF=$!

echo "Running ViewCriteria Scan."
$cmd_vcScan >"$outfile".vcScan 2>&1 &
pidVCScan=$!

echo "Running ViewCriteria Performance Scans."
$cmd_vcPerfScan >"$outfile".vcPerfScan 2>&1 &
pidVCPerfScan=$!

echo "Running RowSetIterator Scan."
$cmd_rsiScan >"$outfile".rsiScan 2>&1 &
pidRSIScan=$!

echo "Running BIExtenderAutomation Scan."
$cmd_biScan >"$outfile".biScan 2>&1 &
pidBIScan=$!

echo "Running UISearch Scan."
$cmd_uiSearchScan >"$outfile".uisearchScan 2>&1 &
pidUISearchScan=$!

echo "Running Sysdate Scan."
$cmd_sysdateScan >"$outfile".sysdateScan 2>&1 &
pidSysdateScan=$!

echo "Running Enum Scan."
$cmd_enumScan >"$outfile".enumScan 2>&1 &
pidEnumScan=$!

echo "Running AutoExecuteVC Scan."
$cmd_autoExecuteVCScan >"$outfile".autoExecuteVC 2>&1 &
pidAutoExecuteVCScan=$!

echo "Running BIP Files Folder Checker."
$cmd_bifolderchk >"$outfile".fdr 2>&1 &
pidFdr=$!

echo "Running MethodAction with viewCriteria."
$cmd_vcMethodAction >"$outfile".vcMethodAction 2>&1 &
pidVcMethodAction=$!

echo "Running ADFdi Scans."
$cmd_adfdi >"$outfile".adfdi 2>&1 &
pidAdfdi=$!

echo "Running ADFdi RowLevelBinding Scans."
$cmd_rowlevelbinding >"$outfile".adfrowlevelbinding 2>&1 &
pidAdfrowlevelbinding=$!

echo "Running PLSQL Autonomous Scan."
$cmd_plsqlauto >"$outfile".plsqlAuto 2>&1 &
pidPlsqlAuto=$!

echo "Running ContentDelivery Scan."
$cmd_contentDelivery >"$outfile".contentDelivery 2>&1 &
pidContentDelivery=$!  

echo "Running VONotPassivated Scan."
$cmd_voNotPassivateCheck >"$outfile".voNotPassivated 2>&1 &
pidVONotPassivated=$! 

echo "Running Alta Icon Issues Scan."
$cmd_altaIssuesCheck >"$outfile".altaIssues 2>&1 &
pidAltaIssues=$! 

echo "Running EOHistory Columns Scan."
$cmd_eoHistoryCols >"$outfile".eoHistory 2>&1 &
pidEoHistory=$! 

echo "Running ChangeIndicatorNotSet Scan."
$cmd_changeIndicator >"$outfile".changeIndicator 2>&1 &
pidChangeIndicator=$! 

echo "Running FndConvertNumber Scan."
$cmd_fndConvertNumber >"$outfile".fndConvertNumber 2>&1 &
pidFndConvertNumber=$! 

echo "Running TaskMenuChk Scan."
$cmd_taskMenuChk >"$outfile".taskMenuChk 2>&1 &
pidTaskMenuChk=$! 

echo "Running essStrChk Scan."
$cmd_essStrChk >"$outfile".essStrChk 2>&1 &
pidEssStrChk=$!

echo "Running ADFdi Version Check."
$cmd_adfdiVersionCheck >"$outfile".adfdiVersionCheck 2>&1 &
pidAdfdiVersionCheck=$!

echo "Running BindVarKindMismatch Scan."
$cmd_bindVarCheck >"$outfile".bindVarKindMismatch 2>&1 &
pidBindVarCheck=$!

echo "Running SeedEndDates Scan."
$cmd_seedEndDates >"$outfile".seedEndDates 2>&1 &
pidSeedEndDates=$!

echo "Running SessionTimeout Scan."
$cmd_sessiontimeout >"$outfile".sessionTimeout 2>&1 &
pidSessionTimeout=$!

echo "Running AvoidLoadingTranslatorNotes Scan."
$cmd_avoidTranslator >"$outfile".translatorNotes 2>&1 &
pidTranslatorNotes=$!

echo "Running UnusedAutoSubmit & NonExistentPartialTrigger Scan."
$cmd_unusedAutoSubmit >"$outfile".unusedAutoSubmit 2>&1 &
pidUnusedAutoSubmit=$! 

if [[ $PerformFreValidation -eq 1 ]];then
  echo "Invoking FRE Validations."
  $cmd_frevalidation >"$outfile".fre 2>&1 &
  pidFre=$!
fi  

if [[ $PerformPseudoTranslation -eq 1 ]];then
  echo "Invoking Pseudo Translation."
  $cmd_psuedotranslation >"$outfile".pseudo 2>&1 &
  pidPseudo=$!
fi  

echo "Invoking Hard Coded Strings Check."
  $cmd_hrdCodeStrCheck >"$outfile".hrdCodeStr 2>&1 &
  pidHardCodeStr=$!

if [[ $patch -eq 1 ]];then
  echo "Creating patch."
  $cmd_arupatch >"$outfile".ptch 2>&1 &
  pidPtch=$!
fi  

echo "Running UTPLSQL Scan."
$cmd_utplsql >"$outfile".utplsql 2>&1 &
pidUtplsql=$!

if [[ $runOatsIAT -eq 1 ]];then
  echo "Running OATS Impact Assessment."
  $cmd_oatsIAT >"$outfile".oats 2>&1 &
  pidOatsIAT=$!
fi

PidList="$pidBld $pidJut $pidAdt $pidGsc $pidStr $pidCntrl $pidHdr $pidCptn $pidPasv $pidDepTrig $pidSqlhdr $pidRct $pidFdr $pidPtch $pidSelectCnt $pidSdoInterface $pidUnusedTF $pidVCScan $pidVCPerfScan $pidUtplsql $pidOatsIAT $pidRSIScan $pidBIScan $pidUISearchScan $pidSysdateScan $pidEnumScan $pidAutoExecuteVCScan $pidVcMethodAction $pidAdfdi $pidAdfrowlevelbinding $pidSeed $pidPlsqlAuto $pidContentDelivery $pidVONotPassivated $pidAltaIssues $pidEoHistory $pidChangeIndicator $pidFndConvertNumber $pidTaskMenuChk $pidEssStrChk $pidAdfdiVersionCheck $pidBindVarCheck $pidSeedEndDates $pidSessionTimeout $pidTranslatorNotes $pidFre $pidPseudo $pidHardCodeStr $pidUnusedAutoSubmit" 

if [[ $BreakOnError -le 0 ]];then
	echo
	echo "BreakOnError is unset."|tee -a $outfile
fi

echo ""
$cmd_showprogress "Please wait while the validations are progressing in the background " "$PidList" &
pidPrgs=$!

KilledPids=":"

trap "ProcessBreak" 2

# Wait for the background processes to end

filesForBugFtp=""
filesForBugBody="$destdir"/$txnDescFile

# 0 - Success -> errCode 0
# 1 - Failure -> errCode (errCode+$ERR)
# 2 - Warning, manual check needed ->  errCode 1 to $ERR
# 3 - Validation skipped as the files in the transaction are irrelevant -> No change to errCode
# errCode -1 reserved for Abort
# -2 - Validatoin was killed. Mark the validation as failed, but upload the output to bugftp

CallWait $pidCntrl
retCntrl=$?
retCntrl=`echo $KilledPids|grep ":$pidCntrl:" >/dev/null && echo -2||echo $retCntrl`
echo
if [[ $retCntrl -eq -2 ]];then
        echo "***Terminated - Central File Check."|tee -a $outfile
        errCode=`expr $errCode + 64`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_centralfilechecker.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "fincentralfilechecker.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retCntrl -eq 0 ]];then
        echo "Success - No central file found in the transaction." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_centralfilechecker.out
elif [[ $retCntrl -eq 2 ]];then
        echo "*Warning - Central files found in the transaction. Please make sure you are authorized to modify them." |tee -a $outfile
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_centralfilechecker.out
	errCode=`expr $errCode + 64`
elif [[ $retCntrl -eq 1 ]];then
	echo "***Failure - Central files found in the transaction. Please do not change them in your transaction because they are managed centrally." |tee -a $outfile
	isErr=1
	errCode=`expr $errCode + 64`
else
	echo "*Warning - Central files check returned unexpected error - $retCntrl. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 64`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_centralfilechecker.out
fi

CallWait $pidTaskMenuChk
retTaskMenuChk=$?
retTaskMenuChk=`echo $KilledPids|grep ":$pidTaskMenuChk:" >/dev/null && echo -2||echo $retTaskMenuChk`
if [[ $retTaskMenuChk -eq -2 ]];then
        echo "***Terminated - Task Menu Check scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' 'taskmenu.log
	if [[ -n  "$TIME_LOG" ]]; then
		echo "taskMenuChkPM.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retTaskMenuChk -eq 0 ]];then
        echo "Success - Task Menu Check scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' 'taskmenu.log
elif [[ $retTaskMenuChk -eq 1 ]];then
        echo "***Failure - Task Menu Check scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' 'taskmenu.log
else
        echo "*Warning - Task Menu Check scan returned unexpected error - $retTaskMenuChk. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' 'taskmenu.log
fi

#cwm ess
CallWait $pidEssStrChk
retEssStrChk=$?
retEssStrChk=`echo $KilledPids|grep ":$pidEssStrChk:" >/dev/null && echo -2||echo $retEssStrChk`
if [[ $retEssStrChk -eq -2 ]];then
        echo "***Terminated - ESS String Check scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' 'essstr.log
        if [[ -n  "$TIME_LOG" ]]; then
                echo "essStrChkPM.sh,999,0,0,0,0%">>$TIME_LOG
        fi
elif [[ $retEssStrChk -eq 0 ]];then
        echo "Success - ESS String Check scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' 'essstr.log
elif [[ $retEssStrChk -eq 1 ]];then
        echo "***Failure - ESS String Check scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' 'essstr.log
else
        echo "*Warning - ESS String Check scan returned unexpected error - $retEssStrChk. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' 'essstr.log
fi
#cwm ess end

CallWait $pidStr
retStr=$?
retStr=`echo $KilledPids|grep ":$pidStr:" >/dev/null && echo -2||echo $retStr`
if [[ $retStr -eq -2 ]];then
        echo "***Terminated - Model String Check."|tee -a $outfile
        errCode=`expr $errCode + 32`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_stringchecker.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finmodelstringchecker.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retStr -eq 0 ]];then
        echo "Success - Model String Check." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_stringchecker.out
elif [[ $retStr -eq 1 ]];then
        echo "***Failure - Model String Check." |tee -a $outfile
	errCode=`expr $errCode + 32`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_stringchecker.out
elif [[ $retStr -eq 2 ]];then
        echo "*Warning - Model String Check did not complete successfully. Please check manually."|tee -a $outfile
	errCode=`expr $errCode + 32`
elif [[ $retStr -eq 3 ]];then
	echo "Skipped - Model String Check."|tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_stringchecker.out
else
	echo "*Warning - Model String Check returned unexpected error - $retStr. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 32`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_stringchecker.out
fi

CallWait $pidGsc
retGsc=$?
retGsc=`echo $KilledPids|grep ":$pidGsc:" >/dev/null && echo -2||echo $retGsc`
if [[ $retGsc -eq -2 ]];then
        echo "***Terminated - GSCC execution."|tee -a $outfile
        errCode=`expr $errCode + 16`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_gscc.out
        if [[ -n  "$TIME_LOG" ]]; then
		echo "fingscccli.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retGsc -eq 0 ]];then
	echo "Success - GSCC." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_gscc.out
elif [[ $retGsc -eq 1 ]];then
	echo "***Failure - GSCC." |tee -a $outfile
	errCode=`expr $errCode + 16`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_gscc.out
else
	echo "*Warning - GSCC execution returned unexpected error - $retGsc. Please check the output manually. You may run GSCC impact report from ARU (GSCC tab on http://aru.us.oracle.com/) and update the bug manually."|tee -a $outfile
	errCode=`expr $errCode + 16`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_gscc.out
fi

CallWait $pidHdr
retHdr=$?
retHdr=`echo $KilledPids|grep ":$pidHdr:" >/dev/null && echo -2||echo $retHdr`
if [[ $retHdr -eq -2 ]];then
        echo "***Terminated - BIP Files Header Check."|tee -a $outfile
        errCode=`expr $errCode + 128`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_xliff.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finHdrCheck.pl,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retHdr -eq 0 ]];then
        echo "Success - BIP Files Header Check." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_xliff.out
elif [[ $retHdr -eq 1 ]];then
	echo "***Failure - BIP Files Header Check." |tee -a $outfile
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_xliff.out
	errCode=`expr $errCode + 128`
        isErr=1
elif [[ $retHdr -eq 3 ]];then
	echo "Skipped - BIP Files Header Check." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_xliff.out
else
	echo "*Warning - BIP Files Header Check returned unexpected error - $retHdr. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 128`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_xliff.out
fi

CallWait $pidCptn
retCptn=$?
retCptn=`echo $KilledPids|grep ":$pidCptn:" >/dev/null && echo -2||echo $retCptn`
if [[ $retCptn -eq -2 ]];then
        echo "***Terminated - BIP files captions check."|tee -a $outfile
        errCode=`expr $errCode + 128`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_finCaptionCheck.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finCaptionsCheck.pl,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retCptn -eq 0 ]];then
        echo "Success - BIP files captions check." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_finCaptionCheck.out
elif [[ $retCptn -eq 1 ]];then
	echo "***Failure - BIP files captions check." |tee -a $outfile
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_finCaptionCheck.out
        isErr=1
	errCode=`expr $errCode + 128`
elif [[ $retCptn -eq 3 ]];then
	echo "Skipped - BIP files captions check." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_finCaptionCheck.out
else
	echo "*Warning - BIP files captions check returned unexpected error - $retCptn. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 128`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_finCaptionCheck.out
fi

CallWait $pidPasv
retPasv=$?
retPasv=`echo $KilledPids|grep ":$pidPasv:" >/dev/null && echo -2||echo $retPasv`
if [[ $retPasv -eq -2 ]];then
        echo "***Terminated - Passivation scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_passivation.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finpassivation.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retPasv -eq 0 ]];then
	echo "Success - Passivation scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_passivation.out
elif [[ $retPasv -eq 1 ]];then
	echo "***Failure - Passivation scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_passivation.out
else
	echo "*Warning - Passivation scan returned unexpected error - $retPasv. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_passivation.out
fi

CallWait $pidDepTrig
retDepTrig=$?
retDepTrig=`echo $KilledPids|grep ":$pidDepTrig:" >/dev/null && echo -2||echo $retDepTrig`
if [[ $retDepTrig -eq -2 ]];then
        echo "***Terminated - Deprecated Triggers scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_deprecatedtriggers.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "findeprecatedtriggers.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retDepTrig -eq 0 ]];then
	echo "Success - Deprecated Triggers scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_deprecatedtriggers.out
elif [[ $retDepTrig -eq 1 ]];then
	echo "***Failure - Deprecated Triggers scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_deprecatedtriggers.out
else
	echo "*Warning - Deprecated Triggers scan returned unexpected error - $retDepTrig. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_deprecatedtriggers.out
fi

CallWait $pidSqlhdr
retSqlhdr=$?
retSqlhdr=`echo $KilledPids|grep ":$pidSqlhdr:" >/dev/null && echo -2||echo $retSqlhdr`
if [[ $retSqlhdr -eq -2 ]];then
        echo "***Terminated - SQL Headers scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_sqlheaders.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finbadheaders.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retSqlhdr -eq 0 ]];then
        echo "Success - SQL Headers scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_sqlheaders.out
elif [[ $retSqlhdr -eq 1 ]];then
        echo "***Failure - SQL Header scan. Detected unusual header syntax/location."|tee -a $outfile
	errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_sqlheaders.out
else
	echo "*Warning - SQL Headers scan returned unexpected error - $retSqlhdr. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_sqlheaders.out
fi

CallWait $pidRct
retRct=$?
retRct=`echo $KilledPids|grep ":$pidRct:" >/dev/null && echo -2||echo $retRct`
if [[ $retRct -eq -2 ]];then
        echo "***Terminated - RowCount Threshold scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_rct.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finrct.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retRct -eq 0 ]];then
	echo "Success - RowCount Threshold scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_rct.out
elif [[ $retRct -eq 1 ]];then
	echo "***Failure - RowCount Threshold scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_rct.out
else
	echo "*Warning - RowCount Threshold scan returned unexpected error - $retRct. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_rct.out
fi

CallWait $pidSelectCnt
retSelectCnt=$?
retSelectCnt=`echo $KilledPids|grep ":$pidSelectCnt:" >/dev/null && echo -2||echo $retSelectCnt`
if [[ $retSelectCnt -eq -2 ]];then
        echo "***Terminated - SelectCount scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_selectCount.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finSelectCount.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retSelectCnt -eq 0 ]];then
	echo "Success - SelectCount scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_selectCount.out
elif [[ $retSelectCnt -eq 1 ]];then
	echo "***Failure - SelectCount scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_selectCount.out
else
	echo "*Warning - SelectCount scan returned unexpected error - $retSelectCnt. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_selectCount.out
fi

CallWait $pidSdoInterface
retSdoInterface=$?
retSdoInterface=`echo $KilledPids|grep ":$pidSdoInterface:" >/dev/null && echo -2||echo $retSdoInterface`
if [[ $retSdoInterface -eq -2 ]];then
        echo "***Terminated - SDO Interface scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_sdoInterface.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finSDOInterface.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retSdoInterface -eq 0 ]];then
	echo "Success - SDO Interface scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_sdoInterface.out
elif [[ $retSdoInterface -eq 1 ]];then
	echo "***Failure - SDO Interface scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_sdoInterface.out
else
	echo "*Warning - SDO Interface scan returned unexpected error - $retSdoInterface. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_sdoInterface.out
fi

CallWait $pidUnusedTF
retUnusedTF=$?
retUnusedTF=`echo $KilledPids|grep ":$pidUnusedTF:" >/dev/null && echo -2||echo $retUnusedTF`
if [[ $retUnusedTF -eq -2 ]];then
        echo "***Terminated - Unused TaskFlow scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_unusedTF.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finUnusedTF.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retUnusedTF -eq 0 ]];then
	echo "Success - Unused TaskFlow scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_unusedTF.out
elif [[ $retUnusedTF -eq 1 ]];then
	echo "***Failure - Unused TaskFlow scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_unusedTF.out
else
	echo "*Warning - Unused TaskFlow scan returned unexpected error - $retUnusedTF. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_unusedTF.out
fi

CallWait $pidVCScan
retVcScan=$?
retVcScan=`echo $KilledPids|grep ":$pidVCScan:" >/dev/null && echo -2||echo $retVcScan`
if [[ $retVcScan -eq -2 ]];then
        echo "***Terminated - ViewCriteria scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_vc.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finVCScan.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retVcScan -eq 0 ]];then
	echo "Success - ViewCriteria scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_vc.out
elif [[ $retVcScan -eq 1 ]];then
	echo "***Failure - ViewCriteria scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_vc.out
else
	echo "*Warning - ViewCriteria scan returned unexpected error - $retVcScan. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_vc.out
fi

CallWait $pidVCPerfScan
retVcPerfScan=$?
retVcPerfScan=`echo $KilledPids|grep ":$pidVCPerfScan:" >/dev/null && echo -2||echo $retVcPerfScan`
if [[ $retVcPerfScan -eq -2 ]];then
        echo "***Terminated - ViewCriteria Performance scans (BadViewCriteria/NoViewCriteria)."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_vcPerf.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finVCNewScans.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retVcPerfScan -eq 0 ]];then
	echo "Success - ViewCriteria  performance scans (BadViewCriteria/NoViewCriteria)." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_vcPerf.out
elif [[ $retVcPerfScan -eq 1 ]];then
	echo "***Failure - ViewCriteria performance scans (BadViewCriteria/NoViewCriteria)." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_vcPerf.out
else
	echo "*Warning - ViewCriteria performance scans (BadViewCriteria/NoViewCriteria) returned unexpected error - $retVcPerfScan. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_vcPerf.out
fi

CallWait $pidRSIScan
retRSIScan=$?
retRSIScan=`echo $KilledPids|grep ":$pidRSIScan:" >/dev/null && echo -2||echo $retRSIScan`
if [[ $retRSIScan -eq -2 ]];then
        echo "***Terminated - RowSetIterator scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_rsi.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finRSIScan.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retRSIScan -eq 0 ]];then
	echo "Success - RowSetIterator  scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_rsi.out
elif [[ $retRSIScan -eq 1 ]];then
	echo "***Failure - RowSetIterator  scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_rsi.out
else
	echo "*Warning - RowSetIterator scan returned unexpected error - $retRSIScan. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_rsi.out
fi

CallWait $pidBIScan
retBIScan=$?
retBIScan=`echo $KilledPids|grep ":$pidBIScan:" >/dev/null && echo -2||echo $retBIScan`
if [[ $retBIScan -eq -2 ]];then
        echo "***Terminated - BIExtenderAutomation scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_bi.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finBIScan.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retBIScan -eq 0 ]];then
	echo "Success - BIExtenderAutomation  scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_bi.out
elif [[ $retBIScan -eq 1 ]];then
	echo "***Failure - BIExtenderAutomation  scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_bi.out
else
	echo "*Warning - BIExtenderAutomation scan returned unexpected error - $retBIScan. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_bi.out
fi

CallWait $pidUISearchScan
retUISearchScan=$?
retUISearchScan=`echo $KilledPids|grep ":$pidUISearchScan:" >/dev/null && echo -2||echo $retUISearchScan`
if [[ $retUISearchScan -eq -2 ]];then
        echo "***Terminated - UI Search scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_uisearch.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finUISearchScan.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retUISearchScan -eq 0 ]];then
	echo "Success - UI Search scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_uisearch.out
elif [[ $retUISearchScan -eq 1 ]];then
	echo "***Failure - UI Search scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_uisearch.out
else
	echo "*Warning - UI Search scan returned unexpected error - $retUISearchScan. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_uisearch.out
fi

CallWait $pidSysdateScan
retSysdateScan=$?
retSysdateScan=`echo $KilledPids|grep ":$pidSysdateScan:" >/dev/null && echo -2||echo $retSysdateScan`
if [[ $retSysdateScan -eq -2 ]];then
        echo "***Terminated - Sysdate scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_sysdate.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finSysdateScan.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retSysdateScan -eq 0 ]];then
	echo "Success - Sysdate scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_sysdate.out
elif [[ $retSysdateScan -eq 1 ]];then
	echo "***Failure - Sysdate scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_sysdate.out
else
	echo "*Warning - Sysdate scan returned unexpected error - $retSysdateScan. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_sysdate.out
fi


CallWait $pidUtplsql
retUtplsql=$?
retUtplsql=`echo $KilledPids|grep ":$pidUtplsql:" >/dev/null && echo -2||echo $retUtplsql`
if [[ $retUtplsql -eq -2 ]];then
        echo "***Terminated - UTPLSQL execution."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_utplsql.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finrunutplsql.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retUtplsql  -eq 0 ]];then
	echo "Success - UTPLSQL execution." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_utplsql.out
elif [[ $retUtplsql -eq 1 ]];then
	echo "***Failure - UTPLSQL execution." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_utplsql.out
elif [[ $retUtplsql -eq 3 ]];then
	echo "Skipped - UTPLSQL execution."|tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_utplsql.out
else
	echo "*Warning - UTPLSQL execution returned unexpected error - $retUtplsql. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_utplsql.out
fi

if [[ $runOatsIAT -eq 1 ]];then
    CallWait $pidOatsIAT
    retOatsIAT=$?
    retOatsIAT=`echo $KilledPids|grep ":$pidOatsIAT:" >/dev/null && echo -2||echo $retOatsIAT`
    if [[ $retOatsIAT -eq -2 ]];then
        echo "***Terminated - Oats Impact Assessment."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_oatsImpactCheck.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finOatsVerify.sh,999,0,0,0,0%">>$TIME_LOG
	fi
    elif [[ $retOatsIAT -eq 0 ]];then
        echo "Success - Oats Impact Assessment."|tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_oatsImpactCheck.out
    elif [[ $retOatsIAT -eq 1 ]];then
        echo "***Failure - Oats Impact Assessment."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_oatsImpactCheck.out
    elif [[ $retOatsIAT -eq 3 ]];then
	echo "Skipped - Oats Impact Assessment."|tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_oatsImpactCheck.out
    else
	echo "*Warning - Oats Impact Assessment returned unexpected error - $retOatsIAT "|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_oatsImpactCheck.out
    fi
fi

CallWait $pidFdr
retFdr=$?
retFdr=`echo $KilledPids|grep ":$pidFdr:" >/dev/null && echo -2||echo $retFdr`
if [[ $retFdr -eq -2 ]];then
        echo "***Terminated - BIP files folder check."|tee -a $outfile
        errCode=`expr $errCode + 128`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$destdir"/"$txn"_faBipFolderChk.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "faBipFolderChk.pl,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retFdr -eq 0 ]];then
        echo "Success - BIP files folder check." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$destdir"/"$txn"_faBipFolderChk.out
elif [[ $retFdr -eq 1 ]];then
	echo "***Failure - BIP files folder check." |tee -a $outfile
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_faBipFolderChk.out
	errCode=`expr $errCode + 128`
        isErr=1
elif [[ $retFdr -eq 3 ]];then
	echo "Skipped - BIP files folder check." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_faBipFolderChk.out
else
	echo "*Warning - BIP files folder check returned unexpected error - $retFdr. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 128`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_faBipFolderChk.out
fi

CallWait $pidVcMethodAction
retVcMethodAction=$?
retVcMethodAction=`echo $KilledPids|grep ":$pidVcMethodAction:" >/dev/null && echo -2||echo $retVcMethodAction`
if [[ $retVcMethodAction -eq -2 ]];then
        echo "***Terminated - Method Action with VC."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_methodActionVc.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finMethodActionVC.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retVcMethodAction -eq 0 ]];then
        echo "Success - Method Action with VC." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_methodActionVc.out
elif [[ $retVcMethodAction -eq 1 ]];then
        echo "***Failure - Method Action with VC."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_methodActionVc.out
else
        echo "*Warning - Method Action with VC  scan returned unexpected error - $retVcMethodAction. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_methodActionVc.out
fi

CallWait $pidEnumScan
retEnumScan=$?
retEnumScan=`echo $KilledPids|grep ":$pidEnumScan:" >/dev/null && echo -2||echo $retEnumScan`
if [[ $retEnumScan -eq -2 ]];then
        echo "*** Terminated - Enum scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_enum.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finEnumScan.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retEnumScan -eq 0 ]];then
	echo "Success - Enum  scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_enum.out
elif [[ $retEnumScan -eq 1 ]];then
	echo "*** Failure - Enum  scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_enum.out
else
	echo "*Warning - Enum scan returned unexpected error - $retEnumScan. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_enum.out
fi

CallWait $pidAutoExecuteVCScan
retAutoExecuteVCScan=$?
retAutoExecuteVCScan=`echo $KilledPids|grep ":$pidAutoExecuteVCScan:" >/dev/null && echo -2||echo $retAutoExecuteVCScan`
if [[ $retAutoExecuteVCScan -eq -2 ]];then
        echo "*** Terminated - AutoExecuteVC scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_autoexecute.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finAutoExecuteVCScan.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retAutoExecuteVCScan -eq 0 ]];then
	echo "Success - AutoExecuteVC  scan." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_autoexecute.out
elif [[ $retAutoExecuteVCScan -eq 1 ]];then
	echo "*** Failure - AutoExecuteVC  scan." |tee -a $outfile
	errCode=`expr $errCode + 256`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_autoexecute.out
else
	echo "*Warning - AutoExecuteVC scan returned unexpected error - $retAutoExecuteVCScan. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 256`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_autoexecute.out
fi

CallWait $pidAdfdi
retAdfdi=$?
retAdfdi=`echo $KilledPids|grep ":$pidAdfdi:" >/dev/null && echo -2||echo $retAdfdi`
if [[ $retAdfdi -eq -2 ]];then
        echo "***Terminated - Adfdi scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_diScan.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finDiScans.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retAdfdi -eq 0 ]];then
        echo "Success - Adfdi Scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_diScan.out
elif [[ $retAdfdi -eq 1 ]];then
        echo "***Failure - Adfdi Scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_diScan.out
else
        echo "*Warning - Adfdi Scan returned unexpected error - $retAdfdi. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_diScan.out
fi

CallWait $pidAdfrowlevelbinding
retAdfrowbinding=$?
retAdfrowbinding=`echo $KilledPids|grep ":$pidAdfrowlevelbinding:" >/dev/null && echo -2||echo $retAdfrowbinding`
if [[ $retAdfrowbinding -eq -2 ]];then
        echo "***Terminated - Adfdi RowLevelBingding scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_adfdiRowLevelBindingCheck.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finADFdiRowLevelBindingCheck.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retAdfrowbinding -eq 0 ]];then
        echo "Success - Adfdi RowlevelBinding Scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_adfdiRowLevelBindingCheck.out
elif [[ $retAdfrowbinding -eq 1 ]];then
        echo "***Failure - Adfdi RowlevelBinding Scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_adfdiRowLevelBindingCheck.out
else
        echo "*Warning - Adfdi RowlevelBinding Scan returned unexpected error - $retAdfdi. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_adfdiRowLevelBindingCheck.out
fi

CallWait $pidPlsqlAuto
retPlsqlAuto=$?
retPlsqlAuto=`echo $KilledPids|grep ":$pidPlsqlAuto:" >/dev/null && echo -2||echo $retPlsqlAuto`
if [[ $retPlsqlAuto -eq -2 ]];then
        echo "***Terminated - PLSql Autonomous scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_plsqlAutonomous.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finPlsqlAutonomous.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retPlsqlAuto -eq 0 ]];then
        echo "Success - PLSql Autonomous scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_plsqlAutonomous.out
elif [[ $retPlsqlAuto -eq 1 ]];then
        echo "***Failure - PLSql Autonomous scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_plsqlAutonomous.out
else
        echo "*Warning - PLSql Autonomous scan returned unexpected error - $retPlsqlAuto. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_plsqlAutonomous.out
fi

CallWait $pidContentDelivery
retContentDelivery=$?
retContentDelivery=`echo $KilledPids|grep ":$pidContentDelivery:" >/dev/null && echo -2||echo $retContentDelivery`
if [[ $retContentDelivery -eq -2 ]];then
        echo "***Terminated - ContentDelivery scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_contentDelivery.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finContentDelivery.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retContentDelivery -eq 0 ]];then
        echo "Success - ContentDelivery scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_contentDelivery.out
elif [[ $retContentDelivery -eq 1 ]];then
        echo "***Failure - ContentDelivery scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_contentDelivery.out
else
        echo "*Warning - ContentDelivery scan returned unexpected error - $retContentDelivery. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_contentDelivery.out
fi

CallWait $pidVONotPassivated
retVONotPassivated=$?
retVONotPassivated=`echo $KilledPids|grep ":$pidVONotPassivated:" >/dev/null && echo -2||echo $retVONotPassivated`
if [[ $retVONotPassivated -eq -2 ]];then
        echo "***Terminated - VONotPassivated scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_voNotPasivated.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finVONotPassivated.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retVONotPassivated -eq 0 ]];then
        echo "Success - VONotPassivated scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_voNotPasivated.out
elif [[ $retVONotPassivated -eq 1 ]];then
        echo "***Failure - VONotPassivated scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_voNotPasivated.out
else
        echo "*Warning - VONotPassivated scan returned unexpected error - $retVONotPassivated. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_voNotPasivated.out
fi

CallWait $pidFndConvertNumber
retFndConvertNumber=$?
retFndConvertNumber=`echo $KilledPids|grep ":$pidFndConvertNumber:" >/dev/null && echo -2||echo $retFndConvertNumber`
if [[ $retFndConvertNumber -eq -2 ]];then
        echo "***Terminated - Fnd ConvertNumber scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_fndConvertNumber.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finFndConvertNumber.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retFndConvertNumber -eq 0 ]];then
        echo "Success - Fnd ConvertNumber scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_fndConvertNumber.out
elif [[ $retFndConvertNumber -eq 1 ]];then
        echo "***Failure - Fnd ConvertNumber scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_fndConvertNumber.out
else
        echo "*Warning - Fnd ConvertNumber scan returned unexpected error - $retFndConvertNumber. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_fndConvertNumber.out
fi




CallWait $pidAltaIssues
retAltaIssues=$?
retAltaIssues=`echo $KilledPids|grep ":$pidAltaIssues:" >/dev/null && echo -2||echo $retAltaIssues`
if [[ $retAltaIssues -eq -2 ]];then
        echo "***Terminated - Alta Icon Issues scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_altaIssues.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finAltaIssues.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retAltaIssues -eq 0 ]];then
        echo "Success - Alta Icon Issues scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_altaIssues.out
elif [[ $retAltaIssues -eq 1 ]];then
        echo "***Failure - Alta Icon Issues scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_altaIssues.out
else
        echo "*Warning - Alta Icon Issues scan returned unexpected error - $retAltaIssues. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_altaIssues.out
fi

CallWait $pidEoHistory
retEoHistory=$?
retEoHistory=`echo $KilledPids|grep ":$retEoHistory:" >/dev/null && echo -2||echo $retEoHistory`
if [[ $retEoHistory -eq -2 ]];then
        echo "***Terminated - EO History Columns scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_eoHistory.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finScanEOHistoryCols.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retEoHistory -eq 0 ]];then
        echo "Success - EO History Columns scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_eoHistory.out
elif [[ $retEoHistory -eq 1 ]];then
        echo "***Failure - EO History Columns scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_eoHistory.out
else
        echo "*Warning - EO History Columns scan returned unexpected error - $retEoHistory. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_eoHistory.out
fi

CallWait $pidChangeIndicator
retChangeIndicator=$?
retChangeIndicator=`echo $KilledPids|grep ":$retChangeIndicator:" >/dev/null && echo -2||echo $retChangeIndicator`
if [[ $retChangeIndicator -eq -2 ]];then
        echo "***Terminated - ChangeIndicatorNotSet scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_changeIndicator.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finChangeIndicatorNotSet.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retChangeIndicator -eq 0 ]];then
        echo "Success - ChangeIndicatorNotSet scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_changeIndicator.out
elif [[ $retChangeIndicator -eq 1 ]];then
        echo "***Failure - ChangeIndicatorNotSet scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_changeIndicator.out
else
        echo "*Warning - ChangeIndicatorNotSet scan returned unexpected error - $retChangeIndicator. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_changeIndicator.out
fi


CallWait $pidUnusedAutoSubmit
retUnusedAutoSubmit=$?
retUnusedAutoSubmit=`echo $KilledPids|grep ":$pidUnusedAutoSubmit:" >/dev/null && echo -2||echo $retUnusedAutoSubmit`
if [[ $retUnusedAutoSubmit -eq -2 ]];then
        echo "***Terminated - UnusedAutoSubmit & NonExistentPartialTrigger scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_unusedAutoSubmit.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finUnusedAutoSubmit.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retUnusedAutoSubmit -eq 0 ]];then
        echo "Success - UnusedAutoSubmit & NonExistentPartialTrigger scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_unusedAutoSubmit.out
elif [[ $retUnusedAutoSubmit -eq 1 ]];then
        echo "***Failure - UnusedAutoSubmit & NonExistentPartialTrigger scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_unusedAutoSubmit.out
else
        echo "*Warning - UnusedAutoSubmit & NonExistentPartialTrigger scan returned unexpected error - $retUnusedAutoSubmit. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_unusedAutoSubmit.out
fi

CallWait $pidAdfdiVersionCheck
retAdfdiVersionCheck=$?
retAdfdiVersionCheck=`echo $KilledPids|grep ":$pidAdfdiVersionCheck:" >/dev/null && echo -2||echo $retAdfdiVersionCheck`
if [[ $retAdfdiVersionCheck -eq -2 ]];then
        echo "***Terminated - ADFdiVersionCheck scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_adfdiVersionCheck.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finADFdiVersionCheck.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retAdfdiVersionCheck -eq 0 ]];then
        echo "Success - ADFdiVersionCheck scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_adfdiVersionCheck.out
elif [[ $retAdfdiVersionCheck -eq 1 ]];then
        echo "***Failure - ADFdiVersionCheck scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_adfdiVersionCheck.out
else
        echo "*Warning - ADFdiVersionCheck scan returned unexpected error - $retAdfdiVersionCheck. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_adfdiVersionCheck.out
fi

CallWait $pidBindVarCheck
retBindVarCheck=$?
retBindVarCheck=`echo $KilledPids|grep ":$pidBindVarCheck:" >/dev/null && echo -2||echo $retBindVarCheck`
if [[ $retBindVarCheck -eq -2 ]];then
        echo "***Terminated - BindVariable Kind Mismatch scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_bindVarKindMismatch.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finBindVarKinds.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retBindVarCheck -eq 0 ]];then
        echo "Success - BindVariable Kind Mismatch scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_bindVarKindMismatch.out
elif [[ $retBindVarCheck -eq 1 ]];then
        echo "***Failure - BindVariable Kind Mismatch scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_bindVarKindMismatch.out
else
        echo "*Warning - BindVariable Kind Mismatch scan returned unexpected error - $retBindVarCheck. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_bindVarKindMismatch.out
fi

CallWait $pidSeedEndDates
retSeedEndDates=$?
retSeedEndDates=`echo $KilledPids|grep ":$pidSeedEndDates:" >/dev/null && echo -2||echo $retSeedEndDates`
if [[ $retSeedEndDates -eq -2 ]];then
        echo "***Terminated - SeedEndDates scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_seedEndDate.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finSeedEndDates.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retSeedEndDates -eq 0 ]];then
        echo "Success - SeedEndDates scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_seedEndDate.out
elif [[ $retSeedEndDates -eq 1 ]];then
        echo "***Failure - SeedEndDates scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_seedEndDate.out
else
        echo "*Warning - SeedEndDates returned unexpected error - $retSeedEndDates. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_seedEndDate.out
fi

if [[ $PerformFreValidation -eq 1 ]];then
	CallWait $pidFre
	retFre=$?
	retFre=`echo $KilledPids|grep ":$pidFre:" >/dev/null && echo -2||echo $retFre`

	if [[ $retFre -eq -2 ]];then
		echo "***Terminated - FreValidations. Please see "$destdir"/bugOutputForFreValidation.txt"|tee -a $outfile
		errCode=`expr $errCode + 1024`
		#isErr=1
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForFreValidation.txt
                filesForBugFtp="$filesForBugFtp"' 'raw_freValidation.txt
		if [[ -n  "$TIME_LOG" ]]; then
			echo "runFreValidate.sh,999,0,0,0,0%">>$TIME_LOG
		fi
	elif [[ $retFre -eq 0 ]];then
		echo "Success - FRE Validations"  |tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForFreValidation.txt
                filesForBugFtp="$filesForBugFtp"' 'raw_freValidation.txt
        elif [[ $retFre -eq 1 ]];then
		echo "Success - FRE Validations (only label checks performed)"  |tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForFreValidation.txt
                filesForBugFtp="$filesForBugFtp"' 'raw_freValidation.txt
	elif [[ $retFre -eq 2 ]];then
		echo "***WARNING - FRE Validation failures found. Please see "$destdir"/bugOutputForFreValidation.txt" |tee -a $outfile
		errCode=`expr $errCode + 1024`
		#isErr=1
		filesForBugBody="$filesForBugBody"' 'bugOutputForFreValidation.txt
                filesForBugFtp="$filesForBugFtp"' 'raw_freValidation.txt
        elif [[ $retFre -eq 3 ]];then
		echo "***ERROR - FRE Validation failures found (only label checks performed). Please see "$destdir"/bugOutputForFreValidation.txt" |tee -a $outfile
		errCode=`expr $errCode + 1024`
		isErr=1
		filesForBugBody="$filesForBugBody"' 'bugOutputForFreValidation.txt
                filesForBugFtp="$filesForBugFtp"' 'raw_freValidation.txt
	elif [[ $retFre -eq 5 ]];then
	        echo "***WARNING. FRE Validations could not be performed by premerge. Please see "$destdir"/bugOutputForFreValidation.txt" |tee -a $outfile
		errCode=`expr $errCode + 1024`
		filesForBugBody="$filesForBugBody"' 'bugOutputForFreValidation.txt
                filesForBugFtp="$filesForBugFtp"' 'raw_freValidation.txt
	elif [[ $retFre -eq 4 ]];then
		echo "Skipped - FRE Validations: txn has no database script files to validate using freValidate." |tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForFreValidation.txt
                filesForBugFtp="$filesForBugFtp"' 'raw_freValidation.txt
	else
		echo "*WARNING - FRE Validation returned unexpected error - $retFre. Please check the output manually("$destdir"/bugOutputForFreValidation.txt)."|tee -a $outfile
		errCode=`expr $errCode + 1024`
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForFreValidation.txt
                filesForBugFtp="$filesForBugFtp"' 'raw_freValidation.txt
	fi
fi

if [[ $PerformPseudoTranslation -eq 1 ]];then
	CallWait $pidPseudo
	retPseudo=$?
	retPseudo=`echo $KilledPids|grep ":$retPseudo:" >/dev/null && echo -2||echo $retPseudo`

	if [[ $retPseudo -eq -2 ]];then
		echo "***Terminated - Pseudo Translation. Please see "$destdir"/bugOutputForPseudoTranslation.txt"|tee -a $outfile
		errCode=`expr $errCode + 1024`
		isErr=1
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForPseudoTranslation.txt
                filesForBugFtp="$filesForBugFtp"' 'pseudotranslation.txt
		if [[ -n  "$TIME_LOG" ]]; then
			echo "finTranslation.sh,999,0,0,0,0%">>$TIME_LOG
		fi
	elif [[ $retPseudo -eq 0 ]];then
		echo "Success - Pseudo Translation testing"  |tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForPseudoTranslation.txt
                filesForBugFtp="$filesForBugFtp"' 'pseudotranslation.txt
	elif [[ $retPseudo -eq 2 ]];then
		echo "Skipped - Pseudo Translation testing. Please see "$destdir"/bugOutputForPseudoTranslation.txt" |tee -a $outfile
		filesForBugFtp="$filesForBugBody"' 'bugOutputForPseudoTranslation.txt
                filesForBugFtp="$filesForBugFtp"' 'pseudotranslation.txt
        elif [[ $retPseudo -eq 1 ]];then
		echo "***Failure - Pseudo Translation testing. Please see "$destdir"/bugOutputForPseudoTranslation.txt" |tee -a $outfile
		errCode=`expr $errCode + 1024`
		isErr=1
		filesForBugBody="$filesForBugBody"' 'bugOutputForPseudoTranslation.txt
                filesForBugFtp="$filesForBugFtp"' 'pseudotranslation.txt
	else
		echo "*Warning - Pseudo Translation testing unexpected error - $retPseudo. Please check the output manually("$destdir"/bugOutputForPseudoTranslation.txt)."|tee -a $outfile
		errCode=`expr $errCode + 1024`
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForPseudoTranslation.txt
                filesForBugFtp="$filesForBugFtp"' 'pseudotranslation.txt
	fi
fi	
	
CallWait $pidHardCodeStr
retHardCodeStr=$?
retHardCodeStr=`echo $KilledPids|grep ":$pidHardCodeStr:" >/dev/null && echo -2||echo $retHardCodeStr`
if [[ $retHardCodeStr -eq -2 ]];then
        echo "***Terminated - Hard Coded Strings Check."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' 'hardcodeStrChk.trace
        filesForBugFtp="$filesForBugFtp"' 'rptHardStr.csv
	if [[ -n  "$TIME_LOG" ]]; then
		echo "hardcodeStrChkPM.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retHardCodeStr -eq 0 ]];then
        echo "Success - Hard Coded Strings Check." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' 'hardcodeStrChk.trace
        filesForBugFtp="$filesForBugFtp"' 'rptHardStr.csv
elif [[ $retHardCodeStr -eq 1 ]];then
        echo "***Failure - Hard Coded Strings Check. Please see "$destdir"/rptHardStr.csv for the list of violations."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' 'hardcodeStrChk.trace
        filesForBugBody="$filesForBugFtp"' 'rptHardStr.csv
else
        echo "*Warning - Hard Coded Strings Check returned unexpected error - $retHardCodeStr. Please check the output manually ("$destdir"/rptHardStr.csv and "$destdir"/hardcodeStrChk.trace)."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' 'hardcodeStrChk.trace
        filesForBugFtp="$filesForBugFtp"' 'rptHardStr.csv
fi

CallWait $pidSessionTimeout
retSessionTimeout=$?
retSessionTimeout=`echo $KilledPids|grep ":$pidSessionTimeout:" >/dev/null && echo -2||echo $retSessionTimeout`
if [[ $retSessionTimeout -eq -2 ]];then
        echo "***Terminated - SessionTimeout scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_sessionTimeout.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finSessionTimeout.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retSessionTimeout -eq 0 ]];then
        echo "Success - SessionTimeout scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_sessionTimeout.out
elif [[ $retSessionTimeout -eq 1 ]];then
        echo "***Failure - SessionTimeout scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_sessionTimeout.out
else
        echo "*Warning - SessionTimeout scan returned unexpected error - $retSessionTimeout. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_sessionTimeout.out
fi

CallWait $pidTranslatorNotes
retTranslatorNotes=$?
retTranslatorNotes=`echo $KilledPids|grep ":$pidTranslatorNotes:" >/dev/null && echo -2||echo $retTranslatorNotes`
if [[ $retTranslatorNotes -eq -2 ]];then
        echo "***Terminated - AvoidLoadingTranslatorNotes scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' '"$txn"_avoidTranslator.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finAvoidLoadingTranslator.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retTranslatorNotes -eq 0 ]];then
        echo "Success - AvoidLoadingTranslatorNotes scan." |tee -a $outfile
        filesForBugFtp="$filesForBugFtp"' '"$txn"_avoidTranslator.out
elif [[ $retTranslatorNotes -eq 1 ]];then
        echo "***Failure - AvoidLoadingTranslatorNotes scan."|tee -a $outfile
        errCode=`expr $errCode + 256`
        isErr=1
        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_avoidTranslator.out
else
        echo "*Warning - AvoidLoadingTranslatorNotes returned unexpected error - $retTranslatorNotes. Please check the output manually."|tee -a $outfile
        errCode=`expr $errCode + 256`
        filesForBugFtp="$filesForBugFtp"' '"$txn"_avoidTranslator.out
fi

CallWait $pidAdt
retAdt=$?
retAdt=`echo $KilledPids|grep ":$pidAdt:" >/dev/null && echo -2||echo $retAdt`
if [[ $retAdt -eq -2 ]];then
        echo "***Terminated - JAudit execution."|tee -a $outfile
        errCode=`expr $errCode + 8`
        isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_jaudit.out
        filesForBugFtp="$filesForBugFtp"' '"$txn"_audit.txt
        filesForBugFtp="$filesForBugFtp"' 'raw_audit.txt
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finjaudit.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retAdt -eq 0 ]];then
	echo "Success - JAudit." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_jaudit.out
        filesForBugFtp="$filesForBugFtp"' 'raw_audit.txt
        filesForBugFtp="$filesForBugFtp"' '"$txn"_audit.txt
elif [[ $retAdt -eq 1 ]];then
	echo "***Failure - JAudit." |tee -a $outfile
	errCode=`expr $errCode + 8`
	isErr=1
        filesForBugBody="$filesForBugBody"' '"$txn"_jaudit.out
        filesForBugFtp="$filesForBugFtp"' 'raw_audit.txt
        filesForBugFtp="$filesForBugFtp"' '"$txn"_audit.txt
elif [[ $retAdt -eq 2 ]];then
	echo "***Failure - JAudit check did not complete successfully. Please check manually. Please see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/JauditChecksinPremergetool for instructions on how to run Jaudit manually." |tee -a $outfile
        errCode=`expr $errCode + 8`
        filesForBugFtp="$filesForBugFtp"' 'raw_audit.txt
        isErr=1
elif [[ $retAdt -eq 3 ]];then
	echo "Skipped - JAudit check." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_jaudit.out
        filesForBugFtp="$filesForBugFtp"' 'raw_audit.txt
        filesForBugFtp="$filesForBugFtp"' '"$txn"_audit.txt
elif [[ $retAdt -eq 4 ]];then	
	echo "*Warning - JAudit completed successfully. Duplicate Component Id check failed." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_jaudit.out
        filesForBugFtp="$filesForBugFtp"' 'raw_audit.txt
        filesForBugFtp="$filesForBugFtp"' '"$txn"_audit.txt
elif [[ $retAdt -eq 5 ]];then
	echo "***Failure - JAudit and Duplicate Component Id check." |tee -a $outfile
	errCode=`expr $errCode + 8`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$txn"_jaudit.out
        filesForBugFtp="$filesForBugFtp"' 'raw_audit.txt
        filesForBugFtp="$filesForBugFtp"' '"$txn"_audit.txt
elif [[ $retAdt -eq 6 ]];then
	echo "***Failure - JAudit check did not complete successfully. Please check manually. Duplicate Component Id check failed. Please see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/JauditChecksinPremergetool for instructions on how to run Jaudit manually." |tee -a $outfile
        errCode=`expr $errCode + 8`
        isErr=1
        filesForBugFtp="$filesForBugFtp"' 'raw_audit.txt
        filesForBugFtp="$filesForBugFtp"' '"$txn"_audit.txt
        filesForBugFtp="$filesForBugFtp"' '"$txn"_jaudit.out
else
	echo "*Warning - JAudit check returned unexpected error - $retAdt. Please check the output manually. Please see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/JauditChecksinPremergetool for instructions on how to run Jaudit manually."|tee -a $outfile
	errCode=`expr $errCode + 8`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_jaudit.out
        filesForBugFtp="$filesForBugFtp"' 'raw_audit.txt
        filesForBugFtp="$filesForBugFtp"' '"$txn"_audit.txt
fi
echo "If you need to request exemptions for any audit violations in this transaction please use the pre-merge exemption helper tool to request those exemptions. Details are here:"|tee -a $outfile
echo "https://stbeehive.oracle.com/teamcollab/wiki/Fusion+Applications+Technical+Architecture:Exemption+Request+Helper"|tee -a $outfile


if [[ $runJUnitsAfterBuild -ne 1 ]];then
	processJUnit
fi

CallWait $pidBld
retBld=$?
retBld=`echo $KilledPids|grep ":$pidBld:" >/dev/null && echo -2||echo $retBld`

if [[ $retBld -eq -2 ]];then
	echo "***Terminated - Incremental Build."|tee -a $outfile
	errCode=`expr $errCode + 2`
	isErr=1
	filesForBugFtp="$filesForBugFtp"' '"$txn"_incrbld.out
	if [[ -n  "$TIME_LOG" ]]; then
		echo "finincrbuild.sh,999,0,0,0,0%">>$TIME_LOG
	fi
elif [[ $retBld -eq 0 ]];then
	echo "Success - Incremental Build." |tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_incrbld.out
elif [[ $retBld -eq 1 ]];then
	echo "***Failure - Incremental Build." |tee -a $outfile
	errCode=`expr $errCode + 2`
	isErr=1
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_incrbld.out
elif [[ $retBld -eq 2 ]];then
        echo "*Warning - Incremental Build - Patching module returned errors. Please review the log ("$destdir"/"$txn"_incrbld.log) carefully." |tee -a $outfile
	errCode=`expr $errCode + 2`
	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_incrbld.out
elif [[ $retBld -eq 3 ]];then
	echo "Incremental Build not necessary. Skipping."|tee -a $outfile
	filesForBugFtp="$filesForBugFtp"' '"$txn"_incrbld.out
else
	echo "*Warning - Incremental Build returned unexpected error - $retBld. Please check the output manually."|tee -a $outfile
	errCode=`expr $errCode + 2`
	filesForBugFtp="$filesForBugFtp"' '"$txn"_incrbld.out
fi

if [[ $patch -eq 1 ]];then
	CallWait $pidPtch
	retPtch=$?
	retPtch=`echo $KilledPids|grep ":$pidPtch:" >/dev/null && echo -2||echo $retPtch`

	if [[ $retPtch -eq -2 ]];then
		echo "***Terminated - Patch Creation."|tee -a $outfile
		errCode=`expr $errCode + 1024`
		isErr=1
		filesForBugFtp="$filesForBugFtp"' '"$txn"_arupatch.out
		if [[ -n  "$TIME_LOG" ]]; then
			echo "finarupatch.sh,999,0,0,0,0%">>$TIME_LOG
		fi
	elif [[ $retPtch -eq 0 ]];then
		echo "Success - Patch Creation." |tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' '"$txn"_arupatch.out
	elif [[ $retPtch -eq 1 ]];then
		echo "***Failure - Patch Creation." |tee -a $outfile
		errCode=`expr $errCode + 1024`
		isErr=1
		filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_arupatch.out
	elif [[ $retPtch -eq 2 ]];then
	        echo "*Warning - Patch Creation reported warnings. Please see the Patch Output for more information." |tee -a $outfile
		errCode=`expr $errCode + 1024`
		filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_arupatch.out
	elif [[ $retPtch -eq 3 ]];then
		echo "Patch Creation not necessary. Skipping."|tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' '"$txn"_arupatch.out
	else
		echo "*Warning - Patch Creation returned unexpected error - $retPtch. Please check the output manually."|tee -a $outfile
		errCode=`expr $errCode + 1024`
		filesForBugFtp="$filesForBugFtp"' '"$txn"_arupatch.out
	fi
fi	

if [[ $PerformSeedValidation -eq 1 ]];then
	CallWait $pidSeed
	retSeed=$?
	retSeed=`echo $KilledPids|grep ":$pidSeed:" >/dev/null && echo -2||echo $retSeed`

	if [[ $retSeed -eq -2 ]];then
		echo "***Terminated - Seed data patching. Please see "$destdir"/bugOutputForSeedValidation.txt"|tee -a $outfile
		errCode=`expr $errCode + 1024`
		isErr=1
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForSeedValidation.txt
		filesForBugFtp="$filesForBugFtp"' 'job_status.txt
                filesForBugFtp="$filesForBugFtp"' '"$txn"_seedvalidation.txt
		if [[ -n  "$TIME_LOG" ]]; then
			echo "finSeedchecksum.sh,999,0,0,0,0%">>$TIME_LOG
		fi
	elif [[ $retSeed -eq 0 ]];then
		echo "Success - Seed data patching"  |tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForSeedValidation.txt
                filesForBugFtp="$filesForBugFtp"' 'job_status.txt
                filesForBugFtp="$filesForBugFtp"' '"$txn"_seedvalidation.txt
	elif [[ $retSeed -eq 1 ]];then
		echo "***Failure - Seed data patching. Please see "$destdir"/bugOutputForSeedValidation.txt" |tee -a $outfile
		errCode=`expr $errCode + 1024`
		isErr=1
		filesForBugBody="$filesForBugBody"' 'bugOutputForSeedValidation.txt
                filesForBugFtp="$filesForBugFtp"' '"$txn"_seedvalidation.txt
                filesForBugFtp="$filesForBugFtp"' 'job_status.txt
	elif [[ $retSeed -eq 2 ]];then
	        echo "***WARNING. Seed data patching could not be performed by premerge. Please see "$destdir"/bugOutputForSeedValidation.txt" |tee -a $outfile
                echo "***It is advisable for developers to test the DB/seed data files before merging transactions."|tee -a $outfile
		errCode=`expr $errCode + 1024`
		filesForBugBody="$filesForBugBody"' 'bugOutputForSeedValidation.txt
                filesForBugFtp="$filesForBugFtp"' '"$txn"_seedvalidation.txt
                filesForBugFtp="$filesForBugFtp"' 'job_status.txt
	elif [[ $retSeed -eq 3 ]];then
		echo "Seed data patching need not be performed as txn has no patch files. Skipping" |tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForSeedValidation.txt
                filesForBugFtp="$filesForBugFtp"' '"$txn"_seedvalidation.txt
                filesForBugFtp="$filesForBugFtp"' 'job_status.txt
        elif [[ $retSeed -eq 4 ]];then
		echo "Source Transaction not found. Please save the transaction before invoking premerge." |tee -a $outfile
                isErr=1
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForSeedValidation.txt
                filesForBugFtp="$filesForBugFtp"' '"$txn"_seedvalidation.txt
                filesForBugFtp="$filesForBugFtp"' 'job_status.txt
	else
		echo "*WARNING - Seed data patching returned unexpected error - $retSeed. Please check the output manually("$destdir"/bugOutputForSeedValidation.txt)."|tee -a $outfile
		errCode=`expr $errCode + 1024`
		filesForBugFtp="$filesForBugFtp"' 'bugOutputForSeedValidation.txt
                filesForBugFtp="$filesForBugFtp"' '"$txn"_seedvalidation.txt
                filesForBugFtp="$filesForBugFtp"' 'job_status.txt
	fi
fi	

wait $pidPrgs

if [[ $retBld -eq 1 || $retBld -eq -2 ]];then

	echo "Skipped - "$jut"Full build and Build cycle checker as incremental build did not complete successfully." |tee -a $outfile
else
	echo "Proceeding to "$jut"Full build and ear generation as necessary."
	$cmd_essbuild >"$outfile".fbd 2>&1 &
	pidFullBld=$!

	if [[ $runJUnitsAfterBuild -eq 1 ]];then
		startJUnit
	fi

       echo "Running VOTuning Scan."                                                                                                                                           
       $cmd_votuningV3 >"$outfile".VOTuning 2>&1 &
       pidVOTuningV3=$!

       echo "Running Invalid Jar References Scan."
       $cmd_invalidref >"$outfile".invref 2>&1 &
       pidInvRef=$!

	PidList="$pidFullBld $pidJut $pidVOTuningV3 $pidInvRef"
	$cmd_showprogress "Please wait while the validations are progressing in the background " "$PidList" &
	pidPrgs=$!

	CallWait $pidInvRef
	retInvRef=$?
	retInvRef=`echo $KilledPids|grep ":$pidInvRef:" >/dev/null && echo -2||echo $retInvRef`
	if [[ $retInvRef -eq -2 ]];then
        	echo "***Terminated - Invalid Jar References scan."|tee -a $outfile
        	errCode=`expr $errCode + 256`
        	isErr=1
		filesForBugFtp="$filesForBugFtp"' '"$txn"_invalidref.out
		if [[ -n  "$TIME_LOG" ]]; then
			echo "fininvalidjarrefs.sh,999,0,0,0,0%">>$TIME_LOG
		fi
	elif [[ $retInvRef -eq 0 ]];then
		echo "Success - Invalid Jar References scan." |tee -a $outfile
		filesForBugFtp="$filesForBugFtp"' '"$txn"_invalidref.out
	elif [[ $retInvRef -eq 1 ]];then
		echo "***Failure - Invalid Jar References scan." |tee -a $outfile
		errCode=`expr $errCode + 256`
		isErr=1
		filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_invalidref.out
	elif [[ $retInvRef -eq 2 ]];then
        	echo "*Warning - Invalid Jar References scan." |tee -a $outfile
        	errCode=`expr $errCode + 256`
        	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_invalidref.out
	else
		echo "*Warning - Invalid Jar References scan returned unexpected error - $retInvRef. Please check the output manually."|tee -a $outfile
		errCode=`expr $errCode + 256`
		filesForBugFtp="$filesForBugFtp"' '"$txn"_invalidref.out
	fi


	CallWait $pidVOTuningV3
	retVOTuningV3=$?
	retVOTuningV3=`echo $KilledPids|grep ":$pidVOTuningV3:" >/dev/null && echo -2||echo $retVOTuningV3`
	if [[ $retVOTuningV3 -eq -2 ]];then
	        echo "***Terminated - VOTuning_V3 scan."|tee -a $outfile
	        errCode=`expr $errCode + 256`
	        isErr=1
	        filesForBugFtp="$filesForBugFtp"' '"$txn"_VOTuning.out
	        if [[ -n  "$TIME_LOG" ]]; then
	                echo "finVOTuningV3.sh,999,0,0,0,0%">>$TIME_LOG
	        fi
	elif [[ $retVOTuningV3 -eq 0 ]];then
	        echo "Success - VOTuning scan." |tee -a $outfile
	        filesForBugFtp="$filesForBugFtp"' '"$txn"_VOTuning.out
	elif [[ $retVOTuningV3 -eq 1 ]];then
	        echo "***Failure - VOTuning scan."|tee -a $outfile
	        errCode=`expr $errCode + 256`
                isErr=1
	        filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_VOTuning.out
	else
	        echo "*Warning - VOTuning scan returned unexpected error - $retVOTuningV3. Please check the output manually."|tee -a $outfile
	        errCode=`expr $errCode + 256`
	        filesForBugFtp="$filesForBugFtp"' '"$txn"_VOTuning.out
	fi


	CallWait $pidFullBld
	retFullBld=$?
	retFullBld=`echo $KilledPids|grep ":$pidFullBld:" >/dev/null && echo -2||echo $retFullBld`

	if [[ $retFullBld -eq -2 ]];then
        	echo "***Terminated - Full build and/or ear generation."|tee -a $outfile
        	errCode=`expr $errCode + 2`
        	isErr=1
        	filesForBugFtp="$filesForBugFtp"' '"$txn"_esscheck.out
		if [[ -n  "$TIME_LOG" ]]; then
			echo "finEssCheck.sh,999,0,0,0,0%">>$TIME_LOG
		fi
	elif [[ $retFullBld -eq 0 ]];then
        	echo "Success - Full build and ear generation." |tee -a $outfile
        	filesForBugFtp="$filesForBugFtp"' '"$txn"_esscheck.out
	elif [[ $retFullBld -eq 1 ]];then
        	echo "***Failure - Full build and/or ear generation." |tee -a $outfile
        	errCode=`expr $errCode + 2`
        	isErr=1
        	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_esscheck.out
	elif [[ $retFullBld -eq 2 ]];then
        	echo "*Warning - Full build and/or ear generation could not be run. Please run manually." |tee -a $outfile
        	errCode=`expr $errCode + 2`
        	filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_esscheck.out
	elif [[ $retFullBld -eq 3 ]];then
        	echo "Skipped - Full build not necessary."|tee -a $outfile
        	filesForBugFtp="$filesForBugFtp"' '"$txn"_esscheck.out
        else
		echo "*Warning - Full build and/or ear generation returned unexpected error - $retFullBld. Please check the output manually."|tee -a $outfile
		errCode=`expr $errCode + 2`
		filesForBugFtp="$filesForBugFtp"' '"$txn"_esscheck.out
        	
	fi
        
        if [[ $retFullBld -eq 1 || $retFullBld -eq -2 ]];then
        	echo "Skipped - build cycle checker as full build did not complete successfully." |tee -a $outfile
        else
          	echo "Proceeding to build cycle checker as necessary."
          	$cmd_buildcycle >"$outfile".bcc 2>&1 &
          	pidCycle=$!
          	PidList="$pidCycle"
          	$cmd_showprogress "Please wait while the validations are progressing in the background " "$PidList" &
          	pidPrgs=$!
          	CallWait $pidCycle
          	retCycle=$?
          	retCycle=`echo $KilledPids|grep ":$pidCycle:" >/dev/null && echo -2||echo $retCycle`

	        if [[ $retCycle -eq -2 ]];then
        		echo "***Terminated - Build cycles check. Build Cycle Checker would have removed existing libraries for checking cycles. You may restore them by doing 'ade refreshview -latest'."|tee -a $outfile
        		errCode=`expr $errCode + 512`
        		isErr=1
			filesForBugFtp="$filesForBugFtp"' '"$txn"_bcc.out
			if [[ -n  "$TIME_LOG" ]]; then
				echo "finbuildcyclechecker.sh,999,0,0,0,0%">>$TIME_LOG
			fi
		elif [[ $retCycle -eq 0 ]];then
        		echo "Success - *Build cycles check. Build Cycle Checker would have removed existing libraries for checking cycles. You may restore them by doing 'ade refreshview -latest'." |tee -a $outfile
			filesForBugFtp="$filesForBugFtp"' '"$txn"_bcc.out
		elif [[ $retCycle -eq 1 ]];then
			echo "***Failure - Build cycles check. Build Cycle Checker would have removed existing libraries for checking cycles. You may restore them by doing 'ade refreshview -latest'." |tee -a $outfile
			filesForBugBody="$filesForBugBody"' '"$destdir"/"$txn"_bcc.out
			errCode=`expr $errCode + 512`
        		isErr=1
		elif [[ $retCycle -eq 3 ]];then
			echo "Skipped - Build cycles check." |tee -a $outfile
			filesForBugFtp="$filesForBugFtp"' '"$txn"_bcc.out
		else
			echo "*Warning - Build cycles check returned unexpected error - $retCycle. Please check the output manually."|tee -a $outfile
			errCode=`expr $errCode + 512`
			filesForBugFtp="$filesForBugFtp"' '"$txn"_bcc.out
		fi
        fi
        
        if [[ $runJUnitsAfterBuild -eq 1 ]];then
                processJUnit
        fi

        wait $pidPrgs
fi

trap "CleanAndExit $ABORT 'User Break'" 2
echo ""

if [[ $isErr -eq 1 ]];then
	errCode=`expr $errCode + $ERR`
fi

process_end=$(date +%s);
process_elapsed=$(($process_end - $process_start));


#ER 19943867 2nd Level Mergereq Approval - Create Marker file.
if [[ -z $skipMRInfo ]];then #Do mergereq info processing by default.
	$cmd_insMRInfo
fi
	


# SendMail marks end of processing.
SendMail $errCode

if [[ $updateBug = "Y" ]];then # Update irrespective of errCode
	updBug=0
elif [[ $updateBug = "N" ]];then # No update irrespective of errCode (badBug, connString included)
        updBug=1
elif [[ $updateBug = "S" ]];then # Update only if Success
        updBug=$errCode
elif [[ $updateBug = "W" ]];then # Update if Success or Warning
        if [[ $errCode -ge 0 && $errCode -lt $ERR ]];then
		updBug=0
	else
		updBug=1
	fi
elif [[ $errCode -eq 0 ]];then #$updateBug = "D" Default - update bug if Success, Ask the user otherwise
        	updBug=0
else
	input_start_time=$(date +%s);
	echo
	echo "Update Bug? (Y/N)"
	Ans='N'
	read  -t$timeOut Ans
	Ans=`echo $Ans|cut -c1|sed 's/[a-z]*/\U&/g'`
	if [[ $Ans = "Y" ]];then
		updBug=0
	else
		updBug=1
	fi
	input_end_time=$(date +%s);
	test -z $input_elapsed_time && input_elapsed_time=0
	input_elapsed_time=$(( $input_end_time - $input_start_time + $input_elapsed_time));

fi
#Bug update time should not include waiting for user confirmation. Bobby 20141023
tupdatebug_start=$(date +%s);

if [[ $updBug -eq 0 ]];then
	tstart_bug="$(date +%s)"
	if [[ ! -z $filesForBugFtp ]];then
		echo "Please wait while reports are uploaded to BugFTP..."
		uploadToBugFTP $destdir
		if [[ $? -eq 0 ]];then
			echo "Reports of successful validations uploaded to :"|tee -a $outfile
			echo "https://bugsftp.us.oracle.com/bug/faces/BugUploadMain.jspx?bug=$bugNo"|tee -a $outfile
		fi
	fi
	bugTxtTmp=`cat $outfile $filesForBugBody| sed s/\'/\'\'/g`
	count=$(( ${#bugTxtTmp} / $MAXLEN ))
	if [[ ${#bugTxtTmp} -gt $(( $count * $MAXLEN )) ]];then
		count=$(( $count + 1 ))
	fi
	i=0
	while [[ $i -lt $count ]];do
		bugTxt=`echo "$bugTxtTmp"|tr '\n' '\346'|cut -c $(( $MAXLEN * $i + 1 ))-$(( $MAXLEN * ( $i + 1 ) ))|tr '\346' '\n'`
		updateBugDb
		i=$(( $i + 1 ))
	done
	#Update the rest of the bugs in bugList with a line..
	bugListTemp=`echo $bugList|sed "s/$bugNo//g"`
	bugTxt="Please refer bug $prelimBug for Pre-Merge results."
	for bugNo in $( echo $bugListTemp |tr ',' ' ' );do
		updateBugDb
	done
	bugNo=$prelimBug
	
	tstop_bug="$(date +%s)"
	telapsed_bug="$(expr $tstop_bug - $tstart_bug)"
	echo -e "Bug update elapsed time: $(date -d "1970-01-01 $telapsed_bug sec" +%H:%M:%S)"|tee -a $outfile
		
fi
tupdatebug_end=$(date +%s)
tupdatebug_elapsed=$(($tupdatebug_end - $tupdatebug_start))


if [[ $validateOnly -eq 1 ]];then
	CleanAndExit $errCode "Validations completed."
fi

# Code beyond this line is unreachable till a decision on mergereq is taken. (validateOnly=1 line will be removed)

if [[ $errCode -gt 0 ]];then
	if [[ $forceMergeRq -ne 0 ]];then
		echo "Processing complete. There are validation failures. Proceeding to mergereq as "Force mergereq" flag is specified." | tee -a $outfile
	else
		CleanAndExit $errCode "Processing complete. There are validation failures. Please fix the issues and try again."
	fi
fi

/usr/local/packages/mergereq/mergereq -y

CleanAndExit $errCode "Processing complete."


