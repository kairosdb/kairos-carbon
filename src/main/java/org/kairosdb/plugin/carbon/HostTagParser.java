package org.kairosdb.plugin.carbon;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 Created with IntelliJ IDEA.
 User: bhawkins
 Date: 10/1/13
 Time: 3:31 PM
 To change this template use File | Settings | File Templates.
 */
public class HostTagParser implements TagParser
{
	public static final String HOST_PATTERN_PROP = "kairosdb.carbon.hosttagparser.host_pattern";
	public static final String HOST_REPLACEMENT_PROP = "kairosdb.carbon.hosttagparser.host_replacement";
	public static final String METRIC_PATTERN_PROP = "kairosdb.carbon.hosttagparser.metric_pattern";
	public static final String METRIC_REPLACEMENT_PROP = "kairosdb.carbon.hosttagparser.metric_replacement";

	private Pattern m_hostPattern;
	private String m_hostReplacement;

	private Pattern m_metricPattern;
	private String m_metricReplacement;

	@Inject
	public HostTagParser(
		@Named(HOST_PATTERN_PROP)String hostPattern,
		@Named(HOST_REPLACEMENT_PROP)String hostReplacement,
		@Named(METRIC_PATTERN_PROP)String metricPattern,
		@Named(METRIC_REPLACEMENT_PROP)String metricReplacement
	)
	{
		m_hostPattern = Pattern.compile(hostPattern);
		m_hostReplacement = hostReplacement;
		m_metricPattern = Pattern.compile(metricPattern);
		m_metricReplacement = metricReplacement;
	}

	@Override
	public CarbonMetric parseMetricName(String metricName) {

		String metric;
		String[] splited = metricName.split("\\.");
		int prefixLength = 5;
		int metricLength = splited.length;

		Matcher metricMatcher = m_metricPattern.matcher(metricName);

		if (metricMatcher.matches()) {
			metric = metricMatcher.replaceAll(m_metricReplacement);
		} else {
			if (metricLength > prefixLength) {
				String tmpMetricName = "";
				for (int i = prefixLength; i < metricLength; i++) {
					tmpMetricName += splited[i];
					if (i < metricLength - 1)
						tmpMetricName += ".";
				}
				metric = tmpMetricName;
			} else {
				metric = splited[metricLength - 1];
			}
		}

		CarbonMetric ret = new CarbonMetric(metric);

		ret.addTag("type", "graphite");

		Matcher hostMatcher = m_hostPattern.matcher(metricName);

		if (hostMatcher.matches())
			ret.addTag("host", hostMatcher.replaceAll(m_hostReplacement));

		for ( int i = 0; i < prefixLength && i < metricLength; i++ ) {
			ret.addTag("tag" + i, splited[i]);
		}

		return ret;

	}
}
