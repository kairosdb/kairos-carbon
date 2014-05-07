package org.kairosdb.plugin.carbon;

/**
 Created with IntelliJ IDEA.
 User: bhawkins
 Date: 10/1/13
 Time: 1:27 PM
 To change this template use File | Settings | File Templates.
 */
public interface TagParser
{
	/**
	 Parse the incoming metric name and return a DataPointSet to represent the
	 data.  The DataPointSet needs to contain the metric name and at least
	 one tag.  If a null is returned the metric is not submitted to the datastore
	 @return CarbonMetric containing metric name and at least one tag or null.
	 */
	public CarbonMetric parseMetricName(String metricName);
}
