package org.kairosdb.plugin.carbon;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.kairosdb.core.DataPoint;
import org.kairosdb.core.KairosDBService;
import org.kairosdb.core.datapoints.DoubleDataPointFactory;
import org.kairosdb.core.datapoints.DoubleDataPointFactoryImpl;
import org.kairosdb.core.datapoints.LongDataPointFactory;
import org.kairosdb.core.datapoints.LongDataPointFactoryImpl;
import org.kairosdb.core.exception.KairosDBException;
import org.kairosdb.core.telnet.WordSplitter;
import org.kairosdb.eventbus.FilterEventBus;
import org.kairosdb.eventbus.Publisher;
import org.kairosdb.events.DataPointEvent;
import org.kairosdb.util.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

/**
 Created with IntelliJ IDEA.
 User: bhawkins
 Date: 9/30/13
 Time: 4:09 PM
 To change this template use File | Settings | File Templates.
 */
public class CarbonTextServer extends SimpleChannelUpstreamHandler implements ChannelPipelineFactory,
		KairosDBService
{
	public static final Logger logger = LoggerFactory.getLogger(CarbonTextServer.class);

	private final int m_port;
	private InetAddress m_address;
	private final Publisher<DataPointEvent> m_publisher;
	private final TagParser m_tagParser;
	private ServerBootstrap m_serverBootstrap;

	@Inject
	private LongDataPointFactory m_longDataPointFactory = new LongDataPointFactoryImpl();

	@Inject
	private DoubleDataPointFactory m_doubleDataPointFactory = new DoubleDataPointFactoryImpl();

	public CarbonTextServer(FilterEventBus eventBus,
			TagParser tagParser, @Named("kairosdb.carbon.text.port") int port)
	{
		this(eventBus, tagParser, port, null);
	}

	@Inject
	public CarbonTextServer(FilterEventBus eventBus,
			TagParser tagParser, @Named("kairosdb.carbon.text.port") int port,
			@Named("kairosdb.carbon.text.address") String address)
	{
		m_port = port;
		m_publisher = eventBus.createPublisher(DataPointEvent.class);
		m_tagParser = tagParser;
		m_address = null;
		try
		{
			m_address = InetAddress.getByName(address);
		}
		catch (UnknownHostException e)
		{
			logger.error("Unknown host name " + address + ", will bind to 0.0.0.0");
		}
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception
	{
		ChannelPipeline pipeline = Channels.pipeline();

		// Add the text line codec combination first,
		DelimiterBasedFrameDecoder frameDecoder = new DelimiterBasedFrameDecoder(
				1024, Delimiters.lineDelimiter());
		pipeline.addLast("framer", frameDecoder);
		pipeline.addLast("decoder", new WordSplitter());
		pipeline.addLast("encoder", new StringEncoder());

		// and then business logic.
		pipeline.addLast("handler", this);

		return pipeline;
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx,
			final MessageEvent msgevent)
	{
		final Object message = msgevent.getMessage();
		if (message instanceof String[])
		{
			try
			{
				String[] msgArr = (String[])message;

				//TODO: Validate data
				CarbonMetric carbonMetric = m_tagParser.parseMetricName(msgArr[0]);

				//Bail out if no data point set is returned
				if (carbonMetric == null)
					return;

				//validate dps has at least one tag
				if (carbonMetric.getTags().size() == 0)
				{
					logger.warn("Metric "+msgArr[0]+" is missing a tag");
					return;
				}

				long timestamp = Long.parseLong(msgArr[2]) * 1000; //Converting to milliseconds

				DataPoint dp;
                if ("NaN".equalsIgnoreCase(msgArr[1])) {
                  logger.info("Metric {} has a value of 'NaN'.  Not sending to Kairos", msgArr[0]);
                  return;
                }

                if (msgArr[1].toLowerCase().contains("infinity")) {
                  logger.info("Metric {} has a value of Infinity/-Infinity.  Not sending to Kairos", msgArr[0]);
                  return;
                }

				if (msgArr[1].contains("."))
					dp = m_doubleDataPointFactory.createDataPoint(timestamp, Double.parseDouble(msgArr[1]));
				else
					dp = m_longDataPointFactory.createDataPoint(timestamp, Long.parseLong(msgArr[1]));

				m_publisher.post(new DataPointEvent(carbonMetric.getName(), carbonMetric.getTags(), dp, carbonMetric.getTtl()));
			}
			catch (Exception e)
			{
				logger.error("Carbon text error", e);
			}
		}
		else
		{
			log("Invalid message. Must be of type String.");
		}
	}

	private static void log(String message)
	{
		log(message, null);
	}

	private static void log(String message, Exception e)
	{
		if (logger.isDebugEnabled())
			if (e != null)
				logger.debug(message, e);
			else
				logger.debug(message);
		else
		{
			if (e instanceof ValidationException)
				message = message + " Reason: " + e.getMessage();
			logger.warn(message);
		}
	}

	@Override
	public void start() throws KairosDBException
	{
		// Configure the server.
		m_serverBootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		// Configure the pipeline factory.
		m_serverBootstrap.setPipelineFactory(this);
		m_serverBootstrap.setOption("child.tcpNoDelay", true);
		m_serverBootstrap.setOption("child.keepAlive", true);
		m_serverBootstrap.setOption("reuseAddress", true);

		// Bind and start to accept incoming connections.
		m_serverBootstrap.bind(new InetSocketAddress(m_address, m_port));
	}

	@Override
	public void stop()
	{
		if (m_serverBootstrap != null)
			m_serverBootstrap.shutdown();
	}

}