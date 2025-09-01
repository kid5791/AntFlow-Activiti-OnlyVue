package org.openoa.engine.bpmnconf.service.biz;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jodd.bean.BeanUtil;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.openoa.base.constant.enums.*;
import org.openoa.base.entity.*;
import org.openoa.base.exception.AFBizException;
import org.openoa.base.interf.BpmBusinessProcessService;
import org.openoa.base.service.AfRoleService;
import org.openoa.base.service.AfUserService;
import org.openoa.base.service.BpmVariableService;
import org.openoa.base.util.DateUtil;
import org.openoa.base.util.MultiTenantUtil;
import org.openoa.base.util.SecurityUtils;
import org.openoa.base.vo.*;
import org.openoa.common.entity.BpmVariableMultiplayer;
import org.openoa.common.entity.BpmVariableMultiplayerPersonnel;
import org.openoa.common.entity.BpmVariableSingle;
import org.openoa.common.service.BpmVariableMultiplayerPersonnelServiceImpl;
import org.openoa.common.service.BpmVariableMultiplayerServiceImpl;
import org.openoa.common.service.BpmVariableSingleServiceImpl;
import org.openoa.engine.bpmnconf.common.ProcessBusinessContans;
import org.openoa.engine.bpmnconf.common.ProcessConstants;
import org.openoa.engine.bpmnconf.service.impl.*;
import org.openoa.engine.bpmnconf.service.interf.biz.BpmVariableMessageBizService;
import org.openoa.engine.bpmnconf.service.interf.repository.BpmProcessForwardService;
import org.openoa.engine.bpmnconf.service.interf.repository.BpmProcessNoticeService;
import org.openoa.engine.bpmnconf.service.interf.repository.BpmVariableApproveRemindService;
import org.openoa.engine.bpmnconf.service.interf.repository.BpmnConfService;
import org.openoa.engine.utils.InformationTemplateUtils;
import org.openoa.engine.utils.UserMsgUtils;
import org.openoa.engine.vo.ProcessInforVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BpmVariableMessageBizServiceImpl implements BpmVariableMessageBizService {
    @Autowired
    private BpmnConfService bpmnConfService;

    @Autowired
    private BpmVariableService bpmVariableService;

    @Autowired
    private BpmVariableApproveRemindService bpmVariableApproveRemindService;

    @Autowired
    private AfUserService employeeService;
    @Autowired
    private AfRoleService roleService;

    @Autowired
    private BpmProcessNoticeService bpmProcessNoticeService;

    @Autowired
    private ProcessBusinessContans processBusinessContans;


    @Autowired
    private BpmBusinessProcessService bpmBusinessProcessService;

    @Autowired
    private ActHiTaskinstServiceImpl hiTaskinstService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessConstants processConstants;

    @Autowired
    private BpmVariableSingleServiceImpl bpmVariableSingleService;

    @Autowired
    private BpmVariableMultiplayerServiceImpl bpmVariableMultiplayerService;

    @Autowired
    private BpmVariableMultiplayerPersonnelServiceImpl bpmVariableMultiplayerPersonnelService;

    @Autowired
    private BpmVariableSignUpPersonnelServiceImpl bpmVariableSignUpPersonnelService;

    @Autowired
    private BpmProcessForwardService bpmProcessForwardService;
    @Autowired
    private InformationTemplateUtils informationTemplateUtils;


    /**
     * insert variable message config
     * @param variableId
     * @param bpmnConfCommonVo
     */
    @Override
    public void insertVariableMessage(Long variableId, BpmnConfCommonVo bpmnConfCommonVo) {


        //variable message list
        List<BpmVariableMessage> bpmVariableMessages = Lists.newArrayList();


        //process node approval remind list
        List<BpmVariableApproveRemind> bpmVariableApproveReminds = Lists.newArrayList();


        // add out of node variable message config
        if (!ObjectUtils.isEmpty(bpmnConfCommonVo.getTemplateVos())) {
            bpmVariableMessages.addAll(getBpmVariableMessages(variableId, bpmnConfCommonVo.getTemplateVos(), StringUtils.EMPTY, 1));
        }


        //add in node message config
        if (!ObjectUtils.isEmpty(bpmnConfCommonVo.getElementList())) {
            for (BpmnConfCommonElementVo elementVo : bpmnConfCommonVo.getElementList()) {
                if (ObjectUtils.isEmpty(elementVo.getTemplateVos())) {
                    continue;
                }
                bpmVariableMessages.addAll(getBpmVariableMessages(variableId, elementVo.getTemplateVos(), elementVo.getElementId(), 2));

                //add process node approval remind list
                if (!ObjectUtils.isEmpty(elementVo.getApproveRemindVo()) &&
                        !ObjectUtils.isEmpty(elementVo.getApproveRemindVo().getDays())) {
                    bpmVariableApproveReminds.add(BpmVariableApproveRemind
                            .builder()
                            .variableId(variableId)
                            .elementId(elementVo.getElementId())
                            .content(JSON.toJSONString(elementVo.getApproveRemindVo()))
                            .tenantId(MultiTenantUtil.getCurrentTenantId())
                            .build());
                }
            }
        }


        // if variable messages are not empty,then save them in batch
        if (!ObjectUtils.isEmpty(bpmVariableMessages)) {
            this.getService().saveBatch(bpmVariableMessages);
        }


        //if approval reminds are not empty then save them in batch
        if (!ObjectUtils.isEmpty(bpmVariableApproveReminds)) {
            bpmVariableApproveRemindService.saveBatch(bpmVariableApproveReminds);
        }
    }
    /**

     * check whether to to send messages by template
     *
     * @param vo
     * @return
     */
    @Override
    public Boolean checkIsSendByTemplate(BpmVariableMessageVo vo) {

        BpmVariable bpmVariable = bpmVariableService.getBaseMapper().selectOne(new QueryWrapper<BpmVariable>().eq("process_num", vo.getProcessNumber()));
        if (ObjectUtils.isEmpty(bpmVariable)) {
            return false;
        }
        if (vo.getMessageType()!=null&& vo.getMessageType()== 2) {//in node messages
            return this.getMapper().selectCount(new QueryWrapper<BpmVariableMessage>()
                    .eq("variable_id", bpmVariable.getId())
                    //.eq("element_id", vo.getElementId())
                    .eq("message_type", 2)
                    .eq("event_type", vo.getEventType())) > 0;
        } else if (vo.getMessageType()!=null&&vo.getMessageType()==1) {//out of node messages
            return this.getMapper().selectCount(new QueryWrapper<BpmVariableMessage>()
                    .eq("variable_id", bpmVariable.getId())
                    .eq("message_type", 1)
                    .eq("event_type", vo.getEventType())) > 0;
        }
        return false;
    }

    /**
     * build variable message vo for sending messages
     *
     * @param vo
     */
    @Override
    public BpmVariableMessageVo getBpmVariableMessageVo(BpmVariableMessageVo vo) {


        if (ObjectUtils.isEmpty(vo)) {
            return null;
        }


        BpmVariable bpmVariable = null;
        List<BpmVariable> bpmVariables = bpmVariableService.getBaseMapper().selectList(new QueryWrapper<BpmVariable>().eq("process_num", vo.getProcessNumber()));
        if(!ObjectUtils.isEmpty(bpmVariables)){
            bpmVariable=bpmVariables.get(0);
        }

        if (ObjectUtils.isEmpty(bpmVariable)) {
            return null;
        }


        //set variable id
        vo.setVariableId(bpmVariable.getId());


        //get bpmn conf
        BpmnConf bpmnConf = bpmnConfService.getBaseMapper().selectOne(new QueryWrapper<BpmnConf>()
                .eq("bpmn_code", bpmVariable.getBpmnCode()));
        if(bpmnConf==null){
            throw new AFBizException(Strings.lenientFormat("can not get bpmnConf by bpmncode:%s",bpmVariable.getBpmnCode()));
        }
        //set bpmn code
        vo.setBpmnCode(bpmnConf.getBpmnCode());

        //set bpmn name
        vo.setBpmnName(bpmnConf.getBpmnName());

        //set form code
        vo.setFormCode(bpmnConf.getFormCode());

        //todo
        //process type info
        //vo.setProcessType(SysDicUtils.getDicNameByCode("DIC_LCLB", bpmnConf.getBpmnType()));

        //set process start variables
        if (!StringUtils.isEmpty(bpmVariable.getProcessStartConditions())) {
            BpmnStartConditionsVo bpmnStartConditionsVo = JSON.parseObject(bpmVariable.getProcessStartConditions(), BpmnStartConditionsVo.class);
            vo.setBpmnStartConditions(bpmnStartConditionsVo);
            //set approval employee id
            vo.setApprovalEmplIds(Optional.ofNullable(bpmnStartConditionsVo.getApprovalEmpls()).orElse(Lists.newArrayList(BaseIdTranStruVo.builder().build())).stream().map(BaseIdTranStruVo::getId).collect(Collectors.toList()));
        }


        //query bpmn business process by process number
        BpmBusinessProcess businessProcess = bpmBusinessProcessService.getBaseMapper().selectOne(new QueryWrapper<BpmBusinessProcess>().eq("BUSINESS_NUMBER", vo.getProcessNumber()));

        if (ObjectUtils.isEmpty(businessProcess)) {
            throw new AFBizException(Strings.lenientFormat("can not get BpmBusinessProcess by process Numbeer:%s",vo.getProcessNumber()));
        }


        //call activiti's history service to query history process instance


        String procInstId = businessProcess.getProcInstId();
        String createUser = businessProcess.getCreateUser();
        Date createTime = businessProcess.getCreateTime();

        //set process instance id
        vo.setProcessInsId(procInstId);

        //set start user id
        vo.setStartUser(createUser);

        //get process's start time

        //set apply date
        vo.setApplyDate(DateUtil.SDF_DATE_PATTERN.format(createTime));

        //set apply time
        vo.setApplyTime(DateUtil.SDF_DATETIME_PATTERN.format(createTime));


        //get a list of history tasks
        List<ActHiTaskinst> hisTask = hiTaskinstService.queryRecordsByProcInstId(procInstId);

        //set already approved employee id
        vo.setApproveds(hisTask
                .stream()
                .map(ActHiTaskinst::getAssignee)
                .filter(assignee -> !StringUtils.isEmpty(assignee))
                .distinct()
                .collect(Collectors.toList()));


        //if the current node approver is empty, then get it from login user info
        if (StringUtils.isEmpty(vo.getAssignee())) {

            vo.setAssignee(SecurityUtils.getLogInEmpIdSafe());
        }


        //if the event type is in node event, then get the node info from activiti process engine
        if (vo.getEventTypeEnum().getIsInNode()) {
            //get current task list by process instance id
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInstId).list();
            if (!ObjectUtils.isEmpty(tasks)) {

                //if node is empty then get from task's definition
                if (ObjectUtils.isEmpty(vo.getElementId())) {
                    String elementId = tasks.stream().map(Task::getTaskDefinitionKey).distinct().findFirst().orElse(StringUtils.EMPTY);
                    vo.setElementId(elementId);
                }

                //if task id is empty then get it from current tasks
                if (ObjectUtils.isEmpty(vo.getTaskId())) {
                    String taskId = tasks.stream().map(Task::getId).distinct().findFirst().orElse(StringUtils.EMPTY);
                    vo.setTaskId(taskId);
                }

                //if link type is empty then set it default to 1
                if (vo.getType()==null) {
                    vo.setType(1);
                }

                //get process's next node info via activiti's pvm
                PvmActivity nextNodePvmActivity = processConstants.getNextNodePvmActivity(procInstId);

                //if next node is not empty and next node is not end event,then process it
                if (!ObjectUtils.isEmpty(nextNodePvmActivity)) {
                    if (!"endEvent".equals(nextNodePvmActivity.getProperty("type"))) {
                        //next element's id
                        String nextElementId = nextNodePvmActivity.getId();
                        //set next node's approvers
                        vo.setNextNodeApproveds(getNextNodeApproveds(bpmVariable.getId(), nextElementId));
                    }
                }
            }
        }

        return vo;
    }
    /**
     * get UrlMap
     *
     * @param vo
     * @param informationTemplateVo
     * @return
     */
    @Override
    public Map<String, String> getUrlMap(BpmVariableMessageVo vo, InformationTemplateVo informationTemplateVo) {
        Map<String, String> urlMap = Maps.newHashMap();

        String emailUrl = StringUtils.EMPTY;
        String appUrl = StringUtils.EMPTY;

        if (informationTemplateVo.getJumpUrl()!=null &&
                (informationTemplateVo.getJumpUrl().equals(JumpUrlEnum.PROCESS_APPROVE.getCode())
                        || informationTemplateVo.getJumpUrl().equals(JumpUrlEnum.PROCESS_VIEW.getCode()))) {

            Integer type = informationTemplateVo.getJumpUrl() == 1 ? 2 : 1;

            //email and in site notice
            emailUrl = processBusinessContans.getRoute(ProcessNoticeEnum.EMAIL_TYPE.getCode(),
                    ProcessInforVo
                            .builder()
                            .processinessKey(vo.getBpmnCode())
                            .businessNumber(vo.getProcessNumber())
                            .formCode(vo.getFormCode())
                            .type(type)
                            .build(), Optional.ofNullable(vo.getIsOutside()).orElse(false));

            //app route
            appUrl = processBusinessContans.getRoute(ProcessNoticeEnum.APP_TYPE.getCode(),
                    ProcessInforVo
                            .builder()
                            .processinessKey(vo.getBpmnCode())
                            .businessNumber(vo.getProcessNumber())
                            .formCode(vo.getFormCode())
                            .type(type)
                            .build(), Optional.ofNullable(vo.getIsOutside()).orElse(false));

        } else if (informationTemplateVo.getJumpUrl()!=null && informationTemplateVo.getJumpUrl().equals(JumpUrlEnum.PROCESS_BACKLOG.getCode())) {
            emailUrl = "/user/workflow/upcoming?page=1&pageSize=10";
            appUrl = "";
        }

        urlMap.put("emailUrl", emailUrl);
        urlMap.put("appUrl", appUrl);

        return urlMap;
    }
    @Override
    public BpmVariableMessageVo fromBusinessDataVo(BusinessDataVo businessDataVo) {


        if (ObjectUtils.isEmpty(businessDataVo)) {
            return null;
        }

        //get event type by operation type
        EventTypeEnum eventTypeEnum = EventTypeEnum.getEnumByOperationType(businessDataVo.getOperationType());

        if (ObjectUtils.isEmpty(eventTypeEnum)) {
            return null;
        }


        //default link type is process type
        Integer type = 2;


        //if event type is cancel operation then link type is view type
        if (eventTypeEnum.equals(EventTypeEnum.PROCESS_CANCELLATION)) {
            type = 1;
        }


        //build message vo
        return this.getBpmVariableMessageVo(BpmVariableMessageVo
                .builder()
                .processNumber(businessDataVo.getProcessNumber())
                .formCode(businessDataVo.getFormCode())
                .eventType(eventTypeEnum.getCode())
                .forwardUsers(Optional.ofNullable(businessDataVo.getUserIds())
                        .orElse(Lists.newArrayList())
                        .stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList()))
                .signUpUsers(Optional.ofNullable(businessDataVo.getSignUpUsers())
                        .orElse(Lists.newArrayList())
                        .stream()
                        .map(BaseIdTranStruVo::getId)
                        .collect(Collectors.toList()))
                .messageType(eventTypeEnum.getIsInNode() ? 2 : 1)
                .eventTypeEnum(eventTypeEnum)
                .type(type)
                .build());

    }
    /**

     * send templated messages in async way
     *
     * @param vo
     */
    @Async
    @Override
    public void sendTemplateMessagesAsync(BpmVariableMessageVo vo) {
        doSendTemplateMessages(vo);
    }
    @Async
    void sendTemplateMessagesAsync(BusinessDataVo businessDataVo){
        BpmVariableMessageVo bpmVariableMessageVo = fromBusinessDataVo(businessDataVo);
        doSendTemplateMessages(bpmVariableMessageVo);
    }

    /**
     * send templated messages in sync way
     *
     * @param vo
     */
    @Override
    public void sendTemplateMessages(BpmVariableMessageVo vo) {
        doSendTemplateMessages(vo);
    }
    @Override
    public void sendTemplateMessages(BusinessDataVo businessDataVo) {
        BpmVariableMessageVo bpmVariableMessageVo = fromBusinessDataVo(businessDataVo);
        doSendTemplateMessages(bpmVariableMessageVo);
    }

    /**
     * get variable messages list
     *
     * @param variableId
     * @param templateVos
     * @param messageType
     * @return
     */
    private List<BpmVariableMessage> getBpmVariableMessages(Long variableId, List<BpmnTemplateVo> templateVos, String elementId, Integer messageType) {
        return templateVos
                .stream()
                .map(o -> BpmVariableMessage
                        .builder()
                        .variableId(variableId)
                        .elementId(elementId)
                        .messageType(getMessageSendType(o.getEvent(),messageType))
                        .eventType(o.getEvent())
                        .content(JSON.toJSONString(o))
                        .tenantId(MultiTenantUtil.getCurrentTenantId())
                        .build())
                .collect(Collectors.toList());
    }

    private Integer getMessageSendType(Integer event,Integer defaultMessageSendType){
        if(event==null){
            return defaultMessageSendType;
        }
        EventTypeEnum eventTypeEnum = EventTypeEnum.getByCode(event);
        if(eventTypeEnum==null){
            return defaultMessageSendType;
        }
        return eventTypeEnum.getIsInNode()?2:1;
    }

    /**
     * get next node's approvers
     *
     * @param variableId
     * @param nextElementId
     * @return
     */
    private List<String> getNextNodeApproveds(Long variableId, String nextElementId) {


        //query to check whether sign variable has parameter,if yes then return;
        if (bpmVariableSingleService.getBaseMapper().selectCount(new QueryWrapper<BpmVariableSingle>()
                .eq("variable_id", variableId)
                .eq("element_id", nextElementId)) > 0) {
            List<String> nextNodeApproveds = Lists.newArrayList();
            BpmVariableSingle bpmVariableSingle = bpmVariableSingleService.getBaseMapper().selectOne(new QueryWrapper<BpmVariableSingle>()
                    .eq("variable_id", variableId)
                    .eq("element_id", nextElementId));
            nextNodeApproveds.add(bpmVariableSingle.getAssignee());
            return nextNodeApproveds;
        }


        //query to check whether multiplayer variables have parameter,if yes then return;
        if (bpmVariableMultiplayerService.getBaseMapper().selectCount(new QueryWrapper<BpmVariableMultiplayer>()
                .eq("variable_id", variableId)
                .eq("element_id", nextElementId)) > 0) {
            List<String> nextNodeApproveds = Lists.newArrayList();
            BpmVariableMultiplayer bpmVariableMultiplayer = bpmVariableMultiplayerService.getBaseMapper().selectOne(new QueryWrapper<BpmVariableMultiplayer>()
                    .eq("variable_id", variableId)
                    .eq("element_id", nextElementId));
            nextNodeApproveds.addAll(bpmVariableMultiplayerPersonnelService.getBaseMapper().selectList(new QueryWrapper<BpmVariableMultiplayerPersonnel>()
                            .eq("variable_multiplayer_id", bpmVariableMultiplayer.getId()))
                    .stream()
                    .map(BpmVariableMultiplayerPersonnel::getAssignee)
                    .collect(Collectors.toList()));
            return nextNodeApproveds;
        }


        //query to check whether sign up node has parameters,if yes,then query and return data
        if (bpmVariableSignUpPersonnelService.getBaseMapper().selectCount(new QueryWrapper<BpmVariableSignUpPersonnel>()
                .eq("variable_id", variableId)
                .eq("element_id", nextElementId)) > 0) {
            List<String> nextNodeApproveds = Lists.newArrayList();
            nextNodeApproveds.addAll(bpmVariableSignUpPersonnelService.getBaseMapper().selectList(new QueryWrapper<BpmVariableSignUpPersonnel>()
                            .eq("variable_id", variableId)
                            .eq("element_id", nextElementId))
                    .stream()
                    .map(BpmVariableSignUpPersonnel::getAssignee)
                    .collect(Collectors.toList()));
            return nextNodeApproveds;
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * do send templated messages
     *
     * @param vo
     */
    private void doSendTemplateMessages(BpmVariableMessageVo vo) {


        //if next node's approvers is empty then query current tasks instead
        if (CollectionUtils.isEmpty(vo.getNextNodeApproveds())) {
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(vo.getProcessInsId()).list();
            if (!ObjectUtils.isEmpty(tasks)) {
                vo.setNextNodeApproveds(tasks.stream().map(Task::getAssignee).distinct().collect(Collectors.toList()));
            }
        }

        if (Objects.equals(vo.getMessageType(), 1)) {//out of node messages
            List<BpmVariableMessage> bpmVariableMessages = this.getMapper().selectList(new QueryWrapper<BpmVariableMessage>()
                    .eq("variable_id", vo.getVariableId())
                    .eq("message_type", 1)
                    .eq("event_type", vo.getEventType()));
            if (!CollectionUtils.isEmpty(bpmVariableMessages)) {
                for (BpmVariableMessage bpmVariableMessage : bpmVariableMessages) {
                    doSendTemplateMessages(bpmVariableMessage, vo);
                }
            }
        } else if (Objects.equals(vo.getMessageType(), 2)) {//in node messages
            List<BpmVariableMessage> bpmVariableMessages = this.getMapper().selectList(new QueryWrapper<BpmVariableMessage>()
                    .eq("variable_id", vo.getVariableId())
                    .eq("event_type", vo.getEventType()));
            if(!StringUtils.isEmpty(vo.getElementId())){
                List<BpmVariableMessage> currentNodeVariableMessages = bpmVariableMessages
                        .stream()
                        .filter(a -> vo.getElementId().equals(a.getElementId())).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(currentNodeVariableMessages)){
                    bpmVariableMessages=currentNodeVariableMessages;//如果当前节点有节点内通知消息,则覆盖全局通用的,否则使用全局的
                }
            }
            if (!CollectionUtils.isEmpty(bpmVariableMessages)) {
                for (BpmVariableMessage bpmVariableMessage : bpmVariableMessages) {
                    doSendTemplateMessages(bpmVariableMessage, vo);
                }
            }
        }
    }
    /**
     * do send templated messages
     *
     * @param bpmVariableMessage
     */
    private void doSendTemplateMessages(BpmVariableMessage bpmVariableMessage, BpmVariableMessageVo vo) {

        BpmnTemplateVo bpmnTemplateVo = new BpmnTemplateVo();
        if (!ObjectUtils.isEmpty(bpmVariableMessage.getContent())) {
            bpmnTemplateVo = JSON.parseObject(bpmVariableMessage.getContent(), BpmnTemplateVo.class);
        }


        //query sender's info
        List<String> sendToUsers = getSendToUsers(vo, bpmnTemplateVo);


        //if senders is empty then return
        if (ObjectUtils.isEmpty(sendToUsers)) {
            return;
        }

        List<DetailedUser> detailedUserDetailByIds = employeeService.getEmployeeDetailByIds(sendToUsers.stream().distinct().collect(Collectors.toList()));
        if(ObjectUtils.isEmpty(detailedUserDetailByIds)){
            return;
        }

        //send messages
        sendMessage(vo, bpmnTemplateVo, detailedUserDetailByIds);

    }

    /**
     * send messages
     *
     * @param vo
     * @param bpmnTemplateVo
     * @param detailedUsers
     */
    private void sendMessage(BpmVariableMessageVo vo, BpmnTemplateVo bpmnTemplateVo, List<DetailedUser> detailedUsers) {
        //query all types of the messages
        List<MessageSendTypeEnum> messageSendTypeEnums = bpmProcessNoticeService.processNoticeList(vo.getFormCode())
                .stream()
                .map(o -> {
                    return MessageSendTypeEnum.getEnumByCode(o.getType());
                })
                .collect(Collectors.toList());

        List<BaseNumIdStruVo> messageSendTypeList = bpmnTemplateVo.getMessageSendTypeList();
        if(!messageSendTypeEnums.isEmpty()&&!CollectionUtils.isEmpty(messageSendTypeList)){//如果有模板自身的通知方式,则使用模板自身的通知方式,前提是有默认通知,即默认通知关闭以后节点也不会再通知
            messageSendTypeEnums= messageSendTypeList.stream().map(a -> MessageSendTypeEnum.getEnumByCode(a.getId().intValue())).filter(Objects::nonNull).collect(Collectors.toList());
        }
        Map<Integer, String> wildcardCharacterMap = getWildcardCharacterMap(vo);
        InformationTemplateVo informationTemplateVo = informationTemplateUtils.translateInformationTemplate(InformationTemplateVo
                .builder()
                .id(bpmnTemplateVo.getTemplateId())
                .wildcardCharacterMap(wildcardCharacterMap)
                .build());

        //get message urls
        Map<String, String> urlMap = getUrlMap(vo, informationTemplateVo);
        String emailUrl = urlMap.get("emailUrl");
        String appUrl = urlMap.get("appUrl");

        for (MessageSendTypeEnum messageSendTypeEnum : messageSendTypeEnums) {
            if (Objects.isNull(messageSendTypeEnum)) {
                continue;
            }
            UserMsgUtils.sendMessageBatchNoUserMessage(detailedUsers
                    .stream()
                    .map(o -> getUserMsgBatchVo(o, informationTemplateVo.getMailTitle(), informationTemplateVo.getMailContent(),
                            vo.getTaskId(), emailUrl, appUrl,messageSendTypeEnum))
                    .collect(Collectors.toList()));
        }
    }



    /**
     * get wildcard characters map
     *
     * @param vo
     * @return
     */
    private Map<Integer, String> getWildcardCharacterMap(BpmVariableMessageVo vo) {
        Map<Integer, String> wildcardCharacterMap = Maps.newHashMap();
        for (WildcardCharacterEnum wildcardCharacterEnum : WildcardCharacterEnum.values()) {
            if (StringUtils.isEmpty(wildcardCharacterEnum.getFilName())) {
                continue;
            }
            Object property = BeanUtil.pojo.getProperty(vo, wildcardCharacterEnum.getFilName());
            if (property!=null) {
                if (wildcardCharacterEnum.getIsSearchEmpl()) {
                    if (property instanceof List) {
                        List<String> propertys = (List<String>) property;
                        if (ObjectUtils.isEmpty(propertys)) {
                            continue;
                        }
                        List<String> emplNames = employeeService.queryUserByIds(propertys)
                                .stream()
                                .map(BaseIdTranStruVo::getName).collect(Collectors.toList());
                        if (!ObjectUtils.isEmpty(emplNames)) {
                            wildcardCharacterMap.put(wildcardCharacterEnum.getCode(), StringUtils.join(emplNames, ","));
                        }
                    } else {
                        if(!property.toString().equals("0")){
                            BaseIdTranStruVo employee = employeeService.getById(property.toString());
                            if (employee!=null) {
                                wildcardCharacterMap.put(wildcardCharacterEnum.getCode(), employee.getName());
                            }
                        }
                    }
                } else {
                    wildcardCharacterMap.put(wildcardCharacterEnum.getCode(), property.toString());
                }
            }
        }
        return wildcardCharacterMap;
    }

    /**
     * get sender's info
     *
     * @param vo
     * @param bpmnTemplateVo
     * @return
     */
    private List<String> getSendToUsers(BpmVariableMessageVo vo, BpmnTemplateVo bpmnTemplateVo) {
        List<String> sendUsers = Lists.newArrayList();
        //specified assignees
        if (!ObjectUtils.isEmpty(bpmnTemplateVo.getEmpIdList())) {
            sendUsers.addAll(new ArrayList<>(bpmnTemplateVo.getEmpIdList()));
        }

        //specified roles
        if (!CollectionUtils.isEmpty(bpmnTemplateVo.getRoleIdList())) {
            List<BaseIdTranStruVo> users = roleService.queryUserByRoleIds(bpmnTemplateVo.getRoleIdList());
            if (!CollectionUtils.isEmpty(users)) {
                sendUsers.addAll(users.stream().map(BaseIdTranStruVo::getId).collect(Collectors.toList()));
            }
        }

        //todo functions
        //node sign up users
        if (!CollectionUtils.isEmpty(vo.getSignUpUsers())) {
            sendUsers.addAll(vo.getSignUpUsers());
        }

        //forwarded
        List<String> forwardUsers = null;
        List<BpmProcessForward> bpmProcessForwards = bpmProcessForwardService.list(new QueryWrapper<BpmProcessForward>()
                .eq("processInstance_Id", vo.getProcessInsId()));
        if (!CollectionUtils.isEmpty(vo.getForwardUsers()) && !CollectionUtils.isEmpty(bpmProcessForwards)) {
            forwardUsers = Lists.newArrayList();
            forwardUsers.addAll(vo.getForwardUsers());
            forwardUsers.addAll(bpmProcessForwards.stream().map(o -> String.valueOf(o.getForwardUserId())).distinct().collect(Collectors.toList()));
            forwardUsers = forwardUsers.stream().distinct().collect(Collectors.toList());
        } else if (CollectionUtils.isEmpty(vo.getForwardUsers()) && !CollectionUtils.isEmpty(bpmProcessForwards)) {
            forwardUsers = Lists.newArrayList();
            forwardUsers.addAll(bpmProcessForwards.stream().map(o -> String.valueOf(o.getForwardUserId())).distinct().collect(Collectors.toList()));
            forwardUsers = forwardUsers.stream().distinct().collect(Collectors.toList());
        } else if (!CollectionUtils.isEmpty(vo.getForwardUsers()) && CollectionUtils.isEmpty(bpmProcessForwards)) {
            forwardUsers = Lists.newArrayList();
            forwardUsers.addAll(vo.getForwardUsers());
            forwardUsers = forwardUsers.stream().distinct().collect(Collectors.toList());
        }
        vo.setForwardUsers(forwardUsers);

        //inform users
        if (!CollectionUtils.isEmpty(bpmnTemplateVo.getInformIdList())) {
            for (String informId : bpmnTemplateVo.getInformIdList()) {
                InformEnum informEnum = InformEnum.getEnumByByCode(Integer.parseInt(informId));
                if(informEnum==InformEnum.ASSIGNED_USER||informEnum==InformEnum.ASSIGNEED_ROLES){
                    continue;
                }
                //todo check whether the result is valid
                Object filObject = BeanUtil.pojo.getProperty(vo, informEnum.getFilName());
                if (filObject instanceof List) {
                    sendUsers.addAll((List) filObject);
                } else if (filObject!=null) {
                    sendUsers.add(filObject.toString());
                }
            }
        }
        return sendUsers;
    }

    /**
     * get user messages in batch
     *
     * @param o
     * @param title
     * @param content
     * @param taskId
     * @param emailUrl
     * @param appUrl
     * @param messageSendTypeEnum
     * @return
     */
    private UserMsgBatchVo getUserMsgBatchVo(DetailedUser o, String title, String content, String taskId,
                                             String emailUrl, String appUrl, MessageSendTypeEnum messageSendTypeEnum) {
        return UserMsgBatchVo
                .builder()
                .userMsgVo(UserMsgVo
                        .builder()
                        .userId(o.getId())
                        .email(o.getEmail())
                        .mobile(o.getMobile())
                        .title(title)
                        .content(content)
                        .emailUrl(emailUrl)
                        .url(emailUrl)
                        .appPushUrl(appUrl)
                        .taskId(taskId)
                        .build())
                .messageSendTypeEnums(Lists.newArrayList(messageSendTypeEnum))
                .build();
    }
}
