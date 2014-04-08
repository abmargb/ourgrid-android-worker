package org.ourgrid.androidworker;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.x509.X509V3CertificateGenerator;

import sun.security.provider.certpath.X509CertPath;
import br.edu.ufcg.lsd.commune.context.ModuleContext;
import br.edu.ufcg.lsd.commune.network.certification.providers.CertificationDataProvider;
import br.edu.ufcg.lsd.commune.network.signature.SignatureConstants;
import br.edu.ufcg.lsd.commune.network.signature.SignatureProperties;
import br.edu.ufcg.lsd.commune.network.signature.Util;
import br.edu.ufcg.lsd.commune.network.xmpp.XMPPProperties;

public class AndroidCertificationDataProvider implements CertificationDataProvider {

	private X509CertPath myCertPath;
	private ModuleContext context;

	private static long VALIDITY_INTERVAL = 1000L * 60 * 60 * 24 * 365; //one year
	
	public AndroidCertificationDataProvider(ModuleContext context) throws CertificateException, IOException {
		this.context = context;
		this.myCertPath = generateMyCertPath();
	}

	private X509CertPath generateMyCertPath() throws CertificateException, IOException {
		try {
			return (X509CertPath) generateListCertPath();
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see br.edu.ufcg.lsd.commune.network.certification.providers.CertificationDataProvider#getMyCertificateChain()
	 */
	public X509CertPath getMyCertificatePath(){
		return this.myCertPath;
	}

	/**
	 * @param certFile
	 * @return
	 * @throws CertificateException
	 * @throws IOException
	 * @throws InvalidKeySpecException 
	 */
	private CertPath generateListCertPath() throws CertificateException, IOException, InvalidKeySpecException {
		
		X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();

		KeyPair keyPair = new KeyPair(
				Util.decodePublicKey(context.getProperty(SignatureProperties.PROP_PUBLIC_KEY)), 
				Util.decodePrivateKey(context.getProperty(SignatureProperties.PROP_PRIVATE_KEY)));
		
		String dn = "CN=" + context.getProperty(XMPPProperties.PROP_USERNAME) + 
				",OU=" + context.getProperty(XMPPProperties.PROP_XMPP_SERVERNAME);
		
		certGenerator.setSerialNumber(BigInteger.valueOf(Math.abs(new Random().nextLong())));
		certGenerator.setPublicKey(keyPair.getPublic());
		certGenerator.setSubjectDN(new X500Principal(dn));
		certGenerator.setIssuerDN(new X500Principal(dn));
		certGenerator.setNotBefore(new Date(System.currentTimeMillis()
				- VALIDITY_INTERVAL));
		certGenerator.setNotAfter(new Date(System.currentTimeMillis()
				+ VALIDITY_INTERVAL));
		certGenerator.setSignatureAlgorithm(SignatureConstants.SIGN_ALGORITHM);

		X509Certificate certificate = null;
		try {
			certificate = certGenerator.generate(keyPair.getPrivate());
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		
		List<X509Certificate> path = new LinkedList<X509Certificate>();
		path.add(certificate);
		
		return new X509CertPath(path);
	}
	
}
