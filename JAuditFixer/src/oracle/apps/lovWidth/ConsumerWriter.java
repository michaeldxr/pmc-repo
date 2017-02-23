package oracle.apps.lovWidth;

import java.io.FileOutputStream;

import java.sql.Connection;

import java.util.ArrayList;
import java.util.Iterator;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;

public class ConsumerWriter {
    String m_fileName = "";
    private Workbook workbook;

    private Worksheet consumerSheet;
    private Worksheet consumerAggSheet;


    private int currentConsumerRow = 2;
    private int currentConsumerAggRow = 2;

    public ConsumerWriter(String fileName,boolean startModel) {
        super();
        m_fileName = fileName;
        
        initializeWorkbooks(startModel);
    }

    public void initializeWorkbooks(boolean startModel) {
        if (startModel) {
            workbook = new Workbook("ComboboxWidths");

            consumerSheet = new Worksheet("Consumers");
            consumerAggSheet = new Worksheet("ConsumerEffWidth");
            initializeConsumerSheetStartModel();
            initializeConsumerAggregationSheetStartModel();
        }
    }

    public void initializeWorkbooks() {
        workbook = new Workbook("ComboboxWidths");

        consumerSheet = new Worksheet("Consumers");
        consumerAggSheet = new Worksheet("ConsumerEffWidth");
        initializeConsumerSheett();
        initializeConsumerAggregationSheet();
    }

    public void initializeConsumerSheett() {

        // Column 3 - ui page name
        consumerSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Consumer Product")));
        consumerSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("consumer UI Page Name")));
        // Column4 - ui page path
        consumerSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Consumer UI Page Path")));
        // Column 3 - lov uses
        consumerSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("LOV Name")));
        // Column 4 - vo name
        consumerSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Producer Product (VO)")));
        consumerSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("VO Name")));
        // Column 5 - vo attribute name
        consumerSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("VO Attribute")));
        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("VO Attribute Width")));
        consumerSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("VO Attribute ColWidth")));
        // Column 5 - vo attribute name
        consumerSheet.addCell(new Cell(new CellLocation("J1"), new CellValue("VO Property Set Name")));
        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("K1"), new CellValue("VO Property Set Width")));
        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("L1"), new CellValue("VO Package")));

        // Column 4 - vo name
        consumerSheet.addCell(new Cell(new CellLocation("M1"), new CellValue("Producer Product (EO)")));
        consumerSheet.addCell(new Cell(new CellLocation("N1"), new CellValue("EO Name")));
        // Column 5 - vo attribute name
        consumerSheet.addCell(new Cell(new CellLocation("O1"), new CellValue("EO Attribute")));

        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("P1"), new CellValue("EO Attribute Width")));
        consumerSheet.addCell(new Cell(new CellLocation("Q1"), new CellValue("EO Attribute Col Width")));
        // Column 5 - vo attribute name
        consumerSheet.addCell(new Cell(new CellLocation("R1"), new CellValue("EO Property Set Name")));
        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("S1"), new CellValue("EO Property Set Width")));
        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("T1"), new CellValue("EO Package")));

        workbook.addWorksheet(consumerSheet);

    }

    public void initializeConsumerSheetStartModel() {

        // Column 3 - ui page name
        consumerSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Base VO Product")));
        consumerSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Base VO Package")));
        // Column4 - ui page path
        consumerSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Base Attribute")));
        // Column 3 - lov uses
        consumerSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("LOV Name")));
        // Column 4 - vo name
        consumerSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Producer Product (VO)")));
        consumerSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("VO Name")));
        // Column 5 - vo attribute name
        consumerSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("VO Attribute")));
        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("VO Attribute Width")));
        consumerSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("VO Attribute ColWidth")));
        // Column 5 - vo attribute name
        consumerSheet.addCell(new Cell(new CellLocation("J1"), new CellValue("VO Property Set Name")));
        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("K1"), new CellValue("VO Property Set Width")));
        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("L1"), new CellValue("VO Package")));

        // Column 4 - vo name
        consumerSheet.addCell(new Cell(new CellLocation("M1"), new CellValue("Producer Product (EO)")));
        consumerSheet.addCell(new Cell(new CellLocation("N1"), new CellValue("EO Name")));
        // Column 5 - vo attribute name
        consumerSheet.addCell(new Cell(new CellLocation("O1"), new CellValue("EO Attribute")));

        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("P1"), new CellValue("EO Attribute Width")));
        consumerSheet.addCell(new Cell(new CellLocation("Q1"), new CellValue("EO Attribute Col Width")));
        // Column 5 - vo attribute name
        consumerSheet.addCell(new Cell(new CellLocation("R1"), new CellValue("EO Property Set Name")));
        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("S1"), new CellValue("EO Property Set Width")));
        // Column 6 - vo attribute Width
        consumerSheet.addCell(new Cell(new CellLocation("T1"), new CellValue("EO Package")));

        workbook.addWorksheet(consumerSheet);

    }

    public void initializeConsumerAggregationSheetStartModel() {
        // page name width
        consumerAggSheet.setColumnWidth(2, 30);

        // Column 3 - ui page name
        consumerAggSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Base VO Product")));
        consumerAggSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Base VO Package")));
        // Column4 - ui page path
        consumerAggSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Base VO Attribute")));
        // Column 3 - lov uses
        consumerAggSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("LOV Name")));
        consumerAggSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Producer Product (VO)")));
        // Column 4 - vo name
        consumerAggSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("VO Name")));
        // Column 4 - vo name
        consumerAggSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("VO Package")));
        // Column 5 - vo attribute name
        consumerAggSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("Displayed Attributes")));
        // Column 6 - vo attribute Width
        // consumerAggSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("EO Name")));
        // Column 4 - vo name
        //  consumerAggSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("EO Name")));
        // Column 5 - vo attribute name
        // consumerAggSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("List EO Attr")));
        // Column 6 - vo attribute Width
        consumerAggSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("LOV Effective Width")));


        workbook.addWorksheet(consumerAggSheet);

    }

    public void initializeConsumerAggregationSheet() {
        // page name width
        consumerAggSheet.setColumnWidth(2, 30);

        // Column 3 - ui page name
        consumerAggSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Consumer Product")));
        consumerAggSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Consumer UI Page Name")));
        // Column4 - ui page path
        consumerAggSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Consumer UI Page Path")));
        // Column 3 - lov uses
        consumerAggSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("LOV Name")));
        consumerAggSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Producer Product (VO)")));
        // Column 4 - vo name
        consumerAggSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("VO Name")));
        // Column 4 - vo name
        consumerAggSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("VO Package")));
        // Column 5 - vo attribute name
        consumerAggSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("Displayed Attributes")));
        // Column 6 - vo attribute Width
        // consumerAggSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("EO Name")));
        // Column 4 - vo name
        //  consumerAggSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("EO Name")));
        // Column 5 - vo attribute name
        // consumerAggSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("List EO Attr")));
        // Column 6 - vo attribute Width
        consumerAggSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("LOV Effective Width")));


        workbook.addWorksheet(consumerAggSheet);

    }

    public void writeConsumerDetails(ConsumerVO consumer, boolean startModel) {
        ArrayList<ProducerLOV> produers = consumer.getProducers();
        if (produers == null) {
            return;
        }
        Iterator<ProducerLOV> producerIter = produers.iterator();
        while (producerIter.hasNext()) {
            ProducerLOV producer = producerIter.next();
            if (startModel) {
                consumerSheet.addCell(new Cell(new CellLocation("A" + currentConsumerRow),
                                               new CellValue(consumer.getProduct())));
                consumerSheet.addCell(new Cell(new CellLocation("B" + currentConsumerRow),
                                               new CellValue(consumer.getVoPackage())));
                consumerSheet.addCell(new Cell(new CellLocation("C" + currentConsumerRow),
                                               new CellValue(consumer.getLovAttribute())));
                consumerSheet.addCell(new Cell(new CellLocation("D" + currentConsumerRow),
                                               new CellValue(consumer.getLovUses())));
            } else {
                consumerSheet.addCell(new Cell(new CellLocation("A" + currentConsumerRow),
                                               new CellValue(consumer.getProduct())));
                consumerSheet.addCell(new Cell(new CellLocation("B" + currentConsumerRow),
                                               new CellValue(consumer.getUiFileName())));
                consumerSheet.addCell(new Cell(new CellLocation("C" + currentConsumerRow),
                                               new CellValue(consumer.getUiFileFullPath())));
                consumerSheet.addCell(new Cell(new CellLocation("D" + currentConsumerRow),
                                               new CellValue(consumer.getLovUses())));
            }
            consumerSheet.addCell(new Cell(new CellLocation("E" + currentConsumerRow), new CellValue(producer.getProduct())));
            consumerSheet.addCell(new Cell(new CellLocation("F" + currentConsumerRow), new CellValue(producer.getBcName())));
            consumerSheet.addCell(new Cell(new CellLocation("G" + currentConsumerRow),
                                           new CellValue(producer.getDisplayAttr())));
            consumerSheet.addCell(new Cell(new CellLocation("H" + currentConsumerRow),
                                           new CellValue(producer.getDisplayWidth())));
            consumerSheet.addCell(new Cell(new CellLocation("I" + currentConsumerRow),
                                           new CellValue(producer.getColumnWidth())));
            consumerSheet.addCell(new Cell(new CellLocation("J" + currentConsumerRow),
                                           new CellValue(producer.getPropertySetName())));
            consumerSheet.addCell(new Cell(new CellLocation("K" + currentConsumerRow),
                                           new CellValue(producer.getPropertySetDWidth())));
            consumerSheet.addCell(new Cell(new CellLocation("L" + currentConsumerRow), new CellValue(producer.getBcPackage())));

            consumerSheet.addCell(new Cell(new CellLocation("M" + currentConsumerRow),
                                           new CellValue(producer.getSecondaryBcProduct())));
            consumerSheet.addCell(new Cell(new CellLocation("N" + currentConsumerRow),
                                           new CellValue(producer.getSecondaryBcName())));
            consumerSheet.addCell(new Cell(new CellLocation("O" + currentConsumerRow),
                                           new CellValue(producer.getSecondaryBcAttr())));
            consumerSheet.addCell(new Cell(new CellLocation("P" + currentConsumerRow),
                                           new CellValue(producer.getSecondaryBcAttrWidth())));
            consumerSheet.addCell(new Cell(new CellLocation("Q" + currentConsumerRow),
                                           new CellValue(producer.getSecondaryColWidth())));
            consumerSheet.addCell(new Cell(new CellLocation("R" + currentConsumerRow),
                                           new CellValue(producer.getSecondaryBcAttrPSName())));
            consumerSheet.addCell(new Cell(new CellLocation("S" + currentConsumerRow),
                                           new CellValue(producer.getSecondaryBcAttrPSDWidth())));
            consumerSheet.addCell(new Cell(new CellLocation("T" + currentConsumerRow),
                                           new CellValue(producer.getSecondaryBcPackage())));
            currentConsumerRow++;

        }
    }

    public void writeConsumerAggregation(ConsumerVO consumer, boolean startModel) {


        ArrayList<ProducerLOV> produers = consumer.getProducers();
        if (produers == null) {

            return;
        }
        Iterator<ProducerLOV> producerIter = produers.iterator();
        if (producerIter.hasNext()) {
            ProducerLOV producer = producerIter.next();
            if (startModel) {
                consumerAggSheet.addCell(new Cell(new CellLocation("A" + currentConsumerAggRow),
                                               new CellValue(consumer.getProduct())));
                consumerAggSheet.addCell(new Cell(new CellLocation("B" + currentConsumerAggRow),
                                               new CellValue(consumer.getVoPackage())));
                consumerAggSheet.addCell(new Cell(new CellLocation("C" + currentConsumerAggRow),
                                               new CellValue(consumer.getLovAttribute())));
                consumerAggSheet.addCell(new Cell(new CellLocation("D" + currentConsumerAggRow),
                                               new CellValue(consumer.getLovUses())));
            } else {
                consumerAggSheet.addCell(new Cell(new CellLocation("A" + currentConsumerAggRow),
                                                  new CellValue(consumer.getProduct())));
                consumerAggSheet.addCell(new Cell(new CellLocation("B" + currentConsumerAggRow),
                                                  new CellValue(consumer.getUiFileName())));
                consumerAggSheet.addCell(new Cell(new CellLocation("C" + currentConsumerAggRow),
                                                  new CellValue(consumer.getUiFileFullPath())));
                consumerAggSheet.addCell(new Cell(new CellLocation("D" + currentConsumerAggRow),
                                                  new CellValue(consumer.getLovUses())));
            }
            consumerAggSheet.addCell(new Cell(new CellLocation("E" + currentConsumerAggRow),
                                              new CellValue(producer.getProduct())));
            consumerAggSheet.addCell(new Cell(new CellLocation("F" + currentConsumerAggRow),
                                              new CellValue(producer.getBcName())));
            consumerAggSheet.addCell(new Cell(new CellLocation("G" + currentConsumerAggRow),
                                              new CellValue(producer.getBcPackage())));
            // F - list of vo attrs
            consumerAggSheet.addCell(new Cell(new CellLocation("H" + currentConsumerAggRow),
                                              new CellValue(getListOfVOAttrs(produers))));

            //            consumerAggSheet.addCell(new Cell(new CellLocation("G" + currentConsumerAggRow),
            //                                              new CellValue(producer.getSecondaryBcName())));
            //            consumerAggSheet.addCell(new Cell(new CellLocation("H" + currentConsumerAggRow),
            //                                              new CellValue(producer.getSecondaryBcPackage())));
            //            // F - list of vo attrs
            //            consumerAggSheet.addCell(new Cell(new CellLocation("I" + currentConsumerAggRow), new CellValue(getListOfEOAttrs(produers))));
            consumerAggSheet.addCell(new Cell(new CellLocation("I" + currentConsumerAggRow),
                                              new CellValue(getEffectiveWidthOfLov(produers))));

            currentConsumerAggRow++;
        }

    }

    public String getEffectiveWidthOfLov(ArrayList<ProducerLOV> prodcerList) {
        Iterator<ProducerLOV> producerIter = prodcerList.iterator();

        Long longWidth = 0L;
        while (producerIter.hasNext()) {
            ProducerLOV producer = producerIter.next();
            // precdence: vo width, vo prop set, eo width, eo propset
            Long width = getEffectiveWidthForAttr(producer);
            longWidth = longWidth + width;

        }

        return longWidth.toString();
    }

    public Long getEffectiveWidthForAttr(ProducerLOV producerAttr) {
        Long effW = 0L;
        if (producerAttr == null)
            return effW;

        String voWidth = producerAttr.getDisplayWidth();
        if (!isEmpty(voWidth)) {
            return convertStrToLong(voWidth);
        }

        voWidth = producerAttr.getPropertySetDWidth();
        if (!isEmpty(voWidth)) {
            return convertStrToLong(voWidth);
        }

        String eoWidth = producerAttr.getSecondaryBcAttrWidth();
        if (!isEmpty(eoWidth)) {
            return convertStrToLong(eoWidth);
        }

        eoWidth = producerAttr.getSecondaryBcAttrPSDWidth();
        if (!isEmpty(eoWidth)) {
            return convertStrToLong(eoWidth);
        }

        eoWidth = producerAttr.getSecondaryColWidth();
        if (!isEmpty(eoWidth)) {
            return convertStrToLong(eoWidth);
        }


        String vocolWidth = producerAttr.getColumnWidth();
        if (!isEmpty(vocolWidth)) {
            return convertStrToLong(vocolWidth);
        }

        return 0L;
    }

    public Long convertStrToLong(String str) {
        if (isEmpty(str))
            return 0L;
        str = str.trim();
        try {
            return Long.parseLong(str);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0L;
        }
    }

    public boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public String getListOfVOAttrs(ArrayList<ProducerLOV> prodcerList) {
        Iterator<ProducerLOV> producerIter = prodcerList.iterator();
        StringBuffer attrList = new StringBuffer();
        while (producerIter.hasNext()) {
            ProducerLOV producer = producerIter.next();
            String attrWidth = producer.getEffectiveAttributeWidth().toString();
            attrList.append(String.format("%s (%s) ,\n", producer.getDisplayAttr(), attrWidth));
        }

        return attrList.toString();
    }

    public String getListOfEOAttrs(ArrayList<ProducerLOV> prodcerList) {
        Iterator<ProducerLOV> producerIter = prodcerList.iterator();
        StringBuffer attrList = new StringBuffer();
        while (producerIter.hasNext()) {
            ProducerLOV producer = producerIter.next();
            attrList.append(String.format("%s ,\n", producer.getSecondaryBcAttr()));
        }

        return attrList.toString();
    }

    public void writeSpreadsheetToDisk() throws Exception {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(m_fileName);
            XLSXCreator.create(workbook, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }
}
