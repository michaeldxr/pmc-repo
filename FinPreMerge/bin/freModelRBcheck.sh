##############################################################################
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/freModelRBcheck.sh /main/2 2011/01/13 18:31:16 hkaniven Exp $
# Purpose: Wrapper to run SR model checks 
#
# Revision:
#     Nov-10-2010   Created         Charmaine Mao
#                                  
##############################################################################
#------Take Parameters or print usage -------------------------
if [ $# -ne 3 ]; then
  echo "Usage : $0 <Fusion Apps Directory> <Log Dir> <Run Name>";
  echo "Where: ";
  echo "      Fusion Apps Directory=A directory name under fusionapps";
  echo "      Log Dir = Output Log Directory";
  echo "      Run Name = Unique Identifier for the Run. This makes the output files unique";
  echo "example : $0 ~/view_storage/my_AdeView/fusionapps/ic/components/ mylog st3b15_ic";
  exit 1;
fi

appsdir=$1
logdir=$2
runname=$3


echo "===Start Time: `date` "
if [ ! -e $appsdir ]; then
   echo "Fusion Apps Directory $appsdir not exist";
   exit 0;
fi

if [ ! -e $logdir ]; then
   mkdir $logdir
fi

if [ ! -e $logdir ]; then
   echo "***ERROR: Cannot create log dir $logdir. Program aborted!"
   exit 0
fi

#**************************************************
#  Message Place Holder
#***************************************************
echo ====Check XML files for Message Place Holder
outFName="msgPlHolder_$runname";
stmt="$cmd_dir/msgPlHolder.pl $appsdir $logdir/$outFName.log $logdir/$outFName.csv";
echo $stmt
$stmt
echo "Completed Message Place Holder Check: `date` "


echo ====Check DFF VO files for Non Atrribute Bundle
outFName="DFFnoAttr_$runname";
stmt="$cmd_dir/noAttrBlDFF.pl $appsdir $logdir/$outFName.log $logdir/$outFName.csv";
echo $stmt
$stmt
echo "Completed DFF VO Non-Attribute Bundle check: `date` "


echo ====Check Non DFF VO files for Non Atrribute Bundle
outFName="noAttr_$runname";
stmt="$cmd_dir/noAttrBl.pl $appsdir $logdir/$outFName.log $logdir/$outFName.csv";
echo $stmt
$stmt
echo "Completed NON DFF VO Non-Attribute Bundle check: `date` "

echo ====Check VO files for null value LOV
outFName="nulLOV_$runname";
stmt="$cmd_dir/nullLOV.pl $appsdir $logdir/$outFName.log $logdir/$outFName.csv";
echo $stmt
$stmt
echo "Completed Null Value LOV check: `date` "

echo "===End Time: `date` "
echo "Please review output files under $logdir"
exit 1

