/*******************************************************************************
 * Copyright 2018 University of Rostock, Institute of Applied Microelectronics and Computer Engineering
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Vikram (University of Rostock)- Initial creation, adapted from ClientHandshaker
 ******************************************************************************/
package org.eclipse.californium.scandium.util;

import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.pskstore.PskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts psk credentials from the current {@code DTLSSession}.
 */
public class PskUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PskUtil.class.getName());
	
	private byte[] psk;
	
	private PreSharedKeyIdentity pskIdentity;
	
	/**
	 * Retrieves preshared key identity and preshared key for the given dtls session and psk store.
	 * 
	 * @param sniEnabled - {@code true} if SNI should be enabled for negotiating the given session
	 * @param session - {@code DTLSSession}
	 * @param pskStore - {@code PskStore}
	 * @throws HandshakeException if no data is available for the provided session
	 * @throws NullPointerException if either session or pskStore is {@code null}
	 */
	public PskUtil(boolean sniEnabled, DTLSSession session, PskStore pskStore) throws HandshakeException {
		if (session == null) {
			throw new NullPointerException("Dtls session cannot be null");
		}
		if (pskStore == null) {
			throw new NullPointerException("psk store cannot be null");
		}
		ServerNames virtualHost = session.getServerNames();
		String identity = null;
		if (sniEnabled && virtualHost != null) {
			if (!session.isSniSupported()) {
				LOGGER.warn("client is configured to use SNI but server does not support it, PSK authentication is likely to fail");
			}
			// look up identity in scope of virtual host
			String virtualHostName = session.getVirtualHost();
			identity = pskStore.getIdentity(session.getPeer(), virtualHost);
			if (identity == null) {
				AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE, session.getPeer());
				throw new HandshakeException(
						String.format("No Identity found for peer [address: %s, virtual host: %s]",
								session.getPeer(), virtualHostName),
						alert);
			}
			this.psk = pskStore.getKey(virtualHost, identity);
			if (psk == null) {
				AlertMessage alert = new AlertMessage(AlertLevel.FATAL,	AlertDescription.HANDSHAKE_FAILURE, session.getPeer());
				throw new HandshakeException(
						String.format("No pre-shared key found for [virtual host: %s, identity: %s]",
								virtualHostName, identity),
						alert);
			} 
			this.pskIdentity = new PreSharedKeyIdentity(virtualHostName, identity);
		} else {
			identity = pskStore.getIdentity(session.getPeer());
			if (identity == null) {
				AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE, session.getPeer());
				throw new HandshakeException(
						String.format("No Identity found for peer [address: %s]", session.getPeer()), alert);
			}
			this.psk = pskStore.getKey(identity);
			if (psk == null) {
				AlertMessage alert = new AlertMessage(AlertLevel.FATAL,	AlertDescription.HANDSHAKE_FAILURE, session.getPeer());
				throw new HandshakeException(
						String.format("No pre-shared key found for [identity: %s]", identity), alert);
			}
			if (sniEnabled) {
				this.pskIdentity = new PreSharedKeyIdentity(null, identity);
			} else {
				this.pskIdentity = new PreSharedKeyIdentity(identity);
			}
		}		
	}
	
	/**
	 * This method returns the psk identity either for the virtual host hosted on session's peer 
	 * or for the session's peer itself.
	 * 
	 * @return {@code PreSharedKeyIdentity}
	 */
	public PreSharedKeyIdentity getPskIdentity() {
		return this.pskIdentity;
	}
	
	/**
	 * This method returns the pre shared key for the current 
	 * {@code DTLSSession} and {@code PskStore}.
	 * 
	 * @return byte array
	 */
	public byte[] getPreSharedKey(){
		return this.psk;
	}
}
