package oracle.apps.accessibilityfixers;

import java.util.List;
import java.util.regex.Pattern;

public class JAuditSearch {

    public enum Component {
        column,
        dialog,
        document,
        icon,
        image,
        inputColor,
        inputComboboxListOfValues,
        inputDate,
        inputFile,
        inputListOfValues,
        inputNumberSlider,
        inputNumberSpinbox,
        inputRangeSlider,
        inputText,
        outputLabel,
        panelBox,
        panelHeader,
        panelLabelAndMessage,
        panelWindow,
        quickQuery,
        richTextEditor,
        selectItem,
        selectBooleanCheckbox,
        selectBooleanRadio,
        selectManyCheckbox,
        selectManyChoice,
        selectManyListbox,
        selectManyShuttle,
        selectOneChoice,
        selectOneListbox,
        selectOneRadio,
        selectOrderShuttle,
        showDetailHeader,
        table,
        tree,
        treeTable;

        @Override
        public String toString() {
            return "af:" + this.name();
        }
    }

    public enum JAudit {
        TABLE_SUMMARIES,
        COMP_LABELED,
        COMP_TITLE,
        COLUMN_HEADERTEXT,
        IMAGE_SHORTDESC,
        COMP_TEXT;

        @Override
        public String toString() {
            switch (this) {
            case TABLE_SUMMARIES:
                return "Verify that tables/trees has summaries";
            case COMP_LABELED:
                return "Verify that the component is labeled";
            case COMP_TITLE:
                return "Verify that the component has a title";
            case COLUMN_HEADERTEXT:
                return "Verify that table columns have headers";
            case IMAGE_SHORTDESC:
                return "Verify that images have a description";
            case COMP_TEXT:
                return "Verify that the component has text";
            }
            return "";
        }
    }

    private final JAudit jAudit;
    private final List<Component> components;
    private final Pattern xlfKeyRegex;
    
    public static final String IMAGE_EXTRA_OPTION = "Use this option to set shortDesc=\"\"";
    public static final String IMAGE_EXTRA_OPTION2 = "Use this option to set shortDesc=&quot;&quot;";

    public JAuditSearch(JAudit jAudit, List<Component> components, String xlfKeyFilter) {
        this.jAudit = jAudit;
        this.components = components;
        this.xlfKeyRegex = Pattern.compile("^.+: \\['(" + xlfKeyFilter + ")\\..+'\\] \\- .+$");
    }

    public JAuditSearch.JAudit getJAudit() {
        return jAudit;
    }

    public List<JAuditSearch.Component> getComponents() {
        return components;
    }

    public Pattern getXlfKeyRegex() {
        return xlfKeyRegex;
    }
}
