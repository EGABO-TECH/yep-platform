package yep.model;

public class Credential {
    private final int credentialId;
    private final String title;
    private final String issuer;
    private final boolean verified;
    private final String certificateCode;

    public Credential(int credentialId, String title, String issuer, boolean verified, String certificateCode) {
        this.credentialId = credentialId;
        this.title = title;
        this.issuer = issuer;
        this.verified = verified;
        this.certificateCode = certificateCode;
    }

    public int getCredentialId() { return credentialId; }
    public String getTitle() { return title; }
    public String getIssuer() { return issuer; }
    public boolean isVerified() { return verified; }
    public String getCertificateCode() { return certificateCode; }
}
