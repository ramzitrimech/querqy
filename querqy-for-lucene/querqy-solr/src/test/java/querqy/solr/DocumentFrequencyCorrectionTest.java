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

public class DocumentFrequencyCorrectionTest extends SolrTestCaseJ4 {

    public void index() throws Exception {

      assertU(adoc("id", "1", "f1", "a"));

      assertU(commit());
      assertU(adoc("id", "2", "f1", "a", "f2", "b"));

      assertU(adoc("id", "3", "f1", "a", "f2", "c"));

      assertU(adoc("id", "4", "f1", "a", "f2", "k"));
      assertU(commit());
      assertU(adoc("id", "5", "f1", "a", "f2", "k"));
      assertU(adoc("id", "6", "f1", "a", "f2", "k"));
      assertU(adoc("id", "7", "f1", "a", "f2", "k"));

      assertU(commit());
   }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common_rules", "configs/commonrules/rules-dfc.txt");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testDfGetsCorrectedForBoostUp() throws Exception {

        String q = "a c";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("wrong df",
                req,
                "//str[@name='2'][contains(.,'7 = n, number of documents containing term')]",
                "//str[@name='2'][not(contains(.,'1 = n, number of documents containing term'))]",
                "//str[@name='7'][contains(.,'10 = n, number of documents containing term')]",
                "//str[@name='7'][not(contains(.,'4 = n, number of documents containing term'))]");

        req.close();
    }

}
