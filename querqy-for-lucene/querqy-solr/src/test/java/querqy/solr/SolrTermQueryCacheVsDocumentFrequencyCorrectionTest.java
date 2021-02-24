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

public class SolrTermQueryCacheVsDocumentFrequencyCorrectionTest extends SolrTestCaseJ4 {

    public void index() throws Exception {

        assertU(adoc("id", "1", "f1", "a"));

        assertU(adoc("id", "2", "f1", "b", "f2", "c"));

        assertU(commit());
     }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-cache.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common_rules", "configs/commonrules/rules-cache-dfc.txt");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }
         

    @Test
    public void testCacheVsDocumentFrequencyCorrection() {
        // The rules contain an UP rule that never matches. Make
        // sure this doesn't cause an Exception in the DocumentFrequencyCorrection
        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
              );
        assertQ("Two results expected",
                req,
                "//result[@name='response'][@numFound='2']");

        req.close();
    }

}
