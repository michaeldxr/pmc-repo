#!/bin/sh

####################################################################################
# Purpose: Script to check Resource Type violation jazn-data files which is run 
#          as part of fin_premerge.ksh
#
# Revision:
#     May-2011   Created                                                 Harikiran
#                                  
####################################################################################

#For independent script testing purposes
if [ $USER == "hkaniven" -a -z "$destdir" ];
then

  set -x
  mkdir -p $ADE_VIEW_ROOT/fusionapps/premerge
  destdir=$ADE_VIEW_ROOT/fusionapps/premerge
  txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
  family=`ade describetrans | grep fusionapps | sed 's/ //g' | cut -f 2 -d '/' | sort | uniq`
  #family=fin
  
fi  

tstart_jrtc="$(date +%s)"

jaznresourcetypecheckeroutfileformail="$destdir"/"$txn"_jaznresourcetypechecker.out
logfile="$destdir"/"$txn"_jaznresourcetypechecker.log
exitStatus=0

echo -e "" > $jaznresourcetypecheckeroutfileformail
echo -e "Jazn Resource Type Checker Output" >> $jaznresourcetypecheckeroutfileformail
echo -e "=================================" >> $jaznresourcetypecheckeroutfileformail

jazndataoutput=`ade describetrans | grep  'jazn-data.xml' | grep fusionapps/$family/components | sed 's/ //g' | cut -f 2- -d '/' | sort | uniq`
echo $jazndataoutput

if [ -z "$jazndataoutput" ];
then

  echo -e "$txn does not contain any jazn-data files so skipping Jazn Resource Type Checker" >> $jaznresourcetypecheckeroutfileformail
  exitstatus=3

else

  echo -e "Running Jazn Resource Type Checker as jazn-data.xml is/are found in the transaction $txn" >> $jaznresourcetypecheckeroutfileformail
  echo -e "" >> $jaznresourcetypecheckeroutfileformail  
  echo -e "Found these jazn-data files" >> $jaznresourcetypecheckeroutfileformail

  for i in $jazndataoutput
  do

    echo -e "" `echo $ADE_VIEW_ROOT/fusionapps/$i` >> $jaznresourcetypecheckeroutfileformail
    echo -e "" >> $jaznresourcetypecheckeroutfileformail

    mv $ADE_VIEW_ROOT/fusionapps/build_metadata/validation_tests/ResourcesTypesValidationOutput.xml $ADE_VIEW_ROOT/fusionapps/build_metadata/validation_tests/ResourcesTypesValidationOutput.xml_old
    
    ant -f $ADE_VIEW_ROOT/fabuildtools/ant/drivers/build-ldap-migration.xml resourcesChecker -Djaznfile=$ADE_VIEW_ROOT/fusionapps/$i -Dvalidate=true > $logfile 
    
    if [ ! -f $ADE_VIEW_ROOT/fusionapps/build_metadata/validation_tests/ResourcesTypesValidationOutput.xml ]
    then

      echo -e "The tool was not able to run Jazn Resource Type Checker for file $ADE_VIEW_ROOT/fusionapps/$i as the ant command running Jazn Resource Type Checker is not working correctly." >> $jaznresourcetypecheckeroutfileformail

      if [[ $exitStatus -eq 0 ]];then

        exitStatus=2

      fi

    else  
    
      if ` grep -q "resourceType " $ADE_VIEW_ROOT/fusionapps/build_metadata/validation_tests/ResourcesTypesValidationOutput.xml `
      then

        echo -e "Jazn Resource Type Checker failed for file $ADE_VIEW_ROOT/fusionapps/$i as invalid Resource Types were found." >> $jaznresourcetypecheckeroutfileformail
         grep "resourceType " $ADE_VIEW_ROOT/fusionapps/build_metadata/validation_tests/ResourcesTypesValidationOutput.xml >> $jaznresourcetypecheckeroutfileformail 
        exitstatus=1
        
      fi  
        
    fi  
    
  done    

  if [[ $exitStatus -eq 0 ]];then
  
    echo -e "No Jazn Resource Type violations found for jazn-data files involved in the transaction $txn" >> $jaznresourcetypecheckeroutfileformail
    echo -e "" >> $jaznresourcetypecheckeroutfileformail    
  
  fi

  echo -e "Running Jazn Resource Type Checker completed for jazn-data files involved in the transaction $txn" >> $jaznresourcetypecheckeroutfileformail
  
    
fi

echo -e "" >> $jaznresourcetypecheckeroutfileformail

tstop_jrtc="$(date +%s)"
telapsed_jrtc="$(expr $tstop_jrtc - $tstart_jrtc)"

echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_jrtc sec" +%H:%M:%S)" >> $jaznresourcetypecheckeroutfileformail

exit $exitStatus


