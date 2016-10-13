package org.kairosdb.plugin.carbon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template
{
	private List<String> tagNames;
	private Pattern filter;
	private String filterSource;
	private Pattern metricNamePattern;
	private Pattern tagsPattern;
	private String sourceSeparator = ".";
	private String targetSeparator = ".";
	private String template;
	private Map<String, String> staticTags = new HashMap<String, String>();

	public Template(String string) {
		List<String> parts = Arrays.asList(string.split(" "));
		if (parts.size() >= 2) {
			this.filterSource = parts.get(0);
			this.filter = Pattern.compile(filterSource);
			this.template = parts.get(1);
			for(int i = 2; i < parts.size(); i++) {
				String part = parts.get(i);
				if (part.matches("^\\[\\S(?:,\\S)?\\]$")) {
					part = part.replaceAll("([\\[\\]])", "");
					List<String> separators = new LinkedList<String>(Arrays.asList(part.split(",")));
					if (separators.size() < 2) { separators.add(separators.get(0)); }
					this.sourceSeparator = separators.get(0);
					this.targetSeparator = separators.get(1);
				} else if (part.matches("^(?:\\w+=\\w+,?)*(?:\\w+=\\w+)$")) {
					List<String> tags = Arrays.asList(part.split(","));
					for (String tag : tags) {
						List<String> tagParts = Arrays.asList(tag.split("="));
						staticTags.put(tagParts.get(0), tagParts.get(1));
					}
				} else {
					throw new IllegalArgumentException("unknown parameter: " + part);
				}
			}
			buildTemplatePatterns();
		} else {
			throw new IllegalArgumentException("less than 2 space separated parts");
		}
	}

	public CarbonMetric addTags(CarbonMetric ret, String metric) {
		Matcher matcher = tagsPattern.matcher(metric);
		if (matcher.matches()) {
			for (int i = 1; i <= matcher.groupCount(); i++) {
				ret.addTag(tagNames.get(i - 1), matcher.group(i));
			}
			return addStaticTags(ret);
		} else {
			return null;
		}
	}

	public CarbonMetric addStaticTags(CarbonMetric ret) {
		for (Map.Entry<String, String> tag : staticTags.entrySet()) {
			ret.addTag(tag.getKey(), tag.getValue());
		}
		return ret;
	}

	public String buildMetricName(String metric) {
		List<String> matches = new ArrayList<String>();
		Matcher matcher = metricNamePattern.matcher(metric);
		if (matcher.matches()) {
			for(int i = 1; i <= matcher.groupCount(); i++) {
				matches.add(matcher.group(i));
			}
			String r_sourceSep = "\\" + sourceSeparator;
			String metricName = String.join(targetSeparator, matches);
			return metricName.replaceAll(r_sourceSep, targetSeparator);
		} else {
			return null;
		}
	}

	private void buildTemplatePatterns() {
		String r_sourceSep = "\\" + sourceSeparator;
		String r_skip = "[^" + sourceSeparator + "]*";
		String r_capture = "(" + r_skip + ")";
		List<String> templateParts = Arrays.asList(template.split(r_sourceSep));

		List<String> metricNamePatternParts = new ArrayList<String>();
		List<String> tagsPatternParts = new ArrayList<String>();
		List<String> tagNames = new ArrayList<String>();

		for (String templatePart : templateParts) {
			if ("metric".equals(templatePart)) {
				metricNamePatternParts.add(r_capture);
				tagsPatternParts.add(r_skip);
			} else if ("metric*".equals(templatePart)) {
				metricNamePatternParts.add("(.*)");
				tagsPatternParts.add(".*");
			} else {
				metricNamePatternParts.add(r_skip);
				if (templatePart.length() > 0) {
					tagsPatternParts.add(r_capture);
					tagNames.add(templatePart);
				} else {
					tagsPatternParts.add(r_skip);
				}
			}
		}

		this.tagNames = tagNames;

		this.metricNamePattern = Pattern.compile(
			"^" + String.join(r_sourceSep, metricNamePatternParts) + "$"
		);
		this.tagsPattern = Pattern.compile(
			"^" + String.join(r_sourceSep, tagsPatternParts) + "$"
		);
	}

	public Pattern getFilter() { return filter; }

	public String getFilterSource() { return filterSource; }

	public Pattern getMetricNamePattern() { return metricNamePattern; }

	public Pattern getTagsPattern() { return tagsPattern; }

	public String getSourceSeparator() { return sourceSeparator; }

	public Map<String,String> getStaticTags() { return staticTags; }

	public String getTargetSeparator() { return targetSeparator; }

	public List<String> getTagNames() { return tagNames; }

	public String getTemplate() { return template; }

	public boolean matches(String metricName) {
		Matcher matcher = filter.matcher(metricName);
		return matcher.lookingAt();
	}
}
