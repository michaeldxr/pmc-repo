create or replace
PROCEDURE upload_time_stat(
	v_txn IN VARCHAR2, v_bug_no IN VARCHAR2, 
	v_username IN VARCHAR2, v_family IN VARCHAR2, 
	v_label IN VARCHAR2, v_hostname IN VARCHAR2, 
	v_elapsed_time IN NUMBER, v_time_invoked IN VARCHAR2, 
	v_log_blob IN VARCHAR2)
AS 
	v_transaction_id NUMBER;
BEGIN

	INSERT INTO PROFILE_TXN(TRANSACTION_NAME, BUG_NO, FAMILY, LABEL, 
	USERNAME, HOSTNAME, TIME_INVOKED, ELAPSED_TIME) 
	VALUES(v_txn, v_bug_no, v_family, v_label, v_username, 
	v_hostname, to_timestamp(v_time_invoked, 'YYYY-MM-DD HH24:MI:SS'), 
	v_elapsed_time) RETURNING transaction_id into v_transaction_id;

	INSERT INTO PROFILE_CMD(transaction_id, command, exit_status, 
			elapsed_time, system_mode, user_mode,cpu_percent)
	SELECT v_transaction_id as transaction_id,
		regexp_substr(entry, '[^,]+',1,1) as command,
		regexp_substr(entry, '[^,]+',1,2) as exit_status,
		regexp_substr(entry, '[^,]+',1,3) as elapsed_time,
		regexp_substr(entry, '[^,]+',1,4) as system_mode,
		regexp_substr(entry, '[^,]+',1,5) as user_mode,
		regexp_substr(entry, '[^,]+',1,6) as cpu_percent
	FROM (
		SELECT regexp_substr(v_log_blob,'[^~]+',1,level) as entry FROM DUAL
		CONNECT BY regexp_substr(v_log_blob,'[^~]+', 1, level) IS NOT NULL
	) time_log;
END;
