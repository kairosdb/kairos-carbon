kairos-carbon
=============
[![Build Status](https://travis-ci.org/kairosdb/kairos-carbon.svg?branch=master)](https://travis-ci.org/kairosdb/kairos-carbon)

KairosDB plugin for the carbon protocol

The Kairos-Carbon plugin starts up two listeners, one for text and the other for pickle protocol.

To install this plugin first build the jar file using the instructions
in the how_to_build.txt file.  Then copy the jar and karios-carbon.conf
files into /opt/kairosdb/lib and /opt/kairosdb/conf directories respectively.

The plugin comes with 3 parsers for translating metrics

##GraphiteTagsParser
```tag_parser: "org.kairosdb.plugin.carbon.GraphiteTagsParser"```

The GraphiteTagsParser reads the new protocol from graphite that has multiple tags as defined 
[here](http://graphite.readthedocs.io/en/latest/tags.html) 

##HostTagParser
```tag_parser: "org.kairosdb.plugin.carbon.HostTagParser"```

The HostTagParser uses a simple regex that extracts host information from the 
metric.

```
    host_tag_parser: {
      host_pattern: "[^.]*\\.([^.]*)\\..*"
      host_replacement: "$1"
      metric_pattern: "([^.]*)\\.[^.]*\\.(.*)"
      metric_replacement: "$1.$2"
    }
```

If my metrics came in looking like this ```foo.shasta.bar``` the host name "shasta" 
would be extracted as a host tag and the metric would be reported as "foo.bar"

##TemplatesTagParser
```tag_parser: "org.kairosdb.plugin.carbon.TemplatesTagParser"```

A template consists of 4 parts each separated by a space.
1. Regex to match the template to an incoming metric
2. Template to identify parts of the metric
3. Optional - Separator to use and or replace with
4. Optional - Additional tags to add to metric

Lets look at an example ```^test.foo .metric.host.metric*```

The first part ```^test.foo``` is what is used to identify what metrics are to be
used with this template.  Anything that starts with "test.foo".  The second part
labels whether each part is to be used as a metric name or as a tag.

If I got a metric ```test.foo.skinny.pop.bar``` the above template would pull
```skinny``` out as the host tag and then use ```foo``` and ```pop.bar``` as the 
metric name ```foo.pop.bar```

The optional third part looks like this ```[.]``` or ```[.,_]```.  If there is a single
character between [] then it is used to identify the metric delimiter.  If there are two 
characters separated by a comma (,) then the second is a replacement.  In the above example
if I added ```[.,_]``` to my template the resulting metric would be ```foo_pop_bar```.

The fourth optional part of the template is a list of tags in the form ```key=value``` and you can 
have as many as you want separated by spaces.