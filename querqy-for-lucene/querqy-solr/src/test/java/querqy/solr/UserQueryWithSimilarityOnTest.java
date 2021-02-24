package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class UserQueryWithSimilarityOnTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "a"));
        assertU(adoc("id", "2", "f1", "a"));
        assertU(adoc("id", "5", "f1", "a"));
        assertU(adoc("id", "6", "f2", "y"));
        assertU(adoc("id", "3", "f2", "a"));
        assertU(adoc("id", "4", "f3", "k a"));


        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testThatDfAndDfAreUsedForRanking() {
        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.0",
                "defType", "querqy",
                "uq.similarityScore", "on",
                "debugQuery", "true"
        );
        assertQ("Ranking",
                req,
                "//doc[1]/str[@name='id'][contains(.,'3')]",
                "//doc[2]/str[@name='id'][contains(.,'4')]",
                "//lst[@name='explain']/str[@name='3'][contains(.,'idf, computed as')]"
        );

        req.close();
    }
}
