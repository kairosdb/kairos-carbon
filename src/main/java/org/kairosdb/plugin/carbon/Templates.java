package org.kairosdb.plugin.carbon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Templates
{
  public static final Logger logger = LoggerFactory.getLogger(Templates.class);
	private static List<Template> templates = new ArrayList<Template>();

	public static void parse(String templates) {
		List<String> list = Arrays.asList(templates.split(";"));
		logger.info("Parsing {} template strings", list.size());
		int count = 0;
		for (String element : list) {
			count++;
			logger.debug("({}) Parsing template string: {}", count, element);
			try {
				Template template = new Template(element);
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
				add(template);
			} catch (IllegalArgumentException e) {
				String msg = "({}) Invalid template string ({}): {}";
				logger.warn(msg, count, e.getMessage(), element);
			}
		}
		logger.info("Built {} templates", getList().size());
	}

	public static void add(Template template) {
		templates.add(template);
	}

	public static List<Template> getList() { return templates; }

	public static Template lookup(String metricName) {
		for (Template template : templates) {
			if (template.matches(metricName)) {
				return template;
			}
		}
		return null;
	}
}
