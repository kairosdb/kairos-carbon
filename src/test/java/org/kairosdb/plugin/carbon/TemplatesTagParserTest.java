/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kairosdb.plugin.carbon;

import com.google.common.collect.ImmutableSortedMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kairosdb.core.datapoints.DoubleDataPoint;
import org.kairosdb.core.datapoints.LongDataPoint;
import org.kairosdb.core.datastore.KairosDatastore;
import org.kairosdb.core.exception.DatastoreException;
import org.kairosdb.core.exception.KairosDBException;
import org.kairosdb.util.Tags;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class TemplatesTagParserTest
{
	private final static int CARBON_PORT = 2003;

	private KairosDatastore m_datastore;
	private CarbonTextServer m_server;
	private CarbonClient m_client;
	private String m_templates;

	@Before
	public void setupDatastore() throws KairosDBException, IOException
	{
		m_datastore = mock(KairosDatastore.class);
		m_templates =
			"^test.metric .metric.host.metric*;" +
			"^test2.metric .metric.host.metric* [.,_];" +
			"^test3.metric .metric.host.metric* type=static";

		TemplatesTagParser templatesTagParser = new TemplatesTagParser(m_templates);

		m_server = new CarbonTextServer(m_datastore, templatesTagParser, CARBON_PORT);
		m_server.start();

		m_client = new CarbonClient("127.0.0.1", CARBON_PORT);
	}

	@After
	public void shutdown() throws IOException
	{
		m_server.stop();
		m_client.close();
	}

	@Test
	public void test_validMetric() throws DatastoreException, InterruptedException
	{
		long now = System.currentTimeMillis() / 1000;

		m_client.sendText("test.metric.host_name.name", now, "1234");

		ImmutableSortedMap<String, String> tags = Tags.create()
				.put("host", "host_name")
				.build();

		verify(m_datastore, timeout(5000).times(1))
				.putDataPoint("metric.name", tags,
						new LongDataPoint(now * 1000, 1234));
	}

	@Test
	public void test_changeSeparator() throws DatastoreException, InterruptedException
	{
		long now = System.currentTimeMillis() / 1000;

		m_client.sendText("test2.metric.host_name.name", now, "1234");

		ImmutableSortedMap<String, String> tags = Tags.create()
				.put("host", "host_name")
				.build();

		verify(m_datastore, timeout(5000).times(1))
				.putDataPoint("metric_name", tags,
						new LongDataPoint(now * 1000, 1234));
	}

	@Test
	public void test_staticTag() throws DatastoreException, InterruptedException
	{
		long now = System.currentTimeMillis() / 1000;

		m_client.sendText("test3.metric.host_name.name", now, "1234");

		ImmutableSortedMap<String, String> tags = Tags.create()
				.put("host", "host_name")
				.put("type", "static")
				.build();

		verify(m_datastore, timeout(5000).times(1))
				.putDataPoint("metric.name", tags,
						new LongDataPoint(now * 1000, 1234));
	}

	@Test
	public void test_noMatchingTemplate() throws DatastoreException, InterruptedException
	{
		long now = System.currentTimeMillis() / 1000;

		m_client.sendText("metric.host_name.name", now, "1234");

		ImmutableSortedMap<String, String> tags = Tags.create()
				.put("cause", "no matching template")
				.put("metricName", "metric.host_name.name")
				.build();

		verify(m_datastore, timeout(5000).times(1))
				.putDataPoint("invalidMetrics", tags,
						new LongDataPoint(now * 1000, 1234));
	}

	@Test
	public void test_notMatchingNamePattern() throws DatastoreException, InterruptedException
	{
		long now = System.currentTimeMillis() / 1000;

		m_client.sendText("test.metric.host_name", now, "1234");

		ImmutableSortedMap<String, String> tags = Tags.create()
				.put("cause", "does not match metric name pattern")
				.put("metricName", "test.metric.host_name")
				.put("templateFilter", "^test.metric")
				.build();

		verify(m_datastore, timeout(5000).times(1))
				.putDataPoint("invalidMetrics", tags,
						new LongDataPoint(now * 1000, 1234));
	}
}
