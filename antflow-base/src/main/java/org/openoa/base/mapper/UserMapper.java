package org.openoa.base.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.openoa.base.entity.DetailedUser;
import org.openoa.base.vo.BaseIdTranStruVo;
import org.openoa.base.vo.TaskMgmtVO;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


@Mapper
public interface UserMapper {
    // a nonce method
    List<BaseIdTranStruVo> queryCompanyByNameFuzzy(@Param("companyName") String companyName);
    //must be implemented
    List<BaseIdTranStruVo> queryByNameFuzzy(@Param("userName") String userName);
    //must be implemented
    List<BaseIdTranStruVo> queryByIds(@Param("userIds") Collection<String> userIds);
    //must be implemented
    DetailedUser getEmployeeDetailById(@Param("employeeId") String id);
    //must be implemented
    List<DetailedUser> getEmployeeDetailByIds(@Param("employeeIds")Collection<String> ids);
    long checkEmployeeEffective(@Param("employeeId") String id);

    //if you want to use level leader sign functions,you must implement it
    List<BaseIdTranStruVo> getLevelLeadersByEmployeeIdAndTier(@Param("employeeId") String employeeId,@Param("tier") Integer tier);
    List<BaseIdTranStruVo> getLevelLeadersByEmployeeIdAndEndGrade(@Param("employeeId") String employeeId,@Param("endGrade") Integer tier);
    List<BaseIdTranStruVo> queryHrpbByEmployeeIds(@Param("employeeIds") List<String> employeeId);
    List<BaseIdTranStruVo> queryDirectLeaderByEmployeeIds(@Param("employeeIds") List<String> employeeIds);

    LinkedList<BaseIdTranStruVo> selectAll(@Param("roleId") Integer roleId);

    List<BaseIdTranStruVo> selectUserPageList(Page page, @Param("vo") TaskMgmtVO taskMgmtVO);

    BaseIdTranStruVo getLeaderByLeventDepartment(@Param("startUserId") String startUserId,@Param("assignLevelGrade")Integer departmentLevel);

    List<BaseIdTranStruVo> queryDepartmentLeaderByIds(List<String> employeeIds);
}
