package org.zstack.header.volume;

import org.zstack.header.core.progress.ChainInfo;
import org.zstack.header.message.MessageReply;

import java.util.HashMap;
import java.util.Map;

public class GetVolumeTaskReply extends MessageReply {
    private Map<String, ChainInfo> results = new HashMap<>();

    public Map<String, ChainInfo> getResults() {
        return results;
    }

    public void setResults(Map<String, ChainInfo> results) {
        this.results = results;
    }
}
