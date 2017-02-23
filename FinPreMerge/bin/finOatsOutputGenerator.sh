#! /bin/ksh
OATSARPATH="faat/fusionapps/fin/oats/src/"
OATSSPLITFILES="$ADE_VIEW_ROOT/fusionapps/premerge/OATSVerify/OutFiles/"

labelFilePath=$OATSLABEL
releaseFileMap="/ade_autofs/ud21_fa/FATOOLS_MAIN_LINUX.rdd/LATEST/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/oatsAnalyzernull/OatsToLabelMap"
OatsSourceDir="$ADE_VIEW_ROOT/fusionapps/premerge/OATSVerify/OatsSourceDir/"
OatsSourceOutputDir="$ADE_VIEW_ROOT/fusionapps/premerge/OATSVerify/OatsSourceOutputDir/"
OATSFILELIST="$ADE_VIEW_ROOT/fusionapps/premerge/OATSVerify/OatsSourceDir/OATSFILELIST"

label=$(ade describetrans | grep 'BASE_LABEL: ' | cut -d ' ' -f2)

BUNDLELIST="$ADE_VIEW_ROOT/fusionapps/premerge/OATSVerify/LabelBundle.txt"
TMPUIFILELIST="$ADE_VIEW_ROOT/fusionapps/premerge/OATSVerify/TMPUIFILELIST.txt"
LABELVERBUNDLEUIFILELOC="$ADE_VIEW_ROOT/fusionapps/premerge/OATSVerify/LABELVERBUNDLEUIFILELOC/"
LABELVERBUNDLEUIFILEOUTPUTLOC="$ADE_VIEW_ROOT/fusionapps/premerge/OATSVerify/LABELVERBUNDLEUIFILEOUTPUTLOC/"

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
