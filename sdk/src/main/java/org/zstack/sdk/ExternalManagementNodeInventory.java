package org.zstack.sdk;

import org.zstack.sdk.ExternalManagementNodeStatus;
import org.zstack.sdk.ExternalManagementNodeDirection;

public class ExternalManagementNodeInventory  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String hostName;
    public void setHostName(java.lang.String hostName) {
        this.hostName = hostName;
    }
    public java.lang.String getHostName() {
        return this.hostName;
    }

    public java.lang.Integer port;
    public void setPort(java.lang.Integer port) {
        this.port = port;
    }
    public java.lang.Integer getPort() {
        return this.port;
    }

    public java.lang.String accessKeyId;
    public void setAccessKeyId(java.lang.String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }
    public java.lang.String getAccessKeyId() {
        return this.accessKeyId;
    }

    public java.lang.String accessKeySecret;
    public void setAccessKeySecret(java.lang.String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }
    public java.lang.String getAccessKeySecret() {
        return this.accessKeySecret;
    }

    public ExternalManagementNodeStatus status;
    public void setStatus(ExternalManagementNodeStatus status) {
        this.status = status;
    }
    public ExternalManagementNodeStatus getStatus() {
        return this.status;
    }

    public ExternalManagementNodeDirection direction;
    public void setDirection(ExternalManagementNodeDirection direction) {
        this.direction = direction;
    }
    public ExternalManagementNodeDirection getDirection() {
        return this.direction;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
