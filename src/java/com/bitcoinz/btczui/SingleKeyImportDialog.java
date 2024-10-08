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


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;


/**
 * Dialog to enter a single private key to import
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class SingleKeyImportDialog
	extends JDialog
{
	protected boolean isOKPressed = false;
	protected String  key    = null;

	protected JLabel     keyLabel = null;
	protected JTextField keyField = null;

	protected JLabel upperLabel;
	protected JLabel lowerLabel;

	protected JProgressBar progress = null;

	protected BTCZClientCaller caller;

	JButton okButon;
	JButton cancelButon;

	public SingleKeyImportDialog(JFrame parent, BTCZClientCaller caller)
	{
		super(parent);
		this.caller = caller;

		this.setTitle("Enter private key...");
	    this.setLocation(parent.getLocation().x + 50, parent.getLocation().y + 50);
		this.setModal(true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
		controlsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel tempPanel = new JPanel(new BorderLayout(0, 0));
		tempPanel.add(this.upperLabel = new JLabel(
			"<html>Please enter a single private key to import." +
		    "</html>"), BorderLayout.CENTER);
		controlsPanel.add(tempPanel);

		JLabel dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 8));
		controlsPanel.add(dividerLabel);

		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(keyLabel = new JLabel("Key: "));
		tempPanel.add(keyField = new JTextField(60));
		controlsPanel.add(tempPanel);

		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 8));
		controlsPanel.add(dividerLabel);

		tempPanel = new JPanel(new BorderLayout(0, 0));
		tempPanel.add(this.lowerLabel = new JLabel(
			"<html><span style=\"font-weight:bold\">" +
		    "Warning:</span> Private key import is a slow operation that " +
		    "requires blockchain rescanning (may take many minutes). <br/>The GUI " +
			"will not be usable for other functions during this time</html>"),
			BorderLayout.CENTER);
		controlsPanel.add(tempPanel);

		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 8));
		controlsPanel.add(dividerLabel);

		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(progress = new JProgressBar());
		controlsPanel.add(tempPanel);

		this.getContentPane().setLayout(new BorderLayout(0, 0));
		this.getContentPane().add(controlsPanel, BorderLayout.NORTH);

		// Form buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
		okButon = new JButton("Import");
		buttonPanel.add(okButon);
		buttonPanel.add(new JLabel("   "));
		cancelButon = new JButton("Cancel");
		buttonPanel.add(cancelButon);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		okButon.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				SingleKeyImportDialog.this.processOK();
			}
		});

		cancelButon.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				SingleKeyImportDialog.this.setVisible(false);
				SingleKeyImportDialog.this.dispose();

				SingleKeyImportDialog.this.isOKPressed = false;
				SingleKeyImportDialog.this.key = null;
			}
		});

		this.setSize(740, 210);
		this.validate();
		this.repaint();

		this.pack();
	}


	protected void processOK()
	{
		final String key = SingleKeyImportDialog.this.keyField.getText();

		if ((key == null) || (key.trim().length() <= 0))
		{
			JOptionPane.showMessageDialog(
				SingleKeyImportDialog.this.getParent(),
				"The key is empty. Please enter it into the text field.", "Empty...",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		SingleKeyImportDialog.this.isOKPressed = true;
		SingleKeyImportDialog.this.key = key;

		// Start import
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.progress.setIndeterminate(true);
		this.progress.setValue(1);

		this.okButon.setEnabled(false);
		this.cancelButon.setEnabled(false);

		SingleKeyImportDialog.this.keyField.setEditable(false);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					String address = SingleKeyImportDialog.this.caller.importPrivateKey(key);
					String addition = "";

					if (!Util.stringIsEmpty(address))
					{
						addition = " It corresponds to address:\n" + address;
					}

					JOptionPane.showMessageDialog(
						SingleKeyImportDialog.this,
						"The private key:\n" +
						key + "\n" +
						"has been imported successfully." + addition,
						"Private key imported successfully...",
						JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception e)
				{
					Log.error("An error occurred when importing private key", e);

					JOptionPane.showMessageDialog(
						SingleKeyImportDialog.this.getRootPane().getParent(),
						"An error occurred when importing private key. Error message is:\n" +
						e.getClass().getName() + ":\n" + e.getMessage() + "\n\n" +
						"Please ensure that bitcoinzd is running and the key is in the correct \n" +
						"form. You may try again later...\n",
						"Error in importing private key", JOptionPane.ERROR_MESSAGE);
				} finally
				{
					SingleKeyImportDialog.this.setVisible(false);
					SingleKeyImportDialog.this.dispose();
				}
			}
		}).start();
	}


	public boolean isOKPressed()
	{
		return this.isOKPressed;
	}


	public String getKey()
	{
		return this.key;
	}
}
