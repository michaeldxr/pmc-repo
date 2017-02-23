REM $Header: fatools/opensource/jauditFixScripts/FinPreMerge/apps_flex_check.sql /main/1 2012/11/19 08:40:16 sokuda Exp $

REM adxml: <src_file bootstrap="~BOOTSTRAP" version="~VERSION"
REM adxml:     translation_level="~TRANS_LEVEL" techstack_level ="~TXK_LEVEL"
REM adxml:     verticalisation_level="~VERT_LEVEL" custom_level="~CUST_LEVEL"
REM adxml:     language="~LANG" needs_translation="~TRANSLATION" ship="~SHIP"
REM adxml:     localization="~LOCALIZATION" object_type="~OBJECT_TYPE">
REM adxml:   <abstract_file te="~PROD" subdir="~PATH"
REM adxml:     file_basename="~BASENAME" file_type="~FILE_TYPE"/>
REM adxml:     <metadata>
REM adxml:       <action identifier="DB_END" category="DB_END"
REM adxml:         portion="D" online_category="N">
REM adxml:         <apply_action>
REM adxml:           <action_details name="sqlplus" >
REM adxml:             <args></args>
REM adxml:           </action_details>
REM adxml:         </apply_action>
REM adxml:         <analysis_data>
REM adxml:         </analysis_data>
REM adxml:         <ahead_of_time_run_conditions>
REM adxml:           <check_file check="Y"/>
REM adxml:         </ahead_of_time_run_conditions>
REM adxml:       </action>
REM adxml:     </metadata>
REM adxml: </src_file>

REM +======================================================================+
REM |   Copyright (c) 2010 Oracle Corporation Belmont, California, USA     |
REM |                       All rights reserved.                           |
REM +======================================================================+
REM FILENAME
REM   apps_flex_check.sql
REM
REM CREATED BY
REM   Shintaro Okuda
REM
REM DESCRIPTION
REM       Flexfields Integrity Check Script
REM
REM NOTES
REM  - Created based on $atgpf_top/applcore/db/sql/fnd_flex_check.sql,
REM    which calls FND_FLEX_DIAGNOSTICS package procedures/functions
REM  - Prints a warning if FND_FLEX_DIAGNOSTICS package status is not valid
REM  - Prints a warning if GET_RESULTS_COUNT package function does not exists,
REM    i.e., the package is old
REM  - Prints an error with a number of invalid Flexfields and the details if 
REM    found
REM
REM MODIFIED                   (MM/DD/YY)
REM   Shintaro Okuda		11/12/12 - Created
REM
REM +======================================================================+

CONNECT &&1/&&2;

SET VERIFY OFF
SET HEADING OFF

SET ECHO OFF
SET FEEDBACK OFF
SET NUMWIDTH 10
SET LINESIZE 200
SET TRIMSPOOL ON
SET TAB OFF
SET PAGESIZE 0

SET SERVEROUTPUT ON SIZE UNLIMITED FORMAT WRAPPED;

WHENEVER SQLERROR EXIT FAILURE ROLLBACK;
WHENEVER OSERROR EXIT FAILURE ROLLBACK;


VARIABLE v_status VARCHAR2(100);

DECLARE
  l_count_obj  NUMBER;
  l_count_proc NUMBER;
  l_count_err  NUMBER;

  PROCEDURE print(p_text in varchar2) IS
  BEGIN 
    DBMS_OUTPUT.PUT_LINE(p_text);
  END print;

BEGIN

  SELECT COUNT(*) INTO l_count_obj
  FROM user_objects
  WHERE object_name = 'FND_FLEX_DIAGNOSTICS'
  AND   status = 'VALID';

  IF (l_count_obj = 2) THEN

    SELECT COUNT(*) INTO l_count_proc
    FROM user_procedures
    WHERE object_name = 'FND_FLEX_DIAGNOSTICS'
    AND   procedure_name = 'GET_RESULTS_COUNT';

    IF (l_count_proc = 1) THEN

      fnd_flex_diagnostics.check_flx_all;

      EXECUTE IMMEDIATE
        'SELECT fnd_flex_diagnostics.get_results_count(''ERROR'') FROM DUAL'
        INTO l_count_err;

      IF (NVL(l_count_err,0) = 0) THEN
        :v_status := 'SUCCESS: Flexfields are valid.';
      ELSE
        :v_status := 'ERROR: Flexfields are invalid. Error count: ' || l_count_err;
      END IF;

    ELSE
      :v_status := 'WARNING: FND_FLEX_DIAGNOSTICS package is old.';
    END IF;

  ELSE
    :v_status := 'WARNING: FND_FLEX_DIAGNOSTICS package is invalid.';
  END IF;

  print('Flexfields Metadata Check Status'); 
  print('---------------------------------'); 
  print(:v_status); 

  IF (:v_status like 'ERROR%') THEN 
    print('Errors'); 
    print('---------------------------------'); 
    DECLARE 
      l_cur sys_refcursor; 
      l_txt varchar2(4000); 
    BEGIN 
      OPEN l_cur FOR 'SELECT * FROM TABLE(fnd_flex_diagnostics.get_results_table(''ERROR''))'; 
      LOOP 
        FETCH l_cur INTO l_txt;
        EXIT WHEN l_cur%NOTFOUND; 
        print(l_txt); 
      END LOOP; 
      CLOSE l_cur; 
    END; 
  END IF;

END;
/

COMMIT;
EXIT;
