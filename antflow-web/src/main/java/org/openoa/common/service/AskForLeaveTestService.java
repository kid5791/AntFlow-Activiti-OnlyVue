package org.openoa.common.service;

import org.openoa.base.constant.enums.ButtonTypeEnum;
import org.openoa.base.interf.ActivitiService;
import org.openoa.base.interf.ActivitiServiceAnno;
import org.openoa.base.interf.FormOperationAdaptor;
import org.openoa.base.util.SecurityUtils;
import org.openoa.base.vo.BpmnStartConditionsVo;
import org.openoa.base.vo.BusinessDataVo;
import org.openoa.entity.BizLeaveTime;
import org.openoa.vo.BizLeaveTimeVo;
import org.openoa.mapper.BizLeaveTimeMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @Classname AskForLeaveTestService
 * @Date 2024-08-26 11:14
 * @Created by AntOffice
 */
@ActivitiServiceAnno(svcName = "LEAVE_WMA",desc = "请假申请")
//formAdaptor
public class AskForLeaveTestService implements FormOperationAdaptor<BizLeaveTimeVo>, ActivitiService {

    @Autowired
    private BizLeaveTimeMapper bizLeaveTimeMapper;

    @Override
    public BpmnStartConditionsVo previewSetCondition(BizLeaveTimeVo vo) {
        String userId =  vo.getStartUserId();
        return BpmnStartConditionsVo.builder()
                .startUserId(userId)
                .leaveHour(vo.getLeaveHour()).build();
    }

    @Override
    public void initData(BizLeaveTimeVo vo) {

    }


    @Override
    public BpmnStartConditionsVo launchParameters(BizLeaveTimeVo vo) {
        String userId =  vo.getStartUserId();
        return BpmnStartConditionsVo.builder()
                .startUserId(userId)
                .leaveHour(vo.getLeaveHour()).build();
    }

    @Override
    public void queryData(BizLeaveTimeVo vo) {
        BizLeaveTime leaveTime = bizLeaveTimeMapper.selectById(vo.getBusinessId());
        BeanUtils.copyProperties(leaveTime,vo);

    }

    @Override
    public void submitData(BizLeaveTimeVo vo) {
        BizLeaveTime leaveTime=new BizLeaveTime();
        BeanUtils.copyProperties(vo,leaveTime);

        leaveTime.setCreateTime(new Date());
        leaveTime.setCreateUser(SecurityUtils.getLogInEmpNameSafe());
        leaveTime.setLeaveUserId(Integer.parseInt(vo.getStartUserId()));
        leaveTime.setLeaveUserName(SecurityUtils.getLogInEmpNameSafe());

        bizLeaveTimeMapper.insert(leaveTime);
        vo.setBusinessId(leaveTime.getId().toString());
        vo.setProcessTitle("请假申请");
        vo.setProcessDigest(vo.getRemark());
        vo.setEntityName(BizLeaveTime.class.getSimpleName());

    }

    @Override
    public void consentData(BizLeaveTimeVo vo) {
        if (vo.getOperationType().equals(ButtonTypeEnum.BUTTON_TYPE_RESUBMIT.getCode())
                && !vo.getOperationType().equals(ButtonTypeEnum.BUTTON_TYPE_AGREE.getCode())){
            BizLeaveTime leaveTime = new BizLeaveTime();
            BeanUtils.copyProperties(vo,leaveTime);
            Integer id=  Integer.valueOf((vo.getBusinessId()).toString());
            leaveTime.setId(id);
            bizLeaveTimeMapper.updateById(leaveTime);
        }

    }

    @Override
    public void backToModifyData(BizLeaveTimeVo vo) {

    }

    @Override
    public void cancellationData(BizLeaveTimeVo vo) {

    }

    @Override
    public void finishData(BusinessDataVo vo) {

    }
}
