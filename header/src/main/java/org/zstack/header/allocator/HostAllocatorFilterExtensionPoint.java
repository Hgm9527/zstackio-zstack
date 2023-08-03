package org.zstack.header.allocator;

import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.host.HostVO;

import java.util.List;

/**
 * Created by frank on 7/2/2015.
 */
public interface HostAllocatorFilterExtensionPoint {
    ErrorableValue<List<HostVO>> filterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec);
}
