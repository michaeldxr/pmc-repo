#!/bin/ksh
#
# $Header: fatools/opensource/jauditFixScripts/FinPreMerge/bin/ShowProgress.ksh /main/3 2011/04/12 12:20:38 bomathew Exp $
#
# ShowProgress.ksh
#
# Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved. 
#
#    NAME
#      ShowProgress.ksh - To show a dummy progress bar
#
#    DESCRIPTION
#      Show the message and a dummy progress bar as long as any of the passed background processes is active.
#
#    NOTES
#      Usage : ShowProgress.ksh <Message> <"list of pids">
#	e.g. : ShowProgress.ksh "Please wait while we test your patience" "1234 5678 9832"
#
#    MODIFIED   (MM/DD/YY)
#    bomathew    02/21/11 - Creation
#


BarLength=10
BarChar="."
CurLoc=0

Progress()
{
        if [[ $CurLoc -le 0 ]];then
                dir=0
        elif [[  $CurLoc -ge $BarLength ]];then
                dir=1
        fi
        if [[ $dir -eq 0 ]];then
                chr=$BarChar
                (( CurLoc=(CurLoc +1) ))
        else
                chr="\b"
                (( CurLoc=(CurLoc -1) ))
        fi
        printf $chr
}

Msg=$1
PidList='^'`echo $2|sed 's/\ /$|^/g'`'$'

trap "CurLoc=0;print -n '$Msg '" 18

print -n "$Msg "

while true;do
	ps -ef | awk '{print $2}' | egrep "$PidList" >/dev/null 2>&1
	if [[ $? -eq 0 ]];then
		Progress
		sleep 1
	else
		break
	fi
done

