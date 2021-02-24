package querqy.solr.it;

import java.io.IOException;
import java.util.Random;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.testcontainers.utility.DockerImageName;
import querqy.solr.QuerqyQParserPlugin;

@RunWith(Parameterized.class)
@Category(IntegrationTest.class)
public class SolrQuerqyIntegrationTest {

    @Parameters(name = "{0}")
    public static Iterable<? extends Object> solrVersionsToTest() {
        return QuerqySolrContainer.getSolrTestVersions();
    }

    /**
     * The Querqy Solr instance gets constructed per test method. Due
     * to the link {@link Parameterized} runner that extracts the Solr
     * version to run in, we cannot use a {@link ClassRule}.
     */
    @Rule
    public QuerqySolrContainer solr;

    // random number of shards
    private final int numShards = new Random().nextInt(3) + 1;

    public SolrQuerqyIntegrationTest(DockerImageName solrImage) {
        this.solr = new QuerqySolrContainer(solrImage, numShards);
        
        System.out.printf("Testing Querqy in %s with %s shards%n", solr.getDockerImageName(), numShards);
    }

    @Test
    public void shouldApplyQuerqyRulesForLaptop() throws SolrServerException, IOException {
        SolrServer client = solr.newSolrClient();

        // container up and running
        // TODO: Figure out where to use  QuerqySolrContainer.QUERQY_IT_COLLECTION_NAME
        SolrPingResponse response = new SolrPing().process(client);
        MatcherAssert.assertThat(response.getStatus(), Matchers.is(0));

        // querqy rules get applied
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(CommonParams.Q, "laptop");
        params.set("defType", "querqy");
        params.set(QuerqyQParserPlugin.PARAM_REWRITERS, "replace,word_break,common_rules");
        params.set(DisMaxParams.QF, "name title product_type short_description ean search_attributes");

        QueryResponse query = client.query(params, SolrRequest.METHOD.POST);
        MatcherAssert.assertThat(query.getResults().getNumFound(), Matchers.is(42L));
    }
}
