/*
 * Copyright 2013 Proofpoint Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import org.kairosdb.eventbus.FilterEventBus;
import org.kairosdb.eventbus.Publisher;
import org.kairosdb.events.DataPointEvent;
import org.kairosdb.util.Tags;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 Created with IntelliJ IDEA.
 User: bhawkins
 Date: 10/4/13
 Time: 1:52 PM
 To change this template use File | Settings | File Templates.
 */
public class CarbonTextServerTest
{
	private final static int CARBON_PORT = 2003;
	private final int ZERO_TTL = 0;

	private Publisher<DataPointEvent> m_publisher;
	private CarbonTextServer m_server;
	private CarbonClient m_client;

	@Before
	public void setupDatastore() throws KairosDBException, IOException
	{
		m_publisher = (Publisher<DataPointEvent>) mock(Publisher.class);
		FilterEventBus eventBus = mock(FilterEventBus.class);
		when(eventBus.createPublisher(DataPointEvent.class)).thenReturn(m_publisher);
		HostTagParser hostTagParser = new HostTagParser(
				"[^.]*\\.([^.]*)\\..*",
				"$1",
				"([^.]*)\\.[^.]*\\.(.*)",
				"$1.$2");

		m_server = new CarbonTextServer(eventBus, hostTagParser, CARBON_PORT);
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
	public void test_putDataPoints_longValue() throws DatastoreException, InterruptedException
	{
		long now = System.currentTimeMillis() / 1000;

		m_client.sendText("test.host_name.metric_name", now, "1234");

		ImmutableSortedMap<String, String> tags = Tags.create()
				.put("host", "host_name")
				.build();

		verify(m_publisher, timeout(5000).times(1))
				.post(new DataPointEvent("test.metric_name", tags,
						new LongDataPoint(now * 1000, 1234), ZERO_TTL));
	}

	@Test
	public void test_putDataPoints_doubleValue() throws DatastoreException, InterruptedException
	{
		long now = System.currentTimeMillis() / 1000;

		m_client.sendText("test.host_name.metric_name", now, "12.34");

		ImmutableSortedMap<String, String> tags = Tags.create()
				.put("host", "host_name")
				.build();

		verify(m_publisher, timeout(5000).times(1))
				.post(new DataPointEvent("test.metric_name", tags,
						new DoubleDataPoint(now * 1000, 12.34),ZERO_TTL));
	}

	@Test
	public void test_putDataPoints_notANumber() throws DatastoreException, InterruptedException
	{
	    long now = System.currentTimeMillis() / 1000;

	    m_client.sendText("test.host_name.metric_name", now, "NaN");

	    verify(m_publisher, never()).post(any());
	}

	@Test
	public void test_putDataPoints_infinity() throws DatastoreException, InterruptedException
	{
	    long now = System.currentTimeMillis() / 1000;

	    m_client.sendText("test.host_name.metric_name", now, "Infinity");

	    verify(m_publisher, never()).post(any());
	}

	@Test
	public void test_putDataPoints_negativeInfinity() throws DatastoreException, InterruptedException
	{
	    long now = System.currentTimeMillis() / 1000;

	    m_client.sendText("test.host_name.metric_name", now, "-Infinity");

	    verify(m_publisher, never()).post(any());
	}
}
