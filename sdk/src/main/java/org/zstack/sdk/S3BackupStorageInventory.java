package org.zstack.sdk;



public class S3BackupStorageInventory extends org.zstack.sdk.BackupStorageInventory {

    public java.lang.String region;
    public void setRegion(java.lang.String region) {
        this.region = region;
    }
    public java.lang.String getRegion() {
        return this.region;
    }

    public java.lang.String endpoint;
    public void setEndpoint(java.lang.String endpoint) {
        this.endpoint = endpoint;
    }
    public java.lang.String getEndpoint() {
        return this.endpoint;
    }

    public java.lang.String bucket;
    public void setBucket(java.lang.String bucket) {
        this.bucket = bucket;
    }
    public java.lang.String getBucket() {
        return this.bucket;
    }

    public java.lang.String akeyUuid;
    public void setAkeyUuid(java.lang.String akeyUuid) {
        this.akeyUuid = akeyUuid;
    }
    public java.lang.String getAkeyUuid() {
        return this.akeyUuid;
    }

    public boolean usePathStyle;
    public void setUsePathStyle(boolean usePathStyle) {
        this.usePathStyle = usePathStyle;
    }
    public boolean getUsePathStyle() {
        return this.usePathStyle;
    }

}