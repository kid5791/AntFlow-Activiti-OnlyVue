package org.openoa.base.service;

import org.openoa.base.entity.DetailedUser;
import org.openoa.base.vo.BaseIdTranStruVo;

import java.util.Collection;
import java.util.List;

public interface AfUserService {
    List<BaseIdTranStruVo> queryByNameFuzzy(String userName);

    List<BaseIdTranStruVo> queryCompanyByNameFuzzy(String companyName);

    List<BaseIdTranStruVo> queryUserByIds(Collection<String> userIds);

    BaseIdTranStruVo getById(String id);

    List<BaseIdTranStruVo> queryLeadersByEmployeeIdAndTier(String employeeId, Integer tier);

    List<BaseIdTranStruVo> queryLeadersByEmployeeIdAndGrade(String employeeId, Integer grade);

    BaseIdTranStruVo queryLeaderByEmployeeIdAndLevel(String employeeId, Integer level);

    List<BaseIdTranStruVo> queryEmployeeHrpbByEmployeeIds(List<String> employeeIds);

    List<BaseIdTranStruVo> queryEmployeeDirectLeaderByIds(List<String> employeeIds);


    DetailedUser getEmployeeDetailById(String id);

    List<DetailedUser> getEmployeeDetailByIds(Collection<String> ids);

    long checkEmployeeEffective(String id);
    List<BaseIdTranStruVo> queryDepartmentLeaderByIds(List<String> employeeIds);
}
