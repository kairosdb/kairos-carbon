package org.kairosdb.plugin.carbon;

import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * implement http://graphite.readthedocs.io/en/latest/tags.html
 */
public class GraphiteTagsParser implements TagParser {
  private static final Logger logger = LoggerFactory.getLogger(GraphiteTagsParser.class);

  @Override
  public CarbonMetric parseMetricName(String metricName) {
    List<String> nameTags = Splitter.on(";").limit(2).splitToList(metricName);
    if (nameTags.size() == 2) {
      CarbonMetric metric = new CarbonMetric(nameTags.get(0));
      Map<String,String> tags = Splitter.on(",").withKeyValueSeparator("=").split(nameTags.get(1));
      if(tags.size() == 0){
        logger.warn("metric must at least have one tag! foo.bar;host=bla.com {} - ignoring metric", metricName);
        return metric;
      }
      for(Map.Entry<String,String> kv : tags.entrySet()){
        metric.addTag(kv.getKey(),kv.getValue());
      }
      return metric;
    } else {
      logger.warn("unparseable metric {} - ignoring metric", metricName);
      return null;
    }
  }
}
