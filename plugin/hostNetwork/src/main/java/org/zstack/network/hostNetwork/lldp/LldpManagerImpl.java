package org.zstack.network.hostNetwork.lldp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.Platform;
import org.zstack.core.db.SQL;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.network.hostNetwork.*;
import org.zstack.network.hostNetwork.lldp.api.*;
import org.zstack.network.hostNetwork.lldp.entity.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class LldpManagerImpl extends AbstractService implements HostAfterConnectedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LldpManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public int getSyncLevel() {
        return super.getSyncLevel();
    }

    @MessageSafe
    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeHostNetworkInterfaceLldpModeMsg) {
            handle((APIChangeHostNetworkInterfaceLldpModeMsg) msg);
        } else if (msg instanceof APIGetHostNetworkInterfaceLldpMsg) {
            handle((APIGetHostNetworkInterfaceLldpMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIChangeHostNetworkInterfaceLldpModeMsg msg) {
        APIChangeHostNetworkInterfaceLldpModeEvent event = new APIChangeHostNetworkInterfaceLldpModeEvent(msg.getId());

        final LldpKvmAgentCommands.ChangeLldpModeCmd cmd = new LldpKvmAgentCommands.ChangeLldpModeCmd();
        List <HostNetworkInterfaceVO> interfaceVOS = Q.New(HostNetworkInterfaceVO.class)
                .in(HostNetworkInterfaceVO_.uuid, msg.getInterfaceUuids())
                .list();
        List<String> interfaceNames = interfaceVOS.stream().map(HostNetworkInterfaceVO::getInterfaceName).collect(Collectors.toList());
        String hostUuid = interfaceVOS.get(0).getHostUuid();
        cmd.setPhysicalInterfaceNames(interfaceNames);
        cmd.setMode(msg.getMode());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(LldpConstant.CHANGE_LLDP_MODE_PATH);
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                } else {
                    List<HostNetworkInterfaceLldpVO> lldpVOS = new ArrayList<>();
                    for (HostNetworkInterfaceVO interfaceVO : interfaceVOS) {
                        HostNetworkInterfaceLldpVO vo = Q.New(HostNetworkInterfaceLldpVO.class).eq(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceVO.getUuid()).find();
                        if (vo == null) {
                            vo = new HostNetworkInterfaceLldpVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setInterfaceUuid(interfaceVO.getUuid());
                            vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
                            vo.setLastOpDate(new Timestamp(System.currentTimeMillis()));
                            vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                        }
                        vo.setMode(msg.getMode());
                        lldpVOS.add(vo);
                    }
                    dbf.updateCollection(lldpVOS);
                    event.setInventories(HostNetworkInterfaceLldpInventory.valueOf(lldpVOS));
                }
                bus.publish(event);
            }
        });
    }

    private synchronized void syncHostNetworkInterfaceLldpInDb(String interfaceUuid, HostNetworkInterfaceLldpRefInventory inv) {
        if (inv == null) {
            return;
        }

        HostNetworkInterfaceLldpRefVO vo = Q.New(HostNetworkInterfaceLldpRefVO.class).eq(HostNetworkInterfaceLldpRefVO_.interfaceUuid, interfaceUuid).find();
        if (vo == null) {
            vo = new HostNetworkInterfaceLldpRefVO();
            vo.setInterfaceUuid(interfaceUuid);
            vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
            vo.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        }
        vo.setChassisId(inv.getChassisId());
        vo.setTimeToLive(inv.getTimeToLive());
        vo.setManagementAddress(inv.getManagementAddress());
        vo.setSystemName(inv.getSystemName());
        vo.setSystemDescription(inv.getSystemDescription());
        vo.setSystemCapabilities(inv.getSystemCapabilities());
        vo.setPortId(inv.getPortId());
        vo.setPortDescription(inv.getPortDescription());
        vo.setVlanId(inv.getVlanId());
        vo.setAggregationPortId(inv.getAggregationPortId());
        vo.setMtu(inv.getMtu());

        dbf.updateAndRefresh(vo);
    }

    private void handle(APIGetHostNetworkInterfaceLldpMsg msg) {
        APIGetHostNetworkInterfaceLldpReply greply = new APIGetHostNetworkInterfaceLldpReply();

        HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceVO.class);
        final LldpKvmAgentCommands.GetLldpInfoCmd cmd = new LldpKvmAgentCommands.GetLldpInfoCmd();
        cmd.setPhysicalInterfaceName(interfaceVO.getInterfaceName());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(LldpConstant.GET_LLDP_INFO_PATH);
        kmsg.setHostUuid(interfaceVO.getHostUuid());
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, interfaceVO.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    greply.setError(reply.getError());
                    bus.reply(msg, greply);
                    return;
                } else {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    LldpKvmAgentCommands.GetLldpInfoResponse rsp = r.toResponse(LldpKvmAgentCommands.GetLldpInfoResponse.class);
                    logger.debug(String.format("00000000000000000 reply : %s", r));
                    logger.debug(String.format("00000000000000000 rsp : %s", rsp.getLldpInfo()));
                    if (!rsp.isSuccess()) {
                        greply.setError(operr("operation error, because %s", rsp.getError()));
                    } else {
                        syncHostNetworkInterfaceLldpInDb(msg.getInterfaceUuid(), rsp.getLldpInfo());
                        HostNetworkInterfaceLldpRefVO lldpRefVO =  Q.New(HostNetworkInterfaceLldpRefVO.class)
                                .eq(HostNetworkInterfaceLldpRefVO_.interfaceUuid, msg.getInterfaceUuid())
                                .find();
                        greply.setLldp(lldpRefVO != null ? HostNetworkInterfaceLldpRefInventory.valueOf(lldpRefVO) : null);
                    }
                }
                bus.reply(msg, greply);
            }
        });
    }

    private void applyHostNetworkLldpConfig(List<HostNetworkInterfaceLldpVO> lldpVOS, Completion completion) {
        LldpKvmAgentCommands.ApplyLldpConfigCmd cmd = new LldpKvmAgentCommands.ApplyLldpConfigCmd();
        List<LldpConfigSyncStruct.LLdpModeConfig> configs = new ArrayList<>();
        for (HostNetworkInterfaceLldpVO lldpVO : lldpVOS) {
            LldpConfigSyncStruct.LLdpModeConfig config = new LldpConfigSyncStruct.LLdpModeConfig();
            HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(lldpVO.getInterfaceUuid(), HostNetworkInterfaceVO.class);
            config.setPhysicalInterfaceName(interfaceVO.getInterfaceName());
            config.setMode(lldpVO.getMode());
            configs.add(config);
        }
        cmd.setLldpConfig(configs);

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(LldpConstant.APPLY_LLDP_CONFIG_PATH);
        String hostUuid = Q.New(HostNetworkInterfaceVO.class)
                .select(HostNetworkInterfaceVO_.hostUuid)
                .eq(HostNetworkInterfaceVO_.uuid, lldpVOS.get(0).getInterfaceUuid())
                .findValue();
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        logger.debug(String.format("00000000000 host connected :%s", host.getUuid()));
        List<String> interfaceUuids = Q.New(HostNetworkInterfaceLldpVO.class)
                .select(HostNetworkInterfaceLldpVO_.interfaceUuid)
                .listValues();

        if (interfaceUuids != null && !interfaceUuids.isEmpty()) {
            interfaceUuids = Q.New(HostNetworkInterfaceVO.class)
                    .select(HostNetworkInterfaceVO_.uuid)
                    .eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                    .notIn(HostNetworkInterfaceVO_.uuid, interfaceUuids)
                    .listValues();
        } else {
            interfaceUuids = Q.New(HostNetworkInterfaceVO.class)
                    .select(HostNetworkInterfaceVO_.uuid)
                    .eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                    .listValues();
        }

        logger.debug(String.format("11111111111111 :%s", interfaceUuids));
        List<HostNetworkInterfaceLldpVO> lldpVOS = new ArrayList<>();
        for (String interfaceUuid : interfaceUuids) {
            HostNetworkInterfaceLldpVO vo = new HostNetworkInterfaceLldpVO();
            vo.setUuid(Platform.getUuid());
            vo.setInterfaceUuid(interfaceUuid);
            vo.setMode(LldpConstant.mode.rx_only.toString());
            vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
            vo.setLastOpDate(new Timestamp(System.currentTimeMillis()));
            vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
            lldpVOS.add(vo);
        }
        dbf.updateCollection(lldpVOS);

        lldpVOS = Q.New(HostNetworkInterfaceLldpVO.class).list();
        if (lldpVOS == null || lldpVOS.isEmpty()) {
            return;
        }
        logger.debug(String.format("222222222222222 :%s", lldpVOS));

        applyHostNetworkLldpConfig(lldpVOS, new Completion(null) {
            @Override
            public void success() {
                logger.debug("apply the lldp configuration after host reconnected successfully");
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("fail to apply the lldp configuration after host reconnected:%s", errorCode.toString()));
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(LldpConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

}
