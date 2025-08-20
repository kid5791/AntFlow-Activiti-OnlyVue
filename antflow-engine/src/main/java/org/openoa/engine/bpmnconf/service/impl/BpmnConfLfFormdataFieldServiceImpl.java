package org.openoa.engine.bpmnconf.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.openoa.base.entity.BpmnConfLfFormdataField;
import org.openoa.base.exception.AFBizException;
import org.openoa.engine.bpmnconf.mapper.BpmnConfLfFormdataFieldMapper;
import org.openoa.engine.bpmnconf.service.interf.repository.BpmnConfLfFormdataFieldService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BpmnConfLfFormdataFieldServiceImpl extends ServiceImpl<BpmnConfLfFormdataFieldMapper, BpmnConfLfFormdataField> implements BpmnConfLfFormdataFieldService {
    @Override
    public Map<String,BpmnConfLfFormdataField> qryFormDataFieldMap(Long confId){
        List<BpmnConfLfFormdataField> allFields = this.list(Wrappers.<BpmnConfLfFormdataField>lambdaQuery()
                .eq(BpmnConfLfFormdataField::getBpmnConfId, confId));
        if(CollectionUtils.isEmpty(allFields)){
            throw new AFBizException("lowcode form data has no fields");
        }
        Map<String,BpmnConfLfFormdataField> id2SelfMap=new HashMap<>();
        for (BpmnConfLfFormdataField field : allFields) {
            String fieldId = field.getFieldId();
            id2SelfMap.put(fieldId,field);
        }
        return id2SelfMap;
    }
}
