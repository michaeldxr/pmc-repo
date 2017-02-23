#!/bin/sh

####################################################################################
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/finbackportsupport.sh /main/20 2011/07/29 18:02:51 hkaniven Exp $
# Purpose: Script to provide back port support for developers 
#
# Revision:
#     Jul-2011   Created                                                 Harikiran
#                                  
####################################################################################


cmd_dir=$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/FinPreMerge/bin
cmd_this=$0
BranchesFile=$cmd_dir/finbackportbrancheslist.txt

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

exitStatus=0

while getopts D: opt
do	case "$opt" in
	D)	ParseKeyValue "$OPTARG";;
	:)	if [[ $OPTARG = "D" ]];then
			exitStatus=2;
		fi;;
	*)	exitStatus=3;;
	esac
done

if [ "$debug" == 1 ]
then

  set -x

fi

if [[ $exitStatus -gt 0 ]];then
	
	exit $exitStatus;

fi


if [ -z "$family" ] 
then

	family=`ade describetrans $txn | grep fusionapps | sed 's/ //g' | cut -f 2 -d '/' | sort | uniq`
	
	if [ -z "$family" ] 
	then
	
		echo -e "Unable to get the family. Cannot proceed."
		exit;
	
	fi
	
fi	

echo -e ""
echo -e ""
echo -e "Back port Support tool"
echo -e "======================"

while  [ -z "$txn" ] 
do

	echo -e "Enter transaction name for which back port bugs have to be created"
	read txn
	
done

bugNo=`echo $txn | tr -d "[a-z]" | cut -d\- -f2 | cut -d. -f1 | cut -d- -f1 | cut -d_ -f1`

echo $bugNo | grep -wq "[0-9][0-9]*"

if [[ $? -ne 0 ]];then

	echo -e "Unable to get the bug no from the transaction $txn. Cannot proceed." 
	exit;

fi

mkdir -p $ADE_VIEW_ROOT/fusionapps/backport/$txn
out_dir=$ADE_VIEW_ROOT/fusionapps/backport/$txn
TransDiffOutputFile=$out_dir/transdiffoutput.txt
BackportBugsBranchInfoFile=$out_dir/BackportBugsBranchInfo.txt
BugNosInfoFile=$out_dir/BugNosInfoFile.txt

echo -e ""
echo -e "Creating Back port bugs for the transaction $txn and base bug $bugNo"
echo -e ""

if [ -z "$backportBranches" ]
then

	baseBugBranchName=`ade describetrans $txn | grep BASE_LABEL | cut -d ":" -f 2 | cut -f 1-3 -d "_" | tr -d " "`
	#baseBugBranchName=FUSIONAPPS_MAIN_LINUX
	#source $BranchesFile
	#backportBranchestmp="echo \$$baseBugBranchName"
	#backportBranches=`eval $backportBranchestmp`

	backportBranches=`grep -i $family"_"$baseBugBranchName $BranchesFile | cut -d "=" -f 2`
	echo $backportBranches

	if [ -z "$backportBranches" ]
	then
	
		echo -e "No backport branches defined for branch name $baseBugBranchName in $BranchesFile"
		exit;

	fi
	
fi	

echo -e "Back port bugs will be created for branches $backportBranches. Press Enter to continue or Ctrl-C to exit"
read

while  [ -z "$bugUid" ] 
do

	echo -e "Enter bug username using which the back port bugs have to be created"
	read bugUid

done

while  [ -z "$bugPasswd" ] 
do

	echo -e "Enter password for bug username $bugUid"
	stty -echo
	read bugPasswd
	stty echo

done

connectStr='(DESCRIPTION= (ADDRESS_LIST= (LOAD_BALANCE=ON) (ADDRESS=(PROTOCOL=tcp)(HOST=apd211-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd211-crs.us.oracle.com)(PORT=1522)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd212-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd212-crs.us.oracle.com)(PORT=1522)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd213-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd213-crs.us.oracle.com)(PORT=1522)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd214-crs.us.oracle.com)(PORT=1521)) (ADDRESS=(PROTOCOL=tcp)(HOST=apd214-crs.us.oracle.com)(PORT=1522)) ) (CONNECT_DATA=(SERVICE_NAME=bugap.us.oracle.com)))'


for branch in `echo $backportBranches | tr ',' '\n'`
do

	echo -e "Creating bug for branch $branch"
	fusionSuiteId=23;
	
	sqlplus -s  /nolog <<-EOF
	connect $bugUid/$bugPasswd@"$connectStr" 
        
        SPOOL $BackportBugsBranchInfoFile
	SET SERVEROUTPUT ON
	DECLARE

	bugNumber NUMBER;
	l_productId NUMBER(4);
	l_component VARCHAR2(10);
	l_subject VARCHAR2(80);
	bugErrorCode NUMBER;
	bugErrorMessage VARCHAR2(10000); 
	bugProductID NUMBER;
	l_assignee VARCHAR2(10);
	
	BEGIN
	
	  BEGIN
	  
	  	IF nvl('$assignee', NULL) IS NOT NULL THEN 
	  	
	  		SELECT username
	  		into l_assignee
	  		FROM all_users
	  		WHERE username = UPPER('$assignee');
	  		
		ELSE
		
			l_assignee:='$bugUid';	  		
	  	
	  	END IF;


	  EXCEPTION

        	WHEN NO_DATA_FOUND then
                	dbms_output.put_line('Invalid assignee $assignee');
                	RETURN;
	  END;
	
	  
	  BEGIN
	  
	        SELECT h.product_id, h.category, h.subject
	        INTO l_productId, l_component, l_subject
	        FROM rpthead h
	        WHERE h.rptno=$bugNo
                AND product_id IN (SELECT product_id 
                		   FROM product_suites, product_groups, product 
                		   WHERE product_suites.id=$fusionSuiteId 
                		   	 AND product_groups.product_suite_id = product_suites.id 
	              		         AND product.PRODUCT_GROUP_ID=product_groups.PRODGROUP_ID);
	              		         
	  EXCEPTION

        	WHEN NO_DATA_FOUND then
                	dbms_output.put_line('Bug doesnt belong to Fusion product');
                	RETURN;
        	WHEN OTHERS THEN 
                	dbms_output.put_line('Bug product could not be ascertained');
                	RETURN;
	  END;
	              		         
                	
          -- Create bug	
 	  bug.bug_api_v1.CREATE_BUG
				(p_rptno                    => bugNumber       
				,p_product_id               => l_productId                
				,p_component                => l_component
				,p_utility_version          => '11.1.1.5.0'       
				,p_status                   => 11
				,p_genport                  => 'G'            
				,p_relstat                  => 'D'             
				,p_version                  => '10.2.0.4'
				,p_product_line_id          => 289               
				,p_os_version               => NULL             
				,p_release_id               => 556             
				,p_release                  => NULL
				,p_cs_priority              => 2                
				,p_enh_desirability         => NULL              
				,p_support_rep              => '$bugUid'
				,p_programmer               => l_assignee
				,p_customer                 => 'INTERNAL -- FUSIONAPPS'
				,p_abstract                 => 'Back port bug for $bugNo for branch $branch'     
				,p_base_rptno               => $bugNo
				,p_error_code               => BugErrorCode
				,p_error_mesg               => bugErrorMessage 
				); 
				
	  IF bugErrorCode IS NULL THEN

		   bug.bug_api.create_bug_text
		   			(p_rptno	=> bugNumber
		   			,p_text		=> 'Back port Bug created by Back port Bug Support Tool v1.0' || chr(10) || '--------------------------------------------------------' || chr(10) || l_subject || chr(10)
		   			,p_line_type	=> 'P'
					,p_error_code   => BugErrorCode
					,p_error_mesg   => bugErrorMessage 
					,p_hide		=> 'Y'
					); 
		   
		   bug.bug_api.create_bug_line
		   			(p_rptno	=> $bugNo
		   			,p_comments	=> 'Back port bug ' || bugNumber || ' created for branch $branch'
		   			,p_line_type	=> 'P'
					,p_error_code   => BugErrorCode
					,p_error_mesg   => bugErrorMessage 
					); 

		   bug.bug_tags_api.create_or_append_tag
		   			(p_rptno	=> bugNumber
		   			,p_tag		=> 'BACKPORT BACKPORTSUPPORTTOOLv1.0' 
					,p_error_code   => BugErrorCode
					,p_error_mesg   => bugErrorMessage 
					); 
					

		   COMMIT;
		   
		   dbms_output.put_line('Bug ' || bugNumber || ' has been created for branch $branch' ); 
		   
	  ELSE

		   dbms_output.put_line('Error code for CREATE_BUG: ' || bugErrorCode); 
		   dbms_output.put_line('Error message for CREATE_BUG: ' || bugErrorMessage);    
		   
		   ROLLBACK;
		   
	  END IF;
		
	END;
	/
	SPOOL OFF
	EOF

done;

echo -e "Back port bugs creation for transaction $txn and base bug $bugNo completed."

if [[ $createBackportTrans -eq 1 ]];then

        grep 'has been created' $BackportBugsBranchInfoFile | cut -f 2 -d " " > $BugNosInfoFile
        
        for backportBugNo in `cat $BugNosInfoFile`
        do
        
		backportBranch=`grep $backportBugNo $BackportBugsBranchInfoFile | cut -f 8 -d " "`;
		newViewName=$backportBranch"_"$txn"_view";
		
		ade createview -series $backportBranch -latest $newViewName -force;
#		ade useview $newViewName -exec "ade begintrans -bug $backportBugNo -no_restore";
		
	done;		
 
fi

if [[ $generateDiffOutput -eq 1 ]];then

	echo -e ""
	echo -e "Generating diff output for the files involved in the transaction $txn"
	echo -e ""

	#adeviewroot=`echo $ADE_VIEW_ROOT | sed 's/\//\\\\\//g'`
	#desctranscmd="ade describetrans | grep fusionapps/$family/components | sed 's/ //g' | cut -f 2- -d "." | sort | uniq | sed 's/fusionapps/$adeviewroot\/fusionapps/'"
	desctranscmd="ade describetrans -long $txn | grep '@@/main' | sed 's/ /;/g'"
	#echo $desctranscmd
	desctransoutput=`eval $desctranscmd`
	#echo $desctransoutput

	if [ -z "$desctransoutput" ]
	then

		echo -e "Transaction $txn is not merged so diff output will not be generated" 

	else

		echo -e "Diff Output for files involved in transaction $txn" > $TransDiffOutputFile
		echo -e "==================================================" >> $TransDiffOutputFile
		echo -e "" >> $TransDiffOutputFile

		for filename in $desctransoutput
		do

		        mergedBranchWithVersion=`echo $filename | cut -d ';' -f 3` 
		        mergedBranch=`dirname $mergedBranchWithVersion`
			currentVersion=`basename $mergedBranchWithVersion`
			previousVersion=$(( $currentVersion - 1 ))
			filenameWithoutVersion=`echo $filename | cut -d "@" -f 1`
			filenameWithPreviousVersion=$filenameWithoutVersion"@@"$mergedBranch"/"$previousVersion
			filenameWithCurrentVersion=$filenameWithoutVersion"@@"$mergedBranch"/"$currentVersion

	        	fullfilenameWithPreviousVersion=$ADE_VIEW_ROOT"/"$filenameWithPreviousVersion
			fullfilenameWithCurrentVersion=$ADE_VIEW_ROOT"/"$filenameWithCurrentVersion
		
			ade diff $fullfilenameWithCurrentVersion $fullfilenameWithPreviousVersion | tee -a $TransDiffOutputFile > $out_dir/`basename \`echo $filename | cut -d "@" -f 1\``".diff"
	
			echo -e "" >> $TransDiffOutputFile
			echo -e "" >> $TransDiffOutputFile	
		        echo -e "---------------------------------------------------------------" >> $TransDiffOutputFile
			echo -e "" >> $TransDiffOutputFile
		  	echo -e "" >> $TransDiffOutputFile
	  	
			if `grep -q 'ade ERROR: File does not exist' $TransDiffOutputFile`
			then
		
				echo -e "The tool was not able to generate the diff output as the files included in transaction $txn is not there in the current view"
		
			fi
  	
		done

		echo -e "Diff Output of files involved in the transaction $txn written to $TransDiffOutputFile"
		echo -e ""
	
	fi	

fi

echo -e ""
exit $exitStatus;


