#!/bin/sh
#
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/finpsrcheck.sh /main/5 2013/07/17 11:08:37 sudgupta Exp $
#
# finpsrcheck.sh
#
# Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
#
#    NAME
#      finpsrcheck.sh - Checks for any PSR violations
#
#    DESCRIPTION
#      <short description of component this file declares/defines>
#
#    NOTES
#      <other useful comments, qualifications, etc.>
#
#    MODIFIED   (MM/DD/YY)
#    vtkrishn    01/24/11 - Creation
#
#!/bin/sh
#set -x

dir=$destdir
psrcheckeroutfileformail="$dir"/"$txn"_psrcheck.out
exitstatus=0

echo -e "PSR Checker Output" > $psrcheckeroutfileformail
echo -e "===========================" >> $psrcheckeroutfileformail
echo -e "Checking for getEstimatedRowCount,getCappedRowCount and getRowCount in java files" >> $psrcheckeroutfileformail
echo -e "" >> $psrcheckeroutfileformail

	javaOutput=`ade describetrans | grep -i java | sed 's/ //g' | sort | uniq | cut -f 2- -d'/'`
for i in $javaOutput
	do
			echo -e "Checking in " `echo $ADE_VIEW_ROOT/fusionapps/$i` >> $psrcheckeroutfileformail
			echo -e "" >> $psrcheckeroutfileformail
			echo -e "getEstimatedRowCount ::" `grep -n getEstimatedRowCount $ADE_VIEW_ROOT/fusionapps/$i` >> $psrcheckeroutfileformail
			echo -e "getCappedRowCount ::"`grep -n getCappedRowCount $ADE_VIEW_ROOT/fusionapps/$i` >> $psrcheckeroutfileformail
			echo -e "getRowCount ::"`grep -n getRowCount $ADE_VIEW_ROOT/fusionapps/$i` >> $psrcheckeroutfileformail
			echo -e "" >> $psrcheckeroutfileformail
	done

echo -e "Checking for SELECT COUNT(1),VO Fetchsize=-1,QueryHint="" in xml files" >> $psrcheckeroutfileformail
echo -e "" >> $psrcheckeroutfileformail

	xmlOutput=`ade describetrans | grep -i xml | sed 's/ //g' | sort | uniq | cut -f 2- -d'/'`
for i in $xmlOutput
	do
			echo -e "Checking in " `echo $ADE_VIEW_ROOT/fusionapps/$i` >> $psrcheckeroutfileformail
			echo -e "" >> $psrcheckeroutfileformail
			echo -e "SELECT COUNT(1) ::" `grep -in "select count(1)" $ADE_VIEW_ROOT/fusionapps/$i` >> $psrcheckeroutfileformail
			echo -e "VO Fetchsize=-1 ::" `grep -in 'FetchSize="-1"' $ADE_VIEW_ROOT/fusionapps/$i` >> $psrcheckeroutfileformail
			echo -e "QueryHint="" ::" `grep -in 'QueryHint=""' $ADE_VIEW_ROOT/fusionapps/$i` >> $psrcheckeroutfileformail
			echo -e "" >> $psrcheckeroutfileformail
	done

exit $exitstatus;
