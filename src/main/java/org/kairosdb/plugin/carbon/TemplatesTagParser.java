package org.kairosdb.plugin.carbon;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Map;
import java.util.Arrays;

public class TemplatesTagParser implements TagParser
{
	public static final String TEMPLATES_LIST_PROP = "kairosdb.carbon.templatestagparser.templates";

	private String m_templates;

	@Inject
	public TemplatesTagParser(
		@Named(TEMPLATES_LIST_PROP)String templates)
	{
		m_templates = templates;

		Templates.parse(m_templates);
	}

	@Override
	public CarbonMetric parseMetricName(String metricName)
	{
		CarbonMetric ret;
		Template template = Templates.lookup(metricName);

		if (template == null) {
			ret = invalidMetric(metricName, "no matching template");
		} else {
			String targetMetric = template.buildMetricName(metricName);
			if (targetMetric == null) {
				ret = invalidMetric(metricName, "does not match metric name pattern", template);
			} else {
				ret = template.addTags(new CarbonMetric(targetMetric), metricName);
				if (ret == null) {
					ret = invalidMetric(metricName, "does not match tags pattern", template);
				}
			}
		}

		return ret;
	}

	private CarbonMetric invalidMetric(String metricName, String cause)
	{
		CarbonMetric ret = new CarbonMetric("invalidMetrics");
		ret.addTag("metricName", metricName);
		ret.addTag("cause", cause);
		return ret;
	}

	private CarbonMetric invalidMetric(String metricName, String cause, Template template)
	{
		CarbonMetric ret = invalidMetric(metricName, cause);
		ret.addTag("templateFilter", template.getFilterSource());
		return ret;
	}
}
