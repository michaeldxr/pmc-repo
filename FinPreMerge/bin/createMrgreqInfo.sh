#!/bin/sh

# This script is owned by TA. It is part of the process to determine whether second level
# mrgreq is required.
#
# This script should be run at the end of premerge to create mrgreqInfo_<transaction>.xml using the
# second level mrgreq tag in patchReport.xml.  patchReport.xml is created by the build, and the backward 
# compatibility pcl injects custom elements with the validation results and second level mrgreq requirements.
#
# In the future the script will be extended to describe the transaction, parse the list of artifacts, and identify 
# data model objects, jazn files, and anything else not covered by the build pcl framework, but for now
# the existing pattern match will be retained in mrgreq.
#
# $ADE_VIEW_ROOT/mrgreqInfo_<transaction>.xml will be read by Mrgreq. Mrgreq will look at the 
# SecondLevelMrgReq tag to tell if second level approval is required.  Initially mrgreq will retain
# the existing file pattern matching algorithm to trigger second level mrgreq, and the flag in this 
# file will supplement the pattern match.  So if either (or both) indicate second level approval is 
# required, second level mrgreq will be enabled.
#
# For now the plan is to trigger second level mrgreq if is there is a problem, or premerge is skipped.
# The <PremergeResult> tag in mrgreqInfo*.xml provides  an audit trail to help understand where a failure occurs.
# 
# Eventually mrgreqInfo will be extended to include an artifact level audit trail, and this will be 
# uploaded into the mrgreq system so should be availble to the approver.
#
# Also the second level approver in mrgreqInfo will eventually be used by mrgreq to route second
# level approvals.

if [ ! -d $NDE_PRODUCT_ROOT ];
then
        echo "NDE_PRODUCT_ROOT $NDE_PRODUCT_ROOT is not set to a valid directory"
        exit 1;
fi

if [ ! -d $ADE_VIEW_ROOT ];
then
        echo "ADE_VIEW_ROOT $ADE_VIEW_ROOT is not set to a valid directory" 1>&2
        exit 1;
fi

echo "NDE_PRODUCT_ROOT=$NDE_PRODUCT_ROOT";

#cd $ADE_VIEW_ROOT;
#if [ ! "$?" = "0" ];
#then
#        echo "Cannot change directory to ADE_VIEW_ROOT=$ADE_VIEW_ROOT"
#        exit 1
#fi

# Get the transaction name and remove whitespace
#var2=$(ade describetrans | grep "TRANSACTION:" | cut -d":" -f2)
#transaction_name=$(sed -e 's/^[[:space:]]*//' <<<"$var2");
#echo "The current transaction name is "$transaction_name;

# premerge should export variable $txn with the transaction name
if [ -z $txn ];
then
	echo "variable txn is not set"
	exit 1;
fi

echo "The current transaction name is "$txn;

# This files should only exist in the local view so should be updateable
mrgreq_info=$ADE_VIEW_ROOT"/mrgreqInfo_"$txn".xml";
echo "creating $mrgreq_info";

# If the file already exists delete it.  It should exist as the delete script creates it with
# default values at the start of premerge in case there is a premerge failure.  
if [ -e $mrgreq_info ];
then
        rm -f $mrgreq_info;
        # verify that the delete suceeded
        if [ ! "$?" = "0" ];
        then
                echo "Failed to delete existing mrgReqInfo.xml"
                exit 1;
        fi
fi

# Now create the new mrgReqInfo_<transaction>.xml with results from patchReport.xml
touch $mrgreq_info 1>&2;
if [ ! "$?" = "0" ];
then
        echo "Cannot create file $mrgreq_info"
        exit 1
fi



patch_report=$NDE_PRODUCT_ROOT"/patchReport.xml";

# 2nd level mrgreq approval required is the default, unless explicitly overridden.
# MUST KEEP THIS IN SYNC WITH backardIncompatibleListener.java output

# file exists and is readable
if [ -r $patch_report ];
then
	mrgreq_approval=$(echo $patch_report | xargs grep "SECOND_LEVEL_APPROVAL");
	# grep return code will be 0 if a match was found
	if [ "$?" = "0" ];
	then
		premerge_result="Mergreq approval extracted from patchReport.xml";
	else
		mrgreq_approval="SECOND_LEVEL_APPROVAL_REQUIRED";
		premerge_result="patchReport.xml exists but does not contain SecondLevelMrgReq tag";
	fi
else 
  # see bug 21629617 - second level approval not required if incremental build was skipped
	incrbldout=$ADE_VIEW_ROOT"/fusionapps/premerge/"$txn"_incrbld.out";
	if [ -r $incrbldout ];
	then
		grep -qe "No files involved in the transaction .* requires incremental build to be run, so skipping it" $incrbldout
		if [ "$?" = "0" ]
		then 
			mrgreq_approval="SECOND_LEVEL_APPROVAL_NOT_REQUIRED";
			premerge_result="patchReport.xml does not exist, incremental build was skipped";
		else
			mrgreq_approval="SECOND_LEVEL_APPROVAL_REQUIRED";
      premerge_result="patchReport.xml does not exist, incremental build was performed";
		fi
	else
		mrgreq_approval="SECOND_LEVEL_APPROVAL_REQUIRED";
		premerge_result="patchReport.xml does not exist";
	fi
fi
echo $premerge_result;

# Write oupput to the file
echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > $mrgreq_info;
echo "<MrgreqInfo>" >> $mrgreq_info;
echo -e "\t<PremergeResult>$premerge_result</PremergeResult>" >> $mrgreq_info;
echo -e "\t<SecondLevelApprover>FAMRGREQ</SecondLevelApprover>" >> $mrgreq_info;

# Explicitly test for NOT REQUIRED, so it defaults to REQUIRED if there is a problem.
echo $mrgreq_approval | grep "SECOND_LEVEL_APPROVAL_NOT_REQUIRED" 
if [ "$?" = "0" ];
then
	echo "setting APPROVAL NOT REQUIRED";
	echo -e "\t<SecondLevelMrgReq>SECOND_LEVEL_APPROVAL_NOT_REQUIRED</SecondLevelMrgReq>" >> $mrgreq_info;
else
	echo "setting APPROVAL REQUIRED";
	echo -e "\t<SecondLevelMrgReq>SECOND_LEVEL_APPROVAL_REQUIRED</SecondLevelMrgReq>" >> $mrgreq_info;
fi
echo "</MrgreqInfo>" >> $mrgreq_info;

exit 0;
