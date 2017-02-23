#!/bin/sh
################################################
# Purpose: Checks hardcoded strings in UI files that are not sourced
#          from SR generated resource bundles.
#          The main engine findOldReferencesNHardCoded_64
#          is provided by Shrikant N.
#          
# History:
#    Sep-25-2013   Charmaine M.   Initial Rel to Angry Bird
#                                 
################################################
#  location for this script
#will evetually be changed to something like $ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/stringScans/hardcodeStr when the code is merged to FATOOLS branch...
CODEHOME=$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/bin/i18n_scans/hardcodedStrChk

FINDOLDREF="findOldReferencesNHardCoded_64"

echo "===Start Time: `date` "

#added to direct exception to FATOOLS dev
fatools_label=`ade showlabels -series FATOOLS_MAIN_LINUX -latest | tail -n 1`
fatools_labelserver=`ade desc -l $fatools_label -labelserver`
EXDIR=$fatools_labelserver/fatools/opensource/jauditFixScripts/bin/i18n_scans/hardcodedStrChk
CLASSPATH="$fatools_labelserver/fatools/opensource/lib/JarJAuditFixer.jar:$fatools_labelserver/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xml.jar:$fatools_labelserver/fmwtools/BUILD_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar"

if [ -e $destdir/sumHardStr.csv ]; then
   #echo "Deleting sumHardStr.csv"
   rm $destdir/sumHardStr.csv
fi

if [ -e $destdir/rptHardStr.csv ]; then
   #echo "Deleting rptHardStr.csv"
   rm $destdir/rptHardStr.csv
fi

txnFiles="txnFilestemp.txt"
txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
export txnDescFile="$txn"_describe.out
ade describetrans > $txnDescFile 
grep 'fusionapps\/' $txnDescFile | awk '{print $2}' | sort | uniq  > $txnFiles

errCd=0
hrdstrOutfile="rptHardStr.csv"

# Get the list of infolet config files
java -classpath $CLASSPATH oracle.apps.stringscan.FindInfoletConfigFiles $ADE_VIEW_ROOT $destdir/infoletConfigFiles.txt

echo "Retrieved list of infolet config files"

# Start building the infolet portion of the 'grep' command
infoletportion=""

while read infoletfile; do
    infoletportion="$infoletportion|$infoletfile"
done < $destdir/infoletConfigFiles.txt

echo "===Start to find hard coded strings: `date` " > $destdir/hardcodeStrChk.trace
#### Loop through each jsff/jspx/taskmenu.xml file in jpr ####

counter=0

catcommand="cat $txnFiles | grep -E '(\.jsff|\.jspx|_taskmenu.xml$infoletportion)$' | grep -iv 'test\/'| grep -iv '\/test'|grep -iv 'test[0-9]*\.' | grep -vhFif  $EXDIR/hardExceptionFiles.dat"

for fl in `eval $catcommand`; do
    echo "Processing $fl " >> $destdir/hardcodeStrChk.trace
    fullFl=$ADE_VIEW_ROOT/$fl
    counter=`expr $counter + 1`

    # Carry out the appropriate processing based on the file type
    if [[ $fl == *.jsff ]] || [[ $fl == *.jspx ]] || [[ $fl == *_taskmenu.xml ]] ;
    then
        $CODEHOME/$FINDOLDREF -v -conffile $CODEHOME/UIStrings.conf -lba $CODEHOME/Strings_LBA_PackageNames.dat -applcorefile $EXDIR/applcoreStr.dat -reservedwordfile $EXDIR/resourceReservedWords.dat -xlfbundlefile $CODEHOME/xlfbundle.dat -outfile $destdir/sumHardStr.csv -file $fullFl -errors >> $destdir/hardcodeStrChk.trace 2>&1 
    else
        java -classpath $CLASSPATH oracle.apps.stringscan.InfoletConfigCheck $fullFl $destdir/sumHardStr.csv
    fi
done

echo "Total Number of Files Found: $counter" >> $destdir/hardcodeStrChk.trace;
echo "Exemption Files taken from: $EXDIR" >> $destdir/hardcodeStrChk.trace;
perl $CODEHOME/filterHardcodeStr.pl $destdir/sumHardStr.csv $destdir/rptHardStr.csv $EXDIR/hardExceptionStr.dat

#cp $destdir/rptHardStr.csv $hrdstrOutfile
if `grep  -q "fusionapps/" $hrdstrOutfile`
then
   errCd=1
   echo "" >> $hrdstrOutfile
   echo "Please review the KIX for more details: https://kix.oraclecorp.com/KIX/display.php?labelId=10989&articleId=385588" >> $hrdstrOutfile
   echo "" >> $hrdstrOutfile
fi
echo "===Program completed with status $errCd: `date` " >> $destdir/hardcodeStrChk.trace
exit $errCd
