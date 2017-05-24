/*
** Copyright (c) Alexis Megas.
** All rights reserved.
**
** Redistribution and use in source and binary forms, with or without
** modification, are permitted provided that the following conditions
** are met:
** 1. Redistributions of source code must retain the above copyright
**    notice, this list of conditions and the following disclaimer.
** 2. Redistributions in binary form must reproduce the above copyright
**    notice, this list of conditions and the following disclaimer in the
**    documentation and/or other materials provided with the distribution.
** 3. The name of the author may not be used to endorse or promote products
**    derived from Smoke without specific prior written permission.
**
** SMOKE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
** IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
** OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
** IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
** INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
** NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
** DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
** THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
** (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
** SMOKE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.purple.smoke;

import android.os.Build;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class TcpNeighbor extends Neighbor
{
    private AtomicInteger m_isValidCertificate = null;
    private InetSocketAddress m_inetSocketAddress = null;
    private InetSocketAddress m_proxyInetSocketAddress = null;
    private SSLSocket m_socket = null;
    private String m_protocols[] = null;
    private String m_proxyIpAddress = "";
    private String m_proxyType = "";
    private TrustManager m_trustManagers[] = null;
    private final static int CONNECTION_TIMEOUT = 5000; // 5 Seconds
    private final static int HANDSHAKE_TIMEOUT = 10000; // 10 Seconds
    private int m_proxyPort = -1;

    protected String getLocalIp()
    {
	try
	{
	    if(m_socket != null && m_socket.getLocalAddress() != null)
		return m_socket.getLocalAddress().getHostAddress();
	}
	catch(Exception exception)
	{
	}

	if(m_version.equals("IPv4"))
	    return "0.0.0.0";
	else
	    return "::";
    }

    protected String getSessionCipher()
    {
	try
	{
	    if(m_socket != null &&
	       m_socket.getSession() != null &&
	       m_socket.getSession().isValid())
		return m_socket.getSession().getCipherSuite();
	}
	catch(Exception exception)
	{
	}

	return "";
    }

    protected boolean connected()
    {
	try
	{
	    return m_socket != null && !m_socket.isClosed() &&
		m_socket.getSession() != null &&
		m_socket.getSession().isValid();
	}
	catch(Exception exception)
	{
	}

	return false;
    }

    protected boolean send(String message)
    {
	if(m_isValidCertificate.get() == 0)
	    return false;

	if(!connected())
	    return false;

	try
	{
	    if(m_socket == null || m_socket.getOutputStream() == null)
		return false;

	    OutputStream outputStream = m_socket.getOutputStream();

	    outputStream.write(message.getBytes());
	    outputStream.flush();
	    Kernel.writeCongestionDigest(message);
	    m_bytesWritten.getAndAdd(message.length());
	}
	catch(Exception exception)
	{
	    setError("A socket error occurred on send().");
	    disconnect();
	    return false;
	}

	return true;
    }

    protected int getLocalPort()
    {
	try
	{
	    if(m_socket != null && !m_socket.isClosed())
		return m_socket.getLocalPort();
	}
	catch(Exception exception)
	{
	}

	return 0;
    }

    protected void disconnect()
    {
	try
	{
	    if(m_socket != null)
	    {
		m_socket.getInputStream().close();
		m_socket.getOutputStream().close();
		m_socket.close();
	    }
	}
	catch(Exception exception)
	{
	}
	finally
	{
	    m_bytesRead.set(0);
	    m_bytesWritten.set(0);
	    m_isValidCertificate.set(0);
	    m_socket = null;
	    m_startTime.set(System.nanoTime());
	}
    }

    protected void sendCapabilities()
    {
	if(m_isValidCertificate.get() == 0)
	    return;

	if(!connected())
	    return;

	try
	{
	    String capabilities = "";

	    if(m_socket == null || m_socket.getOutputStream() == null)
		return;

	    OutputStream outputStream = m_socket.getOutputStream();

	    capabilities = getCapabilities();
	    outputStream.write(capabilities.getBytes());
	    outputStream.flush();
	    m_bytesWritten.getAndAdd(capabilities.length());
	}
	catch(Exception exception)
	{
	    setError("A socket error occurred on sendCapabilities().");
	    disconnect();
	}
    }

    public TcpNeighbor(String proxyIpAddress,
		       String proxyPort,
		       String proxyType,
		       String ipAddress,
		       String ipPort,
		       String scopeId,
		       String version,
		       int oid)
    {
	super(ipAddress, ipPort, scopeId, "TCP", version, oid);

	try
	{
	    m_proxyPort = Integer.parseInt(proxyPort);
	}
	catch(Exception exception)
	{
	    m_proxyPort = -1;
	}

	int port = 4710;

	try
	{
	    port = Integer.parseInt(m_ipPort);
	}
	catch(Exception exception)
	{
	    port = 4710;
	}

	try
	{
	    m_inetSocketAddress = new InetSocketAddress(m_ipAddress, port);
	}
	catch(Exception exception)
	{
	    m_inetSocketAddress = null;
	}

	m_isValidCertificate = new AtomicInteger(0);

	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
	    m_protocols = new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"};
	else
	    m_protocols = new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"};

	m_proxyIpAddress = proxyIpAddress;
	m_proxyType = proxyType;

	if(!m_proxyIpAddress.isEmpty() && m_proxyPort != -1 &&
	   !m_proxyType.isEmpty())
	    try
	    {
		m_proxyInetSocketAddress = new InetSocketAddress
		    (m_proxyIpAddress, m_proxyPort);
	    }
	    catch(Exception exception)
	    {
		m_proxyInetSocketAddress = null;
	    }

	m_readSocketScheduler = Executors.newSingleThreadScheduledExecutor();
	m_readSocketScheduler.scheduleAtFixedRate(new Runnable()
	{
	    @Override
	    public void run()
	    {
		if(m_isValidCertificate.get() == 0)
		    return;

		try
		{
		    if(Thread.currentThread().isInterrupted())
			return;
		    else
			Thread.sleep(5);
		}
		catch(InterruptedException exception)
		{
		    Thread.currentThread().interrupt();
		}
		catch(Exception exception)
		{
		}

		if(!connected())
		    return;

		try
		{
		    long bytesRead = 0;

		    if(m_socket == null ||
		       m_socket.getInputStream() == null)
			return;
		    else
			m_socket.setSoTimeout(SO_TIMEOUT);

		    int i = m_socket.getInputStream().read(m_bytes);

		    if(i < 0)
			bytesRead = -1;
		    else if(i > 0)
			bytesRead += i;

		    if(bytesRead < 0)
		    {
			setError("A socket read() error occurred.");
			disconnect();
			return;
		    }

		    m_bytesRead.getAndAdd(bytesRead);
		    m_lastTimeRead.set(System.nanoTime());

		    synchronized(m_stringBuilder)
		    {
			m_stringBuilder.append
			    (new String(m_bytes, 0, (int) bytesRead));
		    }
		}
		catch(java.net.SocketException exception)
		{
		    setError("A socket error occurred while reading data.");
		    disconnect();
		}
		catch(Exception exception)
		{
		}
	    }
	}, 0, READ_SOCKET_INTERVAL, TimeUnit.MILLISECONDS);
	m_trustManagers = new TrustManager[]
	{
	    new X509TrustManager()
	    {
		public X509Certificate[] getAcceptedIssuers()
		{
		    return new X509Certificate[0];
		}

		public void checkClientTrusted
		    (X509Certificate chain[], String authType)
		{
		}

		public void checkServerTrusted
		    (X509Certificate chain[], String authType)
		{
		    if(authType == null || authType.length() == 0)
			m_isValidCertificate.set(0);
		    else if(chain == null || chain.length == 0)
			m_isValidCertificate.set(0);
		    else
		    {
			try
			{
			    chain[0].checkValidity();

			    byte bytes[] = Database.getInstance().
				neighborRemoteCertificate
				(Cryptography.getInstance(), m_oid.get());

			    if(bytes == null || bytes.length == 0)
			    {
				Database.getInstance().neighborRecordCertificate
				    (Cryptography.getInstance(),
				     String.valueOf(m_oid.get()),
				     chain[0].getEncoded());
				m_isValidCertificate.set(1);
			    }
			    else if(!Cryptography.memcmp(bytes,
							 chain[0].getEncoded()))
			    {
				setError("The stored server's " +
					 "certificate does not match the " +
					 "certificate that was provided by " +
					 "the server.");
				m_isValidCertificate.set(0);
			    }
			    else
				m_isValidCertificate.set(1);
			}
			catch(Exception exception)
			{
			    setError("The server's certificate has expired.");
			    m_isValidCertificate.set(0);
			}
		    }

		    if(m_isValidCertificate.get() == 0)
			synchronized(m_errorMutex)
			{
			    if(m_error.length() == 0)
				m_error.append
				    ("A generic certificate error occurred.");
			}
		}
	    }
	};
    }

    public void abort()
    {
	disconnect();
	super.abort();
	m_isValidCertificate.set(0);
	m_readSocketScheduler.shutdown();

	try
	{
	    m_readSocketScheduler.awaitTermination(60, TimeUnit.SECONDS);
	}
	catch(Exception exception)
	{
	}
    }

    public void connect()
    {
	if(connected())
	    return;

	setError("");

	try
	{
	    m_bytesRead.set(0);
	    m_bytesWritten.set(0);
	    m_lastTimeRead.set(System.nanoTime());

	    SSLContext sslContext = null;

	    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		sslContext = SSLContext.getInstance("TLS");
	    else
		sslContext = SSLContext.getInstance("SSL");

	    sslContext.init(null, m_trustManagers, null);

	    if(m_proxyInetSocketAddress == null)
	    {
		m_socket = (SSLSocket) sslContext.getSocketFactory().
		    createSocket();
		m_socket.connect(m_inetSocketAddress, CONNECTION_TIMEOUT);
	    }
	    else
	    {
		Socket socket = null;

		if(m_proxyType.equals("HTTP"))
		    socket = new Socket
			(new Proxy(Proxy.Type.HTTP, m_proxyInetSocketAddress));
		else
		    socket = new Socket
			(new Proxy(Proxy.Type.SOCKS, m_proxyInetSocketAddress));

		socket.connect(m_inetSocketAddress, CONNECTION_TIMEOUT);
		m_socket = (SSLSocket) sslContext.getSocketFactory().
		    createSocket(socket, m_proxyIpAddress, m_proxyPort, true);
	    }

	    m_socket.setEnabledProtocols(m_protocols);
	    m_socket.setSoTimeout(HANDSHAKE_TIMEOUT); // SSL/TLS process.
	    m_socket.setTcpNoDelay(true);
	    m_startTime.set(System.nanoTime());
	}
	catch(Exception exception)
	{
	    setError("An error occurred while attempting a connection.");
	    disconnect();
	}
    }
}
