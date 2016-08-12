package org.kairosdb.plugin.carbon.pickle;

import net.razorvine.pickle.Opcodes;

import java.io.IOException;
import java.util.ArrayList;


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
	static int i=0;
	private boolean is_tuple = false;

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
		else if ((key == Opcodes.TUPLE))
		{

			i++;
			System.out.println("i = " + i);
			if(i%2 == 0){
				i =0 ;
				return ;
			}

			if (m_firstTuple)
			{

				m_firstTuple = true;
				//Pop three items from stack
				Object value = stack.pop();
				long time = ((Number)stack.pop()).longValue();
				stack.pop();
				String path = /*(String)*/stack.pop().toString();


				PickleMetric metric;
				if (/*value instanceof Double*/ value.toString().contains(".")){
					metric = new PickleMetric(path, time, Double.parseDouble(value.toString()));
				}
				else{
					metric = new PickleMetric(path, time, Long.parseLong(value.toString()));
				}

				stack.add(metric);

			}
			else{
				m_firstTuple = false;
			}

			is_tuple = true;
		}
		else if(key == Opcodes.APPEND && is_tuple){
			is_tuple = false;
			Object value = stack.pop();
			stack.pop();

			@SuppressWarnings("unchecked")
			ArrayList<Object> list = (ArrayList<Object>) stack.peek();

			list.add(value);

		}
		else
			super.dispatch(key);

	}
}
