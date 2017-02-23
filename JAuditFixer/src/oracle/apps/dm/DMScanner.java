package oracle.apps.dm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.NullEntityResolver;
import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.jdbc.OracleDriver;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class DMScanner extends JoesBaseClass {

    // R12 Database Info
    private static String URL;
    private static String USERNAME;
    private static String PASSWORD;
    private static boolean USE_R12 = false;

    private static final List<String> exceptions = Arrays.asList("abkhazian",
                                                                 "abn",
                                                                 "acceptgiro",
                                                                 "accounting flexfield",
                                                                 "accounting flexfield segment",
                                                                 "accrual reversal gl date source",
                                                                 "acd",
                                                                 "ach",
                                                                 "achang",
                                                                 "achinese",
                                                                 "acoli",
                                                                 "acrs",
                                                                 "activité principale exercée",
                                                                 "actual capitalizable cost",
                                                                 "actual capitalizable raw cost",
                                                                 "actual nonbillable cost",
                                                                 "actual nonbillable equipment effort",
                                                                 "actual nonbillable labor effort",
                                                                 "actual nonbillable raw cost",
                                                                 "actual noncapitalizable cost",
                                                                 "actual noncapitalizable raw cost",
                                                                 "adangme",
                                                                 "adf",
                                                                 "adjustment for ad hoc tax amounts",
                                                                 "adygei",
                                                                 "adyghe",
                                                                 "aeo",
                                                                 "afrihili",
                                                                 "airbill",
                                                                 "akan",
                                                                 "allow ad hoc recovery rate",
                                                                 "allow ad hoc tax rate",
                                                                 "allow autoextend",
                                                                 "anhui sheng",
                                                                 "annualization factor",
                                                                 "annualize",
                                                                 "antiguan",
                                                                 "apac",
                                                                 "api",
                                                                 "approval workflow required",
                                                                 "approver",
                                                                 "ar4ecsp",
                                                                 "aragonese",
                                                                 "asbn",
                                                                 "aseries",
                                                                 "asn",
                                                                 "aspp",
                                                                 "asr",
                                                                 "asset subinventory",
                                                                 "assignee",
                                                                 "assignee filter",
                                                                 "asturian",
                                                                 "athapascan",
                                                                 "ato",
                                                                 "auel",
                                                                 "autoaccounting",
                                                                 "autoadopt global clauses",
                                                                 "autoadoption",
                                                                 "autoapply",
                                                                 "autocash",
                                                                 "autocreate",
                                                                 "autocreate shipment",
                                                                 "autodemote",
                                                                 "autoextend",
                                                                 "autoextend in auctions",
                                                                 "autoextend in minutes",
                                                                 "autoextend period",
                                                                 "autoextend settings",
                                                                 "autoinvoice",
                                                                 "automatch",
                                                                 "autopost set name",
                                                                 "autopromote",
                                                                 "autopropagate",
                                                                 "autoprovision",
                                                                 "autorate",
                                                                 "autoreconcile",
                                                                 "autoreconciliation",
                                                                 "autoreverse after open period",
                                                                 "autosave",
                                                                 "autotransact code",
                                                                 "autoupdate",
                                                                 "avaric",
                                                                 "avc",
                                                                 "avestan",
                                                                 "awadhi",
                                                                 "bable",
                                                                 "bac",
                                                                 "backflush",
                                                                 "backorder",
                                                                 "backordered item",
                                                                 "backordered quantity",
                                                                 "bacs",
                                                                 "bahasa",
                                                                 "bahraini",
                                                                 "bai",
                                                                 "balance subledger entries by ledger currency",
                                                                 "balkar",
                                                                 "baluchi",
                                                                 "bambara",
                                                                 "bamileke",
                                                                 "banda",
                                                                 "basa",
                                                                 "base uom",
                                                                 "bashkir",
                                                                 "basotho",
                                                                 "batak",
                                                                 "bban",
                                                                 "beijing shi",
                                                                 "beja",
                                                                 "belarusian",
                                                                 "belizean",
                                                                 "bemba",
                                                                 "bhd",
                                                                 "bhojpuri",
                                                                 "bik",
                                                                 "bikol",
                                                                 "bislama",
                                                                 "blang",
                                                                 "bokmal",
                                                                 "bom",
                                                                 "bonan",
                                                                 "bpel",
                                                                 "bpi",
                                                                 "bpm",
                                                                 "braj",
                                                                 "bsc",
                                                                 "buginese",
                                                                 "bulleted",
                                                                 "buriat",
                                                                 "burkinabe",
                                                                 "buyei",
                                                                 "caddo",
                                                                 "cae",
                                                                 "cao",
                                                                 "cape verdean",
                                                                 "capitalizable",
                                                                 "carddav",
                                                                 "cas",
                                                                 "castilian",
                                                                 "catchweight",
                                                                 "ccb",
                                                                 "ccd",
                                                                 "ccd+",
                                                                 "ccdp",
                                                                 "cfda",
                                                                 "cfr",
                                                                 "chamic",
                                                                 "chewa",
                                                                 "chichewa",
                                                                 "china zhi gong dang",
                                                                 "chongqing shi",
                                                                 "chuang",
                                                                 "chuukese",
                                                                 "cif",
                                                                 "cii",
                                                                 "cin",
                                                                 "cip",
                                                                 "class 1a nics",
                                                                 "clave unica de identificacion tributaria",
                                                                 "cleardown",
                                                                 "clli",
                                                                 "cma",
                                                                 "cmec",
                                                                 "coa",
                                                                 "coida",
                                                                 "collectable percentage",
                                                                 "commerce extensible markup language",
                                                                 "commitment control flexfield",
                                                                 "compa-ratio",
                                                                 "compensable",
                                                                 "compshp",
                                                                 "configurator model type",
                                                                 "conjoined arrestment order",
                                                                 "convert requisition uom to agreement uom",
                                                                 "cosr",
                                                                 "cost postprocessor",
                                                                 "cost preprocessor",
                                                                 "cost rollup",
                                                                 "costing uom",
                                                                 "counterparty",
                                                                 "cp",
                                                                 "cpc",
                                                                 "cpla",
                                                                 "cpm",
                                                                 "cpt",
                                                                 "cpt30",
                                                                 "create costed or accounted expenditure batch for third-party application",
                                                                 "create debit memo from rts transaction",
                                                                 "create inline",
                                                                 "create uncosted labor expenditure batch",
                                                                 "create uncosted labor expenditure batch for third-party application",
                                                                 "create uncosted nonlabor expenditure batch",
                                                                 "create uncosted nonlabor expenditure batch for third-party application",
                                                                 "cre-m",
                                                                 "cro",
                                                                 "crs",
                                                                 "csa",
                                                                 "csop",
                                                                 "css",
                                                                 "cta",
                                                                 "ctaeo",
                                                                 "ctaeoable",
                                                                 "ctd",
                                                                 "cuit",
                                                                 "current approver",
                                                                 "current kpi value",
                                                                 "current maintenance arrestment",
                                                                 "current summarization period",
                                                                 "cushitic",
                                                                 "cvc2",
                                                                 "cvv2",
                                                                 "cxml",
                                                                 "daf",
                                                                 "dai",
                                                                 "dargwa",
                                                                 "das2",
                                                                 "daur",
                                                                 "dayak",
                                                                 "dba",
                                                                 "dbg",
                                                                 "dcia",
                                                                 "dcogs",
                                                                 "dcra",
                                                                 "ddi",
                                                                 "ddp",
                                                                 "ddu",
                                                                 "deang",
                                                                 "deconsolidate",
                                                                 "deliverability",
                                                                 "denormalize",
                                                                 "deo",
                                                                 "deprovision",
                                                                 "deq",
                                                                 "deregister",
                                                                 "deregistered",
                                                                 "derung",
                                                                 "descriptive flexfield",
                                                                 "descriptive flexfield segment 1",
                                                                 "descriptive flexfield segment 2",
                                                                 "designee",
                                                                 "destination subinventory",
                                                                 "deutsch",
                                                                 "dialogue",
                                                                 "dinka",
                                                                 "disenroll",
                                                                 "distributed authoring and versioning",
                                                                 "divehi",
                                                                 "djiboutian",
                                                                 "dml",
                                                                 "dns",
                                                                 "dogri",
                                                                 "dogrib",
                                                                 "dongxiang",
                                                                 "download punchout",
                                                                 "drm",
                                                                 "drp",
                                                                 "drs",
                                                                 "dsa",
                                                                 "dso",
                                                                 "duala",
                                                                 "dvr",
                                                                 "dyula",
                                                                 "dzongkha",
                                                                 "eac",
                                                                 "eac burdened cost rate",
                                                                 "earnings arrestment",
                                                                 "ebcdic",
                                                                 "ebs",
                                                                 "ecnr",
                                                                 "edl",
                                                                 "edn",
                                                                 "effectivity",
                                                                 "efik",
                                                                 "ehecs",
                                                                 "eic",
                                                                 "ein",
                                                                 "ekajuk",
                                                                 "emea",
                                                                 "emirati",
                                                                 "employer's annual federal unemployment (futa) tax return",
                                                                 "enable intercompany accounting",
                                                                 "enable intercompany balancing option",
                                                                 "enable multicurrency",
                                                                 "endian",
                                                                 "eod",
                                                                 "eol",
                                                                 "eop",
                                                                 "epc",
                                                                 "epf",
                                                                 "eps",
                                                                 "erisa",
                                                                 "erzya",
                                                                 "esd",
                                                                 "eseries",
                                                                 "esi",
                                                                 "esop",
                                                                 "espanol",
                                                                 "espp",
                                                                 "ess",
                                                                 "euc",
                                                                 "ewc",
                                                                 "ewenki",
                                                                 "ewondo",
                                                                 "expense subinventory",
                                                                 "expensed adjustment",
                                                                 "expensed asset",
                                                                 "expensed inventory",
                                                                 "extensible markup language",
                                                                 "extensible stylesheet language",
                                                                 "external uom",
                                                                 "exw",
                                                                 "facebook",
                                                                 "fanti",
                                                                 "faroese",
                                                                 "fas",
                                                                 "fca",
                                                                 "fefo",
                                                                 "ferc",
                                                                 "financial kpi category",
                                                                 "finno",
                                                                 "finra",
                                                                 "fips",
                                                                 "flexfield",
                                                                 "flexfield segment",
                                                                 "flsa",
                                                                 "fmv",
                                                                 "fngst",
                                                                 "fon",
                                                                 "fqdn",
                                                                 "friulian",
                                                                 "fsa",
                                                                 "fsavc",
                                                                 "fte",
                                                                 "ftz",
                                                                 "fujian sheng",
                                                                 "fulah",
                                                                 "funds unreserve",
                                                                 "futa",
                                                                 "gaap",
                                                                 "gallegan",
                                                                 "ganda",
                                                                 "gansu sheng",
                                                                 "gantt chart",
                                                                 "gaoshan",
                                                                 "gapless",
                                                                 "gapless invoice numbering",
                                                                 "gayo",
                                                                 "gbaya",
                                                                 "gdsn",
                                                                 "gdsn catalog",
                                                                 "gelao",
                                                                 "geocode",
                                                                 "gilbertese",
                                                                 "gl",
                                                                 "gl account",
                                                                 "gl cash account",
                                                                 "gl closed date",
                                                                 "gl offset account",
                                                                 "gmp",
                                                                 "gondi",
                                                                 "gorontalo",
                                                                 "gosi",
                                                                 "gosi number",
                                                                 "gpc",
                                                                 "gps",
                                                                 "grebo",
                                                                 "gst",
                                                                 "gtin",
                                                                 "guangdong sheng",
                                                                 "guangxi zhuangzu zizhiqu",
                                                                 "guid",
                                                                 "guizhou sheng",
                                                                 "gwich'in",
                                                                 "hafiza number",
                                                                 "hainan sheng",
                                                                 "hani",
                                                                 "hcm",
                                                                 "hcra",
                                                                 "hdhp",
                                                                 "health kpi category",
                                                                 "hebei sheng",
                                                                 "heilongjiang sheng",
                                                                 "heisei",
                                                                 "henan sheng",
                                                                 "hezhen",
                                                                 "hijrah date",
                                                                 "hiligaynon",
                                                                 "himachali",
                                                                 "hipaa",
                                                                 "hmis",
                                                                 "hmrc",
                                                                 "hong kong tebiexingzhengqu",
                                                                 "hostname",
                                                                 "hra",
                                                                 "hsa",
                                                                 "https",
                                                                 "hubei sheng",
                                                                 "hukou",
                                                                 "hunan sheng",
                                                                 "hupa",
                                                                 "ias",
                                                                 "iban",
                                                                 "iban",
                                                                 "ic",
                                                                 "icms",
                                                                 "ido",
                                                                 "iec",
                                                                 "ifrs",
                                                                 "igbo",
                                                                 "ijo",
                                                                 "iloko",
                                                                 "im",
                                                                 "implementor",
                                                                 "imposto sobre circulação de mercadorias e serviços",
                                                                 "imposto sobre produtos industrializados",
                                                                 "inari",
                                                                 "incoterms",
                                                                 "indic",
                                                                 "informatica",
                                                                 "ingush",
                                                                 "inss",
                                                                 "instantiability",
                                                                 "instantiable",
                                                                 "integrated postsecondary education system",
                                                                 "intercompany",
                                                                 "intercompany accounting",
                                                                 "intercompany accounts",
                                                                 "intercompany balancing rules",
                                                                 "intercompany billing",
                                                                 "intercompany billing project",
                                                                 "intercompany clearing account",
                                                                 "intercompany cross-charge transaction",
                                                                 "intercompany entered amount",
                                                                 "intercompany gain/loss account",
                                                                 "intercompany invoice base amount",
                                                                 "intercompany invoice currency",
                                                                 "intercompany segment",
                                                                 "intercompany tax receiving task",
                                                                 "intercompany transaction",
                                                                 "intercompany transaction account definition rule",
                                                                 "interface revenue to gl",
                                                                 "interlingue",
                                                                 "interoperating unit cross-charge transaction",
                                                                 "interorganization",
                                                                 "interorganization transfer",
                                                                 "interquartile range",
                                                                 "intersubinventory parameters",
                                                                 "intrabatch",
                                                                 "intraclass",
                                                                 "intracommunity",
                                                                 "intracompany",
                                                                 "intracompany movements",
                                                                 "intracompany transaction",
                                                                 "intraday",
                                                                 "intraoperating unit cross-charge transaction",
                                                                 "intraorganization",
                                                                 "intrastat",
                                                                 "ipeds",
                                                                 "ipi",
                                                                 "iqama",
                                                                 "iqr",
                                                                 "isam",
                                                                 "isic",
                                                                 "isignature",
                                                                 "itd",
                                                                 "item level punchout",
                                                                 "itin",
                                                                 "ivoirian",
                                                                 "ivr",
                                                                 "j2ee",
                                                                 "java ee",
                                                                 "javascript",
                                                                 "jiangsu sheng",
                                                                 "jiangxi sheng",
                                                                 "jiao",
                                                                 "jilin sheng",
                                                                 "jingpo",
                                                                 "jino",
                                                                 "jis",
                                                                 "jit",
                                                                 "jiu san society",
                                                                 "jndi",
                                                                 "jva",
                                                                 "kabardian",
                                                                 "kabyle",
                                                                 "kachin",
                                                                 "kalaallisut",
                                                                 "kamba",
                                                                 "kanban",
                                                                 "kanuri",
                                                                 "kashmiri",
                                                                 "kashubian",
                                                                 "kawi",
                                                                 "kazak",
                                                                 "kazakhstani",
                                                                 "kb",
                                                                 "khasi",
                                                                 "kimbundu",
                                                                 "kinyarwanda",
                                                                 "kirgiz",
                                                                 "kiswahili",
                                                                 "kittitian",
                                                                 "komi",
                                                                 "konkani",
                                                                 "kordofanian",
                                                                 "kosovar",
                                                                 "kosraean",
                                                                 "kpelle",
                                                                 "kpi",
                                                                 "kpi category",
                                                                 "kpi watchlist",
                                                                 "kru",
                                                                 "kuanyama",
                                                                 "kumyk",
                                                                 "kurukh",
                                                                 "kutenai",
                                                                 "kwanyama",
                                                                 "kyrgyzstani",
                                                                 "la déclaration des honoraires",
                                                                 "ladino",
                                                                 "lahnda",
                                                                 "lahu",
                                                                 "lamba",
                                                                 "ldap",
                                                                 "ldg",
                                                                 "ledger intercompany rules",
                                                                 "legal entity intercompany rules",
                                                                 "leichtensteiner",
                                                                 "lel",
                                                                 "letzeburgesch",
                                                                 "lezghian",
                                                                 "lgps",
                                                                 "lhoba",
                                                                 "liaoning sheng",
                                                                 "lifecycle",
                                                                 "lifecycle management",
                                                                 "lifecycle phase",
                                                                 "lifecycle phase type",
                                                                 "lingala",
                                                                 "lisu",
                                                                 "locator",
                                                                 "locator control",
                                                                 "login",
                                                                 "lominger international",
                                                                 "lookup code",
                                                                 "lot-type seiban",
                                                                 "lozi",
                                                                 "lpr",
                                                                 "luba",
                                                                 "luiseno",
                                                                 "lule",
                                                                 "lunda",
                                                                 "luo",
                                                                 "luxembourgish",
                                                                 "macao tebiexingzhengqu",
                                                                 "macrs",
                                                                 "madurese",
                                                                 "magahi",
                                                                 "maithili",
                                                                 "makasar",
                                                                 "mandar",
                                                                 "manipuri",
                                                                 "manobo",
                                                                 "maonan",
                                                                 "mark as nonduplicate",
                                                                 "marshallese",
                                                                 "marwari",
                                                                 "mbo",
                                                                 "mea",
                                                                 "mende",
                                                                 "metadata",
                                                                 "miao",
                                                                 "micr",
                                                                 "minangkabau",
                                                                 "mls",
                                                                 "moksha",
                                                                 "monba",
                                                                 "mongo",
                                                                 "mossi",
                                                                 "motu",
                                                                 "mpf",
                                                                 "mpn",
                                                                 "mpp",
                                                                 "mps",
                                                                 "mrp",
                                                                 "msa",
                                                                 "msc",
                                                                 "msds",
                                                                 "mta",
                                                                 "mtd",
                                                                 "mtom",
                                                                 "mulao",
                                                                 "multicurrency",
                                                                 "multipack",
                                                                 "multiple worksite report",
                                                                 "multiuse",
                                                                 "munda",
                                                                 "mwr",
                                                                 "nace",
                                                                 "nacha",
                                                                 "naf",
                                                                 "naics",
                                                                 "nasd",
                                                                 "nauruan",
                                                                 "navaho",
                                                                 "navteq",
                                                                 "naxi",
                                                                 "nbv",
                                                                 "nces",
                                                                 "ndonga",
                                                                 "ndr",
                                                                 "need reapproval",
                                                                 "nei mongol zizhiqu",
                                                                 "nettable",
                                                                 "newari",
                                                                 "niable",
                                                                 "nias",
                                                                 "nic",
                                                                 "ningxia huizu zizhiqu",
                                                                 "niuean",
                                                                 "nls",
                                                                 "nogai",
                                                                 "nomenclature des activités françaises.",
                                                                 "nomenclature statistique des activités economiques dans la communauté européenne",
                                                                 "nonbillable cost",
                                                                 "nonbillable cost percent of total cost",
                                                                 "noncatalog request",
                                                                 "nonduplicate",
                                                                 "nonduplicate end date",
                                                                 "nonduplicate record",
                                                                 "nonflex benefits",
                                                                 "nonflex program",
                                                                 "noninventory item",
                                                                 "noninventory-based product classification",
                                                                 "nonlabor",
                                                                 "nonlabor invoice burden schedule",
                                                                 "nonlabor resource",
                                                                 "nonlabor resource organization",
                                                                 "nonlabor revenue burden schedule",
                                                                 "nonlabor rule",
                                                                 "nonlabor schedule type",
                                                                 "nonlabor transfer percentage",
                                                                 "nonmonetary",
                                                                 "nonnettable",
                                                                 "nonowned item",
                                                                 "nonpersonal",
                                                                 "nonproject budget",
                                                                 "nonqualified stock options",
                                                                 "nonquota sales credit",
                                                                 "nonretirement funding income",
                                                                 "nonstock item",
                                                                 "nonsubscription item",
                                                                 "nonsufficient funds",
                                                                 "nonworker",
                                                                 "npa",
                                                                 "nqf",
                                                                 "nrfi",
                                                                 "nric",
                                                                 "number of future-enterable periods",
                                                                 "number of nonadjusting periods",
                                                                 "número de identificação bancária",
                                                                 "numero de identificacion tributaria",
                                                                 "numéro interne de classement",
                                                                 "nvq",
                                                                 "nyamwezi",
                                                                 "nyanja",
                                                                 "nyankole",
                                                                 "nynorsk",
                                                                 "nyoro",
                                                                 "nzima",
                                                                 "occitan",
                                                                 "ofccp",
                                                                 "oltp",
                                                                 "omp",
                                                                 "onboard",
                                                                 "onboarding",
                                                                 "online payer verification",
                                                                 "online transaction processing",
                                                                 "oracle peoplesoft",
                                                                 "oracle projects nonlabor",
                                                                 "oracle siebel",
                                                                 "oracle weblogic communication services",
                                                                 "orderable",
                                                                 "oromo",
                                                                 "oroqen",
                                                                 "osp",
                                                                 "ospp",
                                                                 "ossetian",
                                                                 "otn",
                                                                 "otomian",
                                                                 "overapply",
                                                                 "overreceipt action",
                                                                 "overreceipt tolerance",
                                                                 "overreceipt tolerance percentage",
                                                                 "override geocode for taxware",
                                                                 "owlcs",
                                                                 "palauan",
                                                                 "pali",
                                                                 "pampanga",
                                                                 "pangasinan",
                                                                 "panjabi",
                                                                 "papiamento",
                                                                 "papua new guinean",
                                                                 "papuan",
                                                                 "patd",
                                                                 "payables controls",
                                                                 "paye",
                                                                 "paysheet",
                                                                 "pdf",
                                                                 "pdi",
                                                                 "pdp",
                                                                 "pending approver",
                                                                 "periodization",
                                                                 "phf",
                                                                 "phf/si",
                                                                 "pii",
                                                                 "pinless debit card",
                                                                 "pjtd",
                                                                 "pkcs",
                                                                 "pl/sql",
                                                                 "pla",
                                                                 "plm",
                                                                 "pohnpeian",
                                                                 "pos",
                                                                 "postable detail",
                                                                 "postable or reversible status",
                                                                 "postdowntime",
                                                                 "postprocessing days",
                                                                 "postprocessing extension",
                                                                 "postprocessing lead time",
                                                                 "posttransformation",
                                                                 "ppd+",
                                                                 "ppp",
                                                                 "preallocation",
                                                                 "preapproved",
                                                                 "preboard",
                                                                 "preboarding",
                                                                 "precarriage",
                                                                 "predowntime",
                                                                 "pre-ni",
                                                                 "prenote",
                                                                 "prenotification",
                                                                 "prenumbered stock",
                                                                 "prepack",
                                                                 "pre-paye",
                                                                 "preprocess",
                                                                 "preprocessing days",
                                                                 "preprocessing extension",
                                                                 "preprocessing lead time",
                                                                 "preprocessor",
                                                                 "pre-social insurance",
                                                                 "pretransformation",
                                                                 "prevalidation",
                                                                 "prevent changes to items returned from punchout site",
                                                                 "previous kpi value",
                                                                 "primary uom",
                                                                 "product lifecycle management",
                                                                 "prsa",
                                                                 "prsi",
                                                                 "pson",
                                                                 "pstn",
                                                                 "ptd",
                                                                 "ptd balance",
                                                                 "pumi",
                                                                 "punchout",
                                                                 "punchout price variance",
                                                                 "punchout url",
                                                                 "purchasing uom",
                                                                 "pushto",
                                                                 "qa",
                                                                 "qa checklist",
                                                                 "qatd",
                                                                 "qiang",
                                                                 "qinghai sheng",
                                                                 "qst",
                                                                 "qtd",
                                                                 "qtd variance between current and previous forecast margin",
                                                                 "qtd variance between current and previous forecast margin percentage",
                                                                 "qtd variance between current and previous forecast revenue",
                                                                 "quickpay",
                                                                 "rac",
                                                                 "raeto",
                                                                 "rajasthani",
                                                                 "rapidfile database",
                                                                 "rarotongan",
                                                                 "rcea",
                                                                 "reamortization",
                                                                 "reapprove",
                                                                 "rebill",
                                                                 "rebuildable items",
                                                                 "recoupment amount",
                                                                 "rederive",
                                                                 "redline comparisons",
                                                                 "referenceable",
                                                                 "registro informacion fiscal",
                                                                 "registro unico de contribuyentes",
                                                                 "registro unico tributario",
                                                                 "reimport",
                                                                 "reinherit",
                                                                 "reinitiate",
                                                                 "remitter",
                                                                 "replan",
                                                                 "repromise",
                                                                 "requery",
                                                                 "reregister",
                                                                 "reservable",
                                                                 "retainage",
                                                                 "retainage release",
                                                                 "return-and-replace rma",
                                                                 "return-to-stock rma",
                                                                 "revenue summarization errors",
                                                                 "revert nonduplicate",
                                                                 "rfi",
                                                                 "rfq",
                                                                 "rma",
                                                                 "rollup rule filter",
                                                                 "rollup rules",
                                                                 "rpd",
                                                                 "rss",
                                                                 "rtf",
                                                                 "ruc",
                                                                 "rulebase",
                                                                 "rumanian",
                                                                 "rundi",
                                                                 "saca",
                                                                 "sahrawi",
                                                                 "saica",
                                                                 "saint vincentian",
                                                                 "salar",
                                                                 "salishan",
                                                                 "sami",
                                                                 "sammarinese",
                                                                 "sandawe",
                                                                 "sango",
                                                                 "santali",
                                                                 "sao tomean",
                                                                 "saqa",
                                                                 "sar",
                                                                 "sardinian",
                                                                 "sasak",
                                                                 "savepoint",
                                                                 "scac",
                                                                 "scm",
                                                                 "scon",
                                                                 "sdo",
                                                                 "search online help",
                                                                 "secondary uom",
                                                                 "securitization",
                                                                 "selkup",
                                                                 "sep ira",
                                                                 "sepa",
                                                                 "servicer reference",
                                                                 "servlet",
                                                                 "ses",
                                                                 "setpack",
                                                                 "seychellois",
                                                                 "shaanxi sheng",
                                                                 "shan",
                                                                 "shandong sheng",
                                                                 "shanghai shi",
                                                                 "shanxi sheng",
                                                                 "shenzhen",
                                                                 "shona",
                                                                 "showa",
                                                                 "sichuan",
                                                                 "sichuan sheng",
                                                                 "sidamo",
                                                                 "sierra leonan",
                                                                 "siex",
                                                                 "siksika",
                                                                 "siouan",
                                                                 "siret",
                                                                 "sjis",
                                                                 "sku",
                                                                 "sl1",
                                                                 "sl2",
                                                                 "sla",
                                                                 "slc",
                                                                 "smp",
                                                                 "smp1",
                                                                 "smp2",
                                                                 "sms",
                                                                 "soa",
                                                                 "social security ic number",
                                                                 "sociedades anónimas de capital autorizado",
                                                                 "sociedades anónimas inscritas de capital abierto",
                                                                 "sogdian",
                                                                 "soninke",
                                                                 "sorbian",
                                                                 "sotho",
                                                                 "source rollup rule filter",
                                                                 "spif",
                                                                 "spml",
                                                                 "spp",
                                                                 "spp1",
                                                                 "spp2",
                                                                 "sql",
                                                                 "sqwl",
                                                                 "srcop",
                                                                 "sri lankan",
                                                                 "ssn",
                                                                 "ssp",
                                                                 "ssp1",
                                                                 "ssp1(l)",
                                                                 "ssp2",
                                                                 "staging locator",
                                                                 "staging subinventory",
                                                                 "standaard digitale nota",
                                                                 "status for autopropagation",
                                                                 "stl",
                                                                 "subcompetency",
                                                                 "subelements",
                                                                 "subheader",
                                                                 "subinventories to count",
                                                                 "subinventory",
                                                                 "subledger",
                                                                 "subledger accounting",
                                                                 "subledger accounting options",
                                                                 "subledger application",
                                                                 "subledger balance",
                                                                 "subledger gain/loss",
                                                                 "subledger journal entry",
                                                                 "subledger journal entry rule set",
                                                                 "subledger journal entry rule set assignment",
                                                                 "subledger level",
                                                                 "subledger-level reporting currency",
                                                                 "subprocess",
                                                                 "subtask",
                                                                 "sukuma",
                                                                 "summarization",
                                                                 "summarization date and time",
                                                                 "superintendencia de inversiones extranjeras",
                                                                 "susu",
                                                                 "svenska",
                                                                 "svr",
                                                                 "swati",
                                                                 "système didentification du répertoire des entreprises",
                                                                 "système didentification du répertoire des établissements",
                                                                 "tablespace",
                                                                 "tai",
                                                                 "taisho",
                                                                 "taiwan sheng",
                                                                 "tajik",
                                                                 "tajikistani",
                                                                 "tamashek",
                                                                 "taric",
                                                                 "tariff intégré communautaire",
                                                                 "tcc",
                                                                 "tetum",
                                                                 "tfn",
                                                                 "the kuomingtang revolution committee of china",
                                                                 "third-party costed or accounted",
                                                                 "third-party nonlabor",
                                                                 "tiaa-cref",
                                                                 "tianjin shi",
                                                                 "tigre",
                                                                 "time of online entry",
                                                                 "timne",
                                                                 "timorese",
                                                                 "tiv",
                                                                 "tokelau",
                                                                 "tp-1015.3-v",
                                                                 "tp-1015.r.13.1-v",
                                                                 "tpa",
                                                                 "tpcc",
                                                                 "transmission servlet base url",
                                                                 "tsonga",
                                                                 "tuijia",
                                                                 "tumbuka",
                                                                 "turkmen",
                                                                 "tuvinian",
                                                                 "uae",
                                                                 "uap",
                                                                 "uccnet",
                                                                 "ucs",
                                                                 "udex",
                                                                 "udi",
                                                                 "udmurt",
                                                                 "uel",
                                                                 "ui",
                                                                 "uic",
                                                                 "umbundu",
                                                                 "unapplied receipts",
                                                                 "unapply",
                                                                 "unassign",
                                                                 "unassignment description",
                                                                 "unbilled amount",
                                                                 "unbilled receivables",
                                                                 "unbilled receivables balance",
                                                                 "unbilled transaction",
                                                                 "unbudgeted",
                                                                 "uncommit",
                                                                 "unearn",
                                                                 "unenroll",
                                                                 "unfollow",
                                                                 "unhide",
                                                                 "unidad de inversion",
                                                                 "unidad de valor constante",
                                                                 "uniform resource locator",
                                                                 "unimported items",
                                                                 "uninstall",
                                                                 "uninvoiced in contract currency",
                                                                 "union internationale de chemins des fer",
                                                                 "uniplexed information and computing system",
                                                                 "unit effectivity",
                                                                 "unmark",
                                                                 "unparsable",
                                                                 "unpost",
                                                                 "unpublish",
                                                                 "unreconcile",
                                                                 "unregister",
                                                                 "unreserve",
                                                                 "unschedule",
                                                                 "unship",
                                                                 "unspsc",
                                                                 "unsubscribe",
                                                                 "unsuspend",
                                                                 "unvested stock options",
                                                                 "uom",
                                                                 "uom size",
                                                                 "updatable by external parties",
                                                                 "upk",
                                                                 "upsell",
                                                                 "use invoice method for intercompany billing",
                                                                 "use revenue method for intercompany billing",
                                                                 "use subledger options",
                                                                 "use workflow for status changes",
                                                                 "userra",
                                                                 "ussgl",
                                                                 "utf",
                                                                 "uvc",
                                                                 "uygur",
                                                                 "uzbekistani",
                                                                 "vai",
                                                                 "varchar",
                                                                 "vcard",
                                                                 "venda",
                                                                 "vevraa",
                                                                 "virtual business card distributed authoring and versioning",
                                                                 "volapuk",
                                                                 "votic",
                                                                 "vsoe",
                                                                 "wakashan",
                                                                 "walamo",
                                                                 "washo",
                                                                 "watchlist",
                                                                 "waybill airbill number",
                                                                 "wbs",
                                                                 "web-based distributed authoring and versioning",
                                                                 "webdav",
                                                                 "weblogic server",
                                                                 "wiki",
                                                                 "wildcard",
                                                                 "wip",
                                                                 "withholding statement for puerto rico",
                                                                 "wls",
                                                                 "wnu",
                                                                 "wolof",
                                                                 "workflow enabled for status changes",
                                                                 "workflow status",
                                                                 "workflow template",
                                                                 "worklist",
                                                                 "wsdl",
                                                                 "xibe",
                                                                 "xinjiang uygur zizhiqu",
                                                                 "xizang zizhiqu",
                                                                 "xsd",
                                                                 "xsl",
                                                                 "xsl transformations",
                                                                 "xsl-fo style sheet",
                                                                 "xslt",
                                                                 "yao",
                                                                 "yapese",
                                                                 "yatd",
                                                                 "yi",
                                                                 "ytd",
                                                                 "ytd bonus depreciation",
                                                                 "ytd depreciation",
                                                                 "ytd earned value cost variance",
                                                                 "ytd earned value schedule variance",
                                                                 "ytd effective labor multiplier",
                                                                 "ytd equipment effort",
                                                                 "yugur",
                                                                 "yunnan sheng",
                                                                 "yupik",
                                                                 "zande",
                                                                 "zang",
                                                                 "zenaga",
                                                                 "zengin",
                                                                 "zhejiang sheng",
                                                                 "zhuang");
    private DOMParser parser = new DOMParser();

    private Workbook workbook;
    private Worksheet worksheet;
    private int currentRow = 2;

    private int numTabViolations = 0;
    private int numColViolations = 0;
    private int numShortTabViolations = 0;
    private int numTabNameMatchesDesc = 0;
    private int numColNameMatchesDesc = 0;
    private int numShortColViolations = 0;

    private String tableIDCache = "NULL";
    private HashMap<String, String> rowsCache = new HashMap<String, String>();
    private Connection conn;

    public DMScanner() {
        super(JoesBaseClass.CRAWL_TYPE.TABLE);

        // Set up excel output.
        workbook = new Workbook("Data Model Violations");
        worksheet = new Worksheet("Main");

        worksheet.setColumnWidth(1, 8);
        worksheet.setColumnWidth(2, 25);
        worksheet.setColumnWidth(3, 40);
        worksheet.setColumnWidth(4, 5);
        worksheet.setColumnWidth(5, 11);
        worksheet.setColumnWidth(6, 32);
        worksheet.setColumnWidth(7, 32);
        worksheet.setColumnWidth(8, 17);
        worksheet.setColumnWidth(9, 60);
        worksheet.setColumnWidth(10, 60);
        worksheet.setColumnWidth(11, 60);


        worksheet.addCell(new Cell(new CellLocation("A1"),
                                   new CellValue("Product")));
        worksheet.addCell(new Cell(new CellLocation("B1"),
                                   new CellValue("Issue")));
        worksheet.addCell(new Cell(new CellLocation("C1"),
                                   new CellValue("Spelling Warnings")));
        worksheet.addCell(new Cell(new CellLocation("D1"),
                                   new CellValue("File")));
        worksheet.addCell(new Cell(new CellLocation("E1"),
                                   new CellValue("Object Type")));
        worksheet.addCell(new Cell(new CellLocation("F1"),
                                   new CellValue("Table Name")));
        worksheet.addCell(new Cell(new CellLocation("G1"),
                                   new CellValue("Column Name")));
        worksheet.addCell(new Cell(new CellLocation("H1"),
                                   new CellValue("Same as R12 (Y/N)")));
        worksheet.addCell(new Cell(new CellLocation("I1"),
                                   new CellValue("R12 Description")));
        worksheet.addCell(new Cell(new CellLocation("J1"),
                                   new CellValue("Current Fusion Description")));
        worksheet.addCell(new Cell(new CellLocation("K1"),
                                   new CellValue("New Description")));
        workbook.addWorksheet(worksheet);
    }

    protected void processFile(File f, boolean doADE) {
        try {
            System.out.println("INFO: Reviewing file:  " + f.getAbsolutePath());
            XMLDocument doc = null;
            tableIDCache = "NULL";
            rowsCache.clear();

            parser.reset();
            try {
                parser.reset();
                parser.setDebugMode(true);
                parser.setErrorStream(System.out);
                parser.setEntityResolver(new NullEntityResolver());
                parser.showWarnings(true);
                parser.setValidationMode(DOMParser.NONVALIDATING);
                parser.parse("file:" + f.getAbsolutePath());
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println("ERROR:  XML Parse error while parsing file:  " + f.getAbsolutePath());
                System.err.println("ERROR: Skipping...");
                this.logException("Error parsing file: " + f.getAbsolutePath(),
                                  e);
                return;
            }
            doc = parser.getDocument();

            Node table = null;
            // Traversing nodes until reaching table node.
            try {
                Node n = XMLParserHelper.getChildNodeWithName(doc,
                                                              "COMPOSITE");
                n = XMLParserHelper.getChildNodeWithName(n, "BASE_OBJECT");
                n = XMLParserHelper.getChildNodeWithName(n, "TABLE");
                if (n == null) {
                    System.err.println("ERROR: XML does not have the expected format.");
                    System.err.println("ERROR: Skipping...");
                    return;
                }
                table = n;
            }
            catch (NullPointerException e) {
                System.err.println("ERROR: XML does not have the expected format.");
                System.err.println("ERROR: Skipping...");
                return;
            }
            Node table_name = XMLParserHelper.getChildNodeWithName(table,
                                                                   "NAME");
            Node relational_table = XMLParserHelper.getChildNodeWithName(table,
                                                                         "RELATIONAL_TABLE");

            Node table_customer_area = XMLParserHelper.getChildNodeWithName(table,
                                                                            "CUSTOMER_AREA");
            Node table_odb_property_list = XMLParserHelper.getChildNodeWithName(table_customer_area,
                                                                                "ODB_PROPERTY_LIST");
            Node table_comment = XMLParserHelper.getChildNodeWithName(table_odb_property_list,
                                                                      "COMMENT");

            String description_string = "";
            if (table_comment != null) {
                description_string = table_comment.getTextContent();
            }


            // Product
            String product = "";
            Matcher m = Pattern.compile("fin/components/\\w+?/(\\w+?)/").matcher(f.getCanonicalPath());
            if (m.find()) {
                product = m.group(1).toUpperCase(Locale.ENGLISH);
            }

            CellLocation l = new CellLocation("A" + currentRow);
            CellValue v = new CellValue(product);
            worksheet.addCell(new Cell(l, v));

            // Issue
            String issue_string = "";
            if (table_comment == null || table_comment.getTextContent().trim().isEmpty()) {
                issue_string = "No description. - DM.TABLE.020";
                numTabViolations++;
            }
            else if (description_string.equals(table_name.getTextContent())) {
                issue_string = "Description matches name. - DM.WARNING.008";
                numTabNameMatchesDesc++;
            }
            else if (description_string.length() < 31) {
                issue_string = "Short description. - DM.WARNING.005";
                numShortTabViolations++;
            }
            l = new CellLocation("B" + currentRow);
            v = new CellValue(issue_string);
            worksheet.addCell(new Cell(l, v));

            // Spelling Warnings
            String spelling_warnings = getSpellingErrors(table_name.getTextContent(),
                                                         description_string);
            l = new CellLocation("C" + currentRow);
            v = new CellValue(spelling_warnings);
            worksheet.addCell(new Cell(l, v));

            // File
            l = new CellLocation("D" + currentRow);
            v = new CellValue(f.getCanonicalPath());
            worksheet.addCell(new Cell(l, v));

            // Object Type
            l = new CellLocation("E" + currentRow);
            v = new CellValue("TABLE");
            worksheet.addCell(new Cell(l, v));

            // Table Name
            l = new CellLocation("F" + currentRow);
            v = new CellValue(table_name.getTextContent());
            worksheet.addCell(new Cell(l, v));

            // Column Name
            l = new CellLocation("G" + currentRow);
            v = new CellValue("n/a");
            worksheet.addCell(new Cell(l, v));

            if (USE_R12) {
                // Same as R12 (yes/no)
                String r12Table =
                    getR12TableDescription(table_name.getTextContent());
                l = new CellLocation("H" + currentRow);
                v = new CellValue(description_string.equals(r12Table) ? "Y" : "N");
                worksheet.addCell(new Cell(l, v));

                // R12 Description
                l = new CellLocation("I" + currentRow);
                v = new CellValue(r12Table);
                worksheet.addCell(new Cell(l, v));
            }

            // Description
            l = new CellLocation("J" + currentRow);
            v = new CellValue(description_string);
            worksheet.addCell(new Cell(l, v));

            // New Description

            currentRow++;

            Node col_list = XMLParserHelper.getChildNodeWithName(relational_table,
                                                                 "COL_LIST");
            NodeList col_list_items = col_list.getChildNodes();
            int length = col_list_items.getLength();
            for (int i = 0; i < length; i++) {
                Node col_list_item = col_list_items.item(i);
                if (col_list_item.getNodeName().equals("COL_LIST_ITEM")) {
                    Node col_name = XMLParserHelper.getChildNodeWithName(col_list_item,
                                                                         "NAME");
                    Node customer_area = XMLParserHelper.getChildNodeWithName(col_list_item,
                                                                              "CUSTOMER_AREA");
                    Node odb_property_list = XMLParserHelper.getChildNodeWithName(customer_area,
                                                                                  "ODB_PROPERTY_LIST");
                    Node comment = XMLParserHelper.getChildNodeWithName(odb_property_list,
                                                                        "COMMENT");

                    description_string = "";
                    if (comment != null) {
                        description_string = comment.getTextContent();
                    }

                    // Product
                    product = "";
                    m = Pattern.compile("fin/components/\\w+?/(\\w+?)/").matcher(f.getCanonicalPath());
                    if (m.find()) {
                        product = m.group(1).toUpperCase(Locale.ENGLISH);
                    }

                    l = new CellLocation("A" + currentRow);
                    v = new CellValue(product);
                    worksheet.addCell(new Cell(l, v));

                    // Issue
                    issue_string = "";
                    if (comment == null || comment.getTextContent().trim().isEmpty()) {
                        issue_string = "No description. - DM.COLUMN.005";
                        numColViolations++;
                    }
                    else if (description_string.equals(col_name.getTextContent())) {
                        issue_string = "Description matches name. - DM.WARNING.007";
                        numColNameMatchesDesc++;
                    }
                    else if (description_string.length() < 31) {
                        issue_string = "Short description. - DM.WARNING.006";
                        numShortColViolations++;
                    }
                    l = new CellLocation("B" + currentRow);
                    v = new CellValue(issue_string);
                    worksheet.addCell(new Cell(l, v));

                    // Spelling Warnings
                    spelling_warnings = getSpellingErrors(col_name.getTextContent(),
                                                          description_string);
                    l = new CellLocation("C" + currentRow);
                    v = new CellValue(spelling_warnings);
                    worksheet.addCell(new Cell(l, v));

                    // File
                    l = new CellLocation("D" + currentRow);
                    v = new CellValue(f.getCanonicalPath());
                    worksheet.addCell(new Cell(l, v));

                    // Object Type
                    l = new CellLocation("E" + currentRow);
                    v = new CellValue("COLUMN");
                    worksheet.addCell(new Cell(l, v));

                    // Table Name
                    l = new CellLocation("F" + currentRow);
                    v = new CellValue(table_name.getTextContent());
                    worksheet.addCell(new Cell(l, v));

                    // Column Name
                    l = new CellLocation("G" + currentRow);
                    v = new CellValue(col_name.getTextContent());
                    worksheet.addCell(new Cell(l, v));
                    
                    if (USE_R12) {
                        // Same as R12 (yes/no)
                        String r12Col =
                            getR12ColumnDescription(table_name.getTextContent(),
                                                    col_name.getTextContent());
                        l = new CellLocation("H" + currentRow);
                        v = new CellValue(description_string.equals(r12Col) ? "Y" : "N");
                        worksheet.addCell(new Cell(l, v));

                        // R12 Description
                        l = new CellLocation("I" + currentRow);
                        v = new CellValue(r12Col);
                        worksheet.addCell(new Cell(l, v));
                    }

                    // Description
                    l = new CellLocation("J" + currentRow);
                    v = new CellValue(description_string);
                    worksheet.addCell(new Cell(l, v));

                    // New Description

                    currentRow++;
                }
            }
        }
        catch (Exception e) {
            System.err.println("ERROR: Unexpected error:");
            e.printStackTrace();
            this.logException("Unexpected error.", e);
        }
    }

    public Workbook getWorkbook() {
        return workbook;
    }
    
    private static final Pattern NAME_REGEX = Pattern.compile("(?<=\\A|[;,\\.\\s])[A-Z_]+_[A-Z_]+(?=\\Z|[;,\\.\\s])");
    
    private String getSpellingErrors(String name,
                                     String description) throws IOException,
                                                                InterruptedException {
        Set<String> nameExceptions = new HashSet<String>();
        
        Matcher m = NAME_REGEX.matcher(description);
        
        while (m.find()) {
            nameExceptions.addAll(Arrays.asList(m.group().split("_")));
        }
        
        
        description = description.replaceAll(name, "").replaceAll(";", "").toLowerCase();
        

        
        ProcessBuilder pb1 = new ProcessBuilder("/bin/sh", "-c",
                                                "echo \"" + description + "\" | /usr/bin/spell");
        Process process1 = pb1.start();
        process1.waitFor();
        BufferedReader br = new BufferedReader(new InputStreamReader(process1.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            boolean isExempt = false;
            for (String s : exceptions) {
                if (s.contains(line) && description.contains(s)) {
                    isExempt = true;
                    break;
                }
            }
            
            if (nameExceptions.contains(line.toUpperCase())) {
                line += "*";
            }
            
            if (!isExempt) {
                sb.append(line).append(", ");
            }
            line = br.readLine();
        }
        if (sb.length() > 0) {
            sb.delete(sb.length() - 2, sb.length());
            return sb.toString();
        }
        else {
            return "";
        }
    }

    private String getR12ColumnDescription(String tableName,
                                           String columnName) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (tableIDCache.equals("NULL")) {
                PreparedStatement pstmt1 =
                    conn.prepareStatement("SELECT TABLE_ID FROM FND_TABLES WHERE TABLE_NAME = ?");
                pstmt1.setString(1, tableName);
                ResultSet rs1 = pstmt1.executeQuery();
                if (rs1.next()) {
                    tableIDCache = rs1.getString("TABLE_ID");
                } else {
                    rs1.close();
                    pstmt1.close();
                    return "";
                }
                rs1.close();
                pstmt1.close();
            }

            if (rowsCache.isEmpty()) {
                PreparedStatement pstmt2 =
                    conn.prepareStatement("SELECT COLUMN_NAME, DESCRIPTION FROM FND_COLUMNS WHERE TABLE_ID = " +
                                          tableIDCache);
                ResultSet rs2 = pstmt2.executeQuery();
                while (rs2.next()) {
                    rowsCache.put(rs2.getString("COLUMN_NAME"),
                                  rs2.getString("DESCRIPTION"));
                }
                rs2.close();
                pstmt2.close();
            }
            String desc = rowsCache.get(columnName);
            if (desc != null) {
                return desc;
            } else {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.close();

                } catch (SQLException f) {
                    this.logException("DB Connection error.", f);
                }
            }
            System.exit(1);
        }
        
        return "";
    }

    private String getR12TableDescription(String tableName) {
        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement("SELECT DESCRIPTION FROM FND_TABLES WHERE TABLE_NAME = ?");
            pstmt.setString(1, tableName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String ret = rs.getString("DESCRIPTION");
                rs.close();
                pstmt.close();
                return ret;
            }
            else {
              rs.close();
              pstmt.close();
                return "";
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (SQLException f) {
                    this.logException("DB Connection error.", f);
                }
            }
            System.exit(1);
        }
        
        return "";
    }

    private synchronized Connection getConnection() throws Exception {
        //allocate a connection for this UserDAO
        //Load the JDBC driver
        if (conn == null) {
            DriverManager.registerDriver(new OracleDriver());
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        }

        return conn;
    }
    
    

    protected String getSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================");
        sb.append("\n Number of tables without descriptions:                       " + numTabViolations);
        sb.append("\n Number of tables whose description is the same as its name:  " + numTabNameMatchesDesc);
        sb.append("\n Number of tables whose description is < 31 characters:       " + numShortTabViolations);
        sb.append("\n Number of columns without descriptions:                      " + numColViolations);
        sb.append("\n Number of columns whose description is the same as its name: " + numColNameMatchesDesc);
        sb.append("\n Number of columns whose description is < 31 characters:      " + numShortColViolations);
        sb.append("\n============================================================\n");

        return sb.toString();
    }

    protected String getSummaryReportSubject() {
        return "SCRIPT: Data Model Scanner";
    }

    @Override
    protected void logUsageReport(String sCrawlDir) {
        String s = "";

        s += "user.dir set to  --->  " + System.getProperty("user.dir") + "\n";
        s += "Crawl Dir set to --->  " + sCrawlDir + "\n";

        s += getSummaryReport();

        System.out.println(s);
        System.out.println("Report file can be found here: " + sCrawlDir + "/report.xlsx\n");

        s += "\n\n**EXCEPTION REPORT ************************************************\n";
        if (m_numExceptions == 0) {
            s += "No Exceptions to report.\n";
        }
        else {
            s += "A total of " + m_numExceptions + " exception(s) were collected.\n\n";
            s += sExceptions;
        }
        //Mail.sendMail("angel.irizarry@oracle.com", s, getSummaryReportSubject());
    }

    public static void main(String[] argv) {
        int argc = argv.length;
        if (argc != 1 && argc != 4) {
            System.out.println("USAGE: dmscanner.sh [<JDBC Connection String> <username> <password>]");
            System.exit(1);
        }

        if (argc == 4) {
            if (argv[1] == null || argv[1].isEmpty() || argv[2] == null ||
                argv[2].isEmpty() || argv[3] == null || argv[3].isEmpty()) {
                System.out.println("USAGE: dmscanner.sh [<JDBC Connection String> <username> <password>]");
                System.exit(1);
            }
            USE_R12 = true;
            URL = argv[1];
            USERNAME = argv[2];
            PASSWORD = argv[3];
        }

        String crawlDirectory = argv[0];

        DMScanner x = new DMScanner();
        x.crawlDirectory(crawlDirectory, false);

        try {
            if (x.conn != null) {
                x.conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(crawlDirectory + "/" + "report.xlsx");
            XLSXCreator.create(x.getWorkbook(), out);
        }
        catch (Exception e) {
            System.err.println("ERROR: Unable to create xlsx. Is this a read-only file-system?");
            x.logException("error in creating xlsx.", e);
        }

        x.logUsageReport(crawlDirectory);
    }


}
