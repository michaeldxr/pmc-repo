#!/bin/sh

##################################################################################################
# Purpose: Script to run UTPLSQLs of all the modified PLSQL APIs', as part of fin_premerge.ksh.
#
# Revision:
#     Sep-2011   Created                                                 R. Suman Kumar
#     Jan-2012   Bugs 13507270 , 133990331       Script was not considering cases mentioned in these bugs
##################################################################################################

if [ $debug -eq 1 ];then
        set -x
fi

exitStatus=0

#if [ ! -d $destdir ] ; then
destdir=$ADE_VIEW_ROOT/fusionapps/premerge
#fi

dbfiles=$destdir/db_files.txt
txn_files=$destdir/ade_txn_files.txt
sql_files=$destdir/sql_files.txt

if [ -f $dbfiles ] ; then
rm $dbfiles
fi

cat $destdir/$txnDescFile > $txn_files

if [ ! -n $txn ] ; then
logfile_name=`grep "^TRANSACTION" $txn_files | cut -d' ' -f2`
else
logfile_name=$txn
fi

ut="_utplsql.log"
logfile=$destdir/"$logfile_name""$ut"
if [ -f $logfile ] ; then
rm $logfile
fi


out="_utplsql.out"
outfile=$destdir/"$logfile_name""$out"
if [ -f $outfile ] ; then
rm $outfile
fi

echo "UTPLSQL Scan Output" >> $outfile
echo "==================================================" >> $outfile
echo " " >> $outfile


if [ ! -s $txn_files ] ; then
  echo "***Error occured while reading files from ade txn. Please ensure you are in ADE view and transaction is saved. Terminating.." |tee -a $outfile
  exitStatus=1
fi


#################### BUGS  13507270, 133990331 ###################################################

if [ "$exitStatus" = "0" ] ; then
cat $txn_files | grep "\.pk" | sed 's/^.*[0-9]*.*\(fusionapps\/.*\)$/\$ADE_VIEW_ROOT\/\1/' | cut -d' ' -f1 > $sql_files

if [ -s $sql_files ] ; then
exitStatus=0
else
exitStatus=3
fi

fi

if [ "$exitStatus" = "0" ] ; then
cat $sql_files | sed 's/ //g' | sort -u > $sql_files

echo "Checking if identified sql files are eligible for UTPLSQL test" |tee -a $outfile
final_txn_files=$destdir/final_ade_txn_files.txt

while read line
do
file_present=y
echo $line | grep -cm1 "\.pkh" > /dev/null
result=$?
if [ "$result" = "0" ] ; then
extn=".pkb"
str=`echo $line | cut -d'.' -f1`
eval lookfor=$str$extn
file_present=n

if [ -f $lookfor ] ; then
file_present="y"
else
cat $sql_files | grep $str$extn > /dev/null
result=$?
    if [ "$result" = "0" ] ; then
        file_present=y
    fi
fi

fi

#After file check, checking if its UT api is present

if [ "$file_present" = "y" ] ; then
ut_api_for=`basename $line`
ut_api_dir=`echo $line | sed s/$ut_api_for//`
  ut="ut_"
  test="test"
eval ut_api=$ut_api_dir/$test/$ut$ut_api_for

if [ -f $ut_api ] ; then
 echo $line >> $final_txn_files
else
      cat $txn_files | grep $ut$ut_api_for > /dev/null 
      result=$?
      if [ "$result" = "0" ] ; then
          $line >> $final_txn_files
      fi   
   fi
fi

done< $sql_files

if [ ! -f $final_txn_files ] ; then
     echo "Either there no package body files for given header files in txn or these header files do not have corresponding UT files. Skipping UTPLSQL tests" |tee -a $outfile
     exitStatus=3
fi

fi


#################### BUGS  13507270, 133990331 ###################################################


if [ "$exitStatus" = "0" ] ; then
rm $sql_files
echo  "*** Checking for plsql files in the ade transaction"
cat $final_txn_files | grep "\.pk" | sed 's/^.*[0-9]*.*\(fusionapps.*\)$/\$ADE_VIEW_ROOT\/\1/' > $sql_files

if [ -s $sql_files ] ; then
cat $sql_files | cut -d'.' -f1 | sort -u > $sql_files

echo "*** PLSQL file(s) found. PLEASE NOTE: Following package(s), both header and body will be applied to the Junit DB even though either of header/body may not be modified in current ADE transaction."
echo
echo "*** Other dependencies of these packages such as other APIs, tables, views etc WILL NOT BE MODIFIED AND READ AS IS from the db."
   echo 
cat $sql_files
   #echo
   #echo "*** confirm to proceed [y/n]: " 
   #read proceed
else
   echo "*** No plsql files found. Skipping UTPLSQL check"
   echo "*** No plsql files found. Skipping UTPLSQL check" >> $outfile
   echo
   exitStatus=3
fi   


if [ "$exitStatus" = "0" ] ; then

if [ -n "$updateDb" ] ; then
proceed=$updateDb
else
proceed="y"
fi

if [ "$proceed" != "y" ] && [ "$proceed" != "Y" ] && [ "$proceed" != "YES" ] && [ "$proceed" != "Yes" ] && [ "$proceed" != "yes" ] ; then
   exit 0
fi



###### test block #############
#sid=in64017
#host=indl64017.idc.oracle.com
#port=1522
###### test block #############


if [ -z $sid ] ; then
   echo "*** Could not read DB information. Please make sure ".JUnit.db" file is present in $HOME and has intended DB info" >> $outfile
   exitStatus=1
fi


if [ "$exitStatus" = "0" ] ; then

echo "*** These checks will be run on the database: $sid"
echo

connectStr="(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=$host)(PORT=$port))(CONNECT_DATA=(SERVICE_NAME=$sid)))"




###### START LOOP TO APPLY ALL THE IMPACTED PACKAGES ################################
while read line
do
if [ "$exitStatus" = 0 ] ; then

extension=".pkh"
applying_file=$line$extension
   echo "*** Applying file: " $applying_file". Please wait.." 
   echo
sqlplus fusion/fusion@$connectStr @$applying_file <<EOF > $dbfiles   
EOF
grep -cm1 "Package created\." $dbfiles > /dev/null
result=$?
if [ "$result" = "0" ] ; then
echo "Package compilation successful."
echo
else
echo "*** Package: $applying_file compiled with errors. Please fix the same before proceeding. Terminating UTPLSQL checks."
echo "*** Package: $applying_file compiled with errors. Please fix the same before proceeding. Terminating UTPLSQL checks." >> $outfile
cat $dbfiles >> $outfile
echo
echo "*** Details of the error can be accessed from: $outfile"
exitStatus=1
fi


if [ "$exitStatus" = 0 ] ; then
extension=".pkb"
applying_file=$line$extension
  echo "*** Applying file: " $applying_file". Please wait.." 

sqlplus fusion/fusion@$connectStr @$applying_file <<EOF > $dbfiles
EOF
grep -cm1 "Package body created\." $dbfiles > /dev/null
result=$?
if [ "$result" = "0" ]; then
echo "Package body compliation successful."
else
       echo "*** Package body: $applying_file compiled with errors. Please fix the same before proceeding. Terminating UTPLSQL checks."
       echo "*** Package body: $applying_file compiled with errors. Please fix the same before proceeding. Terminating UTPLSQL checks." >> $outfile
       echo
cat $dbfiles >> $outfile
echo
echo "*** Details of the error can be accessed from: $outfile"
exitStatus=1    
fi

fi
fi

done < $sql_files

if [ $? -gt 0 ] ; then
  echo "***Error occured while applying packages to DB. Details of errors can be found at: $outfile. Terminating UTPLSQL checks."
  exitStatus=1
fi
###### END LOOP TO APPLY ALL THE IMPACTED PACKAGES ################################


if [ "$exitStatus" = "0" ] ; then

# Modfiying /tmp/sql_files.txt to remove UT_* packages from the list because there are no UTPLSQLs for UT api's themselves.
#However such UT_ pkg changes will be applied to the DB (in the above loop)
cat $sql_files | xargs -iS basename S | cut -d'.' -f1 | sort -u > $sql_files
cat $sql_files | sed '/^ut_/d' | cut -d'.' -f1 | sort -u > $sql_files

######### START LOOP TO EXECUTE THE UTPLSQLs of all the identified packages################################
while read line
do

echo "*** Running UTPLSQL test cases of: $line. Please wait .."
sqlplus -s fusion/fusion@$connectStr <<EOF > $dbfiles 
SET SERVEROUTPUT ON;
EXECUTE UTPLSQL.TEST('$line');
EOF

grep -cm1 "FAILURE" $dbfiles > /dev/null
result=$?
if [ "$result" = "0" ]; then
resultStr="Failed"
else
resultStr="Success"
fi
echo "*** UTPLSQL test result for:$line is: $resultStr" 
echo 

cat $dbfiles >> $logfile
done < $sql_files

if [ $? -gt 0 ] ; then
  echo "***Error occured while running UTPLSQLs. Terminating.."
  echo "***Error occured while running UTPLSQLs. Terminating.." >> $outfile
  exitStatus=1
fi
######### END LOOP TO EXECUTE THE UTPLSQLs of all the identified packages################################


fi
fi  
fi
fi

#Deleting the intermittent files 
if [ -f $sql_files ] ; then
rm $sql_files
fi
if [ -f $txn_files ] ; then
rm $txn_files
fi
if [ -f $final_txn_files ] ; then
rm $final_txn_files
fi
if [ -f $dbfiles ] ; then
rm $dbfiles
fi
#Deciding on the Overall status

Overall_resultStr="No UTPLSQLs tested"

if [ -f $logfile ] ; then
grep -cm1 "FAILURE" $logfile > /dev/null
read=$?
if [ "$read" = "0" ] ; then
Overall_resultStr="Failed"
  cat $logfile | awk '/^FAILURE -/,/>/' > $outfile
echo
echo "*** Details of all the failed cases can be acessed from: $outfile"
echo
else
grep -cm1 "SUCCESS" $logfile > /dev/null
read=$?
 if [ "$read" = "0" ] ; then
 	Overall_resultStr="Success"
 fi
fi
fi

echo "*** OVERALL UTPLSQL TEST RESULT IS: $Overall_resultStr"
echo "*** OVERALL UTPLSQL TEST RESULT IS: $Overall_resultStr" >> $outfile
echo

if [ -f $logfile ]; then
echo "*** Detailed report can be accessed from: $logfile"
echo "*** Detailed report can be accessed from: $logfile"  >> $outfile
fi

echo
echo "*** Output file can be accessed from: $outfile"
echo "*** bye !"
echo

exit $exitStatus




