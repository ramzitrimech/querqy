package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class QuerqyDismaxQParserPluginWithCommonRulesTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "a", "f2", "c"));

        assertU(adoc("id", "2", "f1", "a", "f2", "b", "f4", "d"));
        
        assertU(adoc("id", "3", "f1", "a", "f3", "c", "f4", "e"));
        
        assertU(adoc("id", "5", "f1", "m", "f2", "b", "f4", "d"));
        
        assertU(adoc("id", "6", "f1", "m", "f2", "c", "f4", "e"));
        
        assertU(adoc("id", "7", "f1", "p", "f2", "x"));

        assertU(adoc("id", "8", "f1", "k", "f2", "qneg", "f3", "qneg2"));

        assertU(adoc("id", "9", "f1", "nok", "f2", "qneg", "f3", "qneg2"));

        assertU(adoc("id", "10", "f1", "k", "f2", "qnegraw", "f3", "qnegraw2"));

        assertU(adoc("id", "11", "f1", "nok", "f2", "qnegraw", "f3", "qnegraw2"));

        assertU(commit());
    }



    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common_rules", "configs/commonrules/rules.txt");
        withCommonRulesRewriter(h.getCore(), "common_rules_empty", "configs/commonrules/rules-empty.txt");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testSolrFilterQuery() {
        
        String q = "a k";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules"
              );

        assertQ("Solr filter query fails",
              req,
              "//result[@name='response' and @numFound='1']"

        );

        req.close();
        
        
    }
    
    @Test
    public void testThatDownRuleIsApplied() {
        String q = "m b";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2 f4",
              DisMaxParams.MM, "1",
              QueryParsing.OP, "OR",
              "defType", "edismax",
              "debugQuery", "on"
              );

        assertQ("Down rule failed",
              req,
              "//result[@name='response' and @numFound='3']/doc[1]/str[@name='id'][text()='5']"
        );

        req.close();
        
        
        req = req("q", q,
                DisMaxParams.QF, "f1 f2 f4",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
                );

          assertQ("Down rule failed",
                req,
                "//result[@name='response' and @numFound='3']/doc[1]/str[@name='id'][text()='6']",
                "//lst[@name='explain']/str[@name='5'][not(contains(., '0.0 = (MATCH) max of'))]"
          );

          req.close();
          
          
    }
    
    
    @Test
    public void testDeleteIsAppliedInContext() {
        String q = "t1 t2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
              );

        assertQ("Delete in context fails",
              req,
              "//str[@name='parsedquery'][not(contains(.,'t2'))]"
        );

        req.close();
    }
    
    @Test
    public void testThatRuleMatchingIsCaseInsensitive() {
        String q = "T1 T2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
              );

        assertQ("Rule matching seems to be case sensitive",
              req,
              "//str[@name='parsedquery'][not(contains(.,'t2'))]",
              "//str[@name='parsedquery'][not(contains(.,'T2'))]"
        );

        req.close();
    }
    
    @Test
    public void testPrefixWithSynoynm() {
        String q = "Px";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "2",
                QueryParsing.OP, "AND",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
              );
        
        assertQ("Synonym for prefix fails",
                req,
                "//result[@name='response' and @numFound='1']"

          );

        

        req.close();
    }
    
    @Test
    public void testPrefixWithNoCharLeftForWildcard() {
        String q = "p";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "2",
                QueryParsing.OP, "AND",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
              );
        
        assertQ("Prefix with no char left for wildcard fails",
                req,
                "//result[@name='response' and @numFound='1']"

          );

        

        req.close();
    }
    
    @Test
    public void testThatSingleDecorationIsApplied() {
        
        String q = "a d1";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "2",
                "defType", "querqy",
                "echoParams", "all",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
              );
        
        assertQ("Single decoration fails",
                req,
                "//arr[@name='querqy_decorations'][count(str)=1]",
                "//arr[@name='querqy_decorations']/str[text()='deco 1']"

          );

        

        req.close();
        
    }
    
    @Test
    public void testThatMultipleDecorationsAreApplied() {
        
        String q = "a d2 d1 d1";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "2",
                "defType", "querqy",
                "echoParams", "all",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
              );
        
        assertQ("Multiple decorations fail",
                req,
                "//arr[@name='querqy_decorations'][count(str)=2]",
                "//arr[@name='querqy_decorations']/str[text()='deco 1']",
                "//arr[@name='querqy_decorations']/str[text()='deco 2']"

          );

        

        req.close();
        
    }

    @Test
    public void testThatSingleNamedDecorationIsApplied() {
        String q = "a d1key";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "2",
                "defType", "querqy",
                "indent", "on",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("Single named decoration fails",
                req,
                "//lst[@name='querqy_named_decorations'][count(arr)=1]",
                "//lst[@name='querqy_named_decorations']/arr[@name='key1']/str[text()='deco 1']"

        );
        req.close();
    }

    @Test
    public void testThatDecorationAndNamedDecorationAreApplied() {
        String q = "a d1 d1key";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "2",
                "defType", "querqy",
                "indent", "on",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("Decoration fails",
                req,
                "//arr[@name='querqy_decorations'][count(str)=1]",
                "//arr[@name='querqy_decorations']/str[text()='deco 1']",
                "//lst[@name='querqy_named_decorations'][count(arr)=1]",
                "//lst[@name='querqy_named_decorations']/arr[@name='key1']/str[text()='deco 1']"

        );
        req.close();
    }

    @Test
    public void testThatMultipleNamedDecorationsWithSameKeyAreApplied() {
        String q = "a d1key d2key d3key";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "2",
                "defType", "querqy",
                "indent", "on",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("Single named decoration fails",
                req,
                "//lst[@name='querqy_named_decorations'][count(arr)=2]",
                "//lst[@name='querqy_named_decorations']/arr[@name='key1'][count(str)=2]",
                "//lst[@name='querqy_named_decorations']/arr[@name='key1']/str[text()='deco 1']",
                "//lst[@name='querqy_named_decorations']/arr[@name='key1']/str[text()='deco 3']",
                "//lst[@name='querqy_named_decorations']/arr[@name='key2'][count(str)=1]",
                "//lst[@name='querqy_named_decorations']/arr[@name='key2']/str[text()='deco 2']"

        );
        req.close();
    }



    @Test
    public void testThatPurelyNegativeFilterIsApplied() {
        String q = "qneg";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "echoParams", "all",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("Purely negative filter fails",
                req,
                "//result[@name='response' and @numFound='1']/doc[1]/str[@name='id'][text()='9']"
        );



        req.close();
    }

    @Test
    public void testThatNegativeFilterIsApplied() {
        String q = "qneg2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "echoParams", "all",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("Negative filter fails",
                req,
                "//result[@name='response' and @numFound='1']/doc[1]/str[@name='id'][text()='9']"
        );



        req.close();
    }


    @Test
    public void testThatPurelyNegativeRawQueryFilterIsApplied() {
        String q = "qnegraw";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "echoParams", "all",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("Purely negative filter fails for raw query",
                req,
                "//result[@name='response' and @numFound='1']/doc[1]/str[@name='id'][text()='11']"
        );



        req.close();
    }

    @Test
    public void testThatNegativeRawQueryFilterIsApplied() {
        String q = "qnegraw2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "echoParams", "all",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("Purely negative filter fails for raw query",
                req,
                "//result[@name='response' and @numFound='1']/doc[1]/str[@name='id'][text()='11']"
        );



        req.close();
    }

    @Test
    public void testThatRewriterWithEmptyRulesCanBeApplied() {

        String q = "a k";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules_empty"
        );

        assertQ("Empty rules fails",
                req,
                "//result[@name='response' and @numFound='5']"

        );

        req.close();

    }

}
