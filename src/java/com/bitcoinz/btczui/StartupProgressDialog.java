/************************************************************************************************
 *  ____ _____ ______ ___   __ _____  __   _ _____ _        __    _ _      _   _   _ ___
 * | __ \_   _|_   __| __|/ _ \_   _||   \| |___  | \      / /_ _| | | ___| |_| | | |_ _|
 * |____/ | |   | |/ /   / / \ \| |  | |\ | |  / / \ \ /\ / / _` | | |/ _ \ __| | | || |
 * | ___ \| |_  | |\ \__ \ \_/ /| |_ | | \  | / /_  \ V  V / (_| | | |  __/ |_| |_| || |
 * |_____/____| |_| \____|\___/_____||_|  \_|/____|  \_/\_/ \__,_|_|_|\___|\__|\___/|___|

 * Copyright (c) 2017-2022 BitcoinZ team
 * Copyright (c) 2016 Ivan Vaklinov <ivan@vaklinov.com>

Code was originally written by developer - https://github.com/zlatinb
Taken from repository https://github.com/zlatinb/zcash-swing-wallet-ui under an MIT license
*/

package com.bitcoinz.btczui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.bitcoinz.btczui.OSUtil.OS_TYPE;
import com.bitcoinz.btczui.BTCZClientCaller.WalletCallException;


public class StartupProgressDialog extends JFrame {


    private static final int POLL_PERIOD = 1200;
    private static final int STARTUP_ERROR_CODE = -28;

    private BorderLayout borderLayout1 = new BorderLayout();
    private JLabel imageLabel = new JLabel();
    private JLabel progressLabel = new JLabel();
    private JPanel southPanel = new JPanel();
    private BorderLayout southPanelLayout = new BorderLayout();
    private JProgressBar progressBar = new JProgressBar();
    private ImageIcon imageIcon;

    private final BTCZClientCaller clientCaller;

    public StartupProgressDialog(BTCZClientCaller clientCaller){

        this.clientCaller = clientCaller;

        URL iconUrl = this.getClass().getClassLoader().getResource("images/BitcoinZ.png");
        imageIcon = new ImageIcon(iconUrl);
        imageLabel.setIcon(imageIcon);
        imageLabel.setBorder(BorderFactory.createEmptyBorder(25, 25, 0, 25));
        Container contentPane = getContentPane();
        contentPane.setLayout(borderLayout1);
        southPanel.setLayout(southPanelLayout);
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        contentPane.add(imageLabel, BorderLayout.NORTH);
    		JLabel bitcoinzWalletLabel = new JLabel(
    			"<html><b><span style=\"font-weight:bold;font-size:2.2em\">" +
    		  "BitcoinZ Wallet UI</span></b><br>&nbsp;Version 2.0.10</html>");

    		bitcoinzWalletLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    		// todo - place in a panel with flow center
    		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));
    		tempPanel.add(bitcoinzWalletLabel);
    		contentPane.add(tempPanel, BorderLayout.CENTER);
        contentPane.add(southPanel, BorderLayout.SOUTH);
        progressBar.setIndeterminate(true);
        southPanel.add(progressBar, BorderLayout.NORTH);
        progressLabel.setText("Starting...");
        southPanel.add(progressLabel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    public void waitForStartup() throws IOException,
        InterruptedException,WalletCallException,InvocationTargetException {

        // special handling of Windows/Mac OS app launch
      	OS_TYPE os = OSUtil.getOSType();
        if ((os == OS_TYPE.WINDOWS) || (os == OS_TYPE.MAC_OS)) {
            ProvingKeyFetcher keyFetcher = new ProvingKeyFetcher();
            keyFetcher.fetchIfMissing(this);
        }

        Log.info("Splash: checking if bitcoinzd is already running...");
        boolean shouldStartBTCZd = false;
        try {
            clientCaller.getDaemonRawRuntimeInfo();
        } catch (IOException e) {
        	// Relying on a general exception may be unreliable
        	// may be thrown for an unexpected reason!!! - so message is checked
        	if (e.getMessage() != null &&
        		e.getMessage().toLowerCase(Locale.ROOT).contains("error: couldn't connect to server")) {
        		shouldStartBTCZd = true;
        	}
        }

        if (!shouldStartBTCZd) {
        	Log.info("Splash: bitcoinzd already running...");
            // What if started by hand but taking long to initialize???
  //            doDispose();
  //            return;
        } else {
        	Log.info("Splash: bitcoinzd will be started...");
        }

        final Process daemonProcess =
        	shouldStartBTCZd ? clientCaller.startDaemon() : null;

        Thread.sleep(POLL_PERIOD); // just a little extra

        int iteration = 0;
        while(true) {
        	iteration++;
            Thread.sleep(POLL_PERIOD);

            JsonObject info = null;

            try {
            	info = clientCaller.getDaemonRawRuntimeInfo();
            } catch (IOException e) {
            	if (iteration > 4) {
            		throw e;
            	} else {
            		continue;
            	}
            }

            JsonValue code = info.get("code");
            if (code == null || (code.asInt() != STARTUP_ERROR_CODE))
                break;
            final String message = info.getString("message", "???");
            setProgressText(message);

        }

        // doDispose(); - will be called later by the main GUI
        if (daemonProcess != null) // Shutdown only if we started it
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	Log.info("Stopping bitcoinzd because we started it - now it is alive: " +
                		           StartupProgressDialog.this.isAlive(daemonProcess));
                try {
                    clientCaller.stopDaemon();
                  long start = System.currentTimeMillis();

                  while (!StartupProgressDialog.this.waitFor(daemonProcess, 3000)) {
                  	long end = System.currentTimeMillis();
                  	Log.info("Waiting for " + ((end - start) / 1000) + " seconds for bitcoinzd to exit...");

                  	if (end - start > 10 * 1000) {
                  		clientCaller.stopDaemon();
                  		daemonProcess.destroy();
                  	}

                  	if (end - start > 1 * 60 * 1000) {
                  		break;
                  	}
                  }

                  if (StartupProgressDialog.this.isAlive(daemonProcess)) {
                  	Log.info("bitcoinzd is still alive although we tried to stop it. " +
                                             "Hopefully it will stop later!");
                      } else
                      	Log.info("bitcoinzd shut down successfully");
                } catch (Exception bad) {
                	Log.error("Couldn't stop bitcoinzd!", bad);
                }
            }
        });

    }

    public void doDispose() {
        SwingUtilities.invokeLater(new Runnable() {
    			@Override
    			public void run() {
    				setVisible(false);
    				dispose();
    			}
  		});
    }

    public void setProgressText(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
    			@Override
    			public void run() {
    				progressLabel.setText(text);
    			}
	     });
    }


    // Custom code - to allow JDK7 compilation.
    public boolean isAlive(Process p){
    	if (p == null){
    		return false;
    	}
        try{
            int val = p.exitValue();
            return false;
        } catch (IllegalThreadStateException itse){
            return true;
        }
    }


    // Custom code - to allow JDK7 compilation.
    public boolean waitFor(Process p, long interval) {
  		synchronized (this) {
  			long startWait = System.currentTimeMillis();
  			long endWait = startWait;
  			do{
  				boolean ended = !isAlive(p);

  				if (ended){
  					return true; // End here
  				}

  				try{
  					this.wait(100);
  				} catch (InterruptedException ie){
  					// One of the rare cases where we do nothing
  					Log.error("Unexpected error: ", ie);
  				}

  				endWait = System.currentTimeMillis();
  			} while ((endWait - startWait) <= interval);
  		}

		return false;
    }
}
