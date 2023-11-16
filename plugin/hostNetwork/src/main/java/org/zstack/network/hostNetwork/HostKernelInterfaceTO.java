package org.zstack.network.hostNetwork;

import java.util.ArrayList;
import java.util.List;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.network.l3.UsedIpTO;

@PythonClass
public class HostKernelInterfaceTO {
    private String interfaceName;
    private int vlanId;
    private List<UsedIpTO> ips;

    public HostKernelInterfaceTO() {
        this.ips = new ArrayList<>();
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public int getVlanId() {
        return vlanId;
    }

    public void setVlanId(int vlanId) {
        this.vlanId = vlanId;
    }

    public List<UsedIpTO> getIps() {
        return ips;
    }

    public void setIps(List<UsedIpTO> ips) {
        this.ips = ips;
    }
}
