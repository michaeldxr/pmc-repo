#!/bin/sh

####################################################################################
# Purpose: Script to run JUnit on individual projects as part of fin_premerge.ksh
#
# Revision:
#     Nov-2010   Created                                                  Harikiran
#     Jan-2011   JUnit made to run in parallel for individual projects    Harikiran
#     Jul-2016   JUnit made to update the database connection for prj     michael dong
#     Nov-2016   JUnit made to update the database connection for hcm     michael dong                             
####################################################################################

if [ $debug -eq 1 ];then
        set -x
fi

#For independent script testing purposes
if [ $USER == "tedong" -a -z "$destdir" ];
then

  set -x
  mkdir -p $ADE_VIEW_ROOT/fusionapps/premerge
  destdir=$ADE_VIEW_ROOT/fusionapps/premerge
  txn=`ade pwv|grep VIEW_TXN_NAME |cut -d: -f2|tr -d [\ ]`
  host=indl144121.idc.oracle.com
  port=1522
  sid=in144121
  uid=fusion
  pswd=fusion
  ade describetrans | grep fusionapps/ | awk '{print $2}' | sort | uniq > $destdir/desctransoutput.txt
  txnFiles=$destdir/desctransoutput.txt
  family=`cat $txnFiles | grep fusionapps | sed 's/ //g' | cut -f 2 -d '/' | sort | uniq`
  customJunitProjs=$1
  
fi  

tstart_jut="$(date +%s)"

junitprojectspassedasparameter=$customJunitProjs

junitoutfileformail="$destdir"/"$txn"_junit.out
db=$host:$port/$sid
junitprojectssublbafilelist="$destdir"/"$txn"_junit_sublbafiles.lst
junitprojectsrunlist="$destdir"/"$txn"_junit.lst
exitStatus=0
junitprojectspassedasparameterflag=0
testxmldir=$ADE_VIEW_ROOT/fusionapps/$family

echo -e "" > $junitprojectsrunlist
echo -e "" > $junitprojectssublbafilelist

echo -e "" > $junitoutfileformail
echo -e "JUnit Output" >> $junitoutfileformail
echo -e "==================================================" >> $junitoutfileformail

if [[ `cat $txnFiles | grep -iv '/[a-zA-Z]*[Uu]i/' | grep -iv dbSchema | grep -i model |grep -v '/WEB-INF/'| grep -v "service-definition\.xml" |  grep fusionapps/$family/components` ||  -n "$junitprojectspassedasparameter" ]]; #IF id 001
then

#  echo -e "Running JUnit for files involved in the transaction $txn" >> $junitoutfileformail
  echo -e "DB used is $host:$port/$sid" >> $junitoutfileformail

  if `cat $txnFiles | grep -iq fusionapps` #IF id 002
  then
  
    actualStr="<target.*name=\"test\""
    patternStr='<patternset id="lrg.tests"> <include name="**\/*RG.java"\/> <include name="**\/*BuildSuite.java"\/>         <include name="**\/All*Tests.java" \/> <exclude name="**\/*SeTests.java" \/> <exclude name="**\/.ade_path\/**"\/> <\/patternset> '

    replaceStr1='<target name="test" xmlns:custom="antlib:oracle.anttasks"> <path id="myemmaclassestoinstrument"><\/path> <path id="myemmasources"><\/path> <property name="test.lrg" value="true"\/>     <property name="emma.enabled" value="true"\/> <condition property="test.filter" value="lrg.tests" else="srg.tests"><equals arg1="${test.lrg}" arg2="true"\/><\/condition><custom:appDbUpdater '
    
    dbStr="host=\"$host\" port=\"$port\" sid=\"$sid\" user=\"$uid\" pass=\"$pswd\">"
    
    replaceStr2='<fileset dir="${basedir}">'

# Add extra tag like this <include name="components\/ledger\/Ledger.jws"\/> for each new workspace
       
    replaceStr4='<\/fileset><\/custom:appDbUpdater>'
 

    replaceStr6=' <antcall target="emma-report" \/> <\/target>'
    
    if [ $family == "ic" ];
    then

      patternStr='<patternset id="lrg.tests"> <include name="**\/*AllTest*.java"\/> <exclude name="**\/.ade_path\/**"\/>     <exclude name="**\/*All*SeTest.java"\/> <\/patternset> ' 

    elif [ $family == "scm" ];
    then
      # Change patternStr as per Bug 12354131 - transaction to check btea_bug-11893672
      patternStr='<patternset id="lrg.tests"> <include name="**\/*All*Tests.java" \/> <exclude name="**\/.ade_path\/**" \/>       <\/patternset> '   
      
    #Bug 12911834  
    elif [ $family == "prc" ];
    then

      patternStr='<patternset id="lrg.tests"> <include name="**\/*RG.java"\/> <include name="**\/*BuildSuite.java"\/> <include name="**\/*Test.java" \/> <include name="**\/Test*.java" \/> <exclude name="**\/*SeTests.java" \/> <exclude name="**\/.ade_path\/**"\/> <\/patternset> '
          
    fi
    
    workspaceStr=""
    testWorkspaceStr=""
    for mydir in `cat $txnFiles| cut -f 2-4 -d "/" | sort | uniq`
    do
  
      for workspaces in `ls $ADE_VIEW_ROOT/fusionapps/$mydir/*.jws`
      do                                                  

        workspace=`echo $workspaces | cut -f 6-8 -d "/" | sed 's/\//\\\\\//g' `
        workspaceStr=$workspaceStr"<include name=\"$workspace\"\/>"
        testWorkspaceStr=$testWorkspaceStr'<custom:test workspace="${basedir}\/'$workspace'" postBuildCallback="emma-instrument">''<instrumentedPath refid="emma.lib" \/> <testProject regex=".*Test"><fileset testdir="src"><patternset refid="${test.filter}"\/><\/fileset><\/testProject><\/custom:test>' 
        
        echo -e $workspaceStr
        echo -e $testWorkspaceStr
        
      done
    
    done
    

    ade co -nc $ADE_VIEW_ROOT/fusionapps/$family/build.xml
    ade unco $ADE_VIEW_ROOT/fusionapps/$family/build.xml
    ade mkprivate $ADE_VIEW_ROOT/fusionapps/$family/build.xml

    #Always Update connection string in build.xml 
    actualUpdateConnStr="<target.*name=\"updateConnXml\""

    replaceUpdateConnStr1='<target name="updateConnXml"  xmlns:custom="antlib:oracle.anttasks"> <custom:appDbUpdater '
    
    dbUpdateConnStr="host=\"$host\" port=\"$port\" sid=\"$sid\" user=\"$uid\" pass=\"$pswd\">"
    
    replaceUpdateConnStr4=' <\/custom:appDbUpdater> <\/target>'

    replaceUpdateConnStr=$replaceUpdateConnStr1$dbUpdateConnStr$replaceUpdateConnStr4  
    grep '<target.*name=\"updateConnXml\"' $ADE_VIEW_ROOT/fusionapps/$family/build.xml>/dev/null
    if [ $? -eq 0 ]; then

      myvar="sed '/$actualUpdateConnStr/,/<\/target>/ s/$actualUpdateConnStr.*<\/target>/$replaceUpdateConnStr/' $ADE_VIEW_ROOT/fusionapps/$family/build.xml | uniq > $ADE_VIEW_ROOT/fusionapps/$family/mybuildconn.xml"
      echo $myvar
      eval $myvar 

      myvar="sed '/$actualUpdateConnStr/,/<\/target>/ s/.*/$replaceUpdateConnStr/' $ADE_VIEW_ROOT/fusionapps/$family/build.xml | uniq > $ADE_VIEW_ROOT/fusionapps/$family/mybuildconn.xml"
      echo $myvar
      eval $myvar
		 
    else
	    myvar="sed 's/<\/project>/$replaceUpdateConnStr<\/project>/' $ADE_VIEW_ROOT/fusionapps/$family/build.xml | uniq > $ADE_VIEW_ROOT/fusionapps/$family/mybuildconn.xml"
		echo $myvar
		eval $myvar 
    fi	    
         
    mv $ADE_VIEW_ROOT/fusionapps/$family/mybuildconn.xml $ADE_VIEW_ROOT/fusionapps/$family/build.xml
    
    if [ -z "$junitBuildFile" ];
    then

      buildfile=$ADE_VIEW_ROOT/fusionapps/$family/build.xml
      echo -e "Build file to be used is not passed as a parameter so using default build file $buildfile" >> $junitoutfileformail  
      
    else
    
      buildfile=$junitBuildFile
      echo -e "Build file to be used is passed as a parameter and it is $buildfile" >> $junitoutfileformail      
      
      ade co -nc $buildfile
      ade unco $buildfile
      ade mkprivate $buildfile

    fi
    if [ $family == "prj" -a -n "$junitBuildFile" -o $family == "hcm" -a -n "$junitBuildFile" ];
    then
     dbHostStr="<property name=\"db.host\" value=\".*\"\/>"
     dbHostReplaceStr="<property name=\"db.host\" value=\"$host\"\/>"
     dbPortStr="<property name=\"db.port\" value=\".*\"\/>"
     dbPortReplaceStr="<property name=\"db.port\" value=\"$port\"\/>"
     dbSidStr="<property name=\"db.sid\" value=\".*\"\/>"
     dbSidReplaceStr="<property name=\"db.sid\" value=\"$sid\"\/>"     
     dbUserStr="<property name=\"db.user\" value=\".*\"\/>"

     dbUserReplaceStr="<property name=\"db.user\" value=\"$uid\"\/>"     
     dbPassStr="<property name=\"db.pass\" value=\".*\"\/>"
     dbPassReplaceStr="<property name=\"db.pass\" value=\"$pswd\"\/>" 
     
     #dbvar="sed -i 's/$dbHostStr/$dbHostReplaceStr/g;s/$dbPortStr/$dbPortReplaceStr/g;s/$dbSidStr/$dbSidReplaceStr/g;s/$dbUserStr/$dbUserReplaceStr/g;s/$dbPassStr/$dbPassReplaceStr/g'  $ADE_VIEW_ROOT/fusionapps/$family/build.xml | uniq > $ADE_VIEW_ROOT/fusionapps/$family/myhbuildconn.xml"
     dbvar="sed 's/$dbHostStr/$dbHostReplaceStr/g' $ADE_VIEW_ROOT/fusionapps/$family/build.xml | uniq > $ADE_VIEW_ROOT/fusionapps/$family/myhbuildconn.xml"
     echo $dbvar
     eval $dbvar

     dbvar="sed 's/$dbPortStr/$dbPortReplaceStr/g' $ADE_VIEW_ROOT/fusionapps/$family/myhbuildconn.xml | uniq > $ADE_VIEW_ROOT/fusionapps/$family/mypbuildconn.xml"
     echo $dbvar
     eval $dbvar

     dbvar="sed 's/$dbSidStr/$dbSidReplaceStr/g' $ADE_VIEW_ROOT/fusionapps/$family/mypbuildconn.xml | uniq > $ADE_VIEW_ROOT/fusionapps/$family/mysbuildconn.xml"
     echo $dbvar
     eval $dbvar

     dbvar="sed 's/$dbUserStr/$dbUserReplaceStr/g' $ADE_VIEW_ROOT/fusionapps/$family/mysbuildconn.xml | uniq > $ADE_VIEW_ROOT/fusionapps/$family/myubuildconn.xml"
     echo $dbvar
     eval $dbvar

     dbvar="sed 's/$dbPassStr/$dbPassReplaceStr/g' $ADE_VIEW_ROOT/fusionapps/$family/myubuildconn.xml | uniq > $ADE_VIEW_ROOT/fusionapps/$family/mypwbuildconn.xml"
     echo $dbvar
     eval $dbvar

     mv $ADE_VIEW_ROOT/fusionapps/$family/mypwbuildconn.xml $ADE_VIEW_ROOT/fusionapps/$family/build.xml     
     
    fi   
    
    
    if [ -z "$junitAntTarget" ];
    then

      junittarget="test"
      
      emmainstrstr='<path id="emma.classes.to.instrument">'

      emmasrcstr='<path id="emma.sources">'
      #This path can be deleted when the <path> and </path> in the same line
      emmavar="sed '/$emmainstrstr/,/<\/path>/ s/$emmainstrstr.*<\/path>//' $buildfile | uniq > $ADE_VIEW_ROOT/fusionapps/$family/mybuildemma.xml"
      echo $emmavar
      eval $emmavar

      #This path should also be deleted when the <path> and </path> in different lines
      emmavar="sed '/$emmainstrstr/,/<\/path>/ s/.*//' $ADE_VIEW_ROOT/fusionapps/$family/mybuildemma.xml | uniq > $buildfile"
      echo $emmavar
      eval $emmavar

      emmavar="sed '/$emmasrcstr/,/<\/path>/ s/$emmasrcstr.*<\/path>//'  $buildfile | uniq > $ADE_VIEW_ROOT/fusionapps/$family/mybuildemma.xml"
      echo $emmavar        
      eval $emmavar    

      emmavar="sed '/$emmasrcstr/,/<\/path>/ s/.*//' $ADE_VIEW_ROOT/fusionapps/$family/mybuildemma.xml | uniq > $buildfile"
      echo $emmavar        
      eval $emmavar      
      replaceStr=$patternStr$replaceStr1$dbStr$replaceStr2$workspaceStr$replaceStr4$testWorkspaceStr$replaceStr6          
      myvar="sed '/$actualStr/,/<\/target>/ s/.*/$replaceStr/' $buildfile | uniq > $ADE_VIEW_ROOT/fusionapps/$family/mybuild.xml"
      echo $myvar
      eval $myvar 
	  
      mv $ADE_VIEW_ROOT/fusionapps/$family/mybuild.xml $buildfile  
      
    else
    
      junittarget="$junitAntTarget"

    fi

    cp $ADE_VIEW_ROOT/fusionapps/$family/build.xml $ADE_VIEW_ROOT/fusionapps/$family/build.xml_bkup
    cp $buildfile $buildfile"_bkup"

    FailCount=0
    
    echo -e "" > $junitprojectssublbafilelist

    if [ -z "$junitprojectspassedasparameter" ];
    then

      junitprojectdirs1=`cat $txnFiles| grep -v '/[a-zA-Z]*[Uu]i/' |grep -iv dbschema|grep -v '/WEB-INF/'| grep -v "service-definition\.xml" | grep fusionapps/$family/components | grep -i model | sed s/Test//g | grep '/src/' | sed 's/\/src\/.*//' | cut -f 2 -d .`

      junitprojectdirs2=`cat $txnFiles | grep -v '/[a-zA-Z]*[Uu]i/' |grep -iv dbschema|grep -v '/WEB-INF/'| grep -v "service-definition\.xml" | grep -v '/src/' | grep fusionapps/$family/components | grep -i model | sed s/Test//g | cut -f 1 -d . | sed 's/\/[a-zA-Z_]*$//' `
      
      junitprojectdirstemp=$junitprojectdirs1" "$junitprojectdirs2
      
      junitprojectdirs=`echo $junitprojectdirstemp | tr ' ' '\n' | sort | uniq`
    
    else
    
      junitprojectdirs=`echo $junitprojectspassedasparameter | tr "," "\n" | sort | uniq`
      junitprojectspassedasparameterflag=1

      echo -e "" >> $junitoutfileformail
      echo -e "JUnit project(s) to be run has been passed as a parameter, so overriding the logic to determine JUnit project(s) based on the files involved in the transaction." >> $junitoutfileformail
      echo -e "JUnit will be run for project(s) $junitprojectspassedasparameter" >> $junitoutfileformail
    
    fi

    for i in $junitprojectdirs
    do

      
      if [[ $junitprojectspassedasparameterflag -eq 1 ]]; then

        #junitProjectDir should be one directory above the *test directory
        customJunitProject=`basename $i`
        
        customJunitProjectDir=`dirname $i`
        i=`dirname $customJunitProjectDir`
        junitProjectDir=$i

      else 

        junitProjectDir=$i
        
      fi  

      if [ ${junitProjectDir:${#junitProjectDir}-1:1} = "/" ]; 
      then
            junitProjectDir=${junitProjectDir:0:${#junitProjectDir}-1}
      fi
  
      if [[ $junitprojectspassedasparameterflag -eq 0 ]]; then

        if `ls $ADE_VIEW_ROOT/$junitProjectDir/modelTest | grep -iq "ModelTest.jpr\|EssTest.jpr"` || `ls $ADE_VIEW_ROOT/$junitProjectDir/uiModelTest | grep -iq "ModelTest.jpr\|EssTest.jpr"` || `ls $ADE_VIEW_ROOT/$junitProjectDir/protectedModelTest | grep -iq "ModelTest.jpr\|EssTest.jpr"` || `ls $ADE_VIEW_ROOT/$junitProjectDir/*Model*Test | grep -iq "Model.*Test.jpr\|EssTest.jpr"`
        then

          lba=`basename \`dirname $i\``
          sublba=`basename $i`
          
        else

          #Some products dont have a sublba and JUnit project is located under lba only
          #Check for that only if basename $i returns model or publicEssModel

          if `basename $i | grep -iq model`
          then


            lba=`basename \`dirname $i\``
            parentdir=`dirname $i`
            sublba=`basename $i`

          else

            lba=`basename \`dirname $i\``
            sublba=`basename $i`
            sublbaoutputfile="$junitoutfileformail"."$lba"."$sublba".`date +%s`
            echo -e "" > $sublbaoutputfile

            echo -e $sublbaoutputfile >> $junitprojectssublbafilelist
   
            echo -e "Running JUnit now for files part of $i" >> $sublbaoutputfile
            echo -e "" >> $sublbaoutputfile
            
            echo -e "Error!!! JUnit Project not found for files part of $i" >> $sublbaoutputfile
            
            echo -e "" >> $sublbaoutputfile
            echo -e "Running JUnit completed for files part of $i" >> $sublbaoutputfile
            echo -e "" >> $sublbaoutputfile

            exitStatus=1
            
            continue
            
          fi
          
        fi
      
      else
      
        lba=`basename \`dirname $i\``
        sublba=`basename $i`
      
      fi
   
      sublbaoutputfile="$junitoutfileformail"."$lba"."$sublba".`date +%s`
      echo -e "" > $sublbaoutputfile

      echo -e $sublbaoutputfile >> $junitprojectssublbafilelist
   
      echo -e "" >> $sublbaoutputfile
      
      if [[ $junitprojectspassedasparameterflag -eq 0 ]]; then
      
        echo -e "Running JUnit now for files part of $i" >> $sublbaoutputfile
        echo -e "" >> $sublbaoutputfile
        

          j=`ls $ADE_VIEW_ROOT/$junitProjectDir"Test" | grep -i "Model.*Test.jpr\|EssTest.jpr" | cut -f 1 -d "." | uniq` 

                  if [ -z "$j" ];
                  then
            
                    echo -e "Error!!! JUnit Project not found for files part of $i" >> $sublbaoutputfile

                    echo -e "" >> $sublbaoutputfile
                    echo -e "Running JUnit completed for files part of $i" >> $sublbaoutputfile
                    echo -e "" >> $sublbaoutputfile

                    exitStatus=1
              
                    continue
              
                  fi  
      else

        echo -e "Warning!!! Running JUnit now for JUnit project passed as parameter" >> $sublbaoutputfile      
        echo -e "" >> $sublbaoutputfile

        j=`ls $ADE_VIEW_ROOT/$customJunitProjectDir | grep -io "$customJunitProject" | cut -f 1 -d "." | sort | uniq` 
        

        if [ -z "$j" ];
        then

          echo -e "Error!!! JUnit Project $customJunitProject which has been passed as a parameter is not found in $ADE_VIEW_ROOT/$customJunitProjectDir Please ensure full path to the project is provided starting with fusionapps" >> $sublbaoutputfile   
          echo -e "" >> $sublbaoutputfile

          echo -e "Warning!!! Running JUnit completed for JUnit project passed as parameter" >> $sublbaoutputfile      
          
          exitStatus=1
          
          continue
        
        
        fi
      
      fi

        
      if `grep -q $j $junitprojectsrunlist`
      then

        echo -e "JUnit has already been run for project $j" >> $sublbaoutputfile

      else
  

        echo -e "JUnit running for project $j" >> $sublbaoutputfile
         
        echo -e "$j" >> $junitprojectsrunlist

        if [ -z "$junitAntTarget" ];
        then

          cp $buildfile"_bkup" $buildfile

          modelprojectname=`echo $j | sed 's/Test/.jpr/'`
          codecoveragesrcproject=`find $ADE_VIEW_ROOT/$junitProjectDir -name $modelprojectname`
          codecoveragesrcdir=`dirname $codecoveragesrcproject | sed 's/\//\\\\\//g'`
        
          emmainstrstr='<path id="myemmaclassestoinstrument"><\/path>'
          emmainstrreplacestr='<path id="emma.classes.to.instrument"> <dirset dir="'$codecoveragesrcdir'">             <include name="**\/classes" \/> <exclude name="**\/dbSchema\/**" \/> <exclude name="**\/di\/**" \/> <exclude name="**\/noship\/**" \/> <exclude name="**\/publicUi\/**" \/> <exclude name="**\/ui\/**" \/> <exclude name="**\/test\/**" \/> <exclude name="**\/*Test\/**" \/> <exclude name="**\/Trace*\/**" \/> <exclude name="**\/.ade_path\/**"\/> <\/dirset> <\/path>'
          emmasrcstr='<path id="myemmasources"><\/path>'
          emmasrcreplacestr='<path id="emma.sources"> <dirset dir="'$codecoveragesrcdir'"> <include name="**\/src"\/>      <exclude name="**\/.ade_path\/**"\/> <\/dirset> <\/path>' 

          emmavar="sed 's/$emmainstrstr/$emmainstrreplacestr/' $buildfile | uniq > $ADE_VIEW_ROOT/fusionapps/$family/mybuildemma.xml"
          echo $emmavar
          eval $emmavar

          emmavar="sed 's/$emmasrcstr/$emmasrcreplacestr/' $ADE_VIEW_ROOT/fusionapps/$family/mybuildemma.xml | uniq > $buildfile"
          echo $emmavar        
          eval $emmavar
          
        fi  

        rm $testxmldir/metadata.emma $testxmldir/coverage.ec $testxmldir/coverage.xml

	if [[ "X$useDBFactory" = "X1" ]];then
		DBFactory='-Dtest.jvmargs="-Djavax.xml.parsers.DocumentBuilderFactory=oracle.xml.jaxp.JXDocumentBuilderFactory -Doracle.jdbc.createDescriptorUseCurrentSchemaForSchemaName=true"'
	else
		DBFactory=""
	fi
        echo -e "ant -Dtest.lrg=true -Dtest.project=$j -f $buildfile $junittarget $DBFactory"
        ant -logger org.apache.tools.ant.NoBannerLogger $DBFactory -Dtest.lrg=true -Dtest.forceChange=true -Dtest.project=$j -f $buildfile $junittarget test-report > "$destdir"/"$j".jut 2>&1

        mkdir -p "$destdir"/"$j"
        grep -n "Test Class:" "$destdir"/"$j".jut | cut -f 5 -d ":" > "$destdir"/"$j"/testclassesrun.txt
        for k in `cat "$destdir"/"$j"/testclassesrun.txt`
        do

          mv $testxmldir/TEST-"$k"-*.xml $destdir/"$j"/
          
        done 

        #TEST-*[0-9]*.xml will be generated if ant runs JUnit successfully
        if [ ! -f $destdir/"$j"/TEST-*[0-9][0-9]*.xml ]
        then

          echo -e "" >> $sublbaoutputfile

          if `grep -q "BUILD FAILED" "$destdir"/"$j".jut `
          then
          
            echo -e "The tool was not able to run JUnit for project $j" >> $sublbaoutputfile    
            echo -e "` grep -A3 \"BUILD FAILED\" \"$destdir\"/\"$j\".jut `" >> $sublbaoutputfile    
            exitStatus=1
          
          elif ` grep -q "$j failed during build" "$destdir"/"$j".jut `
          then

            echo -e "" >> $sublbaoutputfile
            echo -e "The tool was not able to run JUnit for project $j" >> $sublbaoutputfile
            grep -B1 "$j failed during build" "$destdir"/"$j".jut >> $sublbaoutputfile
            grep "Error" "$destdir"/"$j".jut | grep -v "Error: unable to copy to output directory, destination is read-only" >> $sublbaoutputfile
  
            if ` grep -q "Error: unable to copy to output directory, destination is read-only" "$destdir"/"$j".jut `
            then

              echo -e "" >> $sublbaoutputfile
              echo -e "The directories are not fully expanded. Please expand them using 'ade expand' command and run the tool again." >> $sublbaoutputfile
  
            fi
            exitStatus=1            
            
          elif ` grep -q "Total number of valid JUnit test class:0" "$destdir"/"$j".jut `
          then

              echo -e "The tool was not able to run JUnit for project $j as no valid JUnit test class was found" >> $sublbaoutputfile
              exitStatus=1
              
          elif `! cat \`grep "Processing.*.jws" "$destdir"/"$j".jut | cut -f 3 -d " "\` | grep -q $j `  
          then

              echo -e "The tool was not able to run JUnit for project $j as $j is not added to `grep \"Processing.*.jws\" \"$destdir\"/\"$j\".jut | cut -f 3 -d \" \"`" >> $sublbaoutputfile
              exitStatus=1
            
          else 

            echo -e "" >> $sublbaoutputfile
            echo -e "The tool was not able to run JUnit for files part of $i as the ant command running JUnit is not working correctly. Please run JUnit for project $j from within JDeveloper." >> $sublbaoutputfile
            
            if [[ $exitStatus -eq 0 ]];then
              exitStatus=2
            fi
            
          fi
    
    
          FailCount=`expr $FailCount + 100`; #Setting it to more than 100 to have a unique number to represent ant command not working error
            
        else 

          mv $testxmldir/test-report.html $destdir/"$txn"_"$j"_testreport.html
         
          if grep -q "\[custom:test\] INFO: FAILURES" "$destdir"/"$j".jut
          then
         
            echo -e "JUnit failure reported for project $j" >> $sublbaoutputfile
#            echo -e "JUnit Execution Detail Report for project $j uploaded to ftp://bugftp.us.oracle.com/upload/"$bugftpdir/"$txn"_"$j"_testreport.html >> $sublbaoutputfile            
            echo -e "" >> $sublbaoutputfile
            echo -e "Test Execution Summary" >> $sublbaoutputfile
            echo -e "" >> $sublbaoutputfile

            grep -n "Test Class:" "$destdir"/"$j".jut | cut -f 1 -d ":" > "$destdir"/"$j"/testclassesrunlinenos.txt
 
            grep -n 'Failures:\|tests)' "$destdir"/"$j".jut | cut -f 1 -d ":" > "$destdir"/"$j"/testexecutionsummary.txt
 
            for lineno in `paste "$destdir"/"$j"/testclassesrunlinenos.txt "$destdir"/"$j"/testexecutionsummary.txt `
            do

              echo $lineno

              var="sed -n '$lineno p' \"$destdir\"/\"$j\".jut"
              if  `eval $var | grep -q tests`
              then

		echo -e "Tests run:`eval $var | grep tests | cut -f 2- -d\( | cut -f 1 -d\  `,  Failures: 0,  Errors: 0" >> $sublbaoutputfile

              else

                var="sed -n '$lineno p' \"$destdir\"/\"$j\".jut >> $sublbaoutputfile"
                eval $var 

              fi              
 
            done        
            
            FailCount=`expr $FailCount + 1`
            exitStatus=1


          else

            if `cat "$destdir"/"$j".jut | grep -iq  "The current TestProject($j) was killed because timeout(60 min) was reached"`
            then
                
              echo -e "" >> $sublbaoutputfile
              cat "$destdir"/"$j".jut | grep "The current TestProject($j) was killed because timeout(60 min) was reached" |  cut -f 2- -d " " >> $sublbaoutputfile
              echo -e "This will be treated as a timeout and not success as all the test cases might not have been executed so please run JUnit for project $j from within JDeveloper or run this tool again by changing the JUnit db to the one closest to your location." >> $sublbaoutputfile
              FailCount=1  
              if [[ $exitStatus -eq 0 ]];then
                exitStatus=4
              fi
              
            else

              echo -e "JUnit Success for project $j" >> $sublbaoutputfile
#              echo -e "JUnit Execution Detail Report for project $j uploaded to ftp://bugftp.us.oracle.com/upload/"$bugftpdir/"$txn"_"$j"_testreport.html >> $sublbaoutputfile            
              
              echo -e "" >> $sublbaoutputfile
              echo -e "Test Execution Summary" >> $sublbaoutputfile
              echo -e "" >> $sublbaoutputfile
              
              grep -n "Test Class:" "$destdir"/"$j".jut | cut -f 4- -d ":" | cut -f 2- -d " " > "$destdir"/"$j"/testclassesrunlinenos.txt

              cat "$destdir"/"$j".jut | grep -nA1 "\[custom:test\] INFO: OK" | grep tests | cut -f 2- -d "(" | cut -f 1 -d " " > "$destdir"/"$j"/totaltestsrunperclass.txt

              # Truncate the file
              cat /dev/null > "$destdir"/"$j"/testexecutionsummary.txt
              for totaltests in `cat "$destdir"/"$j"/totaltestsrunperclass.txt`
              do

                echo -e "Tests run:$totaltests,  Failures: 0,  Errors: 0" >> "$destdir"/"$j"/testexecutionsummary.txt

              done 

              paste "$destdir"/"$j"/testclassesrunlinenos.txt "$destdir"/"$j"/testexecutionsummary.txt -d "\n" >> $sublbaoutputfile

            fi  
                
          fi # End of if grep -q FAILURES $ADE_VIEW_ROOT/fusionapps/fin/"$j".jut
         
          for k in `ls "$destdir"/"$j"/TEST-*.xml`
          do

            mv "$k" "$k"_old
          
          done 
          
          if [  -f $testxmldir/coverage.html ]
          then

            mv $testxmldir/coverage.html $destdir/"$txn"_"$j"_codecoveragereport.html
            mv $testxmldir/_files $destdir
              
            java -cp "$ADE_VIEW_ROOT/fatools/emma/emmaMod.jar" emma report -r txt -sp "$codecoveragesrcdir/src/:" "-Dreport.out.file=$destdir/"$txn"_"$j"_codecoveragereport.txt" -in $testxmldir/metadata.emma -in $testxmldir/coverage.ec -Dreport.verbosity=info -Dreport.columns=name,class,method,block,line -Dreport.depth=method -Dreport.metrics=method:70,block:80,line:80,class:100 -Dreport.sort=+name,+block,+class,+method,+line -Dreport.units=instr

            if `cat $destdir/"$txn"_"$j"_codecoveragereport.txt | grep "all classes" | grep -q !`
            then

              echo -e "" >> $sublbaoutputfile
              echo -e "Please note that the quality of JUnit test cases is not of production quality">> $sublbaoutputfile
              echo -e "Please write more or better JUnit test cases and aim for class:100%,method:70%,block:80%,line:80%">> $sublbaoutputfile
              echo -e "" >> $sublbaoutputfile                  
              echo -e "Right now it is" >> $sublbaoutputfile
              
              grep -B1 "all classes" $destdir/"$txn"_"$j"_codecoveragereport.txt >> $sublbaoutputfile

              if [ $exitStatus -eq 0 -o  $exitStatus -eq 1 ]; 
              then
	              exitStatus=`expr $exitStatus + 5`
              fi	              

            fi

            echo -e "" >> $sublbaoutputfile
#            echo -e "Code Coverage Detail Report for project $j uploaded to ftp://bugftp.us.oracle.com/upload/"$bugftpdir/"$txn"_"$j"_codecoveragereport.html >> $sublbaoutputfile            
                
          else
              
            echo -e "" >> $sublbaoutputfile
            echo -e "Code Coverage report was not generated successfully" >> $sublbaoutputfile

          fi


        fi # End of if [ ! -f $ADE_VIEW_ROOT/fusionapps/fin/TEST-*[0-9]*.xml ]
	 
      fi #End of if `grep -q $j $junitprojectsrunlist`
     
  
      echo -e "" >> $sublbaoutputfile
      
      if [[ $junitprojectspassedasparameterflag -eq 1 ]]; then

        echo -e "Warning!!! Running JUnit completed for JUnit project passed as parameter" >> $sublbaoutputfile      
      
      else
      
        echo -e "Running JUnit completed for files part of $i" >> $sublbaoutputfile
        
      fi
        
      echo -e "" >> $sublbaoutputfile

    done

    for l in `cat $junitprojectssublbafilelist`
    do

      cat $l >> $junitoutfileformail

    done

    cp $ADE_VIEW_ROOT/fusionapps/$family/build.xml $ADE_VIEW_ROOT/fusionapps/$family/build.xml_modifiedbypremerge
    cp $buildfile $buildfile"_modifiedbypremerge"
    
    # Restore modified build files
    ade co -nc $ADE_VIEW_ROOT/fusionapps/$family/build.xml
    ade unco $ADE_VIEW_ROOT/fusionapps/$family/build.xml
    
    ade co -nc $buildfile
    ade unco $buildfile

    tstop_jut="$(date +%s)"
    telapsed_jut="$(expr $tstop_jut - $tstart_jut)"

    echo -e "Completed for transaction $txn." >> $junitoutfileformail
    echo -e "Elapsed time: $(date -d "1970-01-01 $telapsed_jut sec" +%H:%M:%S)" >> $junitoutfileformail

  else #Else of IF id 002

    echo -e "No files involved in the transaction $txn, so JUnit was not run" >> $junitoutfileformail
    exitStatus=3
  
  fi  #End of IF id 002
  
else

 echo -e "No files modified in the transaction $txn requires JUnit to be run, so skipping it." >> $junitoutfileformail
 exitStatus=3
   
fi #End of IF id 001


echo -e "" >> $junitoutfileformail

FailCount=`grep -c ' failure' $junitoutfileformail`

exit $exitStatus

