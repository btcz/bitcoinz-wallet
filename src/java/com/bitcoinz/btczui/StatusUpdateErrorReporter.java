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

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Reporter for periodic errors. Will later have options to filter errors etc.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class StatusUpdateErrorReporter
{
	private JFrame parent;
	private long lastReportedErrroTime = 0;

	public StatusUpdateErrorReporter(JFrame parent)
	{
		this.parent = parent;
	}

	public void reportError(Exception e)
	{
		reportError(e, true);
	}

	public void reportError(Exception e, boolean isDueToAutomaticUpdate)
	{
		Log.error("Unexpected error: ", e);

		// TODO: Error logging
		long time = System.currentTimeMillis();

		// TODO: More complex filtering/tracking in the future
		if (isDueToAutomaticUpdate && (time - lastReportedErrroTime) < (45 * 1000))
		{
			return;
		}

		if (isDueToAutomaticUpdate)
		{
			lastReportedErrroTime = time;
		}

		String settingsDirectory = ".BitcoinZWallet";

		try
		{
			settingsDirectory = OSUtil.getSettingsDirectory();
		} catch (Exception e2)
		{
			Log.error("Secondary error: ", e2);
		}

		JOptionPane.showMessageDialog(
			parent,
			"An unexpected error occurred during the operation of the GUI wallet.\n" +
			"Details may be found in the log in directory: " + settingsDirectory + "\n" +
			"\n" +
			e.getMessage(),
			"Error in wallet operation.", JOptionPane.ERROR_MESSAGE);
	}
}
