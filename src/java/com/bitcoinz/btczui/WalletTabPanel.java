/************************************************************************************************
 *  ____ _____ ______ ___   __ _____  __   _ _____ _        __    _ _      _   _   _ ___
 * | __ \_   _|_   __| __|/ _ \_   _||   \| |___  | \      / /_ _| | | ___| |_| | | |_ _|
 * |____/ | |   | |/ /   / / \ \| |  | |\ | |  / / \ \ /\ / / _` | | |/ _ \ __| | | || |
 * | ___ \| |_  | |\ \__ \ \_/ /| |_ | | \  | / /_  \ V  V / (_| | | |  __/ |_| |_| || |
 * |_____/____| |_| \____|\___/_____||_|  \_|/____|  \_/\_/ \__,_|_|_|\___|\__|\___/|___|

 * Copyright (c) 2017-2022 BitcoinZ team
 * Copyright (c) 2016 Ivan Vaklinov <ivan@vaklinov.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 **********************************************************************************/
package com.bitcoinz.btczui;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.bitcoinz.btczui.BTCZClientCaller.WalletCallException;


/**
 * Base for all panels contained as wallet TABS.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class WalletTabPanel
	extends JPanel
{
	// Lists of threads and timers that may be stopped if necessary
	protected List<Timer> timers                   = null;
	protected List<DataGatheringThread<?>> threads = null;


	public WalletTabPanel()
		throws IOException, InterruptedException, WalletCallException
	{
		super();

		this.timers = new ArrayList<Timer>();
		this.threads = new ArrayList<DataGatheringThread<?>>();
	}


	public void stopThreadsAndTimers()
	{
		for (Timer t : this.timers)
		{
			t.stop();
		}

		for (DataGatheringThread<?> t : this.threads)
		{
			t.setSuspended(true);
		}
	}


	// Interval is in milliseconds
	// Returns true if all threads have ended, else false
	public boolean waitForEndOfThreads(long interval)
	{
		synchronized (this)
		{
			long startWait = System.currentTimeMillis();
			long endWait = startWait;
			do
			{
				boolean allEnded = true;
				for (DataGatheringThread<?> t : this.threads)
				{
					if (t.isAlive())
					{
						allEnded = false;
					}
				}

				if (allEnded)
				{
					return true; // End here
				}

				try
				{
					this.wait(100);
				} catch (InterruptedException ie)
				{
					// One of the rare cases where we do nothing
					Log.error("Unexpected error: ", ie);
				}

				endWait = System.currentTimeMillis();
			} while ((endWait - startWait) <= interval);
		}

		return false;
	}

} // End class
