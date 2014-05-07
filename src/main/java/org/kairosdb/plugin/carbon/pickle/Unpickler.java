package org.kairosdb.plugin.carbon.pickle;

import net.razorvine.pickle.Opcodes;

import java.io.IOException;

/**
 Created with IntelliJ IDEA.
 User: bhawkins
 Date: 10/7/13
 Time: 2:14 PM
 To change this template use File | Settings | File Templates.
 */
public class Unpickler extends net.razorvine.pickle.Unpickler
{
	private boolean m_firstTuple = true;

	@Override
	protected void dispatch(short key) throws IOException
	{
		if (key == Opcodes.TUPLE2)
		{
			if (!m_firstTuple)
			{
				m_firstTuple = true;
				//Pop three items from stack
				Object value = stack.pop();
				long time = ((Number)stack.pop()).longValue();
				String path = (String)stack.pop();

				PickleMetric metric;
				if (value instanceof Double)
					metric = new PickleMetric(path, time, (Double)value);
				else
					metric = new PickleMetric(path, time, ((Number)value).longValue());

				stack.add(metric);
			}
			else
				m_firstTuple = false;
		}
		else
			super.dispatch(key);

	}
}
