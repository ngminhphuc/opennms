/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.jicmp.jna;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;

public class Win32V6NativeSocket extends Win32NativeSocket {
    
    static {
        Native.register((String)null);
    }

    private final int m_sock;

    public Win32V6NativeSocket(final int family, final int type, final int protocol, final int listenPort) throws Exception {
        m_sock = socket(family, type, protocol);
        final sockaddr_in6 addr_in = new sockaddr_in6(listenPort);
        bind(m_sock, addr_in, addr_in.size());
    }

    public native int bind(int socket, sockaddr_in6 address, int address_len) throws LastErrorException;

    public native int socket(int domain, int type, int protocol) throws LastErrorException;

    public native int sendto(int socket, Buffer buffer, int buflen, int flags, sockaddr_in6 dest_addr, int dest_addr_len) throws LastErrorException;

    public native int recvfrom(int socket, Buffer buffer, int buflen, int flags, sockaddr_in6 in_addr, int[] in_addr_len) throws LastErrorException;

    public native int closesocket(int socket) throws LastErrorException;

    @Override
    public void setTrafficClass(final int tc) throws LastErrorException {
        // it appears that IP_TOS and IPV6_TCLASS do not exist in Win32 anymore
    }

    @Override
    public void allowFragmentation(final boolean frag) throws IOException {
        allowFragmentation(IPPROTO_IPV6, IPV6_DONTFRAG, frag);
    }

    @Override
    public int receive(final NativeDatagramPacket p) throws IOException {
        try {
            final sockaddr_in6 in_addr = new sockaddr_in6();
            final int[] szRef = new int[] { in_addr.size() };

            final ByteBuffer buf = p.getContent();

            final int n = recvfrom(getSock(), buf, buf.capacity(), 0, in_addr, szRef);
            p.setLength(n);
            p.setAddress(in_addr.getAddress());
            p.setPort(in_addr.getPort());

            return n;
        } catch (final LastErrorException e) {
            throw translateException(e);
        }
    }

    @Override
    public int send(final NativeDatagramPacket p) throws IOException {
        try {
            final ByteBuffer buf = p.getContent();
            final sockaddr_in6 destAddr = new sockaddr_in6(p.getAddress(), p.getPort());
            return sendto(getSock(), buf, buf.remaining(), 0, destAddr, destAddr.size());
        } catch (final LastErrorException e) {
            throw translateException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            closesocket(getSock());
        } catch (final LastErrorException e) {
            throw translateException(e);
        }
    }

    @Override
    public int getSock() {
        return m_sock;
    }

}
