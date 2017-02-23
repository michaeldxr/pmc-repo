#! /bin/ksh
cd $ADE_VIEW_ROOT;

echo "**********************************************************"
echo "This OATS Impact Assessment Tool only applys to FIN family"
echo "at the moment."
echo "**********************************************************"
echo ""

if [ -z $family ]; then
	echo "Cannot find family"
	exit 3
fi

if [[ $family != "fin" ]]; then
	exit 3
fi

if [ -z $txn ]; then
	txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
fi

if [ -z $destdir ]; then
	destdir=$ADE_VIEW_ROOT/fusionapps/premerge
	echo "Cannot find pre-defined destdir, use $ADE_VIEW_ROOT/fusionapps/premerge instead."
fi

tstart_gsc=`date +%s`
ScriptExitStatus="3"
start=`date +%s`
logFile="$destdir/OatsVerificationLog.txt"
oatsOutputFileForMail="$destdir"/"$txn"_oatsImpactCheck.out
premergeDir="$destdir/OATSVerify"

echo "finOatsVerifyLogFile results" > $logFile

if [ -d "$premergeDir" ]
then
	echo "$DIR directory exists" >> $logFile
	rm -rf $premergeDir
fi
mkdir -p $premergeDir


echo "" > $oatsOutputFileForMail
echo "OATS Impact Assessment Output" >> $oatsOutputFileForMail
echo "==================================================" >> $oatsOutputFileForMail
#echo -e "Running Oats Impact Assessment for the transaction $txn" >> $oatsOutputFileForMail
#echo "" >> $oatsOutputFileForMail

echo "Start Checking...."

user=${whoami}

viewverFilestmp=$premergeDir"/viewVerSourceFilesTmp"

viewverFiles=$premergeDir"/viewVerSourceFiles"
labelverFiles=$premergeDir"/labelVerSourceFiles"

labelverInputFilesDir=$premergeDir"/labelverInputFilesDir/"
ViewverInputFilesDir=$premergeDir"/ViewverInputFilesDir/"

labelverOutputFilesDir=$premergeDir"/labelverOutputFilesDir/"
ViewverOutputFilesDir=$premergeDir"/ViewverOutputFilesDir/"

IgnoreFile=$premergeDir"/ignoreFile"

OatsSourceDir=$premergeDir"/OatsSourceDir/"
OatsSourceOutputDir=$premergeDir"/OatsSourceOutputDir/"

VOMATCHOUTDIR=$premergeDir"/VOMATCHOUTDIR/"
VOMATCHINDIR=$premergeDir"/VOMATCHINDIR/"

PAGEDEFLISTFILELOCTMP=$premergeDir"/PAGEDEFLISTFILELOCTMP"
PAGEDEFLISTFILELOC=$premergeDir"/PAGEDEFLISTFILELOC"
PAGEDEFFILESDIR=$premergeDir"/PAGEDEFLISTFILEDIR/"

TRXN_NAME=$txn >> $logFile
REPORTFILE="$destdir/$TRXN_NAME""_OATSREPORT.txt"

ViewBUNDLEFILE=$premergeDir"/ViewBundle.txt"
LABELBUNDLEFILE=$premergeDir"/LabelBundle.txt"
ViewVerBundleFilesDir=$premergeDir"/ViewVerBundleFiles/"
LabelVerBundleFilesDir=$premergeDir"/LabelVerBundleFiles/"
DumpFile=$premergeDir"/Dump"


OATSLABEL="/ade_autofs/gd22_fa/FAAT_MAIN_GENERIC.rdd/LATEST/"

# latestLabel=$(ade showlabels|tail -1) >> $logFile

LABEL=$(ade pwv | grep 'VIEW_LABEL    : ' | tr -d ' ' | cut -d ':' -f2)

if [ -z "OATSLABEL" ]
  then
	echo "OATS LABEL PATH NOT SUPPLIED AS ARGUMENT"
	echo "Do You Want To Continue with Default Path : /ade_autofs/gd22_fa/FAAT_PT.R11DEV_GENERIC.rdd/LATEST/  Y/N ? "
	
	#read -p  "Do You Want To Continue with Default Path : /ade_autofs/gd22_fa/FAAT_PT.R11DEV_GENERIC.rdd/LATEST/  Y/N ? " -n 1 -r
	read REPLY	
	if [[ ! $REPLY =~ ^[Yy]$ ]]
	then
	    exit 1
	fi
	OATSLABEL="/ade_autofs/gd22_fa/FAAT_PT.R11DEV_GENERIC.rdd/LATEST/"
fi


labelFilePath=$(ade desc -l $LABEL -labelserver)
echo "labelFilePath = "$labelFilePath >> $logFile

ViewFilePath=$(pwd)fileList
echo "ViewFilePath = "$ViewFilePath >> $logFile

ade describetrans | grep 'fusionapps' > $viewverFilestmp 

cat $viewverFilestmp | tr -d '\040\011' | sed 's/ *[0-9]*.//' | cut -d '[' -f 1 > $viewverFiles
rm $viewverFilestmp

#Validating the type of files. If the transaction contains JSFF XLF UIVO.xml VO.xml and EO.xml then only the tool will run else it will be skipped.
ade describetrans > describetransList.txt
fileTypeFlag=$(grep -E '.xlf|UIVO.xml|.jsff|VO.xml|EO.xml' describetransList.txt | wc -l) 
echo "File Count : ""$fileTypeFlag" >> $logFile
if [ $fileTypeFlag -gt 0 ]
	then
		echo "Contains UI impacting files" >> $oatsOutputFileForMail
		#Setting default value as failure
		ScriptExitStatus="1"
	else
		echo "Transaction doesn't contains UI fixes hence the Oats Verification will be skipped" >> $oatsOutputFileForMail
		echo "" >> $oatsOutputFileForMail
		ScriptExitStatus="0"
		echo "Removing Temporary Directories" >> $logFile
		rm -r -f $destdir/OATSVerify/
		exit "$ScriptExitStatus"
fi
rm describetransList.txt
while read line
do
    name=$line
    echo "$labelFilePath"/"$name" >> $labelverFiles
done < $viewverFiles

find fusionapps/fin/  -name "*PageDef.xml" > $PAGEDEFLISTFILELOCTMP


while read line
do
    name=$line
    echo "$labelFilePath"/"$name" >> $PAGEDEFLISTFILELOC
done < $PAGEDEFLISTFILELOCTMP

echo "Creating Dirs" >> $logFile
mkdir -p $labelverInputFilesDir
mkdir -p $ViewverInputFilesDir
mkdir -p $labelverOutputFilesDir
mkdir -p $ViewverOutputFilesDir
mkdir -p $OatsSourceDir
mkdir -p $OatsSourceOutputDir
mkdir -p $VOMATCHOUTDIR
mkdir -p $VOMATCHINDIR
mkdir -p $PAGEDEFFILESDIR


i=1
echo "Making Label Version Copy" >> $logFile
while read line
do
    if [ -f $line ];
    then
    lftemp=`basename $line`
	cp -L $line $labelverInputFilesDir"$i@"$lftemp
    else
	echo "Cannot stat file $line " >> $logFile
    fi
    i=$(( $i + 1 ))
    #cp -L $line $labelverInputFilesDir
done < $labelverFiles

i=1
echo "Making View Version Copy" >> $logFile
while read line
do
    if [ -f $line ];
    then
    vftemp=`basename $line`
	cp -L $line $ViewverInputFilesDir"$i@"$vftemp
    else
	echo "Cannot stat file $line " >> $logFile
    fi
    i=$(( $i + 1 ))
    #cp -L $line $ViewverInputFilesDir
done < $viewverFiles


echo "Making View Version Bundle Copy" >> $logFile
find fusionapps/fin/ -name "*.xlf" >$ViewBUNDLEFILE
mkdir -p $ViewVerBundleFilesDir

while read line
do
    name=$line
    echo "$labelFilePath"/"$name" >> $LABELBUNDLEFILE
done < $ViewBUNDLEFILE

mkdir -p $LabelVerBundleFilesDir


echo "--------Executing Main Script-------------" >> $logFile
echo "====OutputGenerator========"
#sh $ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/FinPreMerge/bin/finOatsOutputGenerator.sh $OATSLABEL $TRXN_NAME

OATSARPATH="faat/fusionapps/fin/oats/src/"
OATSSPLITFILES=$premergeDir"/OutFiles/"

labelFilePath=$OATSLABEL
releaseFileMap="/ade_autofs/ud21_fa/FATOOLS_MAIN_LINUX.rdd/LATEST/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/oatsAnalyzernull/OatsToLabelMap"
OatsSourceDir=$premergeDir"/OatsSourceDir/"
OatsSourceOutputDir=$premergeDir"/OatsSourceOutputDir/"
OATSFILELIST=$premergeDir"/OatsSourceDir/OATSFILELIST"

label=$(ade describetrans | grep 'BASE_LABEL: ' | cut -d ' ' -f2)

BUNDLELIST=$premergeDir"/LabelBundle.txt"
TMPUIFILELIST=$premergeDir"/TMPUIFILELIST.txt"
LABELVERBUNDLEUIFILELOC=$premergeDir"/LABELVERBUNDLEUIFILELOC/"
LABELVERBUNDLEUIFILEOUTPUTLOC=$premergeDir"/LABELVERBUNDLEUIFILEOUTPUTLOC/"

mkdir -p $LABELVERBUNDLEUIFILELOC
mkdir -p $LABELVERBUNDLEUIFILEOUTPUTLOC

mkdir -p $OATSSPLITFILES

#-------------------------------------Java Code Starts-------------------------------------
echo "Excecution Begins" >> $logFile
LabelPath="$labelFilePath"/"$OATSARPATH"

#To test locally in another transaction
#java -classpath /ade/rackumar_fatools/fatools/opensource/lib/JarJAuditFixer.jar -Djava.ext.dirs=$ADE_VIEW_ROOT/fatools/opensource/lib/ oracle.apps.oatsAnalyzernull.RunOatsVerification $labelverInputFilesDir $ViewverInputFilesDir $labelverOutputFilesDir $ViewverOutputFilesDir $IgnoreFile $OATSFILELIST $OatsSourceOutputDir $VOMATCHOUTDIR $VOMATCHINDIR $PAGEDEFLISTFILELOC $ViewVerBundleFilesDir $LabelVerBundleFilesDir $BUNDLELIST $TMPUIFILELIST $LABELVERBUNDLEUIFILELOC $LABELVERBUNDLEUIFILEOUTPUTLOC $labelverFiles $ViewBUNDLEFILE $OATSARPATH $label $releaseFileMap $OATSSPLITFILES > $REPORTFILE

java -classpath $ADE_VIEW_ROOT/fatools/opensource/lib/JarJAuditFixer.jar -Djava.ext.dirs=$ADE_VIEW_ROOT/fatools/opensource/lib/ oracle.apps.oatsAnalyzernull.RunOatsVerification $labelverInputFilesDir $ViewverInputFilesDir $labelverOutputFilesDir $ViewverOutputFilesDir $IgnoreFile $OATSFILELIST $OatsSourceOutputDir $VOMATCHOUTDIR $VOMATCHINDIR $PAGEDEFLISTFILELOC $ViewVerBundleFilesDir $LabelVerBundleFilesDir $BUNDLELIST $TMPUIFILELIST $LABELVERBUNDLEUIFILELOC $LABELVERBUNDLEUIFILEOUTPUTLOC $labelverFiles $ViewBUNDLEFILE $OATSARPATH $label $releaseFileMap $OATSSPLITFILES > $REPORTFILE

#-------------------------------------Java Code Ends-------------------------------------

echo "Excecution Ends" >> $logFile

echo "===END of OutputGenerator========"

sed -e "s|$OatsSourceOutputDir|/|ig" $REPORTFILE > /scratch/$USER/OATSREPORTTEMPFILE.txt
mv /scratch/$USER/OATSREPORTTEMPFILE.txt $REPORTFILE

echo "Removing Temporary Directories" >> $logFile
rm -r -f $destdir/OATSVerify/

ERRORBOOLEAN=$(grep 'ERROR:' $REPORTFILE | cut -d ':' -f2)
if [[  $ERRORBOOLEAN -gt 0 ]]
	then
	    echo "Oats files are impacted. Verify details from : " >> $oatsOutputFileForMail
	    echo "$REPORTFILE" >> $oatsOutputFileForMail
	    sed '/ERROR:/d' $REPORTFILE > /scratch/$USER/OATSREPORTTMP.txt
	    cp /scratch/$USER/OATSREPORTTMP.txt $REPORTFILE
	    rm /scratch/$USER/OATSREPORTTMP.txt
	    ScriptExitStatus="1"
	else
	    ScriptExitStatus="0"
	    echo "No OATS affected by this transaction" >> $oatsOutputFileForMail
fi
echo "Script Ends" >> $logFile

stop=`date +%s`
runtime=$((stop-start))
#echo $runtime
tstop_gsc="$(date +%s)"
telapsed_gsc="$(expr $tstop_gsc - $tstart_gsc)"

echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_gsc sec" +%H:%M:%S)" >> $oatsOutputFileForMail
cd - >> $logFile;
echo "" >> $oatsOutputFileForMail
exit "$ScriptExitStatus"
