/*
 * Copyright (c) 2014 Magnet Systems, Inc.
 * All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.magnet.android.mms.connection;

import android.content.Context;

import com.magnet.android.mms.settings.MagnetDefaultSettings;
import com.magnet.android.mms.utils.logger.Log;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.StrictHostnameVerifier;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 * This manager provides the SSLSocketFactory and the HostnameVerifier
 * implementations for the MagnetRestConnectionService.
 *
 * @author emok
 */
public final class SslManager {
  private static final String TAG = SslManager.class.getSimpleName();
  private static SslManager sInstance = null;
  private Context mContext;
  private HostnameVerifierEnum mCurrentVerifier = null;
  private HostnameVerifier mHostnameVerifier = null;

  public static final class LAZY_SSL {
    public static final String[] ANONYMOUS_CIPHERS = {
        "SSL_DH_anon_WITH_RC4_128_MD5",
        "TLS_DH_anon_WITH_AES_128_CBC_SHA",
        "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
        "SSL_DH_anon_WITH_DES_CBC_SHA",
        "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
        "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA" };
  }

  public enum HostnameVerifierEnum {
    STRICT, ALLOW_ALL, BROWSER_COMPAT
  }

  private SslManager(Context context) {
    mContext = context;
  }

  synchronized static SslManager getInstance(Context context) {
    if (sInstance == null) {
      sInstance = new SslManager(context);
    }
    return sInstance;
  }

  synchronized HostnameVerifier getHostnameVerifier() {
    String verifierStr = MagnetDefaultSettings.getInstance(mContext).getSslHostnameVerifier();
    Log.d(TAG, "getHostnameVerifier(): Value from settings: " + verifierStr);
    try {
      HostnameVerifierEnum verifier = HostnameVerifierEnum.valueOf(verifierStr);
      if (mHostnameVerifier == null || verifier != mCurrentVerifier) {
        switch (verifier) {
        case ALLOW_ALL:
          mHostnameVerifier = new AllowAllHostnameVerifier();
          break;
        case BROWSER_COMPAT:
          mHostnameVerifier = new BrowserCompatHostnameVerifier();
          break;
        case STRICT:
        default:
          mHostnameVerifier = new StrictHostnameVerifier();
        }
        mCurrentVerifier = verifier;
      }
    } catch (Exception ex) {
      Log.w(TAG, "getHostnameVerifier(): caught exception.  Will default to STRICT.", ex);
      if (mCurrentVerifier != HostnameVerifierEnum.STRICT) {
        mHostnameVerifier = new StrictHostnameVerifier();
        mCurrentVerifier = HostnameVerifierEnum.STRICT;
      }
    }
    return mHostnameVerifier;
  }

  private static class AnonymousSslSocketFactory extends SSLSocketFactory {

//    @Override
//    public Socket connectSocket(Socket socket, String host, int port,
//        InetAddress localAddress, int localPort, HttpParams params)
//            throws IOException, UnknownHostException, ConnectTimeoutException {
//      if (socket instanceof SSLSocket) {
//        ((SSLSocket)socket).setEnabledCipherSuites(LAZY_SSL.ANONYMOUS_CIPHERS);
//      }
//
//      return super.connectSocket( socket, host, port, localAddress, localPort, params );
//    }
//
//    @Override
//    public Socket createSocket( Socket socket, String host, int port, boolean autoClose )
//        throws IOException, UnknownHostException {
//      SSLSocket socket1 = (SSLSocket) getSslContext().getSocketFactory().createSocket(socket,
//          host, port,  autoClose);
//      socket1.setEnabledCipherSuites(LAZY_SSL.ANONYMOUS_CIPHERS);
//      return socket1;
//    }
//
//    public AnonymousSslSocketFactory()
//        throws NoSuchAlgorithmException, KeyManagementException,
//        KeyStoreException, UnrecoverableKeyException {
//      super(null, null, null);
//      X509HostnameVerifier doNothingVerifier = new X509HostnameVerifier() {
//        @Override
//        public void verify( String host, SSLSocket ssl ) throws IOException {
//        }
//
//        @Override
//        public void verify( String host, X509Certificate cert ) throws SSLException {
//        }
//
//        @Override
//        public void verify( String host, String[] cns, String[] subjectAlts ) throws SSLException {
//        }
//
//        @Override
//        public boolean verify( String s, SSLSession sslSession ) {
//          return true;
//        }
//      };
//
//      this.setHostnameVerifier(doNothingVerifier);
//    }
//
//    @Override
//    public Socket createSocket() throws IOException {
//      try {
//        return getSslContext().getSocketFactory().createSocket();
//      } catch (Exception e) {
//        throw new IOException(e);
//      }
//
//    }
//
//    private static SSLContext createContext() throws Exception{
//      SSLContext context = SSLContext.getInstance("TLS");
//      context.init(null, null, null);
//      return context;
//    }
//
//    private SSLContext getSslContext() throws Exception{
//      if (this.sslContext == null) {
//        this.sslContext = createContext();
//      }
//      return this.sslContext;
//    }

    @Override
    public String[] getDefaultCipherSuites() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String[] getSupportedCipherSuites() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port,
        boolean autoClose) throws IOException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException,
        UnknownHostException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost,
        int localPort) throws IOException, UnknownHostException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
        InetAddress localAddress, int localPort) throws IOException {
      // TODO Auto-generated method stub
      return null;
    }
  }

  private static class GullibleSslSocketFactory extends SSLSocketFactory {

    @Override
    public String[] getDefaultCipherSuites() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String[] getSupportedCipherSuites() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port,
        boolean autoClose) throws IOException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException,
        UnknownHostException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost,
        int localPort) throws IOException, UnknownHostException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
        InetAddress localAddress, int localPort) throws IOException {
      // TODO Auto-generated method stub
      return null;
    }

//    @Override
//    public Socket createSocket( Socket socket, String host, int port, boolean autoClose )
//        throws IOException, UnknownHostException {
//      SSLSocket socket1 = (SSLSocket) getSslContext().getSocketFactory().createSocket(socket, host, port,  autoClose);
//      return socket1;
//    }
//
//    public GullibleSslSocketFactory()
//        throws NoSuchAlgorithmException, KeyManagementException,
//        KeyStoreException, UnrecoverableKeyException {
//      super(null, null, null);
//      X509HostnameVerifier doNothingVerifier = new X509HostnameVerifier() {
//        @Override
//        public void verify( String host, SSLSocket ssl ) throws IOException {
//        }
//
//        @Override
//        public void verify( String host, X509Certificate cert ) throws SSLException {
//        }
//
//        @Override
//        public void verify( String host, String[] cns, String[] subjectAlts ) throws SSLException {
//        }
//
//        @Override
//        public boolean verify( String s, SSLSession sslSession ) {
//          return true;
//        }
//      };
//
//      this.setHostnameVerifier(doNothingVerifier);
//    }
//
//    @Override
//    public Socket createSocket() throws IOException {
//      return getSslContext().getSocketFactory().createSocket();
//    }
//
//    private static SSLContext createContext() throws Exception{
//      SSLContext context = SSLContext.getInstance("TLS");
//
//      TrustManager[] trustManager = new TrustManager[] { new X509TrustManager() {
//        @Override
//        public void checkClientTrusted(X509Certificate[] arg0, String arg1)
//            throws CertificateException {
//        }
//
//        @Override
//        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
//            throws CertificateException {
//        }
//
//        @Override
//        public X509Certificate[] getAcceptedIssuers() {
//          return new X509Certificate[] {};
//        }
//
//      }};
//
//      context.init(null, trustManager, null);
//      return context;
//    }
//
//    private SSLContext getSslContext() throws Exception{
//      if (this.sslContext == null) {
//        this.sslContext = createContext();
//      }
//      return this.sslContext;
//    }
  }
}
