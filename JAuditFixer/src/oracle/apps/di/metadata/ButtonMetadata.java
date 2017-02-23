package oracle.apps.di.metadata;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ButtonMetadata extends ADFDiComponentMetadata {
    public ButtonMetadata() {
        super(META_BUTTON);
        componentType = META_BUTTON;
    }


    public static ADFDiComponentMetadata getMetadata(XMLDocument xmlDoc, Element metadataNode) throws Exception {
        //        ADFDiComponentMetadata metadata = new ADFDiComponentMetadata(META_WORKBOOK);
        //
        if (xmlDoc == null || metadataNode == null)
            return null;

        ButtonMetadata button = new ButtonMetadata();
        button.initializePositionAndId(metadataNode);
        Node clickAction = XMLParserHelper.getChildNodeWithName(metadataNode, "ClickActionSet");
        if (clickAction != null) {
            button.dblclickActionSet = ActionSet.getMetadata(xmlDoc, (Element)clickAction);
        }
        button.value=getTextContentOfXPathNode(metadataNode,"Label/Value");
        return button;

    }
}
