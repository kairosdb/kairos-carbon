package org.kairosdb.plugin.carbon;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.kairosdb.plugin.carbon.pickle.Unpickler;

/**
 Created with IntelliJ IDEA.
 User: bhawkins
 Date: 10/2/13
 Time: 12:06 PM
 To change this template use File | Settings | File Templates.
 */
public class PickleDecoder extends OneToOneDecoder
{
	private Unpickler m_unpickler = new Unpickler();

	public PickleDecoder()
	{
	}

	@Override
	protected Object decode(ChannelHandlerContext channelHandlerContext,
			Channel channel, Object o) throws Exception
	{
		ChannelBuffer cb = (ChannelBuffer)o;

		Unpickler unpickler = new Unpickler();

		return (unpickler.load(new ChannelBufferInputStream(cb)));
	}
}
