package org.openoa.engine.bpmnconf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.openoa.base.entity.BpmVariable;
import org.openoa.common.entity.BpmVariableMultiplayer;

import java.util.List;

@Mapper
public interface BpmVariableMapper extends BaseMapper<BpmVariable> {
    //one node can have multiple elements,but all of them share the same elementId,so if the results contains multiple results,they are all the same
    List<String> getElementIdsdByNodeId(@Param("processNum") String processNum,
                                        @Param("nodeId") String nodeId);
    List<String> getNodeIdsByeElementId(@Param("processNum") String processNum,
                                      @Param("elementId") String elementId);
    void resetUnderStatusByProcessNumber(@Param("processNum")String processNum);
    void invalidNodeAssignee(@Param("processNum") String processNum, @Param("elementId") String elementId,@Param("assignee")String assignee);
    List<BpmVariableMultiplayer> querymultiplayersbyprocesselementid(@Param("processNum") String processNum, @Param("elementId")String elementId);
}
