import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.SignatureException;

/*
 * @test
 * @summary Test skip ECDSA SignatureLength Verifying
 * @run main/othervm -Dcom.alibaba.jdk.verifyECDSATrailing=false SkipVerifySignatureLength
 */
public class SkipVerifySignatureLength {

    public static void main(String[] args) throws Exception {
        main0("EC", 256, "SHA256withECDSA", "SunEC");
        main0("DSA", 2048, "SHA256withDSA", "SUN");
    }

    private static void main0(String keyAlgorithm, int keysize,
            String signatureAlgorithm, String provider) throws Exception {
        byte[] plaintext = "aaa".getBytes("UTF-8");

        // Generate
        KeyPairGenerator generator =
            provider == null ?
                (KeyPairGenerator) KeyPairGenerator.getInstance(keyAlgorithm) :
                (KeyPairGenerator) KeyPairGenerator.getInstance(
                                       keyAlgorithm, provider);
        generator.initialize(keysize);
        System.out.println("Generating " + keyAlgorithm + " keypair using " +
            generator.getProvider().getName() + " JCE provider");
        KeyPair keypair = generator.generateKeyPair();

        // Sign
        Signature signer =
            provider == null ?
                Signature.getInstance(signatureAlgorithm) :
                Signature.getInstance(signatureAlgorithm, provider);
        signer.initSign(keypair.getPrivate());
        signer.update(plaintext);
        System.out.println("Signing using " + signer.getProvider().getName() +
            " JCE provider");
        byte[] signature = signer.sign();

        // Invalidate
        System.out.println("Invalidating signature ...");
        byte[] badSignature = new byte[signature.length + 5];
        System.arraycopy(signature, 0, badSignature, 0, signature.length);
        badSignature[signature.length] = 0x01;
        badSignature[signature.length + 1] = 0x01;
        badSignature[signature.length + 2] = 0x01;
        badSignature[signature.length + 3] = 0x01;
        badSignature[signature.length + 4] = 0x01;

        // Verify
        Signature verifier =
            provider == null ?
                Signature.getInstance(signatureAlgorithm) :
                Signature.getInstance(signatureAlgorithm, provider);
        verifier.initVerify(keypair.getPublic());
        verifier.update(plaintext);
        System.out.println("Verifying using " +
            verifier.getProvider().getName() + " JCE provider");


        System.out.println("Valid? " + verifier.verify(badSignature));
    }
}
