package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.logging.CLogger;

import org.apache.commons.lang.StringUtils;

import javax.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.Arrays;

import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

/**
 */
public class SecurityGroupApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSecurityGroupRuleMsg) {
            validate((APIAddSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIAddVmNicToSecurityGroupMsg) {
            validate((APIAddVmNicToSecurityGroupMsg) msg);
        } else if (msg instanceof APIAttachSecurityGroupToL3NetworkMsg) {
            validate((APIAttachSecurityGroupToL3NetworkMsg) msg);
        } else if (msg instanceof APIDeleteSecurityGroupMsg) {
            validate((APIDeleteSecurityGroupMsg) msg);
        } else if (msg instanceof APIDeleteSecurityGroupRuleMsg) {
            validate((APIDeleteSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIDeleteVmNicFromSecurityGroupMsg) {
            validate((APIDeleteVmNicFromSecurityGroupMsg) msg);
        } else if (msg instanceof APIDetachSecurityGroupFromL3NetworkMsg) {
            validate((APIDetachSecurityGroupFromL3NetworkMsg) msg);
        } if (msg instanceof APICreateSecurityGroupMsg) {
            validate((APICreateSecurityGroupMsg) msg);
        } else if (msg instanceof APIChangeSecurityGroupRuleMsg) {
            validate((APIChangeSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIUpdateSecurityGroupRulePriorityMsg) {
            validate((APIUpdateSecurityGroupRulePriorityMsg) msg);
        } else if (msg instanceof APIChangeVmNicSecurityPolicyMsg) {
            validate((APIChangeVmNicSecurityPolicyMsg) msg);
        } else if (msg instanceof APIChangeSecurityGroupRuleStateMsg) {
            validate((APIChangeSecurityGroupRuleStateMsg) msg);
        } else if (msg instanceof APISetVmNicSecurityGroupMsg) {
            validate((APISetVmNicSecurityGroupMsg) msg);
        } else if (msg instanceof APIValidateSecurityGroupRuleMsg) {
            validate((APIValidateSecurityGroupRuleMsg) msg);
        }

        return msg;
    }

    private void validate(APIValidateSecurityGroupRuleMsg msg) {
        if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, msg.getSecurityGroupUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("invalid security group rule, because security group[uuid:%s] not found", msg.getSecurityGroupUuid()));
        }

        if (msg.getRemoteSecurityGroupUuid() != null) {
            if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, msg.getRemoteSecurityGroupUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("invalid security group rule, because remote security group[uuid:%s] not found", msg.getRemoteSecurityGroupUuid()));
            }
        }

        if (msg.getIpVersion() == null) {
            msg.setIpVersion(IPv6Constants.IPv4);
        }
        if (msg.getAction() == null) {
            msg.setAction(SecurityGroupRuleAction.ACCEPT.toString());
        }

        if (msg.getAllowedCidr() == null) {
            msg.setAllowedCidr(msg.getIpVersion() == IPv6Constants.IPv4 ? SecurityGroupConstant.WORLD_OPEN_CIDR : SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6);
        } else {
            validateIps(msg.getAllowedCidr(), msg.getIpVersion());
        }

        if (msg.getStartPort() == null) {
            msg.setStartPort(-1);
        }

        if (msg.getEndPort() == null) {
            msg.setEndPort(-1);
        }

        if (msg.getSrcIpRange() != null) {
            validateIps(msg.getSrcIpRange(), msg.getIpVersion());
        }

        if (msg.getDstIpRange() != null) {
            validateIps(msg.getDstIpRange(), msg.getIpVersion());
        }

        if (msg.getDstPortRange() != null) {
            validatePorts(msg.getDstPortRange());
        }

        if (SecurityGroupRuleProtocolType.ALL.toString().equals(msg.getProtocol()) || SecurityGroupRuleProtocolType.ICMP.toString().equals(msg.getProtocol())) {
            if (msg.getStartPort() != -1 || msg.getEndPort() != -1) {
                throw new ApiMessageInterceptionException(argerr("invalid security group rule, because startPort and endPort must be -1 when protocol is ALL or ICMP"));
            }
        } else {
            if (msg.getStartPort() > msg.getEndPort()) {
                throw new ApiMessageInterceptionException(argerr("invalid security group rule, because invalid endPort[%d], endPort must be greater than or equal to startPort[%d]", msg.getEndPort(), msg.getStartPort()));
            }
            if (msg.getStartPort() > 65535) {
                throw new ApiMessageInterceptionException(argerr("invalid security group rule, because startPort[%d] must less than 65535 when protocol is[%s]", msg.getStartPort(), msg.getProtocol()));
            }
        }
    }

    private void validate(APISetVmNicSecurityGroupMsg msg) {
        if (!Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because vm nic[uuid:%s] not found", msg.getVmNicUuid()));
        }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, msg.getVmNicUuid()).list();

        if (msg.getRefs().isEmpty() && refs.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because the vm nic[uuid:%s] not attached to any security group", msg.getVmNicUuid()));
        }

        Map<Integer, String> aoMap = new HashMap<Integer, String>();
        for (APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO ao : msg.getRefs()) {

            if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, ao.getSecurityGroupUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because security group[uuid:%s] not found", ao.getSecurityGroupUuid()));
            }

            Integer priority = ao.getPriority();
            if (priority < 1) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because invalid priority, priority[%d] cannot be less than 1", priority));
            }
            
            if (aoMap.containsKey(priority)) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because duplicate priority, both security group %s and %s have priority[%d]", aoMap.get(priority), ao.getSecurityGroupUuid(), priority));
            }
            if (aoMap.containsValue(ao.getSecurityGroupUuid())) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because duplicate security group[uuid:%s]", ao.getSecurityGroupUuid()));
            }
            aoMap.put(priority, ao.getSecurityGroupUuid());

            if (!refs.stream().anyMatch(r -> r.getSecurityGroupUuid().equals(ao.getSecurityGroupUuid()))) {
                checkIfVmNicFromAttachedL3Networks(ao.getSecurityGroupUuid(), asList(msg.getVmNicUuid()));
            }
        }
        if (!aoMap.isEmpty()) {
            Integer[] priorities = aoMap.keySet().toArray(new Integer[aoMap.size()]);
            Arrays.sort(priorities);
            if (priorities[0] != 1) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because invalid priority, priority expects to start at 1, but [%d]", priorities[0]));
            }
            for (int i = 0; i < priorities.length - 1; i++) {
                if (priorities[i] + 1 != priorities[i + 1]) {
                    throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because invalid priority, priority[%d] and priority[%d] expected to be consecutive", priorities[i], priorities[i + 1]));
                }
            }
        }
    }

    private void validate(APIChangeSecurityGroupRuleStateMsg msg) {
        if (msg.getRuleUuids().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could no change security group rule state, because ruleUuids is empty"));
        }

        if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, msg.getSecurityGroupUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("could no change security group rule state, because security group[uuid:%s] not found", msg.getSecurityGroupUuid()));
        }

        List<String> toChange = new ArrayList<>();
        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, msg.getSecurityGroupUuid()).in(SecurityGroupRuleVO_.uuid, msg.getRuleUuids()).list();
        msg.getRuleUuids().stream().forEach(r -> {
            SecurityGroupRuleVO vo = rvos.stream().filter(rvo -> rvo.getUuid().equals(r)).findAny().get();
            if (vo == null) {
                throw new ApiMessageInterceptionException(argerr("could no change security group rule state, because security group rule[uuid:%s] not found", r));
            }

            if (!vo.getState().toString().equals(msg.getState())) {
                toChange.add(r);
            }
        });

        if (toChange.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could no change security group rule state, because no security group rule state need to change"));
        } else {
            msg.setRuleUuids(toChange);
        }
    }

    private void validate(APIChangeVmNicSecurityPolicyMsg msg) {
        if (msg.getIngressPolicy() == null && msg.getEgressPolicy() == null) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because ingress policy and egress policy cannot be both null"));
        }
        if (msg.getIngressPolicy() != null && !VmNicSecurityPolicy.isValid(msg.getIngressPolicy())) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because invalid ingress policy[%s]", msg.getIngressPolicy()));
        }

        if (msg.getEgressPolicy() != null && !VmNicSecurityPolicy.isValid(msg.getEgressPolicy())) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because invalid egress policy[%s]", msg.getEgressPolicy()));
        }

        if (!Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because vm nic[uuid:%s] not found", msg.getVmNicUuid()));
        }

        VmNicSecurityPolicyVO policy = Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, msg.getVmNicUuid()).find();
        if (policy == null) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because vm nic[uuid:%s] has no security policy", msg.getVmNicUuid()));
        }

        if (policy.getIngressPolicy().equals(msg.getIngressPolicy()) && policy.getEgressPolicy().equals(msg.getEgressPolicy())) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because the security policy of vm nic[uuid:%s] has not changed, ingressPolicy[%s], egressPolicy[%s]", msg.getVmNicUuid(), msg.getIngressPolicy(), msg.getEgressPolicy()));
        }
        
        if (policy.getIngressPolicy().equals(msg.getIngressPolicy())) {
            msg.setIngressPolicy(null);
        }
        
        if (policy.getEgressPolicy().equals(msg.getEgressPolicy())) {
            msg.setEgressPolicy(null);
        }
    }

    private void validate(APIUpdateSecurityGroupRulePriorityMsg msg) {
        if (!SecurityGroupRuleType.isValid(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because invalid type[%s]", msg.getType()));
        }

        SecurityGroupVO sgvo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
        if (sgvo == null) {
            throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because security group[uuid:%s] is not exist", msg.getSecurityGroupUuid()));
        }

        if (msg.getRules().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because rules is empty"));
        }

        HashMap<Integer, String> map = new HashMap<Integer, String>();
        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, msg.getSecurityGroupUuid())
                .eq(SecurityGroupRuleVO_.type, msg.getType())
                .notEq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY)
                .list();
        if (rvos.size() != msg.getRules().size()) {
            throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because security group[uuid:%s] rules size not match", msg.getSecurityGroupUuid()));
        }

        for (APIUpdateSecurityGroupRulePriorityMsg.SecurityGroupRulePriorityAO ao : msg.getRules()) {
            if (ao.getPriority() == SecurityGroupConstant.DEFAULT_RULE_PRIORITY) {
                throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because rule priority[%d] is invalid", ao.getPriority()));
            }
            if (map.containsKey(ao.getPriority())) {
                throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because priority[%d] has duplicate", ao.getPriority()));
            } else {
                map.put(ao.getPriority(), ao.getRuleUuid());
            }

            rvos.stream().filter(rvo -> rvo.getUuid().equals(ao.getRuleUuid())).findFirst().orElseThrow(() ->
                    new ApiMessageInterceptionException(argerr("could not update security group rule priority, because rule[uuid:%s] not in security group[uuid:%s]", ao.getRuleUuid(), msg.getSecurityGroupUuid())));

            rvos.stream().filter(rvo -> rvo.getPriority() == ao.getPriority()).findFirst().orElseThrow(() ->
                    new ApiMessageInterceptionException(argerr("could not update security group rule priority, because priority[%d] not in security group[uuid:%s]", ao.getPriority(), msg.getSecurityGroupUuid())));
        }
    }

    private void validate(APIChangeSecurityGroupRuleMsg msg) {
        if (msg.getState() != null && !SecurityGroupRuleState.isValid(msg.getState())) {
            throw new ApiMessageInterceptionException(argerr("could not change security group rule, because invalid state[%s]", msg.getState()));
        }

        if (msg.getAction() != null && !SecurityGroupRuleAction.isValid(msg.getAction())) {
            throw new ApiMessageInterceptionException(argerr("could not change security group rule, because invalid action[%s]", msg.getAction()));
        }

        if (msg.getProtocol() != null && !SecurityGroupRuleProtocolType.isValid(msg.getProtocol())) {
            throw new ApiMessageInterceptionException(argerr("could not change security group rule, because invalid protocol[%s]", msg.getProtocol()));
        }

        SecurityGroupRuleVO vo = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.uuid, msg.getUuid()).find();
        if (vo == null) {
            throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule uuid[%s] is not exist", msg.getUuid()));
        }

        if (vo.getPriority() == 0) {
            if (msg.getProtocol() != null || msg.getAction() != null || msg.getRemoteSecurityGroupUuid() != null || msg.getSrcIpRange() != null
                || msg.getDstIpRange() != null || msg.getDstPortRange() != null || msg.getPriority() != null) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] is default rule, only the description and status can be set", msg.getUuid()));
                }
        }

        if (SecurityGroupRuleType.Ingress.equals(vo.getType()) && !StringUtils.isEmpty(msg.getDstIpRange())) {
            throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] type is Ingress, dstIpRange cannot be set", msg.getUuid()));
        }

        if (SecurityGroupRuleType.Egress.equals(vo.getType()) && !StringUtils.isEmpty(msg.getSrcIpRange())) {
            throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] type is Egress, srcIpRange cannot be set", msg.getUuid()));
        }

        Integer priority = msg.getPriority();
        if (priority != null) {
            if (priority == SecurityGroupConstant.DEFAULT_RULE_PRIORITY) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] priority cannot be set to default value[%d]", msg.getUuid(), SecurityGroupConstant.DEFAULT_RULE_PRIORITY));
            }

            Long count = Q.New(SecurityGroupRuleVO.class)
                    .eq(SecurityGroupRuleVO_.securityGroupUuid, vo.getSecurityGroupUuid())
                    .eq(SecurityGroupRuleVO_.type, vo.getType())
                    .notEq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY)
                    .count();
            if (count.intValue() > SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group %s rules number[%d] is out of max limit[%d]", vo.getType(), count.intValue(), SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)));
            }
            if (priority > count.intValue()) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because the maximum priority of the current rule is [%d]", count.intValue()));
            }
            if (priority < 0) {
                msg.setPriority(SecurityGroupConstant.LOWEST_RULE_PRIORITY);
            }
        }

        if (msg.getRemoteSecurityGroupUuid() != null) {
            if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, msg.getRemoteSecurityGroupUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because remote security group[uuid:%s] not exist", msg.getRemoteSecurityGroupUuid()));
            }
            if (msg.getSrcIpRange() != null || msg.getDstIpRange() != null) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because remote security group[uuid:%s] is set, srcIpRange and dstIpRange cannot be set", msg.getRemoteSecurityGroupUuid()));
            }
        }

        if (msg.getSrcIpRange() != null && msg.getDstIpRange() != null) {
            throw new ApiMessageInterceptionException(argerr("could not change security group rule, because srcIpRange and dstIpRange cannot be set at the same time"));
        } else if (msg.getSrcIpRange() != null) {
            validateIps(msg.getSrcIpRange(), vo.getIpVersion());
        } else if (msg.getDstIpRange() != null) {
            validateIps(msg.getDstIpRange(), vo.getIpVersion());
        }

        if (msg.getDstPortRange() != null) {
            if (msg.getProtocol() != null) {
                if (msg.getProtocol().equals(SecurityGroupRuleProtocolType.ICMP.toString()) || msg.getProtocol().equals(SecurityGroupRuleProtocolType.ALL.toString())) {
                    throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] protocol is [%s], dstPortRange cannot be set", msg.getUuid(), msg.getProtocol()));
                } else {
                    validatePorts(msg.getDstPortRange());
                }
            } else {
                if (vo.getProtocol().equals(SecurityGroupRuleProtocolType.ICMP) || vo.getProtocol().equals(SecurityGroupRuleProtocolType.ALL)) {
                    throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] protocol is ICMP or ALL, dstPortRange cannot be set", msg.getUuid()));
                } else {
                    validatePorts(msg.getDstPortRange());
                }
            }
        } else {
            if (msg.getProtocol() != null) {
                if (msg.getProtocol().equals(SecurityGroupRuleProtocolType.TCP.toString()) || msg.getProtocol().equals(SecurityGroupRuleProtocolType.UDP.toString())) {
                    if (vo.getProtocol().equals(SecurityGroupRuleProtocolType.ICMP) || vo.getProtocol().equals(SecurityGroupRuleProtocolType.ALL)) {
                        throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] protocol is [%s], cannot be set to [%s]", msg.getUuid(), vo.getProtocol(), msg.getProtocol()));
                    }
                }
            }
        }
    }

    private void validatePorts(String ports) {
        String portArray[];
        if (ports.contains(SecurityGroupConstant.IP_SPLIT)) {
            String[] tmpPorts = ports.split(String.format("%s|%s", SecurityGroupConstant.IP_SPLIT, SecurityGroupConstant.RANGE_SPLIT));
            if (tmpPorts.length > SecurityGroupConstant.PORT_GROUP_NUMBER_LIMIT) {
                throw new ApiMessageInterceptionException(argerr("invalid ports[%s], port range[%s] number[%d] is out of max limit[%d]", ports, Arrays.toString(tmpPorts), tmpPorts.length, SecurityGroupConstant.PORT_GROUP_NUMBER_LIMIT));
            }

            portArray = ports.split(SecurityGroupConstant.IP_SPLIT);
            Stream<String> stream = Stream.of(portArray).distinct();
            if (portArray.length != stream.count()) {
                throw new ApiMessageInterceptionException(argerr("invalid ports[%s], port duplicate", ports));
            }
        } else {
            portArray = new String[]{ports};
        }

        for (String port : portArray) {
            if (port.contains(SecurityGroupConstant.RANGE_SPLIT)) {
                String portRange[] = port.split(SecurityGroupConstant.RANGE_SPLIT);
                if (portRange.length != 2) {
                    throw new ApiMessageInterceptionException(argerr("invalid port range[%s]", port));
                }

                try {
                    Integer startPort = Integer.valueOf(portRange[0]);
                    Integer endPort = Integer.valueOf(portRange[1]);
                    if (startPort > endPort || startPort < SecurityGroupConstant.PORT_NUMBER_MIN
                        || endPort > SecurityGroupConstant.PORT_NUMBER_MAX) {
                        throw new ApiMessageInterceptionException(argerr("invalid port range[%s]", port));
                    }
                } catch (NumberFormatException e) {
                    throw new ApiMessageInterceptionException(argerr("invalid port range[%s]", port));
                }
            } else {
                try {
                    Integer.valueOf(port);
                    if (Integer.valueOf(port) < SecurityGroupConstant.PORT_NUMBER_MIN
                        || Integer.valueOf(port) > SecurityGroupConstant.PORT_NUMBER_MAX) {
                        throw new ApiMessageInterceptionException(argerr("invalid port[%s]", port));
                    }
                } catch (NumberFormatException e) {
                    throw new ApiMessageInterceptionException(argerr("invalid port[%s]", port));
                }
            }
        }
    }

    private void validateIps(String ips, Integer ipVersion) {
        String ipArray[];
        if (ips.contains(SecurityGroupConstant.IP_SPLIT)) {
            ipArray = ips.split(SecurityGroupConstant.IP_SPLIT);
            if (ipArray.length > SecurityGroupConstant.IP_GROUP_NUMBER_LIMIT) {
                throw new ApiMessageInterceptionException(argerr("invalid ips[%s], ip number[%d] is out of max limit[%d]", ips, ipArray.length, SecurityGroupConstant.IP_GROUP_NUMBER_LIMIT));
            }
            Stream<String> stream = Stream.of(ipArray).distinct();
            if (ipArray.length != stream.count()) {
                throw new ApiMessageInterceptionException(argerr("invalid ips[%s], ip duplicate", ips));
            }
        } else {
            ipArray = new String[]{ips};
        }

        for (String ip : ipArray) {
            if (ip.contains(SecurityGroupConstant.CIDR_SPLIT)) {
                if (!NetworkUtils.isCidr(ip)) {
                    throw new ApiMessageInterceptionException(argerr("invalid cidr[%s]", ip));
                }
                continue;
            }
            if (ip.contains(SecurityGroupConstant.RANGE_SPLIT)) {
                String[] ipRangeArray = ip.split(SecurityGroupConstant.RANGE_SPLIT);
                if (ipRangeArray.length != 2) {
                    throw new ApiMessageInterceptionException(argerr("invalid ip range[%s]", ip));
                }
                String startIp = ipRangeArray[0];
                String endIp = ipRangeArray[1];
                if (ipVersion == IPv6Constants.IPv4) {
                    NetworkUtils.validateIpRange(startIp, endIp);
                } else {
                    if (!IPv6NetworkUtils.isIpv6Address(startIp) || !IPv6NetworkUtils.isIpv6Address(endIp) || startIp.compareTo(endIp) > 0) {
                        throw new ApiMessageInterceptionException(argerr("invalid ip range[%s]", ip));
                    }
                }
                continue;
            }
            if (!NetworkUtils.isIpAddress(ip)) {
                throw new ApiMessageInterceptionException(argerr("invalid ip[%s]", ip));
            }
        }
    }

    private void validate(APIDetachSecurityGroupFromL3NetworkMsg msg) {
        SimpleQuery<SecurityGroupL3NetworkRefVO> q = dbf.createQuery(SecurityGroupL3NetworkRefVO.class);
        q.add(SecurityGroupL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(SecurityGroupL3NetworkRefVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(operr("security group[uuid:%s] has not attached to l3Network[uuid:%s], can't detach",
                            msg.getSecurityGroupUuid(), msg.getL3NetworkUuid()));
        }
    }

    private void validate(APIDeleteVmNicFromSecurityGroupMsg msg) {
        SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        q.select(VmNicSecurityGroupRefVO_.vmNicUuid);
        q.add(VmNicSecurityGroupRefVO_.vmNicUuid, Op.IN, msg.getVmNicUuids());
        q.add(VmNicSecurityGroupRefVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        List<String> vmNicUuids = q.listValue();
        if (vmNicUuids.isEmpty()) {
            APIDeleteVmNicFromSecurityGroupEvent evt = new APIDeleteVmNicFromSecurityGroupEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        msg.setVmNicUuids(vmNicUuids);
    }

    private void validate(APIDeleteSecurityGroupRuleMsg msg) {
        SimpleQuery<SecurityGroupRuleVO> q = dbf.createQuery(SecurityGroupRuleVO.class);
        q.select(SecurityGroupRuleVO_.uuid);
        q.add(SecurityGroupRuleVO_.uuid, Op.IN, msg.getRuleUuids());
        List<String> uuids = q.listValue();
        uuids.retainAll(msg.getRuleUuids());
        if (uuids.isEmpty()) {
            APIDeleteSecurityGroupRuleEvent evt = new APIDeleteSecurityGroupRuleEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        List<SecurityGroupRuleVO> vos = Q.New(SecurityGroupRuleVO.class).in(SecurityGroupRuleVO_.uuid, uuids).list();
        String sguuid = vos.get(0).getSecurityGroupUuid();
        vos.stream().forEach(vo -> {
            if (!sguuid.equals(vo.getSecurityGroupUuid())) {
                throw new ApiMessageInterceptionException(argerr("can't delete rules of different security group"));
            }
            if (vo.getPriority() == SecurityGroupConstant.DEFAULT_RULE_PRIORITY) {
                throw new ApiMessageInterceptionException(argerr("can't delete default rule[uuid:%s]", vo.getUuid()));
            }
        });

        msg.setRuleUuids(uuids);
    }

    private void validate(APIDeleteSecurityGroupMsg msg) {
        if (!dbf.isExist(msg.getUuid(), SecurityGroupVO.class)) {
            APIDeleteSecurityGroupEvent evt = new APIDeleteSecurityGroupEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIAttachSecurityGroupToL3NetworkMsg msg) {
        SimpleQuery<SecurityGroupL3NetworkRefVO> q = dbf.createQuery(SecurityGroupL3NetworkRefVO.class);
        q.add(SecurityGroupL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(SecurityGroupL3NetworkRefVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("security group[uuid:%s] has attached to l3Network[uuid:%s], can't attach again",
                            msg.getSecurityGroupUuid(), msg.getL3NetworkUuid()));
        }

        SimpleQuery<NetworkServiceL3NetworkRefVO> nq = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        nq.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        nq.add(NetworkServiceL3NetworkRefVO_.networkServiceType, Op.EQ, SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE);
        if (!nq.isExists()) {
            throw new ApiMessageInterceptionException(argerr("the L3 network[uuid:%s] doesn't have the network service type[%s] enabled", msg.getL3NetworkUuid(), SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE));
        }
    }

    private void validate(APIAddVmNicToSecurityGroupMsg msg) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.select(VmNicVO_.uuid);
        q.add(VmNicVO_.uuid, Op.IN, msg.getVmNicUuids());
        List<String> uuids = q.listValue();
        if (!uuids.containsAll(msg.getVmNicUuids())) {
            msg.getVmNicUuids().removeAll(uuids);
            throw new ApiMessageInterceptionException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find vm nics[uuids:%s]", msg.getVmNicUuids()
            ));
        }

        checkIfVmNicFromAttachedL3Networks(msg.getSecurityGroupUuid(), uuids);

        msg.setVmNicUuids(uuids);
    }

    @Transactional(readOnly = true)
    private void checkIfVmNicFromAttachedL3Networks(String securityGroupUuid, List<String> uuids) {
        String sql = "select nic.uuid from SecurityGroupL3NetworkRefVO ref, VmNicVO nic, UsedIpVO ip where ref.l3NetworkUuid = ip.l3NetworkUuid and ip.vmNicUuid = nic.uuid " +
                " and ref.securityGroupUuid = :sgUuid and nic.uuid in (:nicUuids)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nicUuids", uuids);
        q.setParameter("sgUuid", securityGroupUuid);
        List<String> nicUuids = q.getResultList();

        List<String> wrongUuids = new ArrayList<String>();
        for (String uuid : uuids) {
            if (!nicUuids.contains(uuid)) {
                wrongUuids.add(uuid);
            }
        }

        if (!wrongUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("VM nics[uuids:%s] are not on L3 networks that have been attached to the security group[uuid:%s]",
                            wrongUuids, securityGroupUuid));
        }
    }

    private boolean checkAllowedCidrValid(String cidr) {
        if (StringUtils.isEmpty(cidr)) {
            return false;
        }

        if (SecurityGroupConstant.WORLD_OPEN_CIDR.equals(cidr) || SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6.equals(cidr)) {
            return false;
        }

        return true;
    }

    private void validate(APIAddSecurityGroupRuleMsg msg) {
        String sgUuid = msg.getSecurityGroupUuid();
        List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> rules = msg.getRules();

        if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, sgUuid).isExists()) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because security group[uuid:%s] does not exist", sgUuid));
        }
        if (rules.isEmpty() || rules.size() > SecurityGroupConstant.ONE_API_RULES_MAX_NUM) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the rules cannot be empty or exceed the max number %d",  SecurityGroupConstant.ONE_API_RULES_MAX_NUM));
        }

        if (msg.getRemoteSecurityGroupUuids() != null && !msg.getRemoteSecurityGroupUuids().isEmpty()) {
            if (msg.getRemoteSecurityGroupUuids().stream().distinct().count() != msg.getRemoteSecurityGroupUuids().size()) {
                throw new ApiMessageInterceptionException(argerr("could not add security group rule, because duplicate uuid in remoteSecurityGroupUuids: %s", msg.getRemoteSecurityGroupUuids()));
            }
            
            List<String> sgUuids = Q.New(SecurityGroupVO.class).select(SecurityGroupVO_.uuid).in(SecurityGroupVO_.uuid, msg.getRemoteSecurityGroupUuids()).listValues();
            msg.getRemoteSecurityGroupUuids().stream().forEach(uuid -> {
                sgUuids.stream().filter(s -> s.equals(uuid)).findFirst().orElseThrow(() -> 
                        new ApiMessageInterceptionException(argerr("could not add security group rule, because security group[uuid:%s] does not exist", uuid)));
            });

            rules.stream().forEach(r -> {
                if (r.getRemoteSecurityGroupUuid() != null) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the remote security group uuid is conflict"));
                }
            });
            
            List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> aos = new ArrayList<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO>();

            msg.getRemoteSecurityGroupUuids().stream().forEach(uuid -> {
                rules.stream().forEach(r -> {
                    APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
                    ao.setType(r.getType());
                    ao.setState(r.getState());
                    ao.setDescription(r.getDescription());
                    ao.setRemoteSecurityGroupUuid(uuid);
                    ao.setIpVersion(r.getIpVersion());
                    ao.setProtocol(r.getProtocol());
                    ao.setSrcIpRange(r.getSrcIpRange());
                    ao.setDstIpRange(r.getDstIpRange());
                    ao.setDstPortRange(r.getDstPortRange());
                    ao.setAction(r.getAction());
                    ao.setAllowedCidr(r.getAllowedCidr());
                    ao.setStartPort(r.getStartPort());
                    ao.setEndPort(r.getEndPort());
                    aos.add(ao);
                });
            });

            if (!aos.isEmpty()) {
                msg.setRules(aos);
            }
        }

        if (msg.getPriority() == null || msg.getPriority() < 0) {
            msg.setPriority(SecurityGroupConstant.LOWEST_RULE_PRIORITY);
        }

        if (msg.getPriority() == SecurityGroupConstant.DEFAULT_RULE_PRIORITY) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because rule priority must greater than %d or equals %d", SecurityGroupConstant.DEFAULT_RULE_PRIORITY, SecurityGroupConstant.LOWEST_RULE_PRIORITY));
        }

        List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> newRules = msg.getRules();

        // Basic check
        for (APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao : newRules) {
            if (!SecurityGroupRuleType.isValid(ao.getType())) {
                throw new ApiMessageInterceptionException(argerr("could not add security group rule, because invalid rule type[%s], valid types are %s", ao.getType(), SecurityGroupRuleType.getAllType()));
            }

            if (ao.getState() == null) {
                ao.setState(SecurityGroupRuleState.Enabled.toString());
            } else {
                if (!SecurityGroupRuleState.isValid(ao.getState())) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because invalid rule state[%s], valid states are %s", ao.getState(), SecurityGroupRuleState.getAllState()));
                }
            }

            if (!SecurityGroupRuleProtocolType.isValid(ao.getProtocol())) {
                throw new ApiMessageInterceptionException(argerr("could not add security group rule, because invalid rule protocol[%s], valid protocols are %s", ao.getProtocol(), SecurityGroupRuleProtocolType.getAllProtocol()));
            }

            if (ao.getAction() == null) {
                ao.setAction(SecurityGroupRuleAction.ACCEPT.toString());
            } else {
                if (!SecurityGroupRuleAction.isValid(ao.getAction())) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because invalid rule action[%s], valid actions are %s", ao.getAction(), SecurityGroupRuleAction.getAllAction()));
                }
            }

            if (ao.getIpVersion() == null) {
                ao.setIpVersion(IPv6Constants.IPv4);
            } else {
                if (ao.getIpVersion() != IPv6Constants.IPv4 && ao.getIpVersion() != IPv6Constants.IPv6) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because invalid rule ipVersion[%d], valid ipVersions are %d/%d", ao.getIpVersion(), IPv6Constants.IPv4, IPv6Constants.IPv6));
                }
            }

            if (StringUtils.isEmpty(ao.getAllowedCidr())) {
                ao.setAllowedCidr(ao.getIpVersion() == IPv6Constants.IPv4 ? SecurityGroupConstant.WORLD_OPEN_CIDR : SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6);
            }

            if (SecurityGroupRuleType.Egress.toString().equals(ao.getType())) {
                if (ao.getSrcIpRange() != null) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the srcIpRange[%s] is not allowed to set for egress rule", ao.getSrcIpRange()));
                }

                if (checkAllowedCidrValid(ao.getAllowedCidr())) {
                    if (ao.getDstIpRange() != null) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the allowedCidr[%s] and dstIpRange[%s] are in conflict", ao.getAllowedCidr(), ao.getDstIpRange()));
                    }
                    ao.setDstIpRange(ao.getAllowedCidr());
                }

                if (ao.getDstIpRange() != null) {
                    if (ao.getRemoteSecurityGroupUuid() != null) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the ip range[%s] and remoteSecurityGroupUuid[%s] are in conflict", ao.getDstIpRange(), ao.getRemoteSecurityGroupUuid()));
                    }
                    validateIps(ao.getDstIpRange(), ao.getIpVersion());
                }
            } else {
                if (ao.getDstIpRange() != null) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the dstIpRange[%s] is not allowed to set for ingress rule", ao.getDstIpRange()));
                }

                if (checkAllowedCidrValid(ao.getAllowedCidr())) {
                    if (ao.getSrcIpRange() != null) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the allowedCidr[%s] and srcIpRange[%s] are in conflict", ao.getAllowedCidr(), ao.getSrcIpRange()));
                    }
                    ao.setSrcIpRange(ao.getAllowedCidr());
                }

                if (ao.getSrcIpRange() != null) {
                    if (ao.getRemoteSecurityGroupUuid() != null) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the ip range[%s] and remoteSecurityGroupUuid[%s] are in conflict", ao.getSrcIpRange(), ao.getRemoteSecurityGroupUuid()));
                    }
                    validateIps(ao.getSrcIpRange(), ao.getIpVersion());
                }
            }

            if (ao.getStartPort() == null) {
                ao.setStartPort(-1);
            }

            if (ao.getEndPort() == null) {
                ao.setEndPort(-1);
            }

            if (SecurityGroupRuleProtocolType.ALL.toString().equals(ao.getProtocol()) || SecurityGroupRuleProtocolType.ICMP.toString().equals(ao.getProtocol())) {
                if (ao.getDstPortRange() != null) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the protocol type ALL or ICMP cant not set dstPortRange[%s]", ao.getDstPortRange()));
                }
            } else {
                if (ao.getStartPort() >= 0 && ao.getEndPort() < 65535) {
                    if (ao.getStartPort() > ao.getEndPort()) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because invalid rule endPort[%d], endPort must be greater than or equal to startPort[%d]", ao.getEndPort(), ao.getStartPort()));
                    }
                    if (ao.getDstPortRange() != null) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because dstPortRange[%s] and starPort[%s] are in conflict", ao.getDstPortRange(), ao.getStartPort()));
                    }

                    if (ao.getStartPort().equals(ao.getEndPort())) {
                        ao.setDstPortRange(String.valueOf(ao.getStartPort()));
                    } else {
                        ao.setDstPortRange(String.format("%s-%s", ao.getStartPort(), ao.getEndPort()));
                    }
                }

                if (ao.getDstPortRange() == null) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the protocol type TCP/UDP must set dstPortRange"));
                }
                validatePorts(ao.getDstPortRange());
            }
        }

        // Deduplicate in API
        for (int i = 0; i < newRules.size() - 1; i++) {
            for (int j = newRules.size() - 1; j > i; j--) {
                if (newRules.get(i).equals(newRules.get(j))) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because rule[%s] and rule[%s] are dupilicated",
                                    JSONObjectUtil.toJsonString(newRules.get(i)), JSONObjectUtil.toJsonString(newRules.get(j))));
                }
            }
        }

        // Deduplicate in DB
        List<SecurityGroupRuleVO> vos = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, sgUuid).list();

        for (SecurityGroupRuleVO vo : vos) {
            APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
            ao.setType(vo.getType().toString());
            ao.setAllowedCidr(vo.getAllowedCidr());
            ao.setProtocol(vo.getProtocol().toString());
            ao.setStartPort(vo.getStartPort());
            ao.setEndPort(vo.getEndPort());
            ao.setIpVersion(vo.getIpVersion());
            ao.setRemoteSecurityGroupUuid(vo.getRemoteSecurityGroupUuid());
            ao.setAction(vo.getAction());
            ao.setSrcIpRange(vo.getSrcIpRange());
            ao.setDstIpRange(vo.getDstIpRange());
            ao.setDstPortRange(vo.getDstPortRange());
            for (APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO sao : newRules) {
                if (ao.equals(sao)) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because rule[%s] is duplicated to rule[uuid:%s] in datebase", JSONObjectUtil.toJsonString(sao), vo.getUuid()));
                }
            }
        }

        int ingressRuleCount = vos.stream().filter(vo -> SecurityGroupRuleType.Ingress.equals(vo.getType()) && vo.getPriority() != SecurityGroupConstant.DEFAULT_RULE_PRIORITY).collect(Collectors.toList()).size();
        int egressRuleCount = vos.stream().filter(vo -> SecurityGroupRuleType.Egress.equals(vo.getType()) && vo.getPriority() != SecurityGroupConstant.DEFAULT_RULE_PRIORITY).collect(Collectors.toList()).size();
        int toCreateIngressRuleCount = newRules.stream().filter(ao -> SecurityGroupRuleType.Ingress.toString().equals(ao.getType())).collect(Collectors.toList()).size();
        int toCreateEgressRuleCount = newRules.stream().filter(ao -> SecurityGroupRuleType.Egress.toString().equals(ao.getType())).collect(Collectors.toList()).size();

        if (ingressRuleCount >= SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class) && toCreateIngressRuleCount > 0) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because security group %s rules has reached the maximum limit[%d]",
                    SecurityGroupRuleType.Ingress, SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)));
        }
        if (egressRuleCount >= SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class) && toCreateEgressRuleCount > 0) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because security group %s rules has reached the maximum limit[%d]",
                    SecurityGroupRuleType.Egress, SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)));
        }
        if ((ingressRuleCount + toCreateIngressRuleCount) > SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because security group %s rules number[%d] is out of max limit[%d]",
                    SecurityGroupRuleType.Ingress, (ingressRuleCount + toCreateIngressRuleCount), SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)));
        }
        if ((egressRuleCount + toCreateEgressRuleCount) > SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because security group %s rules number[%d] is out of max limit[%d]",
                    SecurityGroupRuleType.Egress, (egressRuleCount + toCreateEgressRuleCount), SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)));
        }
        if (msg.getPriority() > (ingressRuleCount + 1)) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because priority[%d] must be consecutive, the ingress rule maximum priority is [%d]", msg.getPriority(), ingressRuleCount));
        }
        if (msg.getPriority() > (egressRuleCount + 1)) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because priority[%d] must be consecutive, the egress rule maximum priority is [%d]", msg.getPriority(), egressRuleCount));
        }
    }

    private void validate(APICreateSecurityGroupMsg msg) {
        if (msg.getIpVersion() == null) {
            msg.setIpVersion(IPv6Constants.IPv4);
        }
    }
}
