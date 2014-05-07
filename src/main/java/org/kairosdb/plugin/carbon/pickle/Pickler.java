package org.kairosdb.plugin.carbon.pickle;

import net.razorvine.pickle.Opcodes;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 Created with IntelliJ IDEA.
 User: bhawkins
 Date: 10/7/13
 Time: 10:31 AM
 To change this template use File | Settings | File Templates.
 */
public class Pickler extends net.razorvine.pickle.Pickler
{
	public static final byte PROTOCOL = 2;

	private OutputStream m_out;

	public void writeMetrics(List<PickleMetric> metrics, OutputStream out) throws IOException
	{
		m_out = out;

		dump(metrics, out);
	}

	@Override
	public void save(Object o) throws IOException
	{
		if (o instanceof PickleMetric)
		{
			PickleMetric metric = (PickleMetric) o;
			save(metric.getPath());
			save((double)metric.getTime());
			/*m_out.write(Opcodes.INT);
			m_out.write(String.valueOf(metric.getTime()).getBytes());
			m_out.write('\n');*/
			if (metric.isLongValue())
				save(metric.getLongValue());
			else
				save(metric.getDoubleValue());

			m_out.write(Opcodes.TUPLE2);
			m_out.write(Opcodes.TUPLE2);
		}
		else
			super.save(o);
	}
}
