#!/usr/local/bin/perl -w
use Cwd qw(getcwd);
use FileHandle;
use File::Find;
use Data::Dumper;
use POSIX qw(ceil floor);

use strict;

my $DEBUG=$ENV{'DEBUG'}||0;
my $logFH;
my $MIN_REQ_LENGTH=10;
my $PERCENT_EXP=2;
my $FAILSTATUS=1;
$|=1;  #make unbuffered
my $totalCt;
my $errCt=0;
my $gErrStrCt=0;
my @gxlfArr;
my $gXmlFile;
my $gXmlFH;

my @gNoRb;

  my $argCount=@ARGV;
  if ($argCount==0 || $argCount <3){
      print <<DOC;
You have not provided the required arguments for this program.
Purpose:  Show Non-AttrBundle reference strings in xml files
Syntax:
    perl $0 <fusionapps directory or subdirectory> <output log file> <output summary csv file>

Usage by example: 
    perl $0 ~/view_storage/my_AdeView/fusionapps/ic/components/ myDetail.log mySum.csv   

DOC

   exit;
   }

  my $gTopDir=$ARGV[0];
  my $gLogFile=$ARGV[1];
  (defined $ARGV[2]) && ($gXmlFile=$ARGV[2]);
 (-e $gTopDir) or die("The directory $gTopDir does not exist\n");

    my $gLogFH= new FileHandle;   
    open($gLogFH, ">$gLogFile") || die("Fatal Error: Cannot open log file $gLogFile\n");

      $gXmlFH= new FileHandle;   
      open($gXmlFH, ">$gXmlFile") || die("Fatal Error: Cannot open xml file $gXmlFile\n");

  $gTopDir =~ s/\/$//;
   if ($gTopDir =~ /fusionapps$/){
        open(FAMDIR, "ls  -d $gTopDir/*/components|") ||
            die "***Error: cannot access $gTopDir: $!\n";

         while (my $curDir= <FAMDIR>){
           print("Looking for *EO|VO|PS.xml files under $curDir\n");
           my $famDir=$curDir;
           chomp $famDir;
           find(\&procDir,"$famDir");
         }
   }else{
      find(\&procDir,$gTopDir);
   }
  $totalCt=@gxlfArr;
  printOut("INFO: Found $totalCt xml files\n");
#  print $gLogFH Dumper(\@gxlfArr);

  procXlfFiles();


  close($gLogFH);
  close($gXmlFH);
  exit 0;



sub procXlfFiles{
   printOut("Top Directory=".$gTopDir."\n");

   printXml("File Name, # Problem Strings, Prod Family, Error Category\n");
   foreach my $curxlf (@gxlfArr){
#      print("cur file=$curxlf\n");
     chomp $curxlf;
       
      my $xlfName=substr($curxlf,length($gTopDir));
      $xlfName =~ s/^\///;

#      print("====Start processing $xlfName\n");
      my @rbArr;
      my $numFound=0;
      my $status=procTransUnit($curxlf,\$numFound,$xlfName);
      if ($numFound){
          my $fam="A";
	  if ($xlfName=~ /fusionapps\/([a-zA-Z]+)\//){
	      $fam=$1;
	  }else{
	      $xlfName=~/^([a-zA-Z]+)?\//;
              $fam=$1;
	  }
          printXml("$xlfName, $numFound, $fam, CATEGORY_3\n");
      }
   }
}

sub procDir{
   #print "var=$_\n";                          #dir or file name
   #print "find:name=".$File::Find::name."\n"; #full path/filename
   #print "find:dir=".$File::Find::dir."\n";  #full path

   my $cur=$_;
   my $fname=$File::Find::name;
   my $fdir=$File::Find::dir;

   if ($cur=~/^(translation|bin|noship|adt|admin|adflibs|j2ee|lib|temp|patchtop|include|jlib|grc|Makefile|deploy|libs|c|etc|perl|sql|db|security|WEB-INF|META-INF)$/){
      $File::Find::prune = 1;      
#      print("$_ prunned\n");
      return;
   }
#look for *VO.xml, *EO.xml, *PS.xml  under <family>/components
  if ($cur=~/VO\.xml$/ && ($fdir=~/fusionapps\/[\w_-]+\/components(\/)?\b/)) {
      #print("xlf found $File::Find::name\n");
      push(@gxlfArr,$File::Find::name);

   }
   return;
}

sub procTransUnit{
    my ($inXlf,$ctPtr,$fName)=@_;

    unless (-e  $inXlf){ 
      die("***Error:Cannot find xlf File $inXlf \n");
    }

    #Ignore FND DFF VOs
    my $isDFF=0;
    if ($fName =~ /VO.xml/){
       my $hasExp=`grep 'Name="FND_ACFF_ModelID"' $inXlf`;
       if ($hasExp){
          #printLog("$inXlf Is DFF VO by line $.:$hasExp\n");
          $isDFF=1; 
       }
    }

    unless ($isDFF){
       return 0;
    }

    my $inFH = new FileHandle;
    open($inFH,"$inXlf") or die("Fatal Error: cannot open xliff file $inXlf!\n");

    my $fileStatus=0;
    my $curln;
    my $numFound=0;
    while ($curln=<$inFH>){
        #skip html comments
        ($curln =~ /^\s*<!--/) && next;
      	if ($curln =~ /ResId=\"/){
#            print("found ResId on line $.: $curln\n");
            if ($curln=~/ResId="\$\{adfBundle\[.+AttrBundle\'\]/){
 #              print("found attrbundle on line $.\n");
               next;
            }elsif ($curln=~ /ResId=\".+:::/){
                next;
            }elsif ($curln=~ /ResBundleId=\".+?applcore\.messages\.FndMessagesResourceBundle"/){
                next;
            }
            $numFound++;
            if ($numFound == 1){
               printLog("==$fName contains\n");
            }
            printLog("Line $.: $curln");
           }
    }
    ${$ctPtr}=$numFound;

    close($inFH);
    return $fileStatus;
}

sub checkRule{
    my ($text,$maxWidth,$lineNum,$errMsgPtr)=@_;
    my $ret=0;
    my $sourceLen=getPureDataLength($text);
    if ($maxWidth < $MIN_REQ_LENGTH){
        recordError("Line $lineNum trans-unit maxwidth=$maxWidth is less than the minium required length of $MIN_REQ_LENGTH",$errMsgPtr);
        $ret=$FAILSTATUS;
        return $ret;
    }

    if ($sourceLen <= 30){
        $PERCENT_EXP=1.5;
    }else{
        $PERCENT_EXP=1.3;
    }
    my $expLen=$sourceLen * $PERCENT_EXP;
    if ($expLen > $maxWidth){
        my $maxAllowed=floor($maxWidth/$PERCENT_EXP);
        my $tooLong=$sourceLen-$maxAllowed;

        #Give grace characters
        if (($sourceLen > 30 and $sourceLen <=40 and $tooLong <=1) or
           ($sourceLen > 40 and $tooLong <=2)){
	    $ret=0;
	}else{
	    recordError("Line $lineNum trans-unit source text is too long by $tooLong chars(source length $sourceLen > allowed width $maxAllowed=maxwidth $maxWidth / expansion $PERCENT_EXP)--source=".substr($text,0,45),$errMsgPtr);
            $ret=$FAILSTATUS;
        }
    }
    return $ret;
}


#-----------------------------------------------------

sub recordError{
    my $ln=shift;
    my $errMsgPtr=shift;

    printLog("***ERROR:".$ln."\n");
    $$errMsgPtr=$$errMsgPtr."\n".$ln;
}

sub printXml{
    my $ln=shift;

    print $gXmlFH $ln;        
}

sub printLog{
    my $ln=shift;

    print $gLogFH $ln;        
}


sub printOut{
    my $ln=shift;

    print $ln;
    print $gLogFH $ln;        
}
