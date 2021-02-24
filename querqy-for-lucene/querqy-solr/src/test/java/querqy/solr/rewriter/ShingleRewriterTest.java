package querqy.solr.rewriter;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;
import static querqy.solr.StandaloneSolrTestSupport.withRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.hamcrest.collection.IsMapContaining;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShingleRewriterTest extends SolrTestCaseJ4 {

    private final static String REWRITERS = "common_rules_before_shingles,shingles,common_rules_after_shingles";

     @BeforeClass
     public static void beforeTests() throws Exception {
         initCore("solrconfig.xml", "schema.xml");
         withCommonRulesRewriter(h.getCore(), "common_rules_before_shingles",
                 "configs/commonrules/rules-before-shingles.txt");
         final Map<String, Object> config = new HashMap<>();
         config.put("acceptGeneratedTerms", false);
         withRewriter(h.getCore(), "shingles", ShingleRewriterFactory.class, config);
         withCommonRulesRewriter(h.getCore(), "common_rules_after_shingles",
                 "configs/commonrules/rules-shingles.txt");
     }

    @Test
    public void testShinglesOnThreeTerms() {
        String q = "a b c";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITERS
              );

        assertQ("Missing shingles",
              req,
              "//str[@name='parsedquery'][contains(.,'ab')]",
              "//str[@name='parsedquery'][contains(.,'bc')]"

        );

        req.close();    
    }
    
    @Test
    public void testShinglesAsInputOnCommonRules() {
        String q = "a b c";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITERS
              );

        assertQ("Missing shingles",
              req,
              "//str[@name='parsedquery'][contains(.,'shingleab')]",
              "//str[@name='parsedquery'][contains(.,'synonymforc')]"

        );

        req.close();    
    }
    
    @Test
    public void testThatShinglesAreNotCreatedOnGeneratedTerms() {
        String q = "t1 t2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITERS
              );

        assertQ("Problem with shingles on generated terms",
              req,
              "//str[@name='parsedquery'][contains(.,'t1t2')]",
              "//str[@name='parsedquery'][not(contains(.,'s1t2'))]"

        );

        req.close();    
    }

    @Test
    public void testConfigRequestAcceptGeneratedTerms() {
        final Map<String, Object> config = new ShingleRewriterFactory.ShingleConfigRequestBuilder()
                .acceptGeneratedTerms(true).buildConfig();

        org.hamcrest.MatcherAssert.assertThat(config, IsMapContaining.hasEntry(
                ShingleRewriterFactory.CONF_ACCEPT_GENERATED_TERMS, Boolean.TRUE));

        final ShingleRewriterFactory factory = new ShingleRewriterFactory("id");

        final List<String> errors = factory.validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());

        // TODO: read the acceptGeneratedTerms property once it becomes accessible

        // make sure that no exception  is thrown
        factory.configure(config);

    }

    @Test
    public void testConfigRequestDoNotAcceptGeneratedTerms() {
        final Map<String, Object> config = new ShingleRewriterFactory.ShingleConfigRequestBuilder()
                .acceptGeneratedTerms(false).buildConfig();

        org.hamcrest.MatcherAssert.assertThat(config, IsMapContaining.hasEntry(
                ShingleRewriterFactory.CONF_ACCEPT_GENERATED_TERMS, Boolean.FALSE));

        final ShingleRewriterFactory factory = new ShingleRewriterFactory("id");

        final List<String> errors = factory.validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());

        // TODO: read the acceptGeneratedTerms property once it becomes accessible

        // make sure that no exception  is thrown
        factory.configure(config);

    }

    @Test
    public void testConfigRequestDefaultAcceptGeneratedTerms() {
        final Map<String, Object> config = new ShingleRewriterFactory.ShingleConfigRequestBuilder().buildConfig();

        assertTrue(config.isEmpty());

        final ShingleRewriterFactory factory = new ShingleRewriterFactory("id");

        final List<String> errors = factory.validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());

        // TODO: read the acceptGeneratedTerms property once it becomes accessible

        // make sure that no exception  is thrown
        factory.configure(config);

    }

}
