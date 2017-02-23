package oracle.apps.premerge;

/**
 * ARU transaction validation for ER 21512175.
 * @author mengxu
 *
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ARUValidation {

	private static String transName = "";
	private static String series = "";
	private static String category = "";
	private static String rel_ver = "";
	private static String ver_abbr = "";
	private static String family = "";
	private static String newest_ver = "11.1.12.0.0";

	private static String bugNo = "";
	private static String backportBugNo = "";

	private static String actual_baseBugNo = "";
	private static String actual_bugType = "";
	private static String actual_portId = "";
	private static String actual_doByRelease = "";
	private static String actual_genericOrPortSpecific = "";
	private static String actual_untilityVer = "";
	private static String actual_subject = "";

	private static boolean hasBugError = false;
	private static boolean needBeginTransPrompt = false;

	private static String statusCode = "0";

	static String m_psrDb = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
	static String m_psrUName = "fintech";
	static String m_psrPwd = "fintech";

	private static BufferedWriter writer = null;
	private static BufferedWriter writerLog = null;
	private static String logDir = "~";
	private static String resultFile = "/ARUValidresult.txt";
	private static String logFile = ".txt.ARUValid.log";

	/**
	 * java ARUValidation $txn $series $family $outdir $baseBugNo $backportBugNo
	 *
	 * @param args
	 *            : arguments obtained from shell
	 */
	public static void main(String[] args) throws Exception {
		
		int argNo = args.length;
		
		transName = args[0];
		series = args[1];
		family = args[2].toLowerCase();
		logDir = args[3];
		bugNo = args[4];
		
		writer = new BufferedWriter(new FileWriter(logDir + resultFile));
		writerLog = new BufferedWriter(new FileWriter(logDir + "/" + args[0] + logFile));

		writerLog.write("Start ARU transaction validation in Java... \n");
		writerLog.write("transName = " + transName + "\n");
		writerLog.write("series = " + series + "\n");
		writerLog.write("family = " + family + "\n");
		writerLog.write("bugNo = " + bugNo + "\n");
		
		Connection con = null;
		try {
			con = DriverManager.getConnection(m_psrDb, m_psrUName, m_psrPwd);
			getSeriesCategoryRelVerAndAbbr(con, writerLog);
                        if(statusCode.equals("3")){
                            writerLog.write("Skipped - ARU transaction validation.\n");
                        }

			if (category.equals("ARU")) {
				if (argNo == 5) { //No bp bug info, e.g. ade begintrans -bug 1234567
					getBaseBugValueOnly(con, writerLog);
					if (actual_baseBugNo != null) { // is bp bug
						writer.write("ERROR! This is a backport bug.");
						statusCode = "1";
						promptARUBeginTransCommand(writer);
					} else {  // Else normal bug.
						writerLog.write("Skipped - ARU transaction validation. This is a normal bug. \n");
						statusCode = "3";
					} 
				} else if (argNo == 6) {
					backportBugNo = args[5];
					writerLog.write("backportBugNo = " + backportBugNo + "\n");

					checkARUTransName(writer, writerLog);

					// Check BP bug info
					writerLog.write("Here're the details of the backport bug:\n");
					getBugInfo(con, backportBugNo, writerLog);
					getBugType(con, backportBugNo, writerLog);
					checkBackportBugParam(writer);

					clearBugInfo();

					// Check Base bug info
					// writerLog.write("Here're the details of the base bug:\n");
					// getBugInfo(con, bugNo, writerLog);
					// getBugType(con, bugNo, writerLog);
					// checkBaseBugParam(writer);

					if (hasBugError) {
						writer.write("Please fix the above problem(s) in Bug/Bugsmart system and\n");
						needBeginTransPrompt = true;
					} else {
						writerLog.write("Success - Bug attribute values has been checked.\n");
					}
					if (needBeginTransPrompt) {
						statusCode = "1";
						promptARUBeginTransCommand(writer);
					}
					if(statusCode.equals("0")){
						writer.write("Success - ARU transaction validation.\n");
					}
				}
			} else {
				writer.write("Skipped - ARU transaction validation. This is a non-ARU branch. \n");
				statusCode = "3";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if(con != null){
					con.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		writer.write("\n");

		writerLog.write("End of ARU transaction validation in Java");
		writerLog.write("\n");
		writerLog.write(statusCode);

		writer.close();
		writerLog.close();

	}

	private static void getSeriesCategoryRelVerAndAbbr(Connection con,
			BufferedWriter writerLog) throws IOException {
		Statement stmt = null;
		ResultSet rs = null;

		String sql = "SELECT CATEGORY,RELEASE_VER,ABBR,FLAG FROM SERIES_CATEGORY WHERE ADE_SERIES LIKE '"
				+ series + "'";

		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				category = rs.getString("CATEGORY");
				rel_ver = rs.getString("RELEASE_VER");
				ver_abbr = rs.getString("ABBR");
                                statusCode = rs.getString("FLAG");

				writerLog.write("category = " + category + "\n");
				writerLog.write("rel_ver = " + rel_ver + "\n");
				writerLog.write("ver_abbr = " + ver_abbr + "\n");
                                writerLog.write("flag = " + statusCode + "\n");
			} else{ //Should NOT have this happen
				writerLog.write("No match in DB for this series!!\n");
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void checkBPSubjectSuffix() {
		System.out
				.println("ERROR! Please keep the Release no as suffix to a bug subject like - Rel9.2, Rel10 etc");
	}

	private static void checkARUTransName(BufferedWriter writer, BufferedWriter writerLog)
			throws IOException {
		String[] subTransName = transName.split("_");
                String branchVersion = series.split("_")[1].toLowerCase(); 
		if (subTransName.length == 5) { // e.g. mengxu_rfi_backport_21649475_11.1.9.2.0
			if ((!subTransName[2].equals("backport"))
					|| (!subTransName[3].equals(String.valueOf(bugNo)))) {
				writer.write("ERROR! Invalid transaction name for a backport bug.\n");
				needBeginTransPrompt = true;
			} else if (!(  (subTransName[4].equals(rel_ver)) || 
                   (subTransName[4].equals(branchVersion.substring(branchVersion.indexOf(rel_ver)))))) {
				writer.write("ERROR! Invalid transaction name for a backport bug.\n");
				needBeginTransPrompt = true;
			} else {
				writerLog.write("Success - Transaction Name has been Checked.\n");
			}
		} else if (subTransName.length == 6) {
			writer.write("ERROR! You might miss the -fromtrans paramter.\n");
			needBeginTransPrompt = true;
		} else {
			writer.write("ERROR! Invalid transaction name for a backport bug.\n");
			needBeginTransPrompt = true;
		}
	}

	private static void getBaseBugValueOnly(Connection con,
			BufferedWriter writerLog) throws IOException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();

			String sql = "SELECT base_rptno FROM rpthead@bugdb WHERE rptno = '"
					+ bugNo + "'";
			rs = stmt.executeQuery(sql);

			if (rs.next()) {
				actual_baseBugNo = rs.getString("base_rptno");
				writerLog.write("actual_baseBugNo = " + actual_baseBugNo + "\n");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
					if (stmt != null) {
						stmt.close();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static void getBugInfo(Connection con, String tmpBugNo,
			BufferedWriter writerLog) throws IOException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();

			String sql = "SELECT base_rptno, generic_or_port_specific, portid, do_by_release, utility_version, subject "
					+ "FROM rpthead@bugdb WHERE rptno = '" + tmpBugNo + "'";
			rs = stmt.executeQuery(sql);

			if (rs.next()) {

				actual_baseBugNo = rs.getString("base_rptno");
				actual_genericOrPortSpecific = rs
						.getString("generic_or_port_specific");
				actual_portId = rs.getString("portid");
				actual_doByRelease = rs.getString("do_by_release");
				actual_untilityVer = rs.getString("utility_version");
				actual_subject = rs.getString("subject");

				writerLog.write("actual_baseBugNo = " + actual_baseBugNo + "\n");
				writerLog.write("actual_genericOrPortSpecific = "
						+ actual_genericOrPortSpecific + "\n");
				writerLog.write("actual_portId = " + actual_portId + "\n");
				writerLog.write("actual_doByRelease = " + actual_doByRelease + "\n");
				writerLog.write("actual_untilityVer = " + actual_untilityVer + "\n");
				writerLog.write("actual_subject = " + actual_subject + "\n");

			} else {
				writerLog.write("Can't find bug " + tmpBugNo + " in bug system.");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
					if (stmt != null) {
						stmt.close();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}

	}

	private static void getBugType(Connection con, String tmpBugNo,
			BufferedWriter writerLog) throws IOException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();

			String sql = "SELECT nvl(nvl(rtg.value,tgv.name),'Undefined') "
					+ "FROM rpthead_tracking_groups@bugdb rtg, tracking_group_values@bugdb tgv, tracking_groups@bugdb tg "
					+ "WHERE rtg.rptno = '" + tmpBugNo + "' "
					+ "AND rtg.tracking_group_value_id = tgv.id "
					+ "AND rtg.tracking_group_id = tg.id "
					+ "AND tg.name = 'Bug Type (FusionApps)'";
			rs = stmt.executeQuery(sql);

			if (rs.next()) {
				actual_bugType = rs.getString("nvl(nvl(rtg.value,tgv.name),'Undefined')");
				writerLog.write("actual_bugType = " + actual_bugType + "\n");
			} else {
				writerLog.write("actual_bugType = null.\n");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
					if (stmt != null) {
						stmt.close();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static void clearBugInfo() {
		actual_baseBugNo = "";
		actual_bugType = "";
		actual_portId = "";
		actual_doByRelease = "";
		actual_genericOrPortSpecific = "";
		actual_untilityVer = "";
		actual_subject = "";
	}

	private static void checkBackportBugParam(BufferedWriter writer)
			throws IOException { // ARU
		if ((actual_baseBugNo == null) || (!actual_baseBugNo.equals(bugNo))) {
			writer.write("ERROR! The base bug number defined in this transaction does not match with the one in the bug system.\n");
			hasBugError = true;
		} 
		if ((actual_bugType == null) || (!actual_bugType.equals("Backport"))) {
			writer.write("ERROR! The Bug Type (FusionApps) attribute should be set to 'Backport' for a backport bug in BugSmart.\n");
			hasBugError = true;
		}
		if (!actual_genericOrPortSpecific.equals("I")) {
			writer.write("ERROR! The Gen/Prt attribute should be set to 'I' for a backport bug.\n");
			hasBugError = true;
		}
		if (!actual_portId.equals("289")) {
			writer.write("ERROR! The O/S attribute should be set to '289' for a backport bug.\n");
			hasBugError = true;
		}
		if ((actual_doByRelease == null) || (!actual_doByRelease.equals(rel_ver))) {
			writer.write("ERROR! This backport bug should be fixed by "
					+ rel_ver  + ".\n");
			hasBugError = true;
		}
		if (!actual_untilityVer.equals(rel_ver)) {
			writer.write("ERROR! This backport bug was observed in "
					+ actual_untilityVer + ", bug this view belongs to "
					+ rel_ver + ".\n");
			hasBugError = true;
		}

	}

	private static void checkBaseBugParam(BufferedWriter writer)
			throws IOException { // LRB
		if (actual_baseBugNo != null) {
			writer.write("ERROR! A base bug should not further has any base bug.\n");
			hasBugError = true;
		}
		// bug type validation TBA
		if ((actual_bugType == null) || (!actual_bugType.equals("Defect"))) {
			writer.write("ERROR! The Bug Type (FusionApps) attribute should be set to 'Defect' for a base bug in BugSmart.\n");
			hasBugError = true;
		}
		if (!actual_genericOrPortSpecific.equals("G")) {
			writer.write("ERROR! The Gen/Prt attribute should be set to 'G' for a base bug.\n");
			hasBugError = true;
		}
		if (!actual_portId.equals("289")) {
			writer.write("ERROR! The O/S attribute should be set to '289' for a base bug.\n");
			hasBugError = true;
		}
		if ((actual_doByRelease == null) || (!actual_doByRelease.equals(newest_ver))) {
			writer.write("ERROR! A base bug should be fixed by a LRB release (i.e. "
					+ newest_ver + ").\n");
			hasBugError = true;
		}
		// if(!actual_untilityVer.equals(newest_ver)){
		// System.out.println("ERROR! The Comp Ver attribute for a base bug should be set as a LRB release (i.e. "
		// + newest_ver
		// + "), but the value set in the bug system was " + actual_untilityVer+
		// ".");
		// hasBugError = true;
		// }

	}

	private static void promptARUBeginTransCommand(BufferedWriter writer)
			throws IOException {
		writer.write("Please begin transaction with the following command:\n");
		writer.write("ade begintrans-backport <backportBugNumber> -fromtrans <LRB codeline txn merged with the base bug> -bl <ARU series label>\n");
		writer.write("    - run this command from ADE_VIEW_ROOT.\n");
		writer.write("    - if the bug type of not defined, it will ask you set at the time of transaction creation. Set it to RFI.\n");
		writer.write("    - backport bug number - is the bug logged by the developer\n");
		writer.write("    - HWM codeline txn - is the transaction that developer created out of support logged bug for merging the transaction to LRB/HWM codeline\n");
		writer.write("    - see ARU help for more info : http://aru.us.oracle.com:8080/docs/apf/aru_apf_Fusion_info.html and Financial's Customer Patch creation process: https://confluence.oraclecorp.com/confluence/display/FFT/Financial%27s+Customer+Patch+creation+process\n");
	}

}
