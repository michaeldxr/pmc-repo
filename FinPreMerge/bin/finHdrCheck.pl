#!/usr/bin/perl
##############################################################################
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/finHdrCheck.pl /main/5 2012/09/13 09:35:16 ndickens Exp $
# Purpose: Script to check that xlfs in transaction contain a header tag. 
# Script will also check that OracleFusionAppsFinFolderCaptions has a header if
# contained within transaction.
#
# Revision:
#     17-Mar-11   Created         ndickens
#     11-Jan-12   Bug 13577572    ndickens
#
##############################################################################

@status=();
$txn=`ade pwv | grep VIEW_TXN_NAME | cut -d: -f2 | sed 's/ //g'`;
chomp($txn);
$outfile = $ENV{destdir} . "/" . $txn . "\_xliff\.out";
$xlfok=0;
$xlferr=0;

&checkHeaders;
&printResults;
&status;

sub checkHeaders {
#Gets files that are in the transaction and fileters out xlfs and OracleFusionAppsFinFolderCaptions 
#before checking if they have headers.
my $file="";
#get files in transaction
# list of file in transaction is exported by main pre-merge script
$txnFiles = $ENV{txnFiles};
@trans= `cat $txnFiles | grep -i fusionapps | cut -f 2- -d "/" | cut -d " " -f1 | sort | uniq`;
#get all xlfs in transaction
@xliffs= grep /.+bipub.+\.xlf/, @trans;
#get caption files if it's also in the transaction;
push (@xliffs,(grep /OracleFusionAppsFinFolderCaptions\.xml/, @trans));
#Get's longest element - used for formatting output.
$longest = (reverse sort { $a <=> $b } map { length($_) } @xliffs)[0];
#loops through files and checks if they have a header.
foreach $xlf (@xliffs) {
   chomp($xlf);
   $paddedXlf=sprintf("%-${longest}s",$xlf);
   $file= $ENV{ADE_VIEW_ROOT} . "/fusionapps/" . $xlf;
   # include an extra \H to avoid a header being injected
   @headers=`grep -n '\$\Header:' $file`;
   if ($#headers==-1) {
      push(@status,"$paddedXlf : No Header.\n");
      $xlferr++;
   }
   elsif ($#headers==0) {
      $skipcnt++;
      if (substr($headers[0],0,1) == '2') {
         $xlfok++;
         push (@status,"$paddedXlf : Already has a header.\n");
      }
      else {
         $xlferr++;
         push (@status,"$paddedXlf : Already has a header but not on second line. Please check.\n");
      }
   }
   elsif ($#headers>0) {
       $xlferr++; 
       push (@status,"$paddedXlf : Multiple occurences of \$Header. Please check.\n");
   }
}
}

sub printResults {
#print results to output file.
open LOG, ">$outfile";
select LOG;
print "\n*** Report ***\n\n";
print "This report checks all the BIP xlf files in your transaction to ensure that they contain a header.\n";
print "If your transaction contains OracleFusionAppsFinFolderCaptions.xml, then this is also checked.\n\n";
print @status;
print "\nNumber of files checked:  " . eval($#xliffs +1) . "\n";
print "Number with Header:       " . $xlfok . "\n";
print "Number with Issues:       " . $xlferr . "\n\n";
close LOG;
}

sub status {
#set exit status
if ($xlferr > 0) {
   exit(1);
}
elsif ($xlfok > 0) {
   exit(0);
}
else { #No relevant files in the txn
   exit(3);
}
}
