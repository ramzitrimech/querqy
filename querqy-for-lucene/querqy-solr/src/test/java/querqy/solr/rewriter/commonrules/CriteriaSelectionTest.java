package querqy.solr.rewriter.commonrules;


import static querqy.rewrite.commonrules.select.RuleSelectionParams.getFilterParamName;
import static querqy.rewrite.commonrules.select.RuleSelectionParams.getIsUseLevelsForLimitParamName;
import static querqy.rewrite.commonrules.select.RuleSelectionParams.getLimitParamName;
import static querqy.rewrite.commonrules.select.RuleSelectionParams.getSortParamName;
import static querqy.rewrite.commonrules.select.RuleSelectionParams.getStrategyParamName;
import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;
import querqy.solr.FactoryAdapter;

import java.util.HashMap;
import java.util.Map;

public class CriteriaSelectionTest extends SolrTestCaseJ4 {

    private static final String REWRITER_ID_1 = "rules1";
    private static final String REWRITER_ID_2 = "rules2";
    private static final String REWRITER_ID_3 = "rules3";
    private static final String REWRITERS = String.join(",", REWRITER_ID_1,REWRITER_ID_2,REWRITER_ID_3);

    public void index() {

        assertU(adoc("id", "1", "f1", "syn1"));
        assertU(adoc("id", "2", "f1", "syn2"));
        assertU(adoc("id", "3", "f1", "syn3"));
        assertU(adoc("id", "4", "f1", "syn4"));

        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {

        initCore("solrconfig.xml", "schema.xml");

        final Map<String, Class<? extends FactoryAdapter<SelectionStrategyFactory>>> ruleSelectionStrategies12 =
                new HashMap<>();
        ruleSelectionStrategies12.put("criteria", PrimitiveValueSelectionStrategyFactory.class);
        withCommonRulesRewriter(h.getCore(), REWRITER_ID_1, "configs/commonrules/rules-criteria1.txt",
                ruleSelectionStrategies12);
        withCommonRulesRewriter(h.getCore(), REWRITER_ID_2, "configs/commonrules/rules-criteria2.txt",
                ruleSelectionStrategies12);

        final Map<String, Class<? extends FactoryAdapter<SelectionStrategyFactory>>> ruleSelectionStrategies3 =
                new HashMap<>();
        ruleSelectionStrategies3.put("criteria", ExpressionSelectionStrategyFactory.class);
        withCommonRulesRewriter(h.getCore(), REWRITER_ID_3, "configs/commonrules/rules-criteria3.txt",
                ruleSelectionStrategies3);


    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();

    }

    @Test
    public void testDefaultBehaviourAppliesAllRules() {
        // definition order, no constraints expected
        SolrQueryRequest req = req("q", "input1 input2 input3 input4",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("default SelectionStrategy doesn't work",
                req,
                "//result[@name='response' and @numFound='4']"
        );

        req.close();
    }

    @Test
    public void testThatExpressionStrategyIsTheDefaultSelectionStrategy() {
        SolrQueryRequest req = req("q", "input1 input2 input3 input4 input5 input6",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                // we set the criteria but don't enable the strategy
                getFilterParamName(REWRITER_ID_1), "$[?(@.priority == 2)]",
                getFilterParamName(REWRITER_ID_2), "$[?(@.group == 44)]",
                getFilterParamName(REWRITER_ID_3), "$[?(@._id == 'id6')]",

                getLimitParamName(REWRITER_ID_1), "1",
                getLimitParamName(REWRITER_ID_2), "1",
                getLimitParamName(REWRITER_ID_3), "1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("default SelectionStrategy=ExpressionStrategy doesn't work",
                req,
                "//result[@name='response' and @numFound='3']",
                "//result/doc/str[@name='id'][text()='1']",
                "//result/doc/str[@name='id'][text()='3']",
                "//result/doc/str[@name='id'][text()='4']"
        );

        req.close();
    }

    @Test
    public void testThatDefaultStrategyIsAppliedIfNoCriteriaParamIsSet() {
        SolrQueryRequest req = req("q", "input1 input2",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_1), "criteria",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("default SelectionStrategy doesn't work",
                req,
                "//result[@name='response' and @numFound='3']"
        );

        req.close();
    }

    @Test
    public void testFilterByProperty() {
        SolrQueryRequest req = req("q", "input1 input2",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_1), "criteria",
                getFilterParamName(REWRITER_ID_1), "group:1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Filter criterion doesn't work",
                req,
                "//result[@name='response' and @numFound='2']"
        );

        req.close();
    }

    @Test
    public void testSortingAndLimiting() {
        SolrQueryRequest req = req("q", "input1 input2",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_1), "criteria",
                getSortParamName(REWRITER_ID_1), "priority asc",
                getLimitParamName(REWRITER_ID_1), "1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("PropertySorting/limit not working",
                req,
                "//result[@name='response' and @numFound='1']",
                "//result/doc/str[@name='id'][text()='2']"
        );

        req.close();
    }

    @Test
    public void testSortingAndLimitingWithLevel() {
        SolrQueryRequest req = req("q", "input1 input2",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getSortParamName(REWRITER_ID_1), "priority asc",
                getLimitParamName(REWRITER_ID_1), "1",
                getIsUseLevelsForLimitParamName(REWRITER_ID_1), "true",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("PropertySorting/limit not working",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result/doc/str[@name='id'][text()='2']",
                "//result/doc/str[@name='id'][text()='3']"
        );

        req.close();
    }

    @Test
    public void testThatSelectionIsAppliedPerRewriter() {
        SolrQueryRequest req = req("q", "input4",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_1), "criteria",
                getStrategyParamName(REWRITER_ID_2), "criteria",
                getFilterParamName(REWRITER_ID_1), "group:4",
                getFilterParamName(REWRITER_ID_2), "group:44",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Rewriter selection not working",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result/doc/str[@name='id'][text()='3']",
                "//result/doc/str[@name='id'][text()='4']"
        );

        req.close();


        req = req("q", "input4",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_1), "criteria",
                getStrategyParamName(REWRITER_ID_2), "criteria",
                getFilterParamName(REWRITER_ID_2), "group:4", // Flipping the groups between rewriters
                getFilterParamName(REWRITER_ID_1), "group:44",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Rewriter selection not working",
                req,
                "//result[@name='response' and @numFound='0']"
        );

        req.close();
    }

    @Test
    public void testJsonFilterEquality() {
        SolrQueryRequest req = req("q", "input5 input6",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_3), "criteria",
                getFilterParamName(REWRITER_ID_3), "$[?(@.tenant)].tenant[?(@.enabled == true)]",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Json eq filter criterion doesn't work",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result/doc/str[@name='id'][text()='1']",
                "//result/doc/str[@name='id'][text()='2']"
        );

        req.close();
    }

    @Test
    public void testJsonList() {
        SolrQueryRequest req = req("q", "input5 input6",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_3), "criteria",
                getFilterParamName(REWRITER_ID_3), "$[?('a' IN @.tt)]",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Json eq filter criterion doesn't work",
                req,
                "//result[@name='response' and @numFound='1']"
        );

        req.close();
    }

    @Test
    public void testJsonFilterEqualityAndGreaterThan() {
        SolrQueryRequest req = req("q", "input5 input6",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_3), "criteria",
                getFilterParamName(REWRITER_ID_3), "$[?(@.tenant && @.priority > 5)].tenant[?(@.enabled == true)]",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Json eq and gt filter criterion doesn't work",
                req,
                "//result[@name='response' and @numFound='1']",
                "//result/doc/str[@name='id'][text()='1']"
        );

        req.close();
    }

}
