package org.kairosdb.plugin.carbon;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Map;
import java.util.Arrays;

public class TemplatesTagParser implements TagParser
{
	@Inject
	public TemplatesTagParser(
		@Named("kairosdb.carbon.templatestagparser.templates")String templates
	)
	{
		Templates.parse(templates);
	}

	@Override
	public CarbonMetric parseMetricName(String metricName)
	{
		Template m_template = Templates.lookup(metricName);
		if (m_template != null) {
			CarbonMetric ret = new CarbonMetric(m_template.buildMetricName(metricName));
			m_template.addTags(ret, metricName);
			return ret;
		} else {
			return null;
		}
	}
}
