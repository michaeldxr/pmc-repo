#!/bin/sh

# This script is owned by TA. It is part of the process to determine whether second level
# mrgreq is required.
#
# This script should be run at the beginning of premerge to clean up
# from previous runs where the view is reused, or premerge is run multiple times for the
# same transaction.
#
# It deletes $AVR/mrgreqInfo_<transaction>.xml if it exists and replaces it with a 
# default version.  This file is subsequently deleted and recreated when the create script 
# is run at the end of premerge, so if it isn't updated we know premerge never completed.
#
# It also deletes any existing instance of the build file $NDE_PRODUCT_ROOT/patchReport.xml in case the 
# incremental build fails and never overwrites it, then the create script run at the end of premerge 
# doesn't pick up results from a previous run.   

# Make sure NDE_PRODUCT_ROOT and ADE_VIEW_ROOT are set.

if [ ! -d $NDE_PRODUCT_ROOT ];
then
	echo "NDE_PRODUCT_ROOT $NDE_PRODUCT_ROOT is not set to a valid directory" 1>&2
	exit 1;
fi

if [ ! -d $ADE_VIEW_ROOT ];
then
        echo "ADE_VIEW_ROOT $ADE_VIEW_ROOT is not set to a valid directory" 1>&2
        exit 1;
fi

echo "NDE_PRODUCT_ROOT=$NDE_PRODUCT_ROOT";
#cd $NDE_PRODUCT_ROOT
#if [ ! "$?" = "0" ]; 
#then
#	echo "Cannot change directory to NDE_PRODUCT_ROOT=$NDE_PRODUCT_ROOT" 1>&2
#	exit 1
#fi

patch_report=$NDE_PRODUCT_ROOT"/patchReport.xml";
echo "Deleting $patch_report";
# if the file exists delete it
if [ -e $patch_report ];
then
	rm -f $patch_report;
	# verify that the delete succeeded, otherwise old data could cause incorrect mrgreq
	if [ ! "$?" = "0" ]; 
	then
		echo "Failed to delete patchReport.xml"
		exit 1;
	fi
fi

#cd $ADE_VIEW_ROOT;
# Get the transaction name and remove whitespace
#var2=$(ade describetrans | grep "TRANSACTION:" | cut -d":" -f2)
#transaction_name=$(sed -e 's/^[[:space:]]*//' <<<"$var2");
#echo "The current transaction name is "$transaction_name;

# premerge should export vatiable $txn with the transaction name
if [ -z $txn ];
then
        echo "variable txn not set"
        exit 1;
fi

echo "The current transaction name is "$txn;

# This files should only exist in the local view so should be updateable
mrgreq_info=$ADE_VIEW_ROOT"/mrgreqInfo_"$txn".xml";
echo "Deleting $mrgreq_info";
# if the file exists delete it
if [ -e $mrgreq_info ];
then
	rm -f $mrgreq_info;
	# verify that the delete suceeded, otherwise old data could cause incorrect mrgreq
	if [ ! "$?" = "0" ]; 
	then
		echo "Failed to delete $mrgreqInfo"
		exit 1;
	fi
fi

# Now re-create the mrgReqInfo.xml just in case the premerge fails and createMrgreqInfo.sh
# never gets run.  The <PremergeResult> tag is an audit trail so we know how far it got.
# Default to second level approval required in this case.
echo "Recreating $mrgreq_info";
touch $mrgreq_info 1>&2;
if [ ! "$?" = "0" ];
then
        echo "Cannot create file $mrgreq_info"
        exit 1
fi

# Write output to the file
echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > $mrgreq_info;
echo "<MrgreqInfo>" >> $mrgreq_info;
echo -e "\t<PremergeResult>deleteMrgreqInfo.sh: Premerge Started</PremergeResult>" >> $mrgreq_info;
echo -e "\t<SecondLevelApprover>FAMRGREQ</SecondLevelApprover>" >> $mrgreq_info;
echo -e "\t<SecondLevelMrgReq>SECOND_LEVEL_APPROVAL_REQUIRED</SecondLevelMrgReq>" >> $mrgreq_info;
echo "</MrgreqInfo>" >> $mrgreq_info;

exit 0;
