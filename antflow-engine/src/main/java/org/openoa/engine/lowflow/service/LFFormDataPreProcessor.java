package org.openoa.engine.lowflow.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.base.Strings;
import org.openoa.base.constant.StringConstants;
import org.openoa.base.constant.enums.VariantFormContainerTypeEnum;
import org.openoa.base.exception.AFBizException;
import org.openoa.base.service.AntFlowOrderPreProcessor;
import org.openoa.base.util.SecurityUtils;
import org.openoa.base.vo.BpmnConfVo;
import org.openoa.base.entity.BpmnConfLfFormdata;
import org.openoa.base.entity.BpmnConfLfFormdataField;
import org.openoa.engine.bpmnconf.service.impl.BpmnConfLfFormdataFieldServiceImpl;
import org.openoa.engine.bpmnconf.service.impl.BpmnConfLfFormdataServiceImpl;
import org.openoa.base.vo.FormConfigWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class LFFormDataPreProcessor implements AntFlowOrderPreProcessor<BpmnConfVo> {
    @Autowired
    private BpmnConfLfFormdataServiceImpl lfFormdataService;
    @Autowired
    private BpmnConfLfFormdataFieldServiceImpl lfFormdataFieldService;


    @Override
    public void preWriteProcess(BpmnConfVo confVo) {
        if(confVo==null){
            return;
        }
        Integer isLowCodeFlow = confVo.getIsLowCodeFlow();
        boolean lowCodeFlowFlag=isLowCodeFlow!=null&&isLowCodeFlow==1;
        if(!lowCodeFlowFlag){
            return;
        }
        Long confId = confVo.getId();
        String lfForm = confVo.getLfFormData();
        BpmnConfLfFormdata lfFormdata=new BpmnConfLfFormdata();
        lfFormdata.setBpmnConfId(confId);
        lfFormdata.setFormdata(lfForm);
        lfFormdata.setCreateUser(SecurityUtils.getLogInEmpName());
        lfFormdataService.save(lfFormdata);
        confVo.setLfFormDataId(lfFormdata.getId());
        FormConfigWrapper formConfigWrapper = JSON.parseObject(lfForm, FormConfigWrapper.class);
        List<FormConfigWrapper.LFWidget> lfWidgetList = formConfigWrapper.getWidgetList();
        if(CollectionUtils.isEmpty(lfWidgetList)){
            throw new AFBizException(Strings.lenientFormat("lowcode form has no widget,confId:%d,formCode:%s",confId,confVo.getFormCode()));
        }
        List<BpmnConfLfFormdataField> formdataFields=new ArrayList<>();
        parseWidgetListRecursively(lfWidgetList,confId,lfFormdata.getId(),formdataFields);
        if(CollectionUtils.isEmpty(formdataFields)){
            throw new AFBizException(Strings.lenientFormat("lowcode form fields can not be empty,confId:%d,formCode:%s",confId,confVo.getFormCode()));
        }
        lfFormdataFieldService.saveBatch(formdataFields);
    }

    @Override
    public void preReadProcess(BpmnConfVo confVo) {
        if(confVo==null){
            return;
        }
        Integer isLowCodeFlow = confVo.getIsLowCodeFlow();
        boolean lowCodeFlowFlag=isLowCodeFlow!=null&&isLowCodeFlow==1;
        if(!lowCodeFlowFlag){
            return;
        }
        Long confId = confVo.getId();
        List<BpmnConfLfFormdata> bpmnConfLfFormdataList = lfFormdataService.list(Wrappers.<BpmnConfLfFormdata>lambdaQuery().eq(BpmnConfLfFormdata::getBpmnConfId, confId));
        if(CollectionUtils.isEmpty(bpmnConfLfFormdataList)){
            throw  new AFBizException(Strings.lenientFormat("can not get lowcode flow formdata by confId:%s",confId));
        }
        BpmnConfLfFormdata lfFormdata = bpmnConfLfFormdataList.get(0);
        confVo.setLfFormData(lfFormdata.getFormdata());
        confVo.setLfFormDataId(lfFormdata.getId());
    }

    private void parseWidgetListRecursively(List<FormConfigWrapper.LFWidget> widgetList,Long confId,Long formDataId,List<BpmnConfLfFormdataField> result){
        for (FormConfigWrapper.LFWidget lfWidget : widgetList) {
            if(!StringConstants.LOWFLOW_FORM_CONTAINER_TYPE.equals(lfWidget.getCategory())){
                FormConfigWrapper.LFWidget.LFOption lfOption = lfWidget.getOptions();
                BpmnConfLfFormdataField formdataField=new BpmnConfLfFormdataField();
                formdataField.setBpmnConfId(confId);
                formdataField.setFormDataId(formDataId);
                formdataField.setFieldType(lfOption.getFieldType());
                formdataField.setFieldId(lfOption.getName());
                formdataField.setFieldName(lfOption.getLabel());
                result.add(formdataField);
            }else{//走到这里一定是容器类型
                String containerType = lfWidget.getType();
                VariantFormContainerTypeEnum containerTypeEnum = VariantFormContainerTypeEnum.getByTypeName(containerType);
                if(containerTypeEnum==null){
                    continue; //未定义低代码表单字段类型，直接跳过
                }
                if(VariantFormContainerTypeEnum.CARD.equals(containerTypeEnum)){
                    List<FormConfigWrapper.LFWidget> subWidgetList = lfWidget.getWidgetList();
                    parseWidgetListRecursively(subWidgetList,confId,formDataId,result);
                }else if(VariantFormContainerTypeEnum.TAB.equals(containerTypeEnum)){
                    List<FormConfigWrapper.LFWidget> tabs = lfWidget.getTabs();
                    for (FormConfigWrapper.LFWidget tab : tabs) {
                        List<FormConfigWrapper.LFWidget> subWidgetList = tab.getWidgetList();
                        parseWidgetListRecursively(subWidgetList,confId,formDataId,result);
                    }
                }else{

                    List<FormConfigWrapper.TableRow> rows = lfWidget.getRows();
                    if(!CollectionUtils.isEmpty(rows)){//table
                        for (FormConfigWrapper.TableRow row : lfWidget.getRows()) {
                            List<FormConfigWrapper.LFWidget> cols = row.getCols();
                            for (FormConfigWrapper.LFWidget col : cols) {
                                List<FormConfigWrapper.LFWidget> subWidgetList = col.getWidgetList();
                                if(CollectionUtils.isEmpty(subWidgetList)){
                                    continue;
                                }
                                parseWidgetListRecursively(subWidgetList,confId,formDataId,result);
                            }
                        }
                    }else{
                        //grid has no rows,only cols
                        List<FormConfigWrapper.LFWidget> cols = lfWidget.getCols();
                        for (FormConfigWrapper.LFWidget col : cols) {
                            List<FormConfigWrapper.LFWidget> subWidgetList = col.getWidgetList();
                            if(CollectionUtils.isEmpty(subWidgetList)){
                                continue;
                            }
                            parseWidgetListRecursively(subWidgetList,confId,formDataId,result);
                        }
                    }
                }

            }
        }
    }

    @Override
    public int order() {
        return 0;
    }
}
