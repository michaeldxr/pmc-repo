#!/usr/bin/perl
##############################################################################
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/faBipFolderChk.pl /main/4 2012/09/13 09:35:16 ndickens Exp $
# Purpose: Script to check that folders specified in the meta files existin the webcat skeletons. 
#
# Revision:
#     13-May-11   1.0  ndickens    Initial Version.
#     25-May-11   2.0  ndickens    Added enhancement for checking if webcat skeletons are present in transaction
##############################################################################
&init;
&processArgs;
if ($mode eq "trans") {
   $txn=`ade pwv | grep VIEW_TXN_NAME | cut -d: -f2 | sed 's/ //g'`;
   chomp($txn);
   $txn = $txn . "\_";
   if ($ENV{destdir} ne "") {
      $destdir= $ENV{destdir} . "/";
   }
}
$outfile = $destdir . $txn . "faBipFolderChk\.out";
$skeletonFile = $destdir . $txn . "faBipFolderChk\.skeleton";
$logfile=$destdir . $txn . "faBipFolderChk\.log";
open LOG, ">$logfile";
print LOG "outfile=$outfile\nSkeleton File=$skeletonFile\n";
&getMetas;
&processSharedSkeleton;
&processFamilySkeleton;
&printSkeleton;
&processMetas;
&printResults;
&status;

sub init {
@status="";
$errors=0;
$pass=0;
$skelerr=0;
$destdir="";
$txn="";
}

sub processSharedSkeleton {
#read skeleton
print LOG "processing skeleton ...OracleFusionAppsSharedSkeleton.zip\n";
open SKEL, ">$skeletonFile";
@skeleton=`unzip -l \$ADE_VIEW_ROOT/fusionapps/com/bi/OracleFusionAppsSharedSkeleton.zip | grep -i '\/shared\/' |grep -v '\.atr'`;
foreach (@skeleton) {
    push(@skfoldersTemp,substr($_,index($_,"\/shared\/")+7));
}
}

sub processFamilySkeleton {
#check family level skeleton
if ($family ne "*") {
   foreach $family (@families) {
      $skel="OracleFusionApps" . ucfirst($family) . "Skeleton.zip";
      print LOG "Processing family skeleton ...$skel\n";
      @famSkel=`unzip -l \$ADE_VIEW_ROOT/fusionapps/$family/bi/export/$skel | grep -i '\/shared\/' |grep -v '\.atr'`;
      foreach (@famSkel) {
         push(@skfoldersTemp,substr($_,index($_,"\/shared\/")+7));
      }
   }
}
else {
   @allSkels=`find \$ADE_VIEW_ROOT/fusionapps/*/bi/export -name 'OracleFusionApps*Skeleton.zip' -print`;
   foreach $allSkel (@allSkels) {
      chomp($allSkel);
      print LOG "processing skeleton ...$allSkel\n";
      @tempSkel=`unzip -l $allSkel | grep -i '\/shared\/' |grep -v '\.atr'`;
      foreach (@tempSkel) {
         push(@skfoldersTemp,substr($_,index($_,"\/shared\/")+7));
      }
   }
}
}

sub printSkeleton {
%hashTemp = map { $_ => 1 } @skfoldersTemp;
@skfolders=sort keys %hashTemp;
select SKEL;
print @skfolders;
select STDOUT;
}

sub getMetas {
#get the meta files
# list of file in transaction is exported by main pre-merge script
$txnFiles = $ENV{txnFiles};
if ($mode eq "trans") {
  $skelerr=0;
  print LOG "Getting Files from Transaction ...\n";
#  @trans=`ade describetrans | grep fusionapps | grep -i bipub | grep -i '\\\.meta' | sed 's/ //g' | sort | uniq`;
#  @transSkel=`ade describetrans | grep fusionapps | grep -i 'OracleFusionApps\.*Skeleton\\\.zip' | sed 's/ //g' | sort | uniq`;
  @trans=`cat $txnFiles | grep fusionapps | grep bipub |  grep '\\\.meta' | sed 's/ //g' | sort | uniq`;
  @transSkel=`cat $txnFiles | grep fusionapps | grep -i 'OracleFusionApps\.*Skeleton\\\.zip' | sed 's/ //g' | sort | uniq`;
  if ($#transSkel > -1) {
     print LOG "**** Skeleton File Found in Transaction - ERROR ***\n";
     push (@status, "Transaction Contains Webcat Skeleton. Approval from your manager is required to patch this file:\n@transSkel\n\n");
     $skelerr=1;
  }
  if ($#trans == -1) {
     push (@status, "No Meta files found in the transaction.");
  }
  else  {
     foreach (@trans) {
        push(@files, $ENV{ADE_VIEW_ROOT} . "/" . substr($_,index($_,"fusionapps")));
        @parts=split(/\//,$_);
        push (@familiesTemp,$parts[1]);
     }
  }
}
elsif ($mode eq "ade") {
   @files = `find \$ADE_VIEW_ROOT/fusionapps/$family/$product/bipub -name '*.meta' -print`;
   push (@familiesTemp, $family);
   if ($#files == -1) {
     push (@status, "No Meta files found.");
   }
}
%hashTemp = map { $_ => 1 } @familiesTemp;
@families=sort keys %hashTemp;
@files=sort @files;
print LOG "Meta Files Found:\n\n@files\n\n";
print LOG "families identified: \n\n@families\n\n";
}

sub processMetas {
# get the folder path from within themeta file and scan the folders from webcat skeletons to see if it exists.
foreach $meta (@files) {
   chomp ($meta);
   $longest = (reverse sort { $a <=> $b } map { length($_) } @files)[0];
   $paddedMeta=sprintf("%-${longest}s",$meta);
   open META, "$meta";
   @metaLines=<META>;
   $pathFound=0;
   close META;
   foreach $metaLine (@metaLines) {
      if ($pathFound==1) {
         $pathFound=0;
         $metaPath=substr($metaLine,index($metaLine,"CDATA[")+6,rindex($metaLine,"]]")-(index($metaLine,"CDATA[")+6));
         $metaPath=~s/\%2F/\//g;
         $metaPath=lc(substr($metaPath,0, rindex($metaPath,"/")) . "/");
      }
      if ($metaLine =~ /path/) {
         $pathFound=1;
      }
   }
   $found =0;
   foreach $skfolder (@skfolders) {
      $foundFolder="";
      chomp($skfolder); 
      chomp($metaPath);
      $skeletonFolder=$skfolder;
      &removeHexCodes;
      if ($metaPath eq $skeletonFolder) {
         $found = 1;
         $foundFolder=$skfolder;
         last;
      }
   }
   if ($found == 1) {
      $pass++;
      push (@status, "$paddedMeta : Valid\n"); 
      push (@status, "    Path in Meta File        : $metaPath\n    Folder found in Skeletons : $foundFolder\n\n");
   }
   else {
      $errors++;
      push (@status, "$paddedMeta : *** INVALID ***\n    $metaPath .........Invalid\n\n");
   }
}
}

sub removeHexCodes {
#webcats contain hex codes in som eof the paths. These need to be removed before the scan.
 $skeletonFolder =~ s/co%6e/con/g;
 $skeletonFolder =~ s/co%6d/com/g;
 $skeletonFolder =~ s/au%78/aux/g;
 $skeletonFolder =~ s/nu%6c/nul/g;
 $skeletonFolder =~ s/pr%6e/prn/g;
 $skeletonFolder =~ s/lp%74/lpt/g;
}

sub printResults {
open STATUS, ">$outfile";
select STATUS;
print "This report checks to ensure that the folders specified within the BIP Artifact Meta Files exist within\n";
print "the webcat skeletons.\n\n";
print @status;
print "\nNumber of files checked    :  " . eval($#files+1) . "\n";
print "Number with Valid Folders  :  " . $pass . "\n";
print "Number with Invalid Folders:  " . $errors . "\n\n";
if ($skelerr==1) {
   print "ERROR: Transaction contains Webcat Skeleton. This requires management approval.\n\n";
}
close STATUS;
}

sub processArgs {

foreach $parameter (@ARGV) {
    @params = split(/=/, $parameter);
    if ($params[0] eq "mode") {
       $mode=$params[1];
    }
    elsif ($params[0] eq "product") {
       $product=$params[1];
    }
    elsif ($params[0] eq "family") {
       $family=$params[1];
    }
}
if ($mode eq "ade" && $product eq "") {
    $product = "*";
}
if ($mode eq "ade" && $family eq "") {
    $family = "*";
    $product = "*";
}
}

sub status {
#set exit status
if ($errors > 0 || $skelerr==1) {# transaction has errors
   print LOG "skelerr=$skelerr\n";
   print LOG "Exit with Status 1";
   exit(1);
}
elsif ($pass > 0 && $skelerr==0) { # every meta files has passed and skeleton file is not being patched.
   print LOG "Exit with Status 0";
   exit(0);
}
else { # no meta files in the transaction
   print LOG "Exit with Status 3";
   exit(3);
}
}
