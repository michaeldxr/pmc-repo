#! /bin/ksh

export txn=$(ade pwv | grep VIEW_TXN_NAME | tr -d ' '| cut -d ':' -f2) 
export runOatsIAT=1
export family="fin"

latestLabel=$(ade showlabels|tail -1)
LABEL=$(ade pwv | grep 'VIEW_LABEL    : ' | tr -d ' ' | cut -d ':' -f2)
if [[ $LABEL != $latestLabel ]];then
                echo "Latest Label : $latestLabel"
                echo "Current Label : $LABEL"
                echo "*** There is a new label available. Current view must be refreshed to the latest before proceeding."
                exit
fi

$ADE_VIEW_ROOT/fatools/opensource/jauditFixScripts/FinPreMerge/bin/finOatsVerify.sh
