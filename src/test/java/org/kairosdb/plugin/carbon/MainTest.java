package org.kairosdb.plugin.carbon;

import org.junit.Test;
import org.kairosdb.core.Main;
import org.kairosdb.core.exception.KairosDBException;

import java.io.IOException;

/**
 Created by bhawkins on 4/18/14.
 */
public class MainTest
{
	@Test
	public void runMainTest() throws IOException, KairosDBException, InterruptedException
	{
		Main main = new Main(null);
		main.startServices();


		main.stopServices();
	}
}
