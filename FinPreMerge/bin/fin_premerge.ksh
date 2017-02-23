#! /bin/ksh

# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/fin_premerge.ksh /main/216 2017/02/12 18:12:45 tedong Exp $
#
# fin_premerge.ksh
#
# Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
#
#    NAME
#      fin_premerge.ksh - Central script that invokes various validations mandated in Financials division.
#
#    DESCRIPTION
#      <short description of component this file declares/defines>
#
#    NOTES
#      <other useful comments, qualifications, etc.>
#
#    MODIFIED   (MM/DD/YY)
#    tedong      12/12/16 - Migrate to ODCS premerge 
#    mengxu      11/20/16 - Add bugAU support.
#    mideng      09/23/16 - allow pass in FAToolsMainLatest from command line
#    mengxu      06/01/16 - Add Angrybirds clarification.
#    mengxu      01/11/16 - Add ER bug update Support
#    wung        07/31/12 - Adding more timing variables.
#    wung        06/20/12 - Adding time profile information
#    bomathew    01/24/11 - Creation
#set -x

if [[ -z $ADE_VIEW_ROOT ]];then
		echo "*************************************************************"
		echo "This Script must be executed from within a view. Cannot proceed."
		echo "*************************************************************"
		exit 99
fi

export txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
if [[ $txn = "NONE" ]];then
		echo "*************************************************************"
		echo "Please begin a transaction and run the script again. Cannot proceed."
		echo "*************************************************************"
		exit 99
fi

#prepare a folder to place the odcs premerge source code
ade expand -recurse $ADE_VIEW_ROOT/fatools
if [[ -d $ADE_VIEW_ROOT/fatools/odcs/pmcpull ]];then
	rm -rf $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	#echo "remove the existing earier version ODCS premerge code."			
fi

mkdir -p $ADE_VIEW_ROOT/fatools/odcs/pmcpull/
chmod 777 -R $ADE_VIEW_ROOT/fatools/odcs/pmcpull/
#echo "create a new folder in $ADE_VIEW_ROOT/fatools/odcs/pmcpull/"
export pmcpull=$ADE_VIEW_ROOT/fatools/odcs/pmcpull



export domain_id=` cat /etc/sysconfig/network|grep NISDOMAIN|cut -d= -f2|cut -d. -f1`

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
			export Ans=`head -1 $MsgFile 2>/dev/null|grep "Answer="|cut -d: -f2|cut -d= -f2`
			if [[ -z $Ans ]];then
				echo "Press Enter to continue." && read -t$timeOut
			else
				export UsrIp=`grep UserAns $ADE_VIEW_ROOT/Ans.txt 2>/dev/null |cut -d: -f2`
				while [[ $Ans != $UsrIp ]];do
					echo "Please read the above message carefully and type $Ans to continue."
					read UsrIp
				done
				echo "UserAns:$Ans" > $ADE_VIEW_ROOT/Ans.txt 
			fi
		fi
		
	fi
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
	export resetFAlabel=`ls -l $ADE_VIEW_ROOT/fatools|grep "FATOOLS[A-Z0-9\_\.]*LINUX.rdd"`
	if [[ -n $resetFAlabel ]];then
		export FAReset=$( basename $( dirname $( echo $resetFAlabel |cut -d\> -f2 ) | sed 's/.rdd\//_/' ) )
	fi

	export label=$PrimaryView
	export labelServer=`ade desc -l $label -labelserver`
	export labelServerTop=`echo $labelServer|cut -d/ -f1-3`
	export FAToolsLatest="$( dirname $( ade desc -l $FAView -labelserver ) )/LATEST"
	export FatoolsLatestLabel=`ade showlabels -series FATOOLS_MAIN_LINUX -latest | tail -n 1`
        if [[ -z $FAToolsMainLatest ]]; then 
        	export FAToolsMainLatest=`ade desc -l $FatoolsLatestLabel -labelserver`
        fi
	
	export MsgFile="$FAToolsLatest/fatools/opensource/jauditFixScripts/FinPreMerge/bin/Message.txt"
}



#export cmd_dir=$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/FinPreMerge/bin

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

ParseAndSave()
{
	user=`echo $1|cut -d\@ -f1`
	db=`echo $1|cut -d\@ -f2`
	which tnsping >/dev/null 2>&1
	if [[ $? -ne 0 ]];then
		echo
                echo "*** Can't find tnsping. Please make sure you are in a view, ORACLE_HOME is pointing to a valid location and PATH includes ORACLE_HOME/bin."
                export errCode=4
                return
	fi
 
	tnsping $db >/dev/null 2>&1
	if [[ $? -ne 0 ]];then
		echo
		echo "*** tnsping to $db failed. Please make sure you are in a view and the db is running."
		export errCode=4
		return
	fi
	export uid=`echo $user|cut -d\/ -f1`
	export pswd=`echo $user|cut -d\/ -f2`
	export host=`echo $db|cut -d\: -f1`
	export port=`echo $db|cut -d\: -f2|cut -d\/ -f1`
	export sid=`echo $db|cut -d\: -f2|cut -d\/ -f2`

	if [[ ! -f $junitdb_file ]];then
		touch $junitdb_file
		if [[ $? -ne 0 ]];then
			echo "Cannot create file $junitdb_file"
			export errCode=5
			return
		fi
		echo "User=$uid" > $junitdb_file
		echo "Password=$pswd" >> $junitdb_file
		echo "Host=$host" >> $junitdb_file
		echo "Port=$port" >> $junitdb_file
		echo "SID=$sid" >> $junitdb_file
		chmod 400 $junitdb_file
		echo "Database information stored to $junitdb_file."
	fi
	export jutDb=1
}

SetJUnits()
{
	export customJUnits=1
	if [[ -z $customJunitProjs ]];then
		export customJunitProjs=$1
	else
		export customJunitProjs=$customJunitProjs","$1
	fi
}

ParseKeyValue()
{

        key_value=`echo $1|awk '$1 ~ "=" {print $1}'`
        key=`echo $key_value|cut -d= -f1|grep -v \-`
        value=`echo $key_value|cut -d= -f2`

        if [[ `echo $value |sed 's/[A-Z]*/\L&/g'` = "true" ]];then
                value="true"
        elif [[ `echo $value |sed 's/[A-Z]*/\L&/g'` = "false" ]];then
                value="false"
        fi


        if [[ -n $key && -n $value ]];then
                export `echo $key`=$value
        fi

}


#Time Stat
ParseAndUploadTimeStat()
{

	export total_time=$(($end_time  - $start_time));
	export time_invoked=$(date -d@$start_time +"%Y-%m-%d %k:%M:%S")
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

	export parsed_time_log=$(awk -F/ '!/^Command exited*/ {gsub(/%/, "",$NF); print $NF}' $TIME_LOG | awk '{if(NF == 1) {print $NF} else{print $1substr($NF, index($NF, ","))}}' | tr '\n' '~')
	
	export hostname=$(hostname -f);
	export username=$(echo "$txn" | awk -F_ '{print $1;}');
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



getEmailID()
{

 export email=`grep -i "^EMail:$curUser:" $cfgFile 2>/dev/null |cut -d: -f3|grep -i "@oracle.com$"`

 if [[ -n $email ]];then
	echo "Reading email id from $cfgFile" | tee -a $outfile
	return
 fi
 export tmp=$curUser".ldap.tmp"
 /usr/local/bin/phone $curUser >$tmp 2>/dev/null

 export nEntries=`cat $tmp 2>/dev/null|grep "numEntries" | cut -d: -f2`
 if [[ $nEntries -eq 1 ]];then
  export email=`grep "^mail:" $tmp| cut -d: -f2`
  echo "Obtained email id from LDAP." | tee -a $outfile
 else
  export validId=0
  while [[ $validId -eq 0 ]];do
	if [[ $nEntries -gt 1 ]];then #Multiple entries returned, just list them.
		grep "^mail:" $tmp| cut -d: -f2
	fi
	while [[ -z $email ]];do
		echo "Please enter your email id (firstname.lastname@oracle.com):"
		read email
		export emailOrignial=$email
	done

	#ensure email domain is always oracle.com
  	export flname=`echo $email|cut -d@ -f1`
	export email=$flname"@oracle.com"
	if [[ $email != $emailOrignial ]];then
		echo "*Warning - E-mail id is set to $email."
	fi

	Ask=0
  	/usr/local/bin/phone $flname|grep -i $curUser >/dev/null || { echo "*Warning - $email doesn't appear to be $curUser's e-mail id." | tee -a $outfile; Ask=1 ;}
	echo $email|egrep -i "[[:alpha:]]+\.[[:alpha:]]+@oracle.com$" >/dev/null && export validId=1 || { echo "*Warning - The id $email doesn't appear to be a valid oracle email id." | tee -a $outfile; Ask=1 ;} 
	if [[ $Ask -eq 1 ]];then
		export validId=0
		echo "Do you want to save it? (y/n)"
		export Ans=n
		read -t$timeOut Ans
		echo $Ans|cut -c1|grep -i "y" >/dev/null && export validId=1 || export email=""
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
	export mail_start=$(date +%s)
	export bugLink=1
	if [[ $mailSent -eq 0 ]];then
		if [[ $1 -eq 0 ]];then
			export status=Successful
			export bugLink=0
		elif [[ $1 -eq $ABORT ]];then
			export status=Aborted
		elif [[ $1 -lt $ERR ]];then
			export status=Warning
		else
			export status=Failed
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
		export mailSubject="[Fusion-Pre-mergereq] - Processing $status for $txn in $ADE_VIEW_NAME"	
		if [[ $1 -eq $ABORT ]];then
			cat $outfile "$destdir"/$txnDescFile 2>/dev/null|mutt -c "$idsToCopyMail" -s "$mailSubject" $email 
		else
			#cat $outfile "$destdir"/"$txn"_*.out 2>/dev/null|mailx -s"[Fusion-Pre-mergereq] - Processing $status for $txn in $ADE_VIEW_NAME" -c "$idsToCopyMail" $email
			#Compatible with Linux6+
			cat $outfile "$destdir"/"$txn"_*.out 2>/dev/null|mutt -c "$idsToCopyMail" -s "$mailSubject" $email 
		fi
		export mailSent=1
	fi
	export mail_end=$(date +%s)
	export mail_elapsed=$(($mail_end - $mail_start))	
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
	export end_time=$(date +%s)
	if [[ -n  "$TIME_LOG" ]]; then
		export fn="$outdir/TimeProfile.`date +%s`.txt"
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
                export errCode=4
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
		
		export db=$host:$port/$sid
	        tnsping $db >/dev/null 2>&1
       	 	if [[ $? -eq 0 ]];then
			echo "tnsping to $db succeeded"	
			echo
		else
       	       	  	echo "*** tnsping to $db failed."
			echo "Please make sure you are in a view, $junitdb_file is not corrupt and the db is not down. Cannot proceed."
       	       	  	echo
       	       	  	export errCode=4
       	       	  	return
	        fi
		export jutDb=1
	fi
}

getBugUserInfo()
{
        if [[ -f $bugdb_file ]];then
                echo
                echo "*** Reading bug user information from $bugdb_file"
                echo
                export bugUid=`grep "^User" $bugdb_file |cut -d= -f2`
                export bugPswd=`grep "^Password" $bugdb_file |cut -d= -f2`

                export bugUser=1
        fi
}

getVerInfo()
{
	echo "Product view on $PrimaryView" >> $outfile
	echo "Dependent FA label - $FAView" >> $outfile
	if [[ -n $FAReset ]];then
		echo "FA label reset to - $FAReset" >> $outfile
	fi

	export vTemp=`cat $VersionFile 2>/dev/null`
	export l=${#vTemp}
	if [[ $is32Bit -eq 1 ]];then
		export Ver2=`echo $vTemp|cut -c $(( ( ( $l + 1 ) / 2 + 1 ) ))-`
	else
		export Ver2=${vTemp: -( l / 2 )}
	fi
	export bugUser=1
	export firstChar=`echo $cmd_this|cut -c 1`
	if [[ $firstChar = "/" ]];then
        	export Param=1011
	else
		echo '**Possibly incorrect execution of the tool. The tool must be run providing absolute path starting with $ADE_VIEW_ROOT.'|tee -a $outfile
        	export Param=1012
	fi
}

askAndSaveBugUserInfo()
{
	echo "Please enter BugDB user name [$curUser]: "
	read -t$timeOut bugId
	if [[ -z $bugId ]];then
		export bugId=$curUser
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
	export bugUser=1
	echo "Stored bug user info to $bugdb_file (readable only by you)."
}

setBugContext()
{

        which sqlplus >/dev/null 2>&1
        if [[ $? -ne 0 ]];then
                echo
                echo "*** Can't find sqlplus. Please make sure you are in a view, ORACLE_HOME is pointing to a valid location and PATH includes ORACLE_HOME/bin. Bug update is turned off."
                export updateBug="N"
                return
        fi

	set +x

	export Ver1=`$VersionCmd $Param $StartDir`


        export SFtp1=`$VersionCmd 2011 $StartDir`
        export vTemp=`cat $BugVersionFile 2>/dev/null`
        export l=${#vTemp}
        export SFtp2=`echo $vTemp|cut -c $(( ( ( $l + 1 ) / 2 + 1 ) ))-`

        export bugVer=$( $VersionCmd 2100 "$SFtp1$SFtp2" 2>/dev/null )

        export oldCon='(DESCRIPTION= (ADDRESS_LIST= (LOAD_BALANCE=ON) (ADDRESS=(PROTOCOL=tcp)(HOST=apd211-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd211-crs.us.oracle.com)(PORT=1522)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd212-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd212-crs.us.oracle.com)(PORT=1522)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd213-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd213-crs.us.oracle.com)(PORT=1522)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd214-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd214-crs.us.oracle.com)(PORT=1522)) ) (CONNECT_DATA=(SERVICE_NAME=bugap.us.oracle.com)))'

        if [[ $dev -eq 1 ]];then
            export newCon='(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=umogridxt08-scan.us.oracle.com)(PORT = 1529))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME = LDAP_BUGAU.us.oracle.com)))'
            export ADE_USE_BUGAU=1
        else
            export newCon='(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=amogridxp09-scan.us.oracle.com)(PORT = 1529))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME = ldap_bugap.us.oracle.com)))'
        fi 

	export myConVer=$( $VersionCmd 1100 "$Ver1$Ver2" 2>/dev/null )

connectStr=""

if [[ -z $bugTns ]];then #Try to use the TNS passed by the user using -DbugTns
	sqlplus -L -S $bugUid/$myConVer@$bugTns >/dev/null 2>&1 >/dev/null 2>&1 << EOF && export connectStr=$bugTns
	exit
EOF
fi

#OK, now check if tns entry for new Exadata is available in the env.
test -z $connectStr && sqlplus -L -S $bugUid/$myConVer@bugap_adx.us.oracle.com >/dev/null 2>&1 << EOF && export connectStr=bugap_adx.us.oracle.com
exit
EOF

#Still no luck, try the new Exadata with the known conn info
test -z $connectStr && sqlplus -L -S $bugUid/$myConVer@"$newCon" >/dev/null 2>&1 <<EOF && export connectStr=$newCon && echo "bugap_adx.us.oracle.com=$connectStr" >> ~/.tnsnames.ora
exit
EOF

#May be the old one is still around?
test -z $connectStr && sqlplus -L -S $bugUid/$myConVer@bugap >/dev/null 2>&1 << EOF && export connectStr=bugap
exit
EOF

#OK, last try, use the known conn info for the old machine.
test -z $connectStr && sqlplus -L -S $bugUid/$myConVer@"$oldCon" >/dev/null 2>&1 <<EOF && export connectStr=$oldCon && echo "bugap=$connectStr" >> ~/.tnsnames.ora
exit
EOF

        if [[ -z $connectStr ]];then
                echo
                echo "*** Could not connect to bugDB. Bug update is turned off."
		export updateBug="N"
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

	export fusionSuiteId=23

	echo "Verifying if bug $bugNo belongs to Fusion family..."

	set +x

	export ret=$(sqlplus  -s  /nolog <<-EOF
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

	echo "the ret value is $ret"

	export programmer=`echo $ret|grep "BugUser:"|cut -d: -f2`
	echo "the programmer is $programmer"
	if [[ $programmer = $curUser ]];then
		echo "Bug $bugNo belongs to Fusion family and assigned to $curUser."|tee -a $outfile
	elif [[ $programmer = 'NonFusion' ]];then
		CleanAndExit $ABORT "Bug $bugNo does not belong to Fusion family or the tool cannot access the bug (e.g. Security Vulnerability bug). Can't proceed. Please remove it from the BUG_NUM property to proceed."
	elif [[ -n $programmer ]];then
		echo "*Warning - Bug $bugNo is not assigned to $curUser."|tee -a $outfile
	elif [[ -z $programmer ]];then
		echo "Cannot verify if bug $bugNo belongs to Fusion family. programmer is empty."
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

#Main starts from here..
export cmdLine="$0 $*"
export jutDb=0
export junitdb_file=~/.JUnit.db
export customJUnits=0
export validateOnly=0
export forceMergeRq=0
export updateBug="D"

export VersionCmd=$cmd_dir/GetVer
export VersionFile=$cmd_dir/Version.info
export BugVersionFile=$cmd_dir/BugVersion.info

export bugdb_file=~/.BugUser.info
export bugUid=fintools

export idsToCopyMail="fusion-premerge_ar@oracle.com"


export mailSent=0

export customJunitProjs=""
export isErr=0
export is32Bit=0
export badBug=0
export runJUnitsAfterBuild=0
export runJUnits=0
export addJUnitsFolder=0
export runARUValid=1
export runOatsIAT=1
export dev=0

export errCode=0
export bugTxt="Dummy text"
export MAXLEN=32000
export FTPCmd=lftp
export FTPMAXLEN=460
export FTPMaxParams=16
export BugFTPServer=sftp://bugsftp.us.oracle.com
export FTPUser=fusion-premerge_ar@oracle.com
export FTPPass="NoPass"
export ERR=2048
export ABORT=-1
export debug=0
export use_odcs=1

export BreakOnError=1
export PerformSeedValidation=1
export PerformFreValidation=1
export PerformPseudoTranslation=1
export timeOut=86400 #One day!

while getopts :vfd:j:D:r:h opt
do	case "$opt" in
	v)	export validateOnly=1;;
	f)	export forceMergeRq=1;;
	d)	ParseAndSave "$OPTARG";;
	j)	SetJUnits "$OPTARG";;
	D)	ParseKeyValue "$OPTARG";;
        h)      showUsageAndExit;;
        r)      export freDB="$OPTARG";;
	:)	if [[ $OPTARG = "d" || $OPTARG = "j" || $OPTARG = "D" || $OPTARG = "r" ]];then
			export errCode=2
		fi;;
	*)	export errCode=3;;
	esac
done

#if [[ $domain_id = "us"  ]];then
host=`cat /etc/sysconfig/network|grep HOSTNAME|cut -d= -f2`
if [[ $host == "rws"* ]];then
	echo "Fetch code from rws server."
	if [[ -d /net/rws66623fwks.us.oracle.com/scratch/Premerge/src/ ]];then 
		cp -rf /net/rws66623fwks.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/slc02vmb.us.oracle.com/scratch/Premerge/src/ ]];then
		cp -rf /net/slc02vmb.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/slc02vml.us.oracle.com/scratch/Premerge/src/ ]];then
		cp -rf /net/slc02vml.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/slc01bpn.us.oracle.com/scratch/Premerge/src/ ]];then 
		cp -rf /net/slc02bpn.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/slc06eql.us.oracle.com/scratch/Premerge/src/ ]];then 
		cp -rf /net/slc06eql.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	else
		echo "There are no apache servers available in us domain." 
		echo "Use the local fatools instead."
		export use_odcs=0
	fi
elif [[ $host == "slc"* ]];then
	echo "Fetch code from slc server."
	if [[ -d /net/slc02vmb.us.oracle.com/scratch/Premerge/src/ ]];then
		cp -rf /net/slc02vmb.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/slc02vml.us.oracle.com/scratch/Premerge/src/ ]];then
		cp -rf /net/slc02vml.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/slc01bpn.us.oracle.com/scratch/Premerge/src/ ]];then 
		cp -rf /net/slc02bpn.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/slc06eql.us.oracle.com/scratch/Premerge/src/ ]];then 
		cp -rf /net/slc06eql.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/rws66623fwks.us.oracle.com/scratch/Premerge/src/ ]];then 
		cp -rf /net/rws66623fwks.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	else
		echo "There are no apache servers available in us domain." 
		echo "Use the local fatools instead."
		export use_odcs=0
	fi
else
	if [[ -d /net/slc02vmb.us.oracle.com/scratch/Premerge/src/ ]];then
		cp -rf /net/slc02vmb.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/slc02vml.us.oracle.com/scratch/Premerge/src/ ]];then
		cp -rf /net/slc02vml.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/slc01bpn.us.oracle.com/scratch/Premerge/src/ ]];then 
		cp -rf /net/slc02bpn.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/slc06eql.us.oracle.com/scratch/Premerge/src/ ]];then 
		cp -rf /net/slc06eql.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	elif [[ -d /net/rws66623fwks.us.oracle.com/scratch/Premerge/src/ ]];then 
		cp -rf /net/rws66623fwks.us.oracle.com/scratch/Premerge/src/* $ADE_VIEW_ROOT/fatools/odcs/pmcpull
	else
		echo "There are no apache servers available in us domain." 
		echo "Use the local fatools instead."
		export use_odcs=0
	fi
	#echo "it is not a rws and slc host"
fi
#fi

#Judge whether the odcs codes are available
if [[ $use_odcs -eq 1 ]]&&[[ -d $pmcpull/FinPreMerge/bin ]];then
	export cmd_dir=$pmcpull/FinPreMerge/bin
else
	export cmd_dir=$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/FinPreMerge/bin
fi

chmod 755 -R $pmcpull

#Replace the JarJAuditFixer.jar package with the odcs premerge codes
if [[ $use_odcs -eq 1 ]]&&[[ -e $pmcpull/JarJAuditFixer.jar ]];then	
	ade mkprivate $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar
	cp -rf $pmcpull/JarJAuditFixer.jar $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar
	if [[ $? = 0 ]];then
		echo "The JarJAuditFixer.jar is updated to the latest FATOOLS_MAIN_LINUX!"
	fi
fi


export start_time=$(date +%s)
export cfgFile=~/.Premerge.cfg

export cmd_this=$0

export profileFile=$cmd_dir/timeProfiling.cfg
export VersionCmd=$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/FinPreMerge/bin/GetVer
export VersionFile=$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/FinPreMerge/bin/Version.info
export BugVersionFile=$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/FinPreMerge/bin/BugVersion.info
#Wrapping the command setters within a function. $time_cmd is set after transaction information is read.

#Main starts from here..

export bugUser=0

# This tool will not raise mergereq. This is still under discussion.
export validateOnly=1

test -n $breakOnError && export BreakOnError=$breakOnError

if [[ $debug -eq 1 ]];then
	set -x
fi

identifyLabels
showBanner

uname -a | grep -v x86_64|grep "i[3-6]86" >/dev/null 2>&1  && export is32Bit=1

if [[ `expr $errCode + $validateOnly + $forceMergeRq` -gt 1 ]];then
	showUsageAndExit
fi

export errCode=0
export curUser=`whoami`
#export txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
export outdir=$ADE_VIEW_ROOT
if [[ -d $outdir/fusionapps ]];then
	export destdir=$outdir/fusionapps
else
	export destdir=$outdir
fi
mkdir "$destdir/premerge" >/dev/null 2>&1
cd "$destdir/premerge" 2>/dev/null && destdir=`pwd`

export outdir=$destdir

if [[ "X$txn" = "XNONE" || -z $txn ]];then
	export txn="NONE"
	export errCode=1
else #Check if backport txn first. Else, check normal bug fix.

        export bugNo=`ade describetrans -properties_only|grep BACKPORT_BUG_NUM |awk '{print $3}'`
        if [[ -z $bugNo ]];then
                export bugNo=`ade describetrans -properties_only|grep "^ *BUG_NUM"|awk '{print $3}'`
        fi
        if [[ -z $bugNo ]];then
		export badBug=1
		export errCode=2
		export runARUValid=0
	else
		export bugList=$bugNo
		export bugNo=`echo $bugList|cut -d, -f1`
		export prelimBug=$bugNo
	fi
fi


export outfile=$outdir/$txn.txt 
rm -f $destdir/$txn* 2>/dev/null

#Always run the tools from fusionapps/premerge directory.
export StartDir=`pwd`
cd $destdir

showBanner skip >$outfile

echo "Fusion pre-merge validations of $txn start at "`date "+%d-%h-%y %H:%M"` >>$outfile
echo "Command - $cmdLine" >>$outfile
echo "ORACLE_HOME is $ORACLE_HOME" >>$outfile
if [[ $is32Bit -eq 1 ]];then
	export VersionCmd=$cmd_dir/GetVer32
	echo "Running 32 bit version." |tee -a $outfile
fi

getEmailID
export fileForAdeMR=$ADE_VIEW_ROOT/fa_premerge_"$txn".sts 	#File to be looked up by mergereq. Deleting if any exists from previous run.
rm -f $fileForAdeMR 2>/dev/null				#Always represent only the last execution status.

trap "CleanAndExit $ABORT 'User Break'" 2

if [[ $errCode -eq 0 ]];then
	if [[ $badBug -eq 0 ]];then
		export bugurl="https://bug.oraclecorp.com/pls/bug/webbug_print.show?c_rptno=$bugNo#Bottomofthepage$bugNo"
		echo "$bugurl" >>$outfile
	fi

	echo $cmd_this |grep "/$ADE_VIEW_NAME/fatools/" >/dev/null 2>&1 
	if [[ $? -ne 0 ]];then
		export errCode=2
		echo "*** The tool location is not matching your current view. Please run it starting with $ADE_VIEW_ROOT."|tee -a $outfile
	fi

	export curLabel=`ade pwv | grep "VIEW_LABEL" | awk '{print $3}'`
	export latestLabel=`ade showlabels|tail -1`
	if [[ $curLabel != $latestLabel ]];then
		echo "*** There is a new label available. Current view must be refreshed to the latest before proceeding."|tee -a $outfile
		export errCode=2
	else
		ade lsco |grep "No files checked-out in view" >/dev/null 2>/dev/null
		if [[ $? -ne 0 ]];then
       		 	echo "There are checked-out files. Please check-in all files and save the transaction."|tee -a $outfile
			export errCode=2
		fi
	fi
elif [[ $errCode -eq 1 ]];then

	echo "*************************************************************"
	echo "Invalid or no transaction. Cannot proceed."
	echo "*************************************************************"
	rm $outfile
	exit 99

else #Bug update impossible, badBug
	echo "BUG_NUM/BACKPORT_BUG_NUM property not set."|tee -a $outfile
	export updateBug="N"
	export bugUser=0 
fi


# - Not forcing an exit here as there could be some irrelevant file checked-out, which will be unchecked-out later. Leaving it to the user.
if [[ $errCode -ne 0 ]];then
	echo "Some validations will be skipped and overall status of the validations will be marked as failed. Do you want to continue? "
	export Ans='N'
	read -t$timeOut Ans
        export Ans=`echo $Ans|cut -c1|sed 's/[a-z]*/\U&/g'`
        if [[ $Ans = "Y" ]];then
		print "Continuing with validations.";errCode=1;BreakOnError=0 # Setting errCode so that mail says "Failure"
	else
		CleanAndExit $ABORT
	fi
	export isErr=1
fi

if [[ $runJUnits -eq 1 && $jutDb -eq 0 ]];then
	getJutDBInfo
	if [[ $jutDb -eq 0 ]];then
		if [[ $errCode -eq 4 ]];then
			exit $errCode
		else
			echo
			echo "Looks like you are running this tool first time. Please provide the connection information using -d option."
			echo "e.g. -d fusion_runtime/fusion_runtime@indl144xxx.idc.oracle.com:1522/in144xxx"
			echo
			exit 99
		fi
	fi
fi

export txnDescFile="$txn"_describe.out
echo >$txnDescFile
echo "Transaction Description" >>$txnDescFile
echo "==================================================" >>$txnDescFile
ade describetrans >>$txnDescFile 
echo "==================================================" >>$txnDescFile
echo >>$txnDescFile

grep "ade WARNING: Initial Kerberos ticket required" $txnDescFile >/dev/null 2>&1
if [[ $? -eq 0 ]];then
	CleanAndExit $ABORT "Initial Kerberos ticket required to proceed. Please see http://ade.oraclecorp.com/twiki/bin/view/ADEFAQ/GetKerberosTicket"
fi


#check whether the angrybirds DB is up
sqlplus -L -S fintech/fintech@angrybirds.oracle.com:1521/codescan >/dev/null 2>&1 << EOF && angrybirdsDBStatus=ok
exit
EOF

exitstatus=$?

if [[ $exitstatus -eq 0 ]] || [[ "x$angrybirdsDBStatus" = "xok" ]];then
	echo "AngryBirds DB is ok."
elif [[ $exitstatus -eq 127 ]];then
	echo "*** Can't find sqlplus. Please make sure you are in a view, ORACLE_HOME is pointing to a valid location and PATH includes ORACLE_HOME/bin. Bug update is turned off."|tee -a $outfile
else
	errmsg=$(sqlplus -L -S fintech/fintech@angrybirds.oracle.com:1521/codescan 2>&1 1>/dev/null)
	proxystatus=`echo $errmsg|grep -i 'proxy'|tee -a $outfile`
	if [[ $proxystatus != '' ]];then
	
		echo "Proxy : $http_proxy is not set up correctly."
	else
		orahomestatus=`echo $errmsg|grep -i 'ORACLE_HOME'|tee -a $outfile`
		if [[ $orahomestatus != '' ]];then
			echo "Please make sure you are in a view, ORACLE_HOME is pointing to a valid location and PATH includes ORACLE_HOME/bin."|tee -a $outfile
			echo "Please refresh your view to latest and re-enter the view."|tee -a $outfile
		else
			tnsstatus=`echo $errmsg|grep -i 'TNS:'|grep -v -i 'listener'|tee -a $outfile`
			if [[ $tnsstatus != '' ]];then
				echo "$errmsg"|tee -a $outfile
			else
				echo -e "AngryBirds DB is down. Unused auto submit and nonexistent partial trigger scan will not report correct result. Please start it up. \n(ExitCode = $exitstatus). \n[$errmsg]"|mutt -s "Angrybirds DB is Down"  -c yao.yao@oracle.com fusion_premerge_support_ww_grp@oracle.com
				echo "AngryBirds DB is down. Unused auto submit and nonexistent partial trigger scan will not report correct result. "|tee -a $outfile
				#exit 99
			fi	
		fi
	fi
fi

export angrybirdsDBStatus

export txnFiles="$txn"_files.lst
export start=$(( $( cat $txnDescFile |grep -n "ALL AFFECTED FILES:"|cut -d: -f1 ) + 1 ))
export end=$(( $( tail -n+$start $txnDescFile |grep "^[A-Z]" -n |head -1|cut -d: -f1 ) - 1 ))
if [[ $end -le 0 ]];then
	 export end=$( cat $txnDescFile |wc -l )
fi
tail -n+$start $txnDescFile|head -$end |grep fusionapps/ |grep -v '/noship/'| awk '{print $2}' | sort | uniq > $txnFiles

if [[ -z $family ]];then
	export family=`cat $txnFiles| cut -d/ -f2 | sort | uniq`
	if [[ $( echo $family |wc -w ) -ne 1 ]];then
		CleanAndExit $ABORT "Cannot ascertain to which Fusion family this transaction belongs based on the files in it. Can't proceed. Please run the tool specifying it using -Dfamily=<family>"
	else
		echo "Product family is identified as $family." |tee -a $outfile
	fi
else
	ls $ADE_VIEW_ROOT/fusionapps/$family/build.xml >/dev/null 2>&1 ||family=`echo $family | tr "A-Z" "a-z"`
	echo "Product family is explicitly set to $family." |tee -a $outfile
	export family
fi

#Profile this run? Giving the user a choice. 0-Do not run, 1-run
if [[ -z $runProfile ]];then
	export series=$(echo $label | awk -F_ '{print substr($0, 1, index($0, $NF)-2)}')

	export runProfile=$(egrep "^[[:space:]]*$series[[:space:]]*:[[:space:]]*($family|all)[[:space:]]*$" $profileFile)
fi

if [[ "$runProfile"  != "0" ]];then 
	export TIME_LOG=$(mktemp)
	export time_cmd="time -o $TIME_LOG -a -f %C,%x,%e,%S,%U,%P -- "
else
	export time_cmd=""
fi

#SetCommands

ls $ADE_VIEW_ROOT/fusionapps/$family/build.xml >/dev/null 2>&1 || echo "*Warning - Product family set to suspicious value - $family" |tee -a $outfile

#The following code checks for private files.

export vp_start_time=$(date +%s)
export startup_elapsed=$(($vp_start_time -$start_time));
echo "Checking for view-private files..." | tee -a $outfile

cd $ADE_VIEW_ROOT        #This is to ensure that the find command returns file names starting with fusionapps/
for i in `cat $ADE_VIEW_ROOT/fusionapps/premerge/$txnFiles|cut -d/ -f1-4|sort|uniq`;do
 echo "Checking $i" | tee -a $outfile
 export files=$files" "`find $i -type f -print|grep -v "/classes/"|grep -v "\.class$"|grep -v "~$"|grep -v "#[0-9]*$"|grep -v "\.jar$"|grep -v "/jlib/"|grep -v "\.properties$"|grep -v "\.war$"|grep -v "\.lck$"|grep -v "GRABTRANS"|grep -v "\/\.\#"`
done

#if all files are from transaction, then there's no view private files and vpFiles should be empty
export vpFiles=""
for i in `echo $files`;do
        grep $i $ADE_VIEW_ROOT/fusionapps/premerge/$txnFiles >/dev/null || export vpFiles=$vpFiles" "$i
done

cd - >/dev/null         #Move back to the old directory

echo $vpFiles|grep "fusionapps/" >/dev/null
if [[ $? -eq 0 ]];then
    export input_start_time=$(date +%s);
    echo ""
    echo "*Warning - The following files are not part of the transaction, but exist as view-private. They will not be merged to ADE:" | tee -a $outfile
    print $vpFiles|sed 's/ /\n/g' | tee -a $outfile
    echo ""
    echo "Press Control-C to exit (Oh! I forgot to add them to the transaction.), or Enter to proceed (I don't need them.)."
    read -t$timeOut 
    export input_end_time=$(date +%s);
    export input_elapsed_time="$(expr $input_end_time - $input_start_time)";
else

    echo "There are no view-private files under the checked LBAs." |tee -a $outfile

fi
export vp_end_time=$(date +%s);
export vp_elapsed_time=$(($vp_end_time - $vp_start_time));

getVerInfo
if [[ $updateBug != "N" ]];then
	export bug_start_time=$(date +%s);
	setBugContext
	for bugNo in $( echo $bugList |tr ',' ' ' );do
		checkIfFusionBug
	done
	export bugNo=$prelimBug

	export bug_end_time=$(date +%s);
	export bug_elapsed_time=$(($bug_end_time - $bug_start_time))
fi # updateBug != "N"

export stop=1
if [[ $badBug -eq 0 ]];then #Don't use updateBug, let this validation happen.
	export txn_start_time=$(date +%s);
	export parentDir="bug_"`echo $bugNo|cut -c 1-$(( ${#bugNo} - 6 ))`
	export bugDir="bug"$bugNo
	export curRun=`date +%Y%m%d-%H%M%S`
	export bugftpdir="$parentDir/$bugDir/$curRun"

	echo "Checking if the transaction is saved..." 

	export savedTxns=$(ade sqlplus <<-EOF |grep "$bugNo"|sed "s/^.* //"
	set sqlprompt ' '
	set sqlnumber off
	set serveroutput on
	set feedback off
	
	variable bugNo varchar2(25);
	
	exec :bugNo :='$bugNo';
	declare
	    transactions jr_common.rm.string_coll;
            status pls_integer;
	begin
            status := jr_common.rm_common_task.get_tasks_for_bug(:bugNo,transactions);
            FOR i IN transactions.FIRST..transactions.LAST LOOP
                dbms_output.put_line(transactions(i));
            END LOOP;
	end;
	/
	EOF)

	export txnCount=`echo $savedTxns |wc -w`
	if [[ $txnCount -eq 1 ]];then
		if [[ $txn = $savedTxns ]];then
			echo "$txn is saved and there are no other transactions saved for $bugNo. Safe to proceed." |tee -a $outfile
			stop=0
		else
			echo "*Warning - $savedTxns already saved for this bug and this transaction ($txn) is not saved (or BUG_NUM property is not set correctly)." |tee -a $outfile
		fi
	elif [[ $txnCount -gt 1 ]];then
        	echo "*Warning - Multiple transactions saved for this bug ($savedTxns). It is not safe to proceed if you are on an ARU transaction patching enabled branch." |tee -a $outfile
	else
		echo "*Warning - This transaction is not saved. Please exit, save the transaction and run the tool again." |tee -a $outfile
	fi
	export txn_end_time=$(date +%s);
	export txn_elapsed_time=$(($txn_end_time - $txn_start_time))
else
	echo "*BUG_NUM/BACKPORT_BUG_NUM property is not set correctly. Will not check if the transaction is saved and the bug will not be updated." |tee -a $outfile
fi # badBug = 0

if [[ $stop -eq 1 ]];then
        export input_start_time="$(date +%s)";
	echo "Press Control-C to exit, Enter to proceed."
	read -t$timeOut 
	export input_end_time="$(date +%s)";
	test -z $input_elapsed_time && export input_elapsed_time=0
	export input_elapsed_time=$(( $input_end_time - $input_start_time + $input_elapsed_time));
fi

if [[ ! -e $FAToolsMainLatest/fatools/opensource/lib/JarJAuditFixer.jar ]];then
	export jAuditJar=`ls -rt $labelServerTop/FATOOLS_MAIN_LINUX.rdd/*/fatools/opensource/lib/JarJAuditFixer.jar | tail -1|grep JarJAuditFixer.jar`
	if [[ -n $jAuditJar ]];then
		echo "Using JAudit Jar - $jAuditJar" |tee -a $outfile
	else
		if [[ $ADE_SITE = "ade_fusion_idc" ]];then
			echo "JAudit jar not found. Using from /net/indl144121.idc.oracle.com/scratch/bomathew/JAuditJar/JarJAuditFixer.jar"  |tee -a $outfile
			export jAuditJar=/net/indl144121.idc.oracle.com/scratch/bomathew/JAuditJar/JarJAuditFixer.jar
		else
			echo "JAudit jar not found. Using from /net/adc00las.us.oracle.com/scratch/bomathew/JAuditJar/JarJAuditFixer.jar"  |tee -a $outfile
			export jAuditJar=/net/adc00las.us.oracle.com/scratch/bomathew/JAuditJar/JarJAuditFixer.jar
			export runARUValid=0
		fi
	fi
	if [[ -f $jAuditJar ]];then
		cp $jAuditJar $outdir
	else
		echo "Unable to copy JAudit jar. Please find and copy JarJAuditFixer.jar to $outdir before proceeding." |tee -a $outfile
        	echo "Press Control-C to exit, Enter to proceed."
        	read -t$timeOut 
	fi
fi


$cmd_dir/premerge.ksh
