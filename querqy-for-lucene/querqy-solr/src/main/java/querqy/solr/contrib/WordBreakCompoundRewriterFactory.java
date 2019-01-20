package querqy.solr.contrib;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.index.IndexReader;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrRequestInfo;
import querqy.rewrite.RewriterFactory;
import querqy.solr.RewriterFactoryAdapter;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class WordBreakCompoundRewriterFactory implements RewriterFactoryAdapter {

    private static final int DEFAULT_MIN_SUGGESTION_FREQ = 1;
    private static final int DEFAULT_MAX_COMBINE_LENGTH = 30;
    private static final int DEFAULT_MIN_BREAK_LENGTH = 3;

    @Override
    public RewriterFactory createRewriterFactory(NamedList<?> args, ResourceLoader resourceLoader) throws IOException {
        // the minimum frequency of the term in the index' dictionary field to be considered a valid compound or constituent
        Integer minSuggestionFreq = getOrDefault(args, "minSuggestionFrequency", DEFAULT_MIN_SUGGESTION_FREQ);

        // the maximum length of a combined term
        Integer maxCombineLength = getOrDefault(args, "maxCombineWordLength", DEFAULT_MAX_COMBINE_LENGTH);

        // the minimum break term length
        Integer minBreakLength = getOrDefault(args, "minBreakLength", DEFAULT_MIN_BREAK_LENGTH);

        // the index "dictionary" field to verify compounds / constituents
        String indexField = (String) args.get("dictionaryField");

        // terms triggering a reversal of the surrounding compound, e.g. "tasche AUS samt" -> samttasche
        List<String> reverseCompoundTriggerWords = (List<String>) args.get("reverseCompoundTriggerWords");

        // define whether we should always try to add a reverse compound
        boolean alwaysAddReverseCompounds = getOrDefault(args, "alwaysAddReverseCompounds", Boolean.FALSE);

        // the indexReader has to be supplied on a per-request basis from a request thread-local
        Supplier<IndexReader> indexReaderSupplier = () ->
                SolrRequestInfo.getRequestInfo().getReq().getSearcher().getIndexReader();

        // FIXME: add trigger words
        return new querqy.lucene.contrib.rewrite.WordBreakCompoundRewriterFactory(indexReaderSupplier, indexField,
                minSuggestionFreq, maxCombineLength, minBreakLength, reverseCompoundTriggerWords,
                alwaysAddReverseCompounds);
    }

    private static <T> T getOrDefault(NamedList<?> args, String key, T def) {
        Object valueInParameter = args.get(key);
        return valueInParameter == null ? def : (T) valueInParameter;
    }
}
