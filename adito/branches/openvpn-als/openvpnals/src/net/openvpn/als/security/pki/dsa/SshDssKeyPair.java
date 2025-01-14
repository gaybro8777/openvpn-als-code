
				/*
 *  OpenVPNALS
 *
 *  Copyright (C) 2003-2006 3SP LTD. All Rights Reserved
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
			
package net.openvpn.als.security.pki.dsa;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;

import net.openvpn.als.security.pki.InvalidKeyException;
import net.openvpn.als.security.pki.SshKeyPair;
import net.openvpn.als.security.pki.SshPrivateKey;
import net.openvpn.als.security.pki.SshPublicKey;
import net.openvpn.als.security.pki.Utils;


/**
 *
 *
 * @author $author$
 */
public class SshDssKeyPair extends SshKeyPair {
    /**
     *
     *
     * @param encoded
     *
     * @return
     *
     * @throws InvalidKeyException
     */
    public SshPrivateKey decodePrivateKey(byte[] encoded)
        throws InvalidKeyException {
        return new SshDssPrivateKey(encoded);
    }

    /**
     *
     *
     * @param encoded
     *
     * @return
     *
     * @throws InvalidKeyException
     */
    public SshPublicKey decodePublicKey(byte[] encoded)
        throws InvalidKeyException {
        return new SshDssPublicKey(encoded);
    }

    /**
     *
     *
     * @param bits
     */
    public void generate(int bits) {
        try {
            // Initialize the generator
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
            keyGen.initialize(bits, Utils.getRND());

            KeyPair pair = keyGen.generateKeyPair();

            // Get the keys
            DSAPrivateKey prvKey = (DSAPrivateKey) pair.getPrivate();
            DSAPublicKey pubKey = (DSAPublicKey) pair.getPublic();

            // Set the private key (the public is automatically generated)
            setPrivateKey(new SshDssPrivateKey(prvKey));
        } catch (NoSuchAlgorithmException nsae) {
        }
    }
}
