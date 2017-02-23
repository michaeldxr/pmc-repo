package oracle.apps.lovWidth;

import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.util.ArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.votuning.DataBindings;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ModifiedComboboxParser {

  boolean m_bDebug = false;

  public String m_jprAbsFilePath = "";

  String m_jprName = "";

  HashMap<String, String> listOfAllVOs;
  HashMap<String, String> listOfAllEOs;
  HashMap<String, ProducerLOV> listProderVOs = new HashMap<String, ProducerLOV>();
  ArrayList<ConsumerVO> list_consumers = new ArrayList<ConsumerVO>();
            
  private ScanType m_scanType = ScanType.EXPERT_VO;

  private String m_family;
 

  public void setScanType(ModifiedComboboxParser.ScanType m_scanType) {
      this.m_scanType = m_scanType;
  }

  public ModifiedComboboxParser.ScanType getScanType() {
      return m_scanType;
  }

  public void setFamily(String m_family) {
      this.m_family = m_family;
  }

  public String getFamily() {
      return m_family;
  }

  public void setJprName(String m_jprName) {
      this.m_jprName = m_jprName;
  }

  public String getJprName() {
      return m_jprName;
  }

  public void setJprAbsFilePath(String m_jprAbsFilePath) {
      this.m_jprAbsFilePath = m_jprAbsFilePath;
  }

  public String getJprAbsFilePath() {
      return m_jprAbsFilePath;
  }



  public void setList_consumers(ArrayList<ConsumerVO> list_consumers) {
      this.list_consumers = list_consumers;
  }

  public ArrayList<ConsumerVO> getList_consumers() {
      return list_consumers;
  }

  public enum ScanType {
      EXPERT_VO,
      VOOVERRIDE,
      EXPERT_AND_EOBASED,
      ALL,
      EOBASED,
      RECORD_ALL_CONSUMERS;
  }

  public ModifiedComboboxParser(String family, ScanType scanType) {
      super();
      m_family = family;
      m_scanType = scanType;
  }
  
  Connection m_connection = null;

  public void analyzeUIFile(File uiFile, DataBindings bindings) {

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

  public void analyzeJpr(File jpr) {
      m_jprAbsFilePath = jpr.getAbsolutePath();
      m_jprName = jpr.getName();
      System.out.println("\n ======= Processing Jpr: " + m_jprName);


      JprHelper jprHelper = new JprHelper();
      listOfAllVOs = jprHelper.getListOfAllViewObjectsInJpr(m_jprAbsFilePath);
      if (m_bDebug) {
          System.out.println(String.format(" Number of VO's in jpr(%s): %d ", m_jprName, listOfAllVOs.size()));
      }
      processAllVOs();
      System.out.println(String.format("======= Done processing Jpr (number of producers:%d)\n\n ", listProderVOs.size()));
  }

  public void processAllVOs() {
      Collection<String> allVOFilePaths = listOfAllVOs.values();
      if (allVOFilePaths == null)
          return;
      Iterator<String> voFilePathIter = allVOFilePaths.iterator();
      while (voFilePathIter.hasNext()) {
          processVO(voFilePathIter.next());
      }
  }

  public ConsumerVO createConsumer(String voFilePath, String voAttributeName, String lovUses) {
      ConsumerVO consumer = new ConsumerVO();
      consumer.setLovUses(lovUses);
      String consumerPackage = ViewObjectHelper.getPackage(voFilePath);
    
        consumer.setProduct(MappingHelper.getProductForPackage(consumerPackage));
      consumer.setVoName(ViewObjectHelper.getVONameFromPackage(consumerPackage));
      consumer.setVoPackage(consumerPackage);
      consumer.setLovAttribute(voAttributeName);
      return consumer;
  }

  public String getAdeRelativePath(String absFilePath) {
      if (isEmpty(absFilePath)) {
          return absFilePath;
      }
      if (absFilePath.contains("/fusionapps/")) {
          int fusionappsInd = absFilePath.indexOf("/fusionapps/");
          return absFilePath.substring(fusionappsInd + 1);
      }
      else if(absFilePath.contains("/fsm/")){
        int fsmInd = absFilePath.indexOf("/fsm/");
        return absFilePath.substring(fsmInd + 1);
        
      }
      return absFilePath;
  }

  private void processVO(String voFilePath) {
      String voName = ViewObjectHelper.getVONameFromFilePath(voFilePath);

      // Dont want to process any flex vos
      if (voFilePath.contains("/flex/")) {
          return;
      }
      if (m_bDebug) {
          System.out.println(" ---- Processing vo: " + voName);
      }
      ViewObjectHelper voHelper = new ViewObjectHelper(voFilePath);
      XMLDocument voXml = XMLParserHelper.getXMLDocument(voFilePath);
      if (voXml == null)
          return;
      ArrayList<Node> voAttributes = ViewObjectHelper.getAllAttributesInVOXMl(voXml);
      Iterator<Node> attrIter = voAttributes.iterator();
      while (attrIter.hasNext()) {
          Node attribute = attrIter.next();
          String attributename = XMLParserHelper.getAttributeValue(attribute, "Name");

          if (voHelper.doesViewAttrHaveComboboxLOV(attribute)) {

              String lovUses = XMLParserHelper.getAttributeValue(attribute, "LOVName");
              if (m_bDebug) {
                  System.out.println(String.format("Found combobx lov on VO: %s, for attribute: %s, LOVUses: %s", voName,
                                                   attributename, lovUses));
              }
              // When you have an LOV defined on an attribute - you have a consumer
              ConsumerVO consumer = createConsumer(voFilePath, attributename, lovUses);

              Node listBindingNode = voHelper.getListBindingWithName(lovUses);
              if (listBindingNode == null) {
                  System.out.println("---- [ERROR] Could not get list binding node: " + voName);
                  return;
              }

              ArrayList<String> displatAttrs = voHelper.getListBindingDisplayAttrNames(listBindingNode);
              Iterator<String> displayAttrsIter = displatAttrs.iterator();
              if (displatAttrs == null || displatAttrs.size() == 0) {
                  System.out.println(" **** How come no display attrs on combobox.. pls check vo..  " + voFilePath);
                  continue;
              }


              String listBindingVoPackage;

              if (isEntityBasedListBinding(listBindingNode)) {
                  //                    if (m_scanType == ScanType.EXPERT_VO) {
                  //                        continue;
                  //                    }
                  // System.out.println("Doing Entity based list binding processing.. ");

                  listBindingVoPackage = getViewAccessorVOPackage(listBindingNode, voHelper, consumer);
                  if (isEmpty(listBindingVoPackage))
                      continue;


              } else {

                  listBindingVoPackage = getListBindingVOPackage(listBindingNode, voHelper.getViewObjectNode());
                  if (isEmpty(listBindingVoPackage)) {
                      //                        // if it is an EO based list binding, it is anyways not an expert mode vo.. so dont show error msg
                      //                        if (m_scanType != ScanType.EXPERT_VO) {
                      System.out.println("**** === [ERROR] could not obtain list binding vo package.. !!!");
                      //}
                      continue;
                  }
              }
              if (!isValidProducerPackage(listBindingVoPackage)) {
                  System.out.println("*** Not a valid producer package; " + listBindingVoPackage);
                  continue;
              }
              XMLDocument listVO = getVOXml(listBindingVoPackage);
              if (listVO == null) {
                  System.out.println("**** could not obtain list vo");
                  continue;
              }


              Node listBindingVONode = ViewObjectHelper.getViewObjectNodeFromxml(listVO);
              if (listBindingVONode == null)
                  continue;
              while (displayAttrsIter.hasNext()) {
                  String displayAttr = displayAttrsIter.next();


                  processModel(listBindingVONode, listBindingVoPackage, displayAttr, consumer);

              }
            list_consumers.add(consumer);


          }
      }
  }

  public String getBCPathFromDB(String amPackage) {
      //
      //      String dbUrl = "jdbc:oracle:thin:@//rws65979fwks:1522/rws65979";
      //      String uname = "fusion";
      //      String pwd = "fusion";
      String bcPath = "";
      Connection connection = null;
      try {
          connection = getConnection();
          PreparedStatement prepareStatement =
              connection.prepareStatement("select bc_filepath from ZEESHA_MODELMAPPER where bc_package=?");
          prepareStatement.setString(1, amPackage);
          ResultSet results = prepareStatement.executeQuery();
          if (results != null && results.next()) {
              bcPath = results.getString(1);
          }
          results.close();
          prepareStatement.close();
      } catch (Exception ex) {
          ex.printStackTrace();

      } finally {
          //          if (connection != null) {
          //              try {
          //                  connection.close();
          //              } catch (Exception ex) {
          //                  ex.printStackTrace();
          //              }
          //          }
      }
      return bcPath;

  }

  public XMLDocument getEOXml(String entityPackage) {
     
      if (listOfAllEOs == null) {
          JprHelper jprHelper = new JprHelper();
          listOfAllEOs = jprHelper.getListOfAllEntityObjectsInJpr(m_jprAbsFilePath);
      }

      XMLDocument eoXml = null;
      if (listOfAllEOs.containsKey(entityPackage)) {
          // va is in the same project
          String listEOAbsPath = listOfAllEOs.get(entityPackage);
          eoXml = XMLParserHelper.getXMLDocument(listEOAbsPath);
      } else {
          // not in the same project.. look at the library dependencies of this project..
          eoXml = findBCFromProjectDependencies(m_jprAbsFilePath, entityPackage);
      }

      return eoXml;
  }

  public XMLDocument getPropertySetXml(String propertySetPackage) {

      JprHelper jprHelper = new JprHelper();
      HashMap<String, String> listOfPsInJpr = jprHelper.getListOfAllPropertySetsInJpr(m_jprAbsFilePath);


      XMLDocument psXml = null;
      if (listOfPsInJpr != null && listOfPsInJpr.containsKey(propertySetPackage)) {
          // va is in the same project
          String psabsPath = listOfPsInJpr.get(propertySetPackage);
          psXml = XMLParserHelper.getXMLDocument(psabsPath);
      } else {
          // not in the same project.. look at the library dependencies of this project..
          psXml = findBCFromProjectDependencies(m_jprAbsFilePath, propertySetPackage);
      }
      if (psXml == null) {
          String psPath = getBCPathFromDB(propertySetPackage);
          if (!isEmpty(psPath)) {
              String adeVieWRpot = getAdeViewRoot(m_jprAbsFilePath);
              psPath = adeVieWRpot + "/" + psPath;
              psXml = XMLParserHelper.getXMLDocument(psPath);
          }
      }
      return psXml;
  }

  public String getAdeViewRoot(String path) {

      int fappsIndex = path.indexOf("/fusionapps");
      if (fappsIndex != -1) {
          return path.substring(0, fappsIndex);
      }
      else {
        fappsIndex = path.indexOf("/fsm");
        if (fappsIndex != -1) 
            return path.substring(0, fappsIndex);
      }

      return "";
  }

  public String getViewAccessorVOPackage(Node listBindingNode, ViewObjectHelper voHelper, ConsumerVO consumer) {
      String viewAccessorName = XMLParserHelper.getAttributeValue(listBindingNode, "ListVOName");
      if (isEmpty(viewAccessorName))
          return "";
      String[] parts = viewAccessorName.split("\\.");
      if (parts == null || parts.length > 2) {
          System.out.println("The view acessor has more than 2 parts.. dont know how to handle");
          System.out.println(viewAccessorName);
          return "";
      }
      String eoName = parts[0];
      String vaName = parts[1];
      Node entityUsageNode = ViewObjectHelper.getEntityUsageNodeWithName(voHelper.getViewObjectNode(), eoName);
      if (entityUsageNode == null) {
          System.out.println("[WEIRD!!! ] Cannot find entity usage node for : " + eoName + " in vo: " + consumer.getVoName());
          return "";
      }
      String entityPackage = XMLParserHelper.getAttributeValue(entityUsageNode, "Entity");
      if (isEmpty(entityPackage)) {
          System.out.println("[WEIRD] could not obtain eneity package from usage node.. ");
          return "";
      }
      if (!isValidProducerPackage(entityPackage)) {
          System.out.println("Not a valid producer package:" + entityPackage);
          return "";
      }
      XMLDocument eoXml = getEOXml(entityPackage);
      if (eoXml == null) {
          System.out.println("Could not find eo xml .. ");
          return "";
      }
      Node eoNode = EntityObjectHelper.getEntityObjectNode(eoXml);
      if (eoNode == null) {
          System.out.println("[WEIRD!!] could not obtain eo node from eoxml");
          return "";
      }
      Node viewAccessorNode = EntityObjectHelper.getViewAccessorWithName(eoNode, vaName);
      String viewAccessorVOPackage = XMLParserHelper.getAttributeValue(viewAccessorNode, "ViewObjectName");
      return viewAccessorVOPackage;


  }


  public void processModel(Node listBindingVONode, String listBindingVOPackage, String attributeName, ConsumerVO consumer) {

      //        if (ViewObjectHelper.isExpertModeVO(listBindingVONode))
      //            return;
      Node listVOAttrNode = ViewObjectHelper.getAttributeByName(listBindingVONode, attributeName.trim());
      if (listVOAttrNode == null)
          return;
      if (m_scanType == ScanType.RECORD_ALL_CONSUMERS) {
          System.out.println(" ========= Have to record all consumers ... ");
          doCummulativeProcessing(listVOAttrNode, listBindingVONode, listBindingVOPackage, attributeName, consumer);
          return;
      }
      if (ViewObjectHelper.isExpertModeAttribute(listVOAttrNode)) {
          // Expert mode processing
          doExpertVoProcessing2(listVOAttrNode, listBindingVOPackage, attributeName, consumer);
      } else {
          if (scanTypeIncludesEoBased()) {
              doEoBasedVOProcessing(listVOAttrNode, listBindingVONode, listBindingVOPackage, attributeName, consumer);
          }
          if (scanTypeIncludesVOOverrideModel()) {
              doVoOverrideProcessing(listVOAttrNode, listBindingVONode, listBindingVOPackage, attributeName, consumer);
          }
      }

  }

  public boolean scanTypeIncludesExpertModel() {
      if (m_scanType == ScanType.EXPERT_AND_EOBASED || m_scanType == ScanType.EXPERT_VO || m_scanType == ScanType.ALL) {
          return true;
      }
      return false;
  }

  public boolean scanTypeIncludesEoBased() {
      if (m_scanType == ScanType.EXPERT_AND_EOBASED || m_scanType == ScanType.EOBASED || m_scanType == ScanType.ALL) {
          return true;
      }
      return false;
  }

  public boolean scanTypeIncludesVOOverrideModel() {
      if (m_scanType == ScanType.VOOVERRIDE || m_scanType == ScanType.ALL) {
          return true;
      }
      return false;
  }

  public void doVoOverrideProcessing(Node listVOAttrNode, Node listBindingVONode, String listBindingVOPackage,
                                     String displayAttributeName, ConsumerVO consumer) {
      if (scanTypeIncludesVOOverrideModel()) {
          System.out.println("-------------------------------Doing VOOverride processing");
          // for each display attr, get the vo , get the vo attr name
          // if (ViewObjectHelper.isExpertModeVO(listBindingVONode)) {
          //            Node listVOAttrNode = ViewObjectHelper.getAttributeByName(listBindingVONode, attributeName.trim());
          if (listVOAttrNode == null)
              return;
          String familyObt = MappingHelper.getFamilyForPackage(listBindingVOPackage);

          String displayWidth = ViewObjectHelper.getDisplayWidthFromAttrNode(listVOAttrNode);
          ProducerLOV producer = new ProducerLOV();
          producer.setFamily(familyObt);
          producer.setProduct(MappingHelper.getProductForPackage(listBindingVOPackage));
          producer.setDisplayAttr(displayAttributeName);
          producer.setDisplayWidth(displayWidth);
          producer.setBcPackage(listBindingVOPackage);
          producer.setBcName(ViewObjectHelper.getVONameFromPackage(listBindingVOPackage));
          producer.setBcProjectName(m_jprName);
          producer.setLovType("vooverride");
          producer.getConsumers().add(consumer);


          // check for property sets
          String vopsName = ViewObjectHelper.getPropertySetForAttr(listVOAttrNode);
          producer.setPropertySetName(vopsName);
          String psvoDisplayWidth = "";
          if (!isEmpty(vopsName)) {
              // get the property set
              XMLDocument psXml = getPropertySetXml(vopsName);
              if (psXml != null) {
                  Node domainNode = ViewObjectHelper.getPSRootNode(psXml);
                  if (domainNode != null) {
                      psvoDisplayWidth = ViewObjectHelper.getDisplayWidthFromAttrNode(domainNode);
                      producer.setPropertySetDWidth(psvoDisplayWidth);
                      // Confirm: I dont think it is important for users of spreadsheet to know that the widht is coming from PS.
                      // If  the width is empty and the psWidth is set, then the width = psWidth
                      //                        if(isEmpty(displayWidth) && !isEmpty(psvoDisplayWidth))
                      //                        {
                      //                           producer.setDisplayWidth(displayWidth);
                      //                        }
                  }
              }
          }


          if (!familyObt.equals(m_family) || !isValidProducerPackage(listBindingVOPackage)) {

              System.out.println(String.format("Family is not the one scanning for (scaning for %s) obtained %s, vo package %s",
                                               m_family, familyObt, listBindingVOPackage));
              return;
          }
          // get the EO Details
          String entityAttrName = ViewObjectHelper.getEntityAttrNameForVOAttribute(listVOAttrNode);
          // If no entity attr name =- it is not a case of vo override
          if (isEmpty(entityAttrName)) {
              System.out.println("Entity attribute name could not be obtained for List VO AttrName: " + displayAttributeName +
                                 ", package: " + listBindingVOPackage);
              return;
          }
          Node entityUsageNode = ViewObjectHelper.getEntityUsageNodeForVOAttributeNode(listBindingVONode, listVOAttrNode);
          if (entityUsageNode == null) {
              System.out.println(String.format("No entity usage node for listBindingVOPackage: %s, VO Attr: %s, entityAttrName: %s ",
                                               listBindingVOPackage, displayAttributeName, entityAttrName));
              return;
          }
          String entityPackage = XMLParserHelper.getAttributeValue(entityUsageNode, "Entity");
          if (isEmpty(entityPackage)) {
              System.out.println("[WEIRD] could not obtain eneity package from usage node.. ");
              return;
          }

          XMLDocument eoXml = getEOXml(entityPackage);
          String eoDisplayWidth = "";
          if (eoXml != null) {

              // get the eo attribute, the display width
              NodeList entityNodes = eoXml.getElementsByTagName("Entity");
              Node entityNode = null;
              if (entityNodes != null) {
                  entityNode = entityNodes.item(0);
              }

              if (entityNode != null) {
                  Node entityAttrNode = EntityObjectHelper.getAttributeByName(entityNode, entityAttrName);
                  if (entityAttrNode == null) {
                      System.out.println(String.format("!!!!! --- [WEIRD] could not find entity attribute node with name: %s in package %s",
                                                       entityAttrName, entityPackage));

                  } else {
                      eoDisplayWidth = EntityObjectHelper.getDisplayWidthFromAttrNode(entityAttrNode);

                      // check for property sets
                      String psName = ViewObjectHelper.getPropertySetForAttr(entityAttrNode);
                      producer.setSecondaryBcAttrPSName(psName);
                      String psDisplayWidth = "";
                      if (!isEmpty(psName)) {
                          // get the property set
                          XMLDocument psXml = getPropertySetXml(psName);
                          if (psXml != null) {
                              Node domainNode = ViewObjectHelper.getPSRootNode(psXml);
                              if (domainNode != null) {
                                  psDisplayWidth = ViewObjectHelper.getDisplayWidthFromAttrNode(domainNode);
                                  producer.setSecondaryBcAttrPSDWidth(psDisplayWidth);
                                  // Confirm: I dont think it is important for users of spreadsheet to know that the widht is coming from PS.
                                  // If  the width is empty and the psWidth is set, then the width = psWidth
                                  //                                    if(isEmpty(eoDisplayWidth) && !isEmpty(psDisplayWidth))
                                  //                                    {
                                  //                                       eoDisplayWidth=psDisplayWidth;
                                  //                                    }
                              }
                          }
                      }

                  }
              }
          } else {
              System.out.println("could not get eo xml : " + entityPackage);
          }


          producer.setSecondaryBcAttr(entityAttrName);
          producer.setSecondaryBcAttrWidth(eoDisplayWidth);
          producer.setSecondaryBcName(EntityObjectHelper.getEONameFromPackage(entityPackage));
          producer.setSecondaryBcPackage(entityPackage);


          addProducerToList(producer);

         


      }

  }


  public void updatePRSInProducer(Node attributeNode, ProducerLOV producer, boolean updateInSecondary) {
      String psName = ViewObjectHelper.getPropertySetForAttr(attributeNode);
      String psAttrWidth = "";
      if (!isEmpty(psName)) {
          // get the property set
          XMLDocument psXml = getPropertySetXml(psName);
          if (psXml != null) {
              Node domainNode = ViewObjectHelper.getPSRootNode(psXml);
              if (domainNode != null) {
                  psAttrWidth = ViewObjectHelper.getDisplayWidthFromAttrNode(domainNode);

              }
          }
      }
      if (updateInSecondary) {
          producer.setSecondaryBcAttrPSName(psName);
          producer.setSecondaryBcAttrPSDWidth(psAttrWidth);
      } else {
          producer.setPropertySetName(psName);
          producer.setPropertySetDWidth(psAttrWidth);
      }
  }

  public void doCummulativeProcessing(Node listVOAttrNode, Node listBindingVONode, String listBindingVOPackage,
                                      String displayAttributeName, ConsumerVO consumer) {

    if (listVOAttrNode == null)
        return;
    
      ProducerLOV producer = new ProducerLOV();
      String familyObt = MappingHelper.getFamilyForPackage(listBindingVOPackage);
      String displayWidth = ViewObjectHelper.getDisplayWidthFromAttrNode(listVOAttrNode);
      producer.setFamily(familyObt);
      producer.setProduct(MappingHelper.getProductForPackage(listBindingVOPackage));
      producer.setDisplayAttr(displayAttributeName);
      producer.setDisplayWidth(displayWidth);
      producer.setColumnWidth(ViewObjectHelper.getAttributePrecision(listVOAttrNode));
      producer.setBcPackage(listBindingVOPackage);
      producer.setBcName(ViewObjectHelper.getVONameFromPackage(listBindingVOPackage));
      producer.setBcProjectName(m_jprName);
      updatePRSInProducer(listVOAttrNode,producer,false);      

      if (ViewObjectHelper.isExpertModeAttribute(listVOAttrNode)) {
          producer.setLovType("expert");
      } else {
        if (!isValidProducerPackage(listBindingVOPackage)) {

            System.out.println(String.format("Family is not the one scanning for (scaning for %s) obtained %s, vo package %s",
                                             m_family, familyObt, listBindingVOPackage));
          consumer.getProducers().add(producer);
          return;
        }
          
        // get the EO Details
        String entityAttrName = ViewObjectHelper.getEntityAttrNameForVOAttribute(listVOAttrNode);
        // If no entity attr name =- it is not a case of vo override
        if (isEmpty(entityAttrName)) {
            System.out.println("Entity attribute name could not be obtained for List VO AttrName: " + displayAttributeName +
                               ", package: " + listBindingVOPackage);
          //TODO: Still should add the consumer  
          consumer.getProducers().add(producer);
          return;
        }
        Node entityUsageNode = ViewObjectHelper.getEntityUsageNodeForVOAttributeNode(listBindingVONode, listVOAttrNode);
        if (entityUsageNode == null) {
            System.out.println(String.format("No entity usage node for listBindingVOPackage: %s, VO Attr: %s, entityAttrName: %s ",
                                             listBindingVOPackage, displayAttributeName, entityAttrName));
            //TODO: Still should add the consumer 
            
            consumer.getProducers().add(producer);
            return;
        }
        String entityPackage = XMLParserHelper.getAttributeValue(entityUsageNode, "Entity");
        if (isEmpty(entityPackage)) {
            System.out.println("[WEIRD] could not obtain eneity package from usage node.. ");
            //TODO: Still should add the consumer 
            consumer.getProducers().add(producer);
            return;
        }
          
  if(!isValidProducerPackage(entityPackage))
  {
      //TODO: Still should add the consumer 
      
      consumer.getProducers().add(producer);
      return;
  }
          producer.setSecondaryBcProduct(MappingHelper.getProductForPackage(entityPackage));
        XMLDocument eoXml = getEOXml(entityPackage);
        String eoDisplayWidth = "";
        if (eoXml != null) {

            // get the eo attribute, the display width
            NodeList entityNodes = eoXml.getElementsByTagName("Entity");
            Node entityNode = null;
            if (entityNodes != null) {
                entityNode = entityNodes.item(0);
            }

            if (entityNode != null) {
                Node entityAttrNode = EntityObjectHelper.getAttributeByName(entityNode, entityAttrName);
                if (entityAttrNode == null) {
                    System.out.println(String.format("!!!!! --- [WEIRD] could not find entity attribute node with name: %s in package %s",
                                                     entityAttrName, entityPackage));

                } else {
                    eoDisplayWidth = EntityObjectHelper.getDisplayWidthFromAttrNode(entityAttrNode);
                    producer.setSecondaryColWidth(ViewObjectHelper.getAttributePrecision(entityAttrNode));
                    updatePRSInProducer(entityAttrNode, producer, true);

                }
            }
        } else {
            System.out.println("could not get eo xml : " + entityPackage);
        }

        producer.setSecondaryBcAttr(entityAttrName);
        producer.setSecondaryBcAttrWidth(eoDisplayWidth);
        producer.setSecondaryBcName(EntityObjectHelper.getEONameFromPackage(entityPackage));
        producer.setSecondaryBcPackage(entityPackage);
          // add producer to consumer
        
       // list_consumers.add(consumer);
      }
      consumer.getProducers().add(producer);


  }

  public void doEoBasedVOProcessing(Node listVOAttrNode, Node listBindingVONode, String listBindingVOPackage,
                                    String displayAttrName, ConsumerVO consumer) {

      // EOBased, all, vooverride still need to do eo based to get the base eo attr
      if (m_scanType == ScanType.EXPERT_VO)
          return;


      if (m_scanType == ScanType.EOBASED || m_scanType == ScanType.EXPERT_AND_EOBASED || m_scanType == ScanType.ALL) {

          System.out.println("-------------------------------Doing EO Based VO processing");
          String entityAttrName = ViewObjectHelper.getEntityAttrNameForVOAttribute(listVOAttrNode);
          if (isEmpty(entityAttrName)) {
              System.out.println("Entity attribute name could not be obtained for List VO AttrName: " + displayAttrName +
                                 ", package: " + listBindingVOPackage);
              return;
          }
          Node entityUsageNode = ViewObjectHelper.getEntityUsageNodeForVOAttributeNode(listBindingVONode, listVOAttrNode);
          if (entityUsageNode == null) {
              System.out.println(String.format("No entity usage node for listBindingVOPackage: %s, VO Attr: %s, entityAttrName: %s ",
                                               listBindingVOPackage, displayAttrName, entityAttrName));
              return;
          }
          String entityPackage = XMLParserHelper.getAttributeValue(entityUsageNode, "Entity");
          if (isEmpty(entityPackage)) {
              System.out.println("[WEIRD] could not obtain eneity package from usage node.. ");
              return;
          }

          if (!isValidProducerPackage(entityPackage)) {
              System.out.println("Not a valid producer package:" + entityPackage);
              return;
          }
          // String familyObt = MappingHelper.getFamilyForPackage(entityPackage);
          if (!MappingHelper.doesPackageBelongToFamily(entityPackage, m_family)) {
              System.out.println(String.format("--- DOES NOT BELONG TO FAMILY THAT IS BEING SCANNED FOR (%s): %s ", m_family,
                                               entityPackage));
              return;
          }

          XMLDocument eoXml = getEOXml(entityPackage);
          if (eoXml == null) {
              System.out.println("could not get eo xml : " + entityPackage);
              return;
          }
          // get the eo attribute, the display width
          NodeList entityNodes = eoXml.getElementsByTagName("Entity");
          if (entityNodes == null) {
              System.out.println("Could not find entity node");
              return;
          }
          Node entityNode = entityNodes.item(0);

          Node entityAttrNode = EntityObjectHelper.getAttributeByName(entityNode, entityAttrName);
          if (entityAttrNode == null) {
              System.out.println(String.format("!!!!! --- [WEIRD] could not find entity attribute node with name: %s in package %s",
                                               entityAttrName, entityPackage));
              return;
          }

          String eoDisplayWidth = EntityObjectHelper.getDisplayWidthFromAttrNode(entityAttrNode);

          // check for property sets
          String psName = ViewObjectHelper.getPropertySetForAttr(entityAttrNode);
          String psDisplayWidth = "";
          if (!isEmpty(psName)) {
              // get the property set
              XMLDocument psXml = getPropertySetXml(psName);
              if (psXml != null) {
                  Node domainNode = ViewObjectHelper.getPSRootNode(psXml);
                  if (domainNode != null) {
                      psDisplayWidth = ViewObjectHelper.getDisplayWidthFromAttrNode(domainNode);
                      // Confirm: I dont think it is important for users of spreadsheet to know that the widht is coming from PS.
                      // If  the width is empty and the psWidth is set, then the width = psWidth
                      //                        if(isEmpty(eoDisplayWidth) && !isEmpty(psDisplayWidth))
                      //                        {
                      //                           eoDisplayWidth=psDisplayWidth;
                      //                        }
                  }
              }
          }

          ProducerLOV producer = new ProducerLOV();
          producer.setFamily(MappingHelper.getFamilyForPackage(entityPackage));
          producer.setProduct(MappingHelper.getProductForPackage(entityPackage));
          producer.setDisplayAttr(entityAttrName);
          producer.setDisplayWidth(eoDisplayWidth);
          producer.setBcPackage(entityPackage);
          producer.setBcName(EntityObjectHelper.getEONameFromPackage(entityPackage));
          producer.getConsumers().add(consumer);
          producer.setLovType("eobased");
          producer.setPropertySetDWidth(psDisplayWidth);
          producer.setPropertySetName(psName);
          addProducerToList(producer);
          
      }
  }
  
  public Node getEntityAttributeNode(Node viewObjectNode, Node viewAttributeNode, String attrName){
      
    String entityAttrName = ViewObjectHelper.getEntityAttrNameForVOAttribute(viewAttributeNode);
    if (isEmpty(entityAttrName)) {
        System.out.println("Entity attribute name could not be obtained for VO Attr: " + attrName);
        return null;
    }
    Node entityUsageNode = ViewObjectHelper.getEntityUsageNodeForVOAttributeNode(viewObjectNode, viewAttributeNode);
    if (entityUsageNode == null) {
        System.out.println(String.format("No entity usage node for VO Attr: %s, entityAttrName: %s ",
                                          attrName, entityAttrName));
        return null;
    }
    String entityPackage = XMLParserHelper.getAttributeValue(entityUsageNode, "Entity");
    if (isEmpty(entityPackage)) {
        System.out.println("[WEIRD] could not obtain entity package from usage node.. ");
        return null;
    }
    
      if(entityPackage.startsWith("oracle.apps.fnd.applcore.")){
          System.out.println("could not get eo xml : " + entityPackage);
          return null;
      }
    
    XMLDocument eoXml = getEOXml(entityPackage);
    if (eoXml == null) {
        System.out.println("could not get eo xml : " + entityPackage + "in ViewObject: " 
                           + XMLParserHelper.getAttributeValue(viewObjectNode, "Name"));
        return null;
    }
    // get the eo attribute, the display width
    NodeList entityNodes = eoXml.getElementsByTagName("Entity");
    if (entityNodes == null) {
        System.out.println("Could not find entity node");
        return null;
    }
    Node entityNode = entityNodes.item(0);
    Node entityAttrNode = EntityObjectHelper.getAttributeByName(entityNode, entityAttrName);
    
    if (entityAttrNode == null) {
        
        String baseEntityPackage = XMLParserHelper.getAttributeValue(entityNode, "Extends");
        
        if(baseEntityPackage != null) {
          eoXml = getEOXml(baseEntityPackage);
          if(eoXml != null){
            entityNodes = eoXml.getElementsByTagName("Entity");
            if(entityNodes != null){
                entityNode = entityNodes.item(0);
                entityAttrNode = EntityObjectHelper.getAttributeByName(entityNode, entityAttrName);
            }
          }
        }
    }
    if (entityAttrNode == null) {
        System.out.println(String.format("!!!!! --- [WEIRD] could not find entity attribute node with name: %s in package %s for ViewObject %s and View Attribute %s",
                                         entityAttrName, entityPackage, XMLParserHelper.getAttributeValue(viewObjectNode, "Name"), XMLParserHelper.getAttributeValue(viewAttributeNode, "Name") ));
        return null;
    }
    return entityAttrNode;
  }

  public void doExpertVoProcessing2(Node listVOAttrNode, String listBindingVOPackage, String attributeName,
                                    ConsumerVO consumer) {
      if (scanTypeIncludesExpertModel()) {
          System.out.println("-------------------------------Doing Expert model processing");
          // for each display attr, get the vo , get the vo attr name
          // if (ViewObjectHelper.isExpertModeVO(listBindingVONode)) {
          //            Node listVOAttrNode = ViewObjectHelper.getAttributeByName(listBindingVONode, attributeName.trim());
          if (listVOAttrNode == null)
              return;
          String familyObt = MappingHelper.getFamilyForPackage(listBindingVOPackage);

          String displayWidth = ViewObjectHelper.getDisplayWidthFromAttrNode(listVOAttrNode);
          ProducerLOV producer = new ProducerLOV();
          producer.setFamily(familyObt);
          producer.setProduct(MappingHelper.getProductForPackage(listBindingVOPackage));
          producer.setDisplayAttr(attributeName);
          producer.setDisplayWidth(displayWidth);
          producer.setBcPackage(listBindingVOPackage);
          producer.setBcName(ViewObjectHelper.getVONameFromPackage(listBindingVOPackage));
          producer.setBcProjectName(m_jprName);
          producer.setLovType("expert");
          producer.getConsumers().add(consumer);


          // check for property sets
          String vopsName = ViewObjectHelper.getPropertySetForAttr(listVOAttrNode);
          producer.setPropertySetName(vopsName);
          String psDisplayWidth = "";
          if (!isEmpty(vopsName)) {
              // get the property set
              XMLDocument psXml = getPropertySetXml(vopsName);
              if (psXml != null) {
                  Node domainNode = ViewObjectHelper.getPSRootNode(psXml);
                  if (domainNode != null) {
                      psDisplayWidth = ViewObjectHelper.getDisplayWidthFromAttrNode(domainNode);
                      producer.setPropertySetDWidth(psDisplayWidth);
                      // Confirm: I dont think it is important for users of spreadsheet to know that the widht is coming from PS.
                      // If  the width is empty and the psWidth is set, then the width = psWidth
                      //                        if(isEmpty(displayWidth) && !isEmpty(psDisplayWidth))
                      //                        {
                      //                            producer.setDisplayWidth(psDisplayWidth);
                      //                        }
                  }
              }
          }

          if (familyObt.equals(m_family)) {
              addProducerToList(producer);


          }


      }
  }

  public void addProducerToDb(ProducerLOV producer, ScanType typeOfProducer) {


      try {
          Connection connection = getConnection();
          //connection = DriverManager.getConnection(dbUrl, uname, pwd);
          if (!doesProducerExist(producer, connection)) {
              String insertQ =
                  "insert into COMBOWIDTH_PROD(family,product,bcname,bcpackage,bcattribute,bcattributeWidth,lovtype) values(?,?,?,?,?,?,?)";
              PreparedStatement stmt = connection.prepareStatement(insertQ);
              stmt.setString(1, producer.getFamily());
              stmt.setString(2, producer.getProduct());
              stmt.setString(3, producer.getBcName());
              stmt.setString(4, producer.getBcPackage());
              stmt.setString(5, producer.getDisplayAttr());
              stmt.setString(6, producer.getDisplayWidth());
              if (typeOfProducer == ScanType.EXPERT_VO) {
                  stmt.setString(7, "expert");
              } else if (typeOfProducer == ScanType.EOBASED) {
                  stmt.setString(7, "eobased");
              } else if (typeOfProducer == ScanType.VOOVERRIDE) {
                  stmt.setString(7, "vooverride");
              } else {
                  stmt.setString(7, "");
              }
              stmt.executeUpdate();
              stmt.close();

          }
          // update list of consumers for the producer
          ArrayList<ConsumerVO> consumers = producer.getConsumers();
          if (consumers != null && consumers.size() > 0) {
              Iterator<ConsumerVO> consumerIter = consumers.iterator();
              while (consumerIter.hasNext()) {
                  ConsumerVO consumer = consumerIter.next();
                  insertConsumerToDb(consumer, connection, producer, typeOfProducer);
              }
          }
      } catch (Exception ex) {
          ex.printStackTrace();

      } finally {
          //            if (connection != null) {
          //                try {
          //                    connection.close();
          //                } catch (Exception ex) {
          //                    ex.printStackTrace();
          //                }
          //            }
      }
  }

  public String getProducerType(ScanType producerType) {
      if (producerType == ScanType.EXPERT_VO) {
          return "expert";
      } else if (producerType == ScanType.EOBASED) {
          return "eobased";
      } else if (producerType == ScanType.VOOVERRIDE) {
          return "vooverride";
      } else {
          return "";
      }
  }

  public void insertConsumerToDb(ConsumerVO consumer, Connection connection, ProducerLOV producer,
                                 ScanType typeOfProducer) throws Exception {

      String producerType = getProducerType(typeOfProducer);
      try {

          //connection = DriverManager.getConnection(dbUrl, uname, pwd);
          if (!doesConsumerExist(producer, consumer, connection, producerType)) {


              String insertQ =
                  "insert into combowidth_consumer(producerpackage,producerattr,producertype,family,product,uiPageName,uiPagePath,comboboxId,lovUses) " +
                  "values(?,?,?,?,?,?,?,?,?)";
              PreparedStatement stmt = connection.prepareStatement(insertQ);
              stmt.setString(1, producer.getBcPackage());
              stmt.setString(2, producer.getDisplayAttr());
              stmt.setString(3, producerType);
              stmt.setString(4, consumer.getFamily());
              stmt.setString(5, consumer.getProduct());
              stmt.setString(6, consumer.getUiFileName());
              stmt.setString(7, consumer.getUiFileFullPath());
              stmt.setString(8, consumer.getComboboxId());
              stmt.setString(9, consumer.getLovUses());


              stmt.executeUpdate();
              stmt.close();

          }

      } catch (Exception ex) {
          ex.printStackTrace();

      } finally {

      }

  }

  public boolean doesProducerExist(ProducerLOV producer, Connection connection) {
      try {
          String packagename = producer.getBcPackage();
          String attrName = producer.getDisplayAttr();
          String query = "SELECT 1 FROM COMBOWIDTH_PROD WHERE bcpackage = ? AND bcattribute=? ";
          PreparedStatement stmt = connection.prepareStatement(query);
          stmt.setString(1, packagename);
          stmt.setString(2, attrName);
          ResultSet results = stmt.executeQuery();
          boolean exists = false;
          if (results != null && results.next())
              exists = true;
          results.close();
          stmt.close();
          return exists;
      } catch (Exception ex) {
          ex.printStackTrace();
          return false;
      }

  }

  public boolean doesConsumerExist(ProducerLOV producer, ConsumerVO consumer, Connection connection, String typeOfProducer) {
      try {

          String packagename = producer.getBcPackage();
          String attrName = producer.getDisplayAttr();
          String lovUses = consumer.getLovUses();
          String query =
              "SELECT 1 FROM combowidth_consumer WHERE  producerattr=? and producerpackage=? and lovUses = ? AND uiPagePath=? and producertype=?";
          PreparedStatement stmt = connection.prepareStatement(query);
          stmt.setString(1, attrName);
          stmt.setString(2, packagename);
          stmt.setString(3, lovUses);
          stmt.setString(4, consumer.getUiFileFullPath());
          stmt.setString(5, typeOfProducer);
          ResultSet results = stmt.executeQuery();
          boolean exists = false;
          if (results != null && results.next())
              exists = true;
          results.close();
          stmt.close();
          return exists;
      } catch (Exception ex) {
          ex.printStackTrace();
          return false;
      }

  }

  public String getProducerKey(ProducerLOV producer) {
      return producer.getBcPackage() + "#" + producer.getDisplayAttr();
  }

  public void addProducerToList(ProducerLOV producer) {
      String producerKey = getProducerKey(producer);
      if (listProderVOs.containsKey(producerKey)) {
          ProducerLOV producerObt = listProderVOs.get(producerKey);
          producerObt.getConsumers().addAll(producer.getConsumers());
          listProderVOs.put(producerKey, producerObt);
      } else {
          if (m_bDebug) {
              System.out.println("Added a new producer.." + producerKey);
          }
          listProderVOs.put(producerKey, producer);
      }
  }

  public boolean isValidProducerPackage(String packageName) {
      if (isEmpty(packageName))
          return false;
      if (!packageName.startsWith("oracle.apps"))
          return false;
      if (packageName.contains(".flex."))
          return false;
      if (packageName.contains(".fnd.") || packageName.contains(".applcore."))
          return false;
      return true;
  }

  public boolean isEntityViewAccessor(String viewAccesorPackage) {
      if (viewAccesorPackage.contains("."))
          return true;
      return false;
  }

  public boolean isEntityBasedListBinding(Node listBindingNode) {
      String viewAccessorName = XMLParserHelper.getAttributeValue(listBindingNode, "ListVOName");
      if (isEmpty(viewAccessorName))
          return false;
      if (isEntityViewAccessor(viewAccessorName)) {
          return true;
      }
      return false;
  }

  public String getListBindingVOPackage(Node listBindingNode, Node voNode) {
      String viewAccessorName = XMLParserHelper.getAttributeValue(listBindingNode, "ListVOName");
      if (isEmpty(viewAccessorName))
          return "";
      if (isEntityViewAccessor(viewAccessorName)) {
          // dont show error msg if it is an expert vo.
          if (m_scanType != ScanType.EXPERT_VO) {
              System.out.println("TODO::::::::::::: ======== View accessor based on entity");
          }
          return "";
      }
      Node viewAccessorNode = ViewObjectHelper.getViewAccessorWithName(voNode, viewAccessorName);
      if (viewAccessorNode == null)
          return null;
      String vaVOPackage = XMLParserHelper.getAttributeValue(viewAccessorNode, "ViewObjectName");
      return vaVOPackage;
  }

  public XMLDocument getListBindingVO(Node listBindingNode, ViewObjectHelper voHelper) {

      String vaVOPackage = getListBindingVOPackage(listBindingNode, voHelper.getViewObjectNode());
      return getVOXml(vaVOPackage);

  }

  public XMLDocument getVOXml(String voPackage) {
      
    if (listOfAllVOs == null) {
        JprHelper jprHelper = new JprHelper();
        listOfAllVOs = jprHelper.getListOfAllViewObjectsInJpr(m_jprAbsFilePath);
    }
      if (listOfAllVOs.containsKey(voPackage)) {
          // va is in the same project
          String listVOAbsPath = listOfAllVOs.get(voPackage);
          return XMLParserHelper.getXMLDocument(listVOAbsPath);
      } else {
          // TODO: not in the same project.. look at the library dependencies of this project..
          // else revert to db
          return findBCFromProjectDependencies(m_jprAbsFilePath, voPackage);
      }
  }

  public XMLDocument findBCFromProjectDependencies(String jprFilePath, String bcPackageToFind) {
      XMLDocument bcXml = null;
      if (isEmpty(jprFilePath))
          return bcXml;
      if (!jprFilePath.endsWith(".jpr"))
          return bcXml;


      HashSet<String> adfLibrarys = getAllAdfLibrarysInJpr(jprFilePath, true);
      bcXml = ViewObjectHelper.findBCFromSetOfJarFiles(adfLibrarys, bcPackageToFind);
      if (bcXml != null)
          return bcXml;

      HashSet<String> adfbcImports = JprHelper.getAllBcImportsInJpr(jprFilePath, true);
      bcXml = ViewObjectHelper.findBCFromSetOfJarFiles(adfbcImports, bcPackageToFind);
      if (bcXml != null)
          return bcXml;
      // Also increase search paths, by looking in ADFLibrarydependencies file
      HashSet<String> secondaryDeps = JprHelper.getAllSecondaryDependencies(jprFilePath);
      bcXml = ViewObjectHelper.findBCFromSetOfJarFiles(secondaryDeps, bcPackageToFind);
      return bcXml;
  }

  public static boolean isEmpty(String str) {
      if (str == null || str.trim().equals(""))
          return true;
      return false;
  }


  public HashMap<String, ProducerLOV> getListProderVOs() {
      return listProderVOs;
  }
  
  public static HashSet<String> getAllAdfLibrarysInJpr(String jprAbsPath, boolean convertToAbsolutePath) {

      String baseDir = getBaseJprLocation(jprAbsPath);
      HashSet<String> set_alladflibs = new HashSet<String>();
      if (isEmpty(jprAbsPath)) {
          System.out.println("JprHelper->getAllAdfLibrarysInJpr: Required Absolute jpr path (jprAbsPath) ");
          return null;
      }
      XMLDocument xmlJpr = XMLParserHelper.getXMLDocument(jprAbsPath);
      xmlJpr.setProperty("SelectionLanguage", "XPath");

      if (xmlJpr == null) {
          System.out.println("JprHelper->getAllAdfLibrarysInJpr: Could not parse jpr as xml " + jprAbsPath);
          return null;
      }

      try {
          NodeList selectedNodes =
              xmlJpr.selectNodes("//hash[@n='internalDefinitions']/list[@n='libraryDefinitions']/hash");
          if (selectedNodes != null) {
              int selectedNodesCnt = selectedNodes.getLength();
              for (int i = 0; i < selectedNodesCnt; i++) {
                  Node node = selectedNodes.item(i);
                  String nodeName = node.getNodeName();
                  if (!isEmpty(nodeName) && nodeName.trim().equals("hash")) {
                      NodeList childrenOfHash = node.getChildNodes();
                      for (int j = 0; j < childrenOfHash.getLength(); j++) {
                          Node listNode = childrenOfHash.item(j);
                          String nValue = XMLParserHelper.getAttributeValue(listNode, "n");
                          if (!isEmpty(nValue) && nValue.trim().equals("classPath")) {
                              NodeList childrenOfList = listNode.getChildNodes();
                              int listLen = childrenOfList.getLength();
                              for (int k = 0; k < listLen; k++) {
                                  Node possibleUrlNode = childrenOfList.item(k);
                                  String urlTagName = possibleUrlNode.getNodeName();
                                  if (!isEmpty(urlTagName) && urlTagName.trim().equals("url")) {
                                      String jarValue = XMLParserHelper.getAttributeValue(possibleUrlNode, "path");
                                      
                                      if(!jarValue.endsWith(".jar")) continue;
                                      
                                      String[] jarParts = jarValue.split("/");
                                      String jarName = jarParts[jarParts.length - 1];
                                      if(!jarName.startsWith("Adf")) continue;
                                      
                                      if (convertToAbsolutePath) {
                                          File f = new File(baseDir, jarValue);
                                          if (f.exists()) {
                                              jarValue = f.getAbsolutePath();
                                          }
                                      }

                                      set_alladflibs.add(jarValue);
                                  }
                              }
                          }
                      }
                  }

              }
          } else {
              System.out.println("Could not obtain any library references in : " + jprAbsPath);
          }
      } catch (Exception ex) {
          System.out.println("Exception: " + ex.getMessage());
      }


      return set_alladflibs;
  }
  
  private static String getBaseJprLocation(String absPath) {
      if (absPath.endsWith(".jpr")) {
          int indexLastSlash = absPath.lastIndexOf("/");
          if (indexLastSlash != -1) {
              return absPath.substring(0, indexLastSlash);
          }

      }
      return null;
  }

}
