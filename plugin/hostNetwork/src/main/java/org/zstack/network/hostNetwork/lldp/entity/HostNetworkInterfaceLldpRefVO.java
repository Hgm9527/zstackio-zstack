package org.zstack.network.hostNetwork.lldp.entity;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.*;
import org.zstack.network.hostNetwork.HostNetworkInterfaceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostNetworkInterfaceVO.class, joinColumn = "interfaceUuid")
})
//@EntityGraph(
//        parents = {
//                @EntityGraph.Neighbour(type = HostNetworkInterfaceLldpVO.class, myField = "interfaceUuid", targetField = "interfaceUuid")
//        }
//)
public class HostNetworkInterfaceLldpRefVO implements ToInventory {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @Index
    @ForeignKey(parentEntityClass = HostNetworkInterfaceLldpVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String interfaceUuid;

    @Column
    private String chassisId;

    @Column
    private Integer timeToLive;

    @Column
    private String managementAddress;

    @Column
    private String systemName;

    @Column
    private String systemDescription;

    @Column
    private String systemCapabilities;

    @Column
    private String portId;

    @Column
    private String portDescription;

    @Column
    private Integer vlanId;

    @Column
    private Long aggregationPortId;

    @Column
    private Integer mtu;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public String getChassisId() {
        return chassisId;
    }

    public void setChassisId(String chassisId) {
        this.chassisId = chassisId;
    }

    public Integer getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Integer timeToLive) {
        this.timeToLive = timeToLive;
    }

    public String getManagementAddress() {
        return managementAddress;
    }

    public void setManagementAddress(String managementAddress) {
        this.managementAddress = managementAddress;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemDescription() {
        return systemDescription;
    }

    public void setSystemDescription(String systemDescription) {
        this.systemDescription = systemDescription;
    }

    public String getSystemCapabilities() {
        return systemCapabilities;
    }

    public void setSystemCapabilities(String systemCapabilities) {
        this.systemCapabilities = systemCapabilities;
    }

    public String getPortId() {
        return portId;
    }

    public void setPortId(String portId) {
        this.portId = portId;
    }

    public String getPortDescription() {
        return portDescription;
    }

    public void setPortDescription(String portDescription) {
        this.portDescription = portDescription;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public Long getAggregationPortId() {
        return aggregationPortId;
    }

    public void setAggregationPortId(Long aggregationPortId) {
        this.aggregationPortId = aggregationPortId;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }
}
