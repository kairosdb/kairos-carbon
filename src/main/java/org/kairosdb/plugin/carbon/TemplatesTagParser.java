package org.kairosdb.plugin.carbon;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.kairosdb.core.KairosRootConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class TemplatesTagParser implements TagParser
{
	public static final String TEMPLATES_LIST_PROP = "kairosdb.carbon.templates_tag_parser.templates";
	public static final Logger logger = LoggerFactory.getLogger(TemplatesTagParser.class);

	private static List<Template> m_templates = new ArrayList<Template>();

	//Used for testing
	public TemplatesTagParser(List<String> templates)
	{
		parse(templates);
	}

	@Inject
	public TemplatesTagParser(
		KairosRootConfig config)
	{
		this(config.getStringList(TEMPLATES_LIST_PROP));
	}

	@Override
	public CarbonMetric parseMetricName(String metricName)
	{
		CarbonMetric ret;
		Template template = lookup(metricName);

		if (template == null) {
			ret = invalidMetric(metricName, "no matching template");
		} else {
			if (!template.has_template()) { return null; }

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

	public void parse(List<String> templates) {
		logger.info("Parsing {} template strings", templates.size());
		int count = 0;
		for (String element : templates) {
			count++;
			logger.debug("({}) Parsing template string: {}", count, element);
			try {
				Template template = new Template(element);
				if (template.has_template()) {
					logger.debug(
							"({}) Built template:\n" +
									"	Separators: Source='{}' Target='{}'\n" +
									"	Metric name pattern: {}\n" +
									"	Tags pattern: {}\n" +
									"	Tag names: {}\n" +
									"	Static tags: {}",
							count,
							template.getSourceSeparator(),
							template.getTargetSeparator(),
							template.getMetricNamePattern(),
							template.getTagsPattern(),
							template.getTagNames(),
							template.getStaticTags()
					);
				} else {
					logger.debug("({}) Built DROP template", count);
				}
				m_templates.add(template);
			} catch (IllegalArgumentException e) {
				String msg = "({}) Invalid template string ({}): {}";
				logger.warn(msg, count, e.getMessage(), element);
			}
		}
		logger.info("Built {} templates", m_templates.size());
	}

	private Template lookup(String metricName) {
		for (Template template : m_templates) {
			if (template.matches(metricName)) {
				return template;
			}
		}
		return null;
	}
}
