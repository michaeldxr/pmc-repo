#!/usr/bin/perl
###################################################################################################################
#                                                                                                                 #
# finCaptionsCheck.pl                                                                                             #
#                                                                                                                 #
# Date          Version           Who                 Description                                                 #
# #########     #######           #######             ######################################                      #
# 22-Mar-12     1.0               ndickens            Initial version                                             #
# 22-May-14     2.0               ndickens            Bug 18645852                                                #
# 12-Aug-14     3.0               ndickens            Bug 18938069                                                #
###################################################################################################################
@status=();
&init;
&getFiles;
if ($#files > -1) {
   &checkCaptions;
   &checkIds;
   &writeLog;
}
else {
  print RUNLOG "No Meta Files Found in Transaction.\n";
}
&status;

sub init {
#get transaction name
#txn is exported by main pre-merge script
$txn = $ENV{txn};
$FAToolsMainLatest = $ENV{FAToolsMainLatest};
$ADE_VIEW_ROOT=$ENV{ADE_VIEW_ROOT};
#get series that view is based upon
$viewSeries =`ade pwv | grep VIEW_LABEL | cut -d: -f2 | sed 's/ //g'`;
chomp($txn);
chomp($viewSeries);
#get the latest label for the view series and also the server path
if ($viewSeries =~ /S$/) {
   #remove timestamp which is of format _DDMMYY_HHMI.S
   $txnSeries = substr($viewSeries, 0, length($viewSeries) - 14);
}
else {
   #remove timestamp which is of format _DDMMYY_HHMI
   $txnSeries = substr($viewSeries, 1, length($viewSeries) - 12);
}
$txnSeriesLatestLabel = `ade showlabels -series $txnSeries -latest | tail -n1`;
chomp($txnSeriesLatestLabel);
$txnSeriesServerTop = `ade desc -l $txnSeriesLatestLabel -labelserver |cut -d/ -f1-3`;
chomp($txnSeriesServerTop);
#read the mapping file to ascertain the correct captions file to check for the series.
$map = $FAToolsMainLatest . "/fatools/opensource/jauditFixScripts/FinPreMerge/bin/finCaptionsCheck.map";
print STDOUT "Using map file: $map\n";
open MAP, $map;
@mapLines = <MAP>;
for $mapLine (@mapLines) {
   @parts = split(' ', $mapLine);
   $series = $parts[0];
   $captionSeries = $parts[1];
   $captionFile = $parts[2];
   if ($viewSeries =~ /$series/) {
      print STDOUT "Transaction Series $txnSeries found in MAP file\n";
      $captionLatestLabel = `ade showlabels -series $captionSeries -latest | tail -n1`;
      chomp( $captionLatestLabel);
      $labelServerTop = `ade desc -l $captionLatestLabel -labelserver |cut -d/ -f1-3`;
      chomp($labelServerTop);
      if ($series eq $captionSeries) {
          # if series and captionSeries match, then can use captions file from current view.
          $captionFilePath = $ADE_VIEW_ROOT;
      }
      else {
          #Use caption file form the captionSeries specified in the map file
          $captionFilePath = $labelServerTop . "/" . $captionSeries . ".rdd/LATEST";
      }
      if ($captionFile ne '') {
           $captionFile = "$captionFilePath/fusionapps/com/bi/$captionFile";
           print STDOUT "Captions from map file: $captionFile\n";
      }
      else {
        print STDOUT "Captions from map file: No caption file specified. Therefore, will first check Family Captions File and if that isn't found will then check captions.xml.\n";
      }
      last;
   }

   $series = '';
   $captionFile = '';
   $captionSeries = '';
}
close MAP;
if ($captionSeries eq '') {
   print STDOUT "Transaction Series $txnSeries is not found in MAP file.\n";
   # series not found in map file, so look in current view for captions file
   $captionFilePath  = $ADE_VIEW_ROOT;
}
$captionsXml =  $captionFile;
$outfile = $ENV{destdir} . "/" . $txn . "\_finCaptionCheck\.out";
open RUNLOG,">$outfile"; 
select RUNLOG;
print "Transaction: $txn\n";
print "Series     : $txnSeries\n";
print "Label      : $viewSeries\n";
if ($captionsXml eq '') {
   print "Captions   : Family Level\n\n";
}
else  { 
   print "Captions   : $captionsXml\n\n"; 
}
}


sub getFiles {
#get files in transaction
# list of file in transaction is exported by main pre-merge script
$txnFiles = $ENV{txnFiles};
@trans=`cat $txnFiles | grep fusionapps | grep -i bipub | sort | uniq`;
#get all meta files in transaction
@files= grep /.+bipub.+\.meta/, @trans;
}


sub checkCaptions {
print STDOUT "Checking Files ...\n";
foreach $file (@files) {
   chomp($file);
   print STDOUT "   File: $file\n";
   @pathParts = split('/', $file);
   $family = $pathParts[1];
   if  ($captionsXml eq '') {
        if ($captionSeries ne '') {
           # transaction series is found in map file. However, caption xml file not specified so will use family caption file. 
           # if it can't find the family captions, then it will look for the captions.xml
           $captionsXml = "$captionFilePath/fusionapps/$family/bi/export/${family}Captions.xml";
           unless (-e "$captionsXml") {
           #family captions xml doesn't exists. Therefore, check for captions.xml
              print STDOUT "      Family Captions File not found: $captionsXml\n";
              $captionsXml = "$captionFilePath/fusionapps/com/bi/captions.xml";
           }
        }
        else {
           # Transaction series not found in map file. Therefore, will use family captions file if it exists or check for captions.xml file in the same series as the transaction.
           $captionsXml = "$captionFilePath/fusionapps/$family/bi/export/${family}Captions.xml";
           unless (-e "$captionsXml") {
           #family captions xml doesn't exists. Therefore, check for captions.xml
              print STDOUT "      Family Captions File not found: $captionsXml\n";
              $captionsXml = "$captionFilePath/fusionapps/com/bi/captions.xml";
           }
        }
        print STDOUT "      Using Captions File: $captionsXml\n";
   }
   else {
      # transaction series and captions file specified in map file, so will use these.
      # but first checks to see if captions.xml file actually existis, otherwise, it will still use family captions.
      unless (-e $captionsXml) {
          print STDOUT "    $captionsXml not found. Using Family captions instead.\n";
          $captionsXml = "$captionFilePath/fusionapps/$family/bi/export/${family}Captions.xml";
      }
      print STDOUT "      Using Captions File: $captionsXml\n"
   }
   if (-e "$captionsXml") {
      $exists = `grep -i '$file' $captionsXml`;
      if ($exists ne "") {
         print STDOUT "      Meta file found in captions file.\n";
         push(@success,$file);
         push(@successLines, $exists);
      }
      else {
         print STDOUT "      Error - Meta file not found in captions file.\n";
         push(@errors,$file . "\n");
     }
   }
   else {
     print STDOUT "      Error - No corresponding Caption File Found. Aborting Caption Check.\n";
     print RUNLOG "#################################################################################################################################################################";
     print RUNLOG "\nERROR - CAPTIONS FILE NOT FOUND. ABORTING CAPTIONS CHECK.\n";
     print RUNLOG "#################################################################################################################################################################";
     exit(1);
   }
}
}

sub checkIds {
print STDOUT "Checking Caption IDs ...\n";
$ind=0;
foreach $meta (@success) {
   print STDOUT "   File: $meta\n";
   $errMsg="";
   #get line numbers of the captionId and the LocalizedDescId
   $localLine=`grep -in 'LocalizedDescId' $ADE_VIEW_ROOT/$meta`;
   $captionLine = `grep -in 'CaptionId' $ADE_VIEW_ROOT/$meta`;
   open META, "$ADE_VIEW_ROOT/$meta";
   @lines = <META>; 
   $localId=substr($lines[$localLine], index($lines[$localLine],'kcap'), index($lines[$localLine], ']]') - index($lines[$localLine],'kcap'));
   $captionId=substr($lines[$captionLine], index($lines[$captionLine],'kcap'), index($lines[$captionLine], ']]') - index($lines[$captionLine],'kcap'));
   if ($localId eq '') {
      $localId='<NULL>';
   }
   if ($captionId eq '') {
      $captionId = '<NULL>';
   }
   $errMsg = "$meta";
   $captionStr=`echo '$successLines[$ind]' | sed 's/^[ \t]*//g' | sed 's/"//g' | cut -f3-4 -d \" \"`;
   chomp($captionStr);
   $xCaptionId=substr($captionStr,index($captionStr,'kcap'),index($captionStr,' ') - index($captionStr,'kcap'));
   $xLocalId=substr($captionStr,rindex($captionStr,'kcap'));
   $xLocalId =~ s/\s+$//;
   push(@results, "File: $meta" . "Caption File: " . $captionStr . "Meta File   : caption=" . $captionId . " shortDesc=" . $localId . "\n\n");

   if ($xLocalId ne  $localId) {
      $errMsg = $errMsg . "  LocalizedDescId $localId does not match captions file value of $xLocalId.\n";
      print STDOUT "         Error - LocalizedDescId $localId does not match captions file xml value of $xLocalId.\n";
      $errCntLocal++;
   }
   else {
     print STDOUT "         LocalizedDescId Matches.\n";
     $sucCntLocal++;
  }
  if ($xCaptionId ne $captionId) {
      $errMsg = $errMsg . "  CaptionId $captionId does not match captions file value of $xCaptionId.\n";
      print  STDOUT "         Error - CaptionId $captionId does not match captions file value of $xCaptionId.\n";
      $errCntCaption++;
   }  
   else {
      print STDOUT "         CaptionId Matches.\n";
      $sucCntCaption++;
   }
   $ind++;   
   $errMsg = $errMsg . "\n";
   if ($errMsg =~ /does not match/) {
      push(@invalidCaptions, $errMsg);
   }
}
}

sub writeLog {
select RUNLOG;
if ($#errors > -1 || $#invalidCaptions > -1) {
   print "#################################################################################################################################################################";
   print "\nPLEASE NOTE: ERRORS EXIST.\n\n";
   print "IF YOU HAVE ALREADY LOGEGD A BUG FOR THE CHANGES BELOW TO THE CAPTION FILE AND THE BUG HAS BEEN CLOSED, THEN INCLUDE THE BUG THAT YOU LOGGED FOR THE\n";
   print "CHANGES IN THIS BUG - THEN THESE ERRORS CAN BE CONSIDERED ADVISORY AND THE BUG CAN BE APPROVED.\n\n";
   print "IF YOU HAVE NOT LOGGED A BUG AGAINST YOUR POC FOR THE CAPTION CHANGES - THEN YOU NEED TO DO SO AND IT NEEDS TO BE CLOSED BEFORE YOU CAN PROCEED\n";
   print "#################################################################################################################################################################";
   if ($#errors > -1) {
      print "\nERRORS: The following " . ($#errors + 1) . " meta files do NOT exist in the captions file:\n\n";
      print @errors, "\n";
   }
   if ($#invalidCaptions > -1) {
      print "\nERRORS: The following CaptionIds or LocalizedDescIds do not match:\n\n";
      print @invalidCaptions;
   }
}
else {
   print "All CaptionId and LocalizedDescId match.\n";
}
}

sub status {
#set exit status
select STDOUT;
if ($#files < 0) { #No relevant files in the txn
    exit(3);
}
elsif ($#errors > -1 || $#invalidCaptions > -1) {
   exit(1);
}
else {
   exit(0);
}
}

