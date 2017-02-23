package oracle.apps.lovWidth;

import java.io.FileOutputStream;

import java.sql.Connection;

import java.sql.DriverManager;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;

public class ComboboxOutputWriter {
  private String m_outputFileName;

  public ComboboxOutputWriter(String fileName) {
      super();
      m_outputFileName = fileName;
      initializeWorkbooks();
  }


  private Workbook workbook;
  private Worksheet expertModeSheet;
  private Worksheet eoBasedVOSheet;
  private Worksheet voOverrideSheet;

  private Worksheet consumerSheet;
  Connection m_connection;
  private int expertRowCounter = 2;
  private int eobasedRowCounter = 2;
  private int vooverrideCounter = 2;
  private int currentConsumerRow = 2;


  public void initializeWorkbooks() {
      workbook = new Workbook("ComboboxWidths");
      expertModeSheet = new Worksheet("ExpertVO");
      eoBasedVOSheet = new Worksheet("EOBased");
      consumerSheet = new Worksheet("Consumers");
      voOverrideSheet = new Worksheet("VOOverride");

      initializeProducerSheet(expertModeSheet);
      initializeProducerSheet(eoBasedVOSheet);
      initializeVOOverrideProducerSheet(voOverrideSheet);
      initializeConsumerSheett();
  }

  public void initializeProducerSheet(Worksheet worksheet) {
      // column 1 - family
      worksheet.setColumnWidth(1, 10);
      // column 2 - product
      worksheet.setColumnWidth(2, 20);
      // Column 3 - bcName
      worksheet.setColumnWidth(3, 40);
      // Column 4 - Attribute
      worksheet.setColumnWidth(4, 30);
      // Column 5 - Attribute Width
      worksheet.setColumnWidth(5, 20);
      // Column 6 -  new Attribute Width
      worksheet.setColumnWidth(6, 20);
      // Column 7 - bC Package
      worksheet.setColumnWidth(7, 50);
      
      // column 8 - ps width
      worksheet.setColumnWidth(8, 20);
      // Column 9 - ps name
      worksheet.setColumnWidth(8, 30);
      



      // column 1 - family
      worksheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Family")));
      // column 2 - product
      worksheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Product")));
      // Column 3 - bcname
      worksheet.addCell(new Cell(new CellLocation("C1"), new CellValue("BCName")));
      // Column4 - attribute
      worksheet.addCell(new Cell(new CellLocation("D1"), new CellValue("Attribute")));
      // Column 3 - bc package
      worksheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Display Width")));
      // Column 3 - attribute widht
      worksheet.addCell(new Cell(new CellLocation("F1"), new CellValue("New Display Width")));
      // Column 3 - new attr width
      worksheet.addCell(new Cell(new CellLocation("G1"), new CellValue("BCPackage")));
      // Column 3 - attribute widht
      worksheet.addCell(new Cell(new CellLocation("H1"), new CellValue("PS Attr Width")));
      // Column 3 - new attr width
      worksheet.addCell(new Cell(new CellLocation("I1"), new CellValue("PS Name")));

      workbook.addWorksheet(worksheet);

  }

  public void initializeVOOverrideProducerSheet(Worksheet worksheet) {
      // column 1 - family
      worksheet.setColumnWidth(1, 10);
      // column 2 - product
      worksheet.setColumnWidth(2, 20);
      // Column 3 - bcName
      worksheet.setColumnWidth(3, 40);
      // Column 4 - Attribute
      worksheet.setColumnWidth(4, 30);
      // Column 5 - Attribute Width
      worksheet.setColumnWidth(5, 20);
      // Column 6 -  new Attribute Width
      worksheet.setColumnWidth(6, 20);
      // Column 7 - bC Package
      worksheet.setColumnWidth(7, 50);


      // column 1 - family
      worksheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Family")));
      // column 2 - product
      worksheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Product")));
      // Column 3 - bcname
      worksheet.addCell(new Cell(new CellLocation("C1"), new CellValue("VO Name")));
      // Column4 - attribute
      worksheet.addCell(new Cell(new CellLocation("D1"), new CellValue("VO Attribute")));
      // Column 3 - bc package
      worksheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Display Width")));
      // Column 3 - attribute widht
      worksheet.addCell(new Cell(new CellLocation("F1"), new CellValue("New Display Width")));
      // Column 3 - new attr width
      worksheet.addCell(new Cell(new CellLocation("G1"), new CellValue("VO  Package")));
      
      // Column 3 - attribute widht
      worksheet.addCell(new Cell(new CellLocation("H1"), new CellValue("PS Attr Width")));
      // Column 3 - new attr width
      worksheet.addCell(new Cell(new CellLocation("I1"), new CellValue("PS Name")));
      
      worksheet.addCell(new Cell(new CellLocation("J1"), new CellValue("EO  Name")));
      worksheet.addCell(new Cell(new CellLocation("K1"), new CellValue("EO  Attribute")));
      worksheet.addCell(new Cell(new CellLocation("L1"), new CellValue("EO  Display Width")));
      worksheet.addCell(new Cell(new CellLocation("M1"), new CellValue("EO Package")));

      worksheet.addCell(new Cell(new CellLocation("N1"), new CellValue("EO Property Set Width")));
      worksheet.addCell(new Cell(new CellLocation("O1"), new CellValue("EO Property Set Name")));
      workbook.addWorksheet(worksheet);

  }

  public void initializeConsumerSheett() {

      // column 2 - bcProducerPackage
      consumerSheet.setColumnWidth(1, 70);
      // Column 3 - producer Attribute
      consumerSheet.setColumnWidth(2, 20);
      // Column 4 - consumer ui page name
      consumerSheet.setColumnWidth(3, 30);
      // Column 5 - consumer ui page path
      consumerSheet.setColumnWidth(4, 50);
      // Column 6 -  lov uses
      consumerSheet.setColumnWidth(5, 20);


      // column 1 - producer packcage
      consumerSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Producer Package")));
      // column 2 - producer attribute
      consumerSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Producer Attribute Name")));
      // Column 3 - bcname
      consumerSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("consumer UI Page Name")));
      // Column4 - attribute
      consumerSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("Consumer UI Page Path")));
      // Column 3 - bc package
      consumerSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("LOV Uses")));


      workbook.addWorksheet(consumerSheet);

  }

  public void initializeConsumerSheetStartModel() {

      // column 2 - bcProducerPackage
      consumerSheet.setColumnWidth(1, 70);
      // Column 3 - producer Attribute
      consumerSheet.setColumnWidth(2, 20);
      // Column 4 - consumer ui page name
      consumerSheet.setColumnWidth(3, 30);
      // Column 5 - consumer ui page path
      consumerSheet.setColumnWidth(4, 50);
      // Column 6 -  lov uses
      consumerSheet.setColumnWidth(5, 20);


      // column 1 - producer packcage
      consumerSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Producer Package")));
      // column 2 - producer attribute
      consumerSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Producer Attribute Name")));
      // Column 3 - bcname
      consumerSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("consumer UI Page Name")));
      // Column4 - attribute
      consumerSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("Consumer UI Page Path")));
      // Column 3 - bc package
      consumerSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("LOV Uses")));


      workbook.addWorksheet(consumerSheet);

  }
  public Connection getConnection() throws Exception {
      if (m_connection == null) {
          String dbUrl = "jdbc:oracle:thin:@//rws65979fwks:1522/rws65979";
          String uname = "fusion";
          String pwd = "fusion";

          m_connection = DriverManager.getConnection(dbUrl, uname, pwd);

      }
      return m_connection;
  }

  public void createWorkbookForFamily(String family) throws Exception {

      Connection connection = getConnection();
      String query =
          "SELECT FAMILY,PRODUCT,BCNAME,BCPACKAGE,BCATTRIBUTE,BCATTRIBUTEWIDTH,LOVTYPE FROM COMBOWIDTH_PROD WHERE FAMILY=?";
      PreparedStatement stmt = connection.prepareStatement(query);
      stmt.setString(1, family);
      ResultSet results = stmt.executeQuery();
      while (results.next()) {
          writeProducer(results);
      }
      results.close();
      stmt.close();


      if (connection != null)
          connection.close();
  }


  public void writeProducer(ResultSet producerResult) throws Exception {
      String family = producerResult.getString(1);
      String product = producerResult.getString(2);
      String bcname = producerResult.getString(3);
      String bcpackage = producerResult.getString(4);
      String bcattr = producerResult.getString(5);
      String bcattrwidth = producerResult.getString(6);
      String lovtype = producerResult.getString(7);
      ProducerLOV producer = new ProducerLOV();
      producer.setFamily(family);
      producer.setProduct(product);
      producer.setBcName(bcname);
      producer.setBcPackage(bcpackage);
      producer.setDisplayAttr(bcattr);
      producer.setDisplayWidth(bcattrwidth);
      writeProducer(producer);

      // get consumers for this producer
  }

  public void writeProducer(ProducerLOV producer) throws Exception {
      String type = producer.getLovType();
      if (type.equals("expert")) {
          writeProducer(producer, expertModeSheet, expertRowCounter);
          expertRowCounter++;
      } else if (type.equals("eobased")) {
          writeProducer(producer, eoBasedVOSheet, eobasedRowCounter);
          eobasedRowCounter++;
      }  else if (type.equals("vooverride")) {
          writeProducerVOOverride(producer, voOverrideSheet, vooverrideCounter);
          vooverrideCounter++;
      } else {
          System.out.println("Unrecognized type: " + type);
      }

  }

  public void writeProducer(ProducerLOV producer, Worksheet worksheet, int rowNumber) throws Exception {

      worksheet.addCell(new Cell(new CellLocation("A" + rowNumber), new CellValue(producer.getFamily())));
      worksheet.addCell(new Cell(new CellLocation("B" + rowNumber), new CellValue(producer.getProduct())));
      worksheet.addCell(new Cell(new CellLocation("C" + rowNumber), new CellValue(producer.getBcName())));
      worksheet.addCell(new Cell(new CellLocation("D" + rowNumber), new CellValue(producer.getDisplayAttr())));
      worksheet.addCell(new Cell(new CellLocation("E" + rowNumber), new CellValue(producer.getDisplayWidth())));
      worksheet.addCell(new Cell(new CellLocation("G" + rowNumber), new CellValue(producer.getBcPackage())));
      worksheet.addCell(new Cell(new CellLocation("H" + rowNumber), new CellValue(producer.getPropertySetDWidth())));
      worksheet.addCell(new Cell(new CellLocation("I" + rowNumber), new CellValue(producer.getPropertySetName())));
  }
  public void writeProducerVOOverride(ProducerLOV producer, Worksheet worksheet, int rowNumber) throws Exception {
    writeProducer(producer, worksheet, rowNumber);
    worksheet.addCell(new Cell(new CellLocation("J" + rowNumber), new CellValue(producer.getSecondaryBcName())));
    worksheet.addCell(new Cell(new CellLocation("K" + rowNumber), new CellValue(producer.getSecondaryBcAttr())));
    worksheet.addCell(new Cell(new CellLocation("L" + rowNumber), new CellValue(producer.getSecondaryBcAttrWidth())));
    worksheet.addCell(new Cell(new CellLocation("M" + rowNumber), new CellValue(producer.getSecondaryBcPackage())));
    worksheet.addCell(new Cell(new CellLocation("N" + rowNumber), new CellValue(producer.getSecondaryBcAttrPSDWidth())));
    worksheet.addCell(new Cell(new CellLocation("O" + rowNumber), new CellValue(producer.getSecondaryBcAttrPSName())));
    
  }

  public void writeConsumer(ConsumerVO consumer, String producerPackage, String producerattr) throws Exception {

      consumerSheet.addCell(new Cell(new CellLocation("A" + currentConsumerRow), new CellValue(producerPackage)));
      consumerSheet.addCell(new Cell(new CellLocation("B" + currentConsumerRow), new CellValue(producerattr)));
      consumerSheet.addCell(new Cell(new CellLocation("C" + currentConsumerRow), new CellValue(consumer.getUiFileName())));
      consumerSheet.addCell(new Cell(new CellLocation("D" + currentConsumerRow), new CellValue(consumer.getUiFileFullPath())));
      consumerSheet.addCell(new Cell(new CellLocation("E" + currentConsumerRow), new CellValue(consumer.getLovUses())));
      currentConsumerRow++;
  }

  /**
   *Write spreadsheet to disk
   * @param outputFileName
   * Output file name to write to
   * @throws Exception
   * Exception if operation is unsuccessful
   */
  public void writeSpreadsheetToDisk() throws Exception {

      FileOutputStream out = null;
      try {
          out = new FileOutputStream(m_outputFileName);
          XLSXCreator.create(workbook, out);
      } catch (Exception e) {
          e.printStackTrace();
      } finally {
          out.close();
      }
  }
}
