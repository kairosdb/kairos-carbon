package org.kairosdb.plugin.carbon;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by vkrish2 on 8/12/16.
 */
public class MultiTagParser implements TagParser {
    private static final String MULTI_PATTERN_PROP = "kairosdb.carbon.multitagparser.patterns";
    private static final String MULTI_REPLACEMENT_PROP = "kairosdb.carbon.multitagparser.replacements";
    private static final String MULTI_TAGS = "kairosdb.carbon.multitagparser.tags";

    private static final String METRIC_PATTERN_PROP = "kairosdb.carbon.multitagparser.metric_pattern";
    private static final String METRIC_REPLACEMENT_PROP = "kairosdb.carbon.multitagparser.metric_replacement";

    private static final String TAGS_CASE = "kairosdb.carbon.multitagparser.tagscase";

    private static final String METRIC_TTL = "kairosdb.carbon.ttl";
    private static final String METRIC_INVALID_TTL = "kairosdb.carbon.invalidTtl";


    private List<Pattern> m_pattern_list = new ArrayList<Pattern>();
    private List<String> m_replacement_list = new ArrayList<String>();
    private List<String> m_tags_list = new ArrayList<String>();

    private Pattern m_metricPattern;
    private String m_metricReplacement;

    private int m_tags_case = 0;
    private int invalidTtl = 0;
    private int ttl = 0;



    @Inject
    public MultiTagParser(
            @Named(MULTI_PATTERN_PROP)String multiPatterns,
            @Named(MULTI_REPLACEMENT_PROP)String multiReplacements,
            @Named(MULTI_TAGS)String multiTags,
            @Named(TAGS_CASE)String tagsCase,
            @Named(METRIC_PATTERN_PROP)String metricPattern,
            @Named(METRIC_REPLACEMENT_PROP)String metricReplacement,
            @Named(METRIC_INVALID_TTL)int invalidTtlValue,
            @Named(METRIC_TTL)int ttlValue)
    {


        String[] m_hostPatterns = multiPatterns.split(";");

        for(String m_hostPatternStr : m_hostPatterns) {
            m_pattern_list.add(Pattern.compile(m_hostPatternStr.trim()));
        }

        m_replacement_list = Arrays.asList(multiReplacements.split(";"));
        m_tags_list = Arrays.asList(multiTags.split(";"));

        m_metricPattern = Pattern.compile(metricPattern);
        m_metricReplacement = metricReplacement;

        invalidTtl = invalidTtlValue;
        ttl = ttlValue;


        if (tagsCase.equalsIgnoreCase("upper")){
            m_tags_case = 1;
        }
        else if (tagsCase.equalsIgnoreCase("lower")){
            m_tags_case = 2;
        }
    }

    @Override
    public CarbonMetric parseMetricName(String metricName)
    {

        switch(m_tags_case){
            case 1:
                metricName = metricName.toUpperCase();
                break;
            case 2:
                metricName = metricName.toLowerCase();
                break;
            default:
                break;
        }

        Matcher metricMatcher = m_metricPattern.matcher(metricName);
        CarbonMetric ret;

        if(!metricMatcher.matches()){
            ret = new CarbonMetric("invalidMetrics");
            ret.addTag("metricName", metricName);
            ret.setTtl(invalidTtl);
            return (ret);
        }

        Matcher hostMatcher;
        ret = new CarbonMetric(metricMatcher.replaceAll(m_metricReplacement));

        Iterator<Pattern> patternIterator = m_pattern_list.iterator();
        Iterator<String> replacementIterator = m_replacement_list.iterator();
        Iterator<String> tagsIterator = m_tags_list.iterator();

        while(patternIterator.hasNext() && replacementIterator.hasNext() && tagsIterator.hasNext()){
            hostMatcher = patternIterator.next().matcher(metricName);
            if (!hostMatcher.matches()){
                ret = new CarbonMetric("invalidMetrics");
                ret.addTag("metricName", metricName);
                ret.setTtl(invalidTtl);
                return (ret);
            }
            ret.addTag(tagsIterator.next().trim(), hostMatcher.replaceAll(replacementIterator.next().trim()));
        }

        ret.setTtl(ttl);
        return (ret);
    }
}
