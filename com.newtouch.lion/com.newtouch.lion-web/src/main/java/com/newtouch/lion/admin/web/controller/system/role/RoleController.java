/*
* Copyright (c)  2015, Newtouch
* All rights reserved. 
*
* $id: RoleController.java 9552 2015年1月7日 上午12:06:04 WangLijun$
*/
package com.newtouch.lion.admin.web.controller.system.role; 

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.newtouch.lion.admin.web.model.system.role.RoleVo;
import com.newtouch.lion.common.lang.LongUtils;
import com.newtouch.lion.json.JSONParser;
import com.newtouch.lion.model.system.Attributes;
import com.newtouch.lion.model.system.Group;
import com.newtouch.lion.model.system.Resource;
import com.newtouch.lion.model.system.Role;
import com.newtouch.lion.model.system.User;
import com.newtouch.lion.query.QueryCriteria;
import com.newtouch.lion.service.system.GroupService;
import com.newtouch.lion.service.system.ResourceService;
import com.newtouch.lion.service.system.RoleService;
import com.newtouch.lion.service.system.UserService;
import com.newtouch.lion.tree.State;
import com.newtouch.lion.tree.TreeNode;
import com.newtouch.lion.web.constant.ConstantMessage;
import com.newtouch.lion.web.controller.AbstractController;
import com.newtouch.lion.web.servlet.view.support.BindMessage;
import com.newtouch.lion.web.servlet.view.support.BindResult;

/**
 * <p>
 * Title: 角色控制器
 * </p>
 * <p>
 * Description: 包括角色查询、添加、删除、修改
 * </p>
 * <p>
 * Copyright: Copyright (c) 2015
 * </p>
 * <p>
 * Company: Newtouch
 * </p>
 * 
 * @author WangLijun
 * @version 1.0
 */
@Controller
@RequestMapping("/system/role")
public class RoleController extends AbstractController{
	/**默认排序字段名称*/
	private static final String DEFAULT_ORDER_FILED_NAME="id";
	/**默认DataGrid表格名称*/
	private static final String DEFAULT_TABLE_ID="sys_rolelist_tb";
	/**角色关联用户列表*/
	private static final String  AUTHROLE_USER_TABLE_ID="sys_authrole_userlist_tb";
	/**角色关联用户组列表*/
	private static final String AUTHROLE_USERGROUPS_TABLE_ID="sys_authrole_grouplist_tb";
	/**角色所有用户组列表*/
	private static final String AUTHROLE_GROUPS_TB="sys_authrole_groups_tb";
	/**角色所有用户列表*/
	private static final String AUTHROLE_USERS_TB="sys_authrole_users_tb";
	/**系统角色首页*/
	private static final String INDEX_RETURN="index";
	/**系统角色添加*/
	private static final String ADD_DIALOG_RETURN="adddialog";
	/**系统角色编辑*/
	private static final String EDIT_DIALOG_RETURN="editdialog";
	/**系角色授权*/
	private static final String AUTH_DIALOG_RETURN="authdialog";
	
	@Autowired
	private RoleService  roleService;
	@Autowired
	private ResourceService  resourceService;
	@Autowired
	private GroupService groupService;
	@Autowired
	private UserService  userService;
	/**显示首页服务*/
	@RequestMapping("/index")
	public String index(){
		return INDEX_RETURN;
	}
	
	@RequestMapping(value = "adddialog")
	public String addDialog(){
		return ADD_DIALOG_RETURN;
	}
	
	@RequestMapping(value="add")
    @ResponseBody
	public ModelAndView add(@Valid @ModelAttribute("role") RoleVo  roleVo,Errors  errors,ModelAndView modelAndView){    	
     
    	if(errors.hasErrors()){
    		modelAndView.addObject(BindMessage.ERRORS_MODEL_KEY,errors);
    		return modelAndView;
    	}    	
    	Role  role=new Role();
     
    	BeanUtils.copyProperties(roleVo,role);
    	roleService.doCreateRole(role);
    	Map<String,String> params=new  HashMap<String,String>();
    	params.put(BindResult.SUCCESS,ConstantMessage.ADD_SUCCESS_MESSAGE_CODE);
    	modelAndView.addObject(BindMessage.SUCCESS,params);
    	return modelAndView;
	}
	
	@RequestMapping(value = "authdialog")
	public String authDialog(@RequestParam(required=false) Long id,Model model){
		if(id!=null){
			Role role=roleService.doFindById(id);
			model.addAttribute("role",role);		
		}else{
			logger.error("Eidt Object id is not null!");
		}
		return AUTH_DIALOG_RETURN;
	}
	/**加载角色所有关联用户列表*/
	@RequestMapping(value = "authusers")
	@ResponseBody
	public String authUsersForRole(@RequestParam(required=false) Long id){
		return this.roleService.doFindAuthUsersById(id, AUTHROLE_USER_TABLE_ID);
	}
	/**加载角色所有关联用户组列表*/
	@RequestMapping(value = "authusergroups")
	@ResponseBody
	public String authUserGroupsForRole(@RequestParam(required=false) Long id){
		return this.roleService.doFindAuthUserGroupsById(id, AUTHROLE_USERGROUPS_TABLE_ID);
	}
	
	/**所有资源列表*/
	@RequestMapping(value = "resources")
	@ResponseBody
	public  String  resources(@RequestParam(required=false) Long roleId){
		List<Resource>  resources=this.resourceService.doFindFirstLevel();
		
		Set<String>  properties=new HashSet<String>();		
		properties.add("id");		
		properties.add("text");		
		properties.add("checked");	
		properties.add("state");		
		properties.add("path");		
		properties.add("attributes");		
		properties.add("children");
		List<TreeNode> treeNodes=new ArrayList<TreeNode>();
		for(Resource resource:resources){
			Boolean checked=Boolean.FALSE;
			Attributes attributes=new Attributes();			
			attributes.setPath(resource.getPath());			
			attributes.setTarget(resource.getTarget());
			for(Role role:resource.getRoles()){
				if(roleId==role.getId()){
					checked=Boolean.TRUE;
					continue;
				}
			}
			List<TreeNode> children=this.resourceAttr(resource.getResources(),roleId);
			Long id=resource.getId();
			String text=resource.getNameZh();
			int  orderId=resource.getSeqNum();
			boolean isLeaf=false;
			logger.info("children.size()"+children.size());
			TreeNode  treeNode=new TreeNode(id,text,State.open,checked,"",orderId,isLeaf,attributes,children);
			treeNodes.add(treeNode);
		}
		return JSONParser.toJSONString(treeNodes,properties);
	}
	
	
	/**显示所有用户组列表*/
	@RequestMapping(value="groups")
	@ResponseBody
	public String groups(){
		QueryCriteria  queryCriteria=new QueryCriteria();
		queryCriteria.setPageSize(99999999);
		return this.groupService.doFindAuthGroups(queryCriteria,AUTHROLE_GROUPS_TB);
	}
	/**显示所有用户列表*/
	@RequestMapping(value="users")
	@ResponseBody
	public String users(){
		QueryCriteria  queryCriteria=new QueryCriteria();
		queryCriteria.setPageSize(99999999);
		return this.userService.doFindAllByCriteria(queryCriteria,AUTHROLE_USERS_TB);
	}
	
	/**为角色添加用户集合*/
	@RequestMapping(value="addusers")
	@ResponseBody
	public ModelAndView addusers(@RequestParam(required=false) Long roleId,@RequestParam(required=false) String userId,ModelAndView modelAndView){
		//TODO 验证输入
		Role role=this.roleService.doGetById(roleId);
		List<Long> targetUserIds =LongUtils.parserStringToLong(userId,LongUtils.SPARATOR_COMMA);
		List<Long> deleteUserIds = new ArrayList<Long>();
		for(User user:role.getUsers()){
			if(targetUserIds.contains(user.getId())){
				targetUserIds.remove(user.getId());
			}else{
				deleteUserIds.add(user.getId());
			}
		}
		
		if(logger.isInfoEnabled()){
			logger.info("targetUserIds[]：" + targetUserIds.toString());
			logger.info("deleteUserIds[]:" + deleteUserIds.toString());
		}
		this.roleService.doAuthUsersToRole(targetUserIds, deleteUserIds, role);
		Map<String,String> params=new  HashMap<String,String>();
    	params.put(BindResult.SUCCESS,ConstantMessage.ADD_SUCCESS_MESSAGE_CODE);
    	modelAndView.addObject(BindMessage.SUCCESS,params);
		return modelAndView;
	}
	
	/**为角色添加用户集合*/
	@RequestMapping(value="addgroups")
	@ResponseBody
	public ModelAndView addgroups(@RequestParam(required=false) Long roleId,@RequestParam(required=false) String groupId,ModelAndView modelAndView){
		//TODO 验证输入
		Role role=this.roleService.doGetById(roleId);
		List<Long> targetGroupIds =LongUtils.parserStringToLong(groupId,LongUtils.SPARATOR_COMMA);
		List<Long> deleteGroupIds = new ArrayList<Long>();
		for(Group group:role.getGroups()){
			if(targetGroupIds.contains(group.getId())){
				targetGroupIds.remove(group.getId());
			}else{
				deleteGroupIds.add(group.getId());
			}
		}
		
		if(logger.isInfoEnabled()){
			logger.info("targetGroupIds[]：" + targetGroupIds.toString());
			logger.info("deleteGroupIds[]:" + deleteGroupIds.toString());
		}
		this.roleService.doAuthGroupsToRole(targetGroupIds, deleteGroupIds, role);
		Map<String,String> params=new  HashMap<String,String>();
    	params.put(BindResult.SUCCESS,ConstantMessage.ADD_SUCCESS_MESSAGE_CODE);
    	modelAndView.addObject(BindMessage.SUCCESS,params);
		return modelAndView;
	}
	
	/**为角色添加资源集合*/
	@RequestMapping(value="addresources")
	@ResponseBody
	public ModelAndView addresources(@RequestParam(required=false) Long roleId,@RequestParam(required=false) String resourceId,ModelAndView modelAndView){
		//TODO 验证输入
		Role role=this.roleService.doGetById(roleId);
		List<Long> targetResourceIds=LongUtils.parserStringToLong(resourceId, LongUtils.SPARATOR_COMMA);
		List<Long> deleteResourceIds = new ArrayList<Long>();
		for(Resource resource:role.getResources()){
			
			if(targetResourceIds.contains(resource.getId())){
				targetResourceIds.remove(resource.getId());
			}else{
				deleteResourceIds.add(resource.getId());
			}
		}
		if(logger.isInfoEnabled()){
			logger.info("targetResourceIds[]：" + targetResourceIds.toString());
			logger.info("targetResourceIds[]:" + targetResourceIds.toString());
		}
		this.roleService.doAuthResourceToRole(targetResourceIds, deleteResourceIds, role);
		Map<String,String> params=new  HashMap<String,String>();
    	params.put(BindResult.SUCCESS,ConstantMessage.ADD_SUCCESS_MESSAGE_CODE);
    	modelAndView.addObject(BindMessage.SUCCESS,params);
		return modelAndView;
	}
	
	
	
	private List<TreeNode> resourceAttr(Set<Resource> resources,Long roleId){
		List<TreeNode>  children=new ArrayList<TreeNode>();
		for(Resource resource:resources){
			Boolean checked=Boolean.FALSE;
			Attributes attributes=new Attributes();
			
			attributes.setPath(resource.getPath());
			
			attributes.setTarget(resource.getTarget());
			
			for(Role role:resource.getRoles()){
				if(roleId==role.getId()){
					checked=Boolean.TRUE;
					continue;
				}
			}
			List<TreeNode> childrenNext=new ArrayList<TreeNode>();
			
			if(resource.getResources().size()>0){
				childrenNext=this.resourceAttr(resource.getResources(),roleId);
			}
			
			Long id=resource.getId();
			String text=resource.getNameZh();
			int  orderId=resource.getSeqNum();
			boolean isLeaf=false;
			logger.info("childrenNext.size()"+childrenNext.size());
			TreeNode  treeNode=new TreeNode(id,text,State.open,checked,"",orderId,isLeaf,attributes,childrenNext);
			children.add(treeNode);
		}
		return children;
	}
	
	@RequestMapping(value="delete")
	@ResponseBody
	public ModelAndView delete(@RequestParam Long id,ModelAndView modelAndView){
    	Map<String,String> params=new  HashMap<String,String>();
    	int updateRow=this.roleService.doDeleteById(id);
    	if(updateRow>0){
    		params.put(BindResult.SUCCESS,ConstantMessage.DELETE_SUCCESS_MESSAGE_CODE);
    	}else{
    		params.put(BindResult.SUCCESS,ConstantMessage.DELETE_FAIL_MESSAGE_CODE);
    	}
    	modelAndView.addObject(BindMessage.SUCCESS,params);
    	return modelAndView;
    }
	
	@RequestMapping(value = "editdialog")
	public String editDialog(@RequestParam(required=false) Long id,Model model){
		if(id!=null){
			Role role=roleService.doFindById(id);
			model.addAttribute("role",role);		
		}else{
			logger.error("Eidt Object id is not null!");
		}
		return EDIT_DIALOG_RETURN;
	}
	
	@RequestMapping(value = "edit")
	@ResponseBody
	public ModelAndView edit(@Valid @ModelAttribute("role") RoleVo  roleVo,Errors  errors,ModelAndView modelAndView){
		
		Role role=null;
		if(roleVo.getId()!=null){
			role=this.roleService.doFindById(roleVo.getId());
			if(role==null){
				errors.reject(ConstantMessage.EDIT_ISEMPTY_FAIL_MESSAGE_CODE);
			}
    	}else{
    		errors.reject(ConstantMessage.EDIT_ISEMPTY_FAIL_MESSAGE_CODE);
    	}
		
    	if(errors.hasErrors()){
    		modelAndView.addObject(BindMessage.ERRORS_MODEL_KEY,errors);
    		return modelAndView;
    	}
    	
    	BeanUtils.copyProperties(roleVo,role);
    	roleService.doUpdate(role);
    	Map<String,String> params=new  HashMap<String,String>();
    	params.put(BindResult.SUCCESS,ConstantMessage.EDIT_SUCCESS_MESSAGE_CODE);
    	modelAndView.addObject(BindMessage.SUCCESS,params);
    	return modelAndView;
	}
	
	@RequestMapping(value = "lists")
	@ResponseBody
	public String list(HttpServletRequest servletRequest,Model model,@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="15") int rows,@RequestParam(required=false) String sort,@RequestParam(required=false) String order,@RequestParam(required=false) String nameZh){
		QueryCriteria queryCriteria=new QueryCriteria();
		
		// 设置分页 启始页
		queryCriteria.setStartIndex(rows*(page-1));
		// 每页大小
		 queryCriteria.setPageSize(rows);
		// 设置排序字段及排序方向
		if (StringUtils.isNotEmpty(sort) && StringUtils.isNotEmpty(order)) {
			queryCriteria.setOrderField(sort);
			queryCriteria.setOrderDirection(order);
		}else{
			queryCriteria.setOrderField(DEFAULT_ORDER_FILED_NAME);
			queryCriteria.setOrderDirection(QueryCriteria.ASC);
		}
		// 查询条件 参数类型
		if (StringUtils.isNotEmpty(nameZh)) {
			queryCriteria.addQueryCondition("nameZh","%"+nameZh.trim()+"%");
		}
		
		String result=this.roleService.doFindByCriteria(queryCriteria,DEFAULT_TABLE_ID);
		
		return result;
	}
}

	