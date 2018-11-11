/**
 * Copyright &copy; 2012-2016 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.thinkgem.jeesite.modules.sys.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.thinkgem.jeesite.common.config.Global;
import com.thinkgem.jeesite.common.servlet.ValidateCodeServlet;
import com.thinkgem.jeesite.common.utils.Encodes;
import com.thinkgem.jeesite.common.utils.SpringContextHolder;
import com.thinkgem.jeesite.common.web.Servlets;
import com.thinkgem.jeesite.modules.sys.entity.Menu;
import com.thinkgem.jeesite.modules.sys.entity.Role;
import com.thinkgem.jeesite.modules.sys.entity.User;
import com.thinkgem.jeesite.modules.sys.service.SystemService;
import com.thinkgem.jeesite.modules.sys.utils.LogUtils;
import com.thinkgem.jeesite.modules.sys.utils.UserUtils;
import com.thinkgem.jeesite.modules.sys.web.LoginController;

/**
 * 系统安全认证实现类
 * @author ThinkGem
 * @version 2014-7-5
 org.apache.shiro.realm.AuthorizingRealm api doc:
 public abstract class AuthorizingRealm extends AuthenticatingRealm
                                        implements Authorizer, Initializable, PermissionResolverAware, RolePermissionResolverAware

 An AuthorizingRealm extends the AuthenticatingRealm's capabilities by adding Authorization (access control) support.

 This implementation will perform all role and permission checks automatically (and subclasses do not have to write this logic)
 as long as the getAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection) method returns an AuthorizationInfo.
 Please see that method's JavaDoc for an in-depth explanation.

 If you find that you do not want to utilize the AuthorizationInfo construct,
 you are of course free to subclass the AuthenticatingRealm directly instead and implement the remaining Realm interface methods directly.
 You might do this if you want have better control over how the Role and Permission checks occur for your specific data source.
 However, using AuthorizationInfo (and its default implementation SimpleAuthorizationInfo) is sufficient in the large majority of Realm cases.
 */
@Service
//@DependsOn({"userDao","roleDao","menuDao"})
public class SystemAuthorizingRealm extends AuthorizingRealm {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private SystemService systemService;			//【扣】为何不直接wire进来？还要后面搞个方法SpringContextHolder.getBean来？
	
	public SystemAuthorizingRealm() {
		this.setCachingEnabled(false);	//不用this也行啊？
		// CachingRealm.setCachingEnabled: Sets whether or not caching should be used if a CacheManager has been configured.
		//                                 即 whether or not to globally enable caching for this realm.
		// 不对realm用cache的意义是？【扣】
	}

	/*
	AuthenticatingRealm.getAuthenticationInfo (thinkGem未改写) 负责call后面的doGetAuthenticationInfo，credential的match工作在此中实作，其实现逻辑(api doc搬过来的)：
	public final AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException		★！final哦！★
	This implementation functions as follows:
	①It attempts to acquire any cached AuthenticationInfo corresponding to the specified AuthenticationToken argument.
	  If a cached value is found, it will be used for credentials matching, alleviating the need to perform any lookups with a data source.
	②If there is no cached AuthenticationInfo found, delegate to the doGetAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken) method to perform the actual lookup.
	  If authentication caching is enabled and possible, any returned info object will be cached to be used in future authentication attempts.
	③If an AuthenticationInfo instance is not found in the cache or by lookup, null is returned to indicate an account cannot be found.
	④If an AuthenticationInfo instance is found (either cached or via lookup),
	  ensure the submitted AuthenticationToken's credentials match the expected AuthenticationInfo's credentials using the credentialsMatcher.【★实作credentials matching的是CredentialsMatcher】
	  This means that credentials are always verified for an authentication attempt.
	  【Interface CredentialsMatcher 仅一个method：】boolean	doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info)
												    Returns true if the provided token credentials match the stored account credentials, false otherwise.
	*/

	/**
	 * 认证 回调函数, 登录时调用    (【猜】此callback返回的AuthenticationInfo，其中有credentials，用于和 AuthenticatingFilter.createToken子类实现method 所产生的token中的credentials比较)
	 原待改写的abstract method  :  AuthenticatingRealm.doGetAuthenticationInfo api doc:
	 protected abstract AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException

	 Retrieves authentication data from an implementation-specific datasource (RDBMS, LDAP, etc) for the given authentication token.

	 For most datasources, this means just 'pulling' authentication data for an associated subject/user and nothing more and letting Shiro do the rest.
	 But in some systems, this method could actually perform EIS specific log-in logic in addition to just retrieving data - it is up to the Realm implementation.

	 A null return value means that no account could be associated with the specified token.

	 Parameters:
	 token - the authentication token containing the user's principal and credentials.
	 Returns:
	 an AuthenticationInfo object containing account data resulting from the authentication ONLY if the lookup is successful (i.e. account exists and is valid, etc.)
	 Throws:
	 AuthenticationException - if there is an error acquiring data or performing realm-specific authentication logic for the specified token
	 */
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) {	//【鸣】不受检异常不要求catch、不要求throws，thingkGem没注明，后面有些方法同样如此；但shiro的源码和api doc都注明了，个人认为注名比较好，至少暗示上层代码会catch...

		UsernamePasswordToken token = (UsernamePasswordToken) authcToken;
		
		int activeSessionSize = getSystemService().getSessionDao().getActiveSessions(false).size();
		if (logger.isDebugEnabled()){
			logger.debug("login submit, active session size: {}, username: {}", activeSessionSize, token.getUsername());
		}
		
		// 校验登录验证码		//【鸣】前端验证不可靠；前端验证时为了快速提醒用户改正输入错误，后端验证是为了服务应用的安全和业务正确性。
		if (LoginController.isValidateCodeLogin(token.getUsername(), false, false)){
			Session session = UserUtils.getSession();
			String code = (String)session.getAttribute(ValidateCodeServlet.VALIDATE_CODE);
			if (token.getCaptcha() == null || !token.getCaptcha().toUpperCase().equals(code)){
				throw new AuthenticationException("msg:验证码错误, 请重试.");
			}
		}
		
		// 校验用户名密码
		User user = getSystemService().getUserByLoginName(token.getUsername());	//这是 到 data source / UserDao或cache 凭token中的username去取 User，不要和shiro的Subject搞混了哦...
		if (user != null) {		// != null说明存在上述login name，这以后就要比对credential了，当然是先返回AuthenticationInfo让已注册的credential matcher去比对...
			if (Global.NO.equals(user.getLoginFlag())){
				throw new AuthenticationException("msg:该已帐号禁止登录.");
			}
			byte[] salt = Encodes.decodeHex(user.getPassword().substring(0,16));
			return new SimpleAuthenticationInfo(new Principal(user, token.isMobileLogin()), //principal - the 'primary' principal associated with the specified realm.
												user.getPassword().substring(16),			//hashedCredentials - the hashed credentials that verify the given principal.	★本例password hash码 如何得到，参考SimpleHash api doc
												ByteSource.Util.bytes(salt),				//credentialsSalt - the salt used when hashing the given hashedCredentials		★
												getName());									//realmName - the realm from where the principal and credentials were acquired.
												// ↑ CachingRealm.getName : Returns the (application-unique) name assigned to this Realm. All realms configured for a single application must have a unique name.
		} else {
			return null;
		}
	}
	
	/**
	 * 获取权限授权信息，如果缓存中存在，则直接从缓存中获取，否则就重新获取， 登录成功后调用
	 原 AuthorizingRealm.getAuthorizationInfo api doc：
	 protected AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals)

	 Returns an account's authorization-specific information for the specified principals, or null if no account could be found.
	 The resulting AuthorizationInfo object is used by the other method implementations in this class to automatically perform access control checks for the corresponding Subject.

	 This implementation obtains the actual AuthorizationInfo object from the subclass's implementation of doGetAuthorizationInfo,【abstract的doGetAuthorizationInfo，待subclass提供实现】
	 and then caches it for efficient reuse if caching is enabled (see below).
	 【然而thinkGem没选择enable caching，而是自己改写了本getAuthorizationInfo，来保证(自己写死)caching，猜原因可能与doGetAuthorizationInfo中具体内容有关...】

	 Invocations of this method should be thought of as completely orthogonal(垂直;正交;互不相关) to acquiring authenticationInfo, since either could occur in any order.	★★★

	 For example, in "Remember Me" scenarios, the user identity is remembered (and assumed) for their current session and an authentication attempt during that session might never occur.
	 But because their identity would be remembered, that is sufficient enough information to call this method to execute any necessary authorization checks.
	 For this reason, authentication and authorization should be loosely coupled and not depend on each other.

	 ------Caching------
	 The AuthorizationInfo values returned from this method are cached for efficient reuse if caching is enabled.
	 Caching is enabled automatically when an authorizationCache (AuthorizingRealm.setAuthorizationCache) instance has been explicitly configured,
	                                   or if a cacheManager has been configured, which will be used to lazily create the authorizationCache as needed.
	 If caching is enabled,
	 the authorization cache will be checked first and if found, will return the cached AuthorizationInfo immediately.
	 If caching is disabled, or there is a cache miss,
	 the authorization info will be looked up from the underlying data store via the doGetAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection) method,
	 which must be implemented by subclasses.

	 ------Changed Data------
	 If caching is enabled and if any authorization data for an account is changed at runtime, such as adding or removing roles and/or permissions, the subclass implementation should clear the cached AuthorizationInfo for that account via the clearCachedAuthorizationInfo method. This ensures that the next call to getAuthorizationInfo(PrincipalCollection) will acquire the account's fresh authorization data, where it will then be cached for efficient reuse. This ensures that stale authorization data will not be reused.

	 Parameters:
	 principals - the corresponding Subject's identifying principals with which to look up the Subject's AuthorizationInfo.
	 Returns:
	 the authorization information for the account associated with the specified principals, or null if no account could be found.

	 【扣】然而再看下thinkGem下面改写的，沿路看过去，thinkGem本身还comment掉了一些code，只看留下的，结果感觉意思和原方法一样啊，感觉是没必要的改写...
	      AuthorizingRealm本身有isAuthorizationCachingEnabled、setAuthorizationCachingEnabled方法来分别查询、设置是否对AuthorizationInfo进行cache...
	 */
	protected AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals) {
		if (principals == null) {
            return null;
        }
		
        AuthorizationInfo info = null;

        info = (AuthorizationInfo)UserUtils.getCache(UserUtils.CACHE_AUTH_INFO);

        if (info == null) {
            info = doGetAuthorizationInfo(principals);
            if (info != null) {
            	UserUtils.putCache(UserUtils.CACHE_AUTH_INFO, info);
            }
        }

        return info;
	}

	/**
	 * 授权查询回调函数, 进行鉴权但缓存中无用户的授权信息时调用
	 原待改写的 abstract method： AuthorizingRealm.doGetAuthorizationInfo api doc：
	 protected abstract AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals)

	 Retrieves the AuthorizationInfo for the given principals from the underlying data store.
	 When returning an instance from this method,
	 you might want to consider using an instance of SimpleAuthorizationInfo, as it is suitable in most cases.

	 Parameters:
	 principals - the primary identifying principals of the AuthorizationInfo that should be retrieved.
	 Returns:
	 the AuthorizationInfo associated with this principals.
	 */
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		Principal principal = (Principal) getAvailablePrincipal(principals);
		// 获取当前已登录的用户
		if (!Global.TRUE.equals(Global.getConfig("user.multiAccountLogin"))){//若 不允许 多点同时登录
			Collection<Session> sessions = getSystemService().getSessionDao().getActiveSessions(true, principal, UserUtils.getSession());
			if (sessions.size() > 0){
				// isAuthenticated 如果是登录进来的，则踢出已在线用户(因为也有可能是rememberme进来的)
				if (UserUtils.getSubject().isAuthenticated()){
					for (Session session : sessions){
						getSystemService().getSessionDao().delete(session);
					}
				}
				// 记住我进来的，并且当前用户已登录，则退出当前用户提示信息。
				else{
					UserUtils.getSubject().logout();
					throw new AuthenticationException("msg:账号已在其它地方登录，请重新登录。");	//BaseController中有	@ExceptionHandler({AuthenticationException.class})...
				}
			}
		}

		User user = getSystemService().getUserByLoginName(principal.getLoginName());
		if (user != null) {		//对应后面的 info.addStringPermission("user") 吧？
			SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
			List<Menu> list = UserUtils.getMenuList();
			for (Menu menu : list){
				if (StringUtils.isNotBlank(menu.getPermission())){		//【猜】permission都是记录在 虚拟的 末级(一般四级)menu item 的 permission field【参见sys_menu表】
					// 添加基于Permission的权限信息
					for (String permission : StringUtils.split(menu.getPermission(),",")){
						info.addStringPermission(permission);
					}
				}
			}
			// 添加用户权限
			info.addStringPermission("user");
			// 添加用户角色信息
			for (Role role : user.getRoleList()){
				info.addRole(role.getEnname());
			}
			// 更新登录IP和时间
			getSystemService().updateUserLoginInfo(user);
			// 记录登录日志
			LogUtils.saveLog(Servlets.getRequest(), "系统登录");
			return info;
		} else {
			return null;
		}
	}
	
	@Override
	protected void checkPermission(Permission permission, AuthorizationInfo info) {
		authorizationValidate(permission);
		super.checkPermission(permission, info);
	}
	
	@Override
	protected boolean[] isPermitted(List<Permission> permissions, AuthorizationInfo info) {
		if (permissions != null && !permissions.isEmpty()) {
            for (Permission permission : permissions) {
        		authorizationValidate(permission);
            }
        }
		return super.isPermitted(permissions, info);
	}
	
	@Override
	public boolean isPermitted(PrincipalCollection principals, Permission permission) {
		authorizationValidate(permission);
		return super.isPermitted(principals, permission);
	}
	
	@Override
	protected boolean isPermittedAll(Collection<Permission> permissions, AuthorizationInfo info) {
		if (permissions != null && !permissions.isEmpty()) {
            for (Permission permission : permissions) {
            	authorizationValidate(permission);
            }
        }
		return super.isPermittedAll(permissions, info);
	}
	
	/**
	 * 授权验证方法
	 * @param permission
	 */
	private void authorizationValidate(Permission permission){
		// 模块授权预留接口
	}
	
	/**
	 * 设定密码校验的Hash算法与迭代次数
	 */
	/*
	The JSR-250 @PostConstruct and @PreDestroy annotations are generally considered best practice for receiving lifecycle callbacks in a modern Spring application.
	Using these annotations means that your beans are not coupled to Spring specific interfaces.

	@Documented
	 @Retention(value=RUNTIME)
	 @Target(value=METHOD)
	public @interface PostConstruct
	The PostConstruct annotation is used on a method that needs to be executed after dependency injection is done to perform any initialization.
	This method MUST be invoked before the class is put into service.
	This annotation MUST be supported on all classes that support dependency injection.
	The method annotated with PostConstruct MUST be invoked even if the class does not request any resources to be injected.
	Only one method can be annotated with this annotation.
	The method on which the PostConstruct annotation is applied MUST fulfill all of the following criteria:
		● The method MUST NOT have any parameters except in the case of interceptors in which case it takes an InvocationContext object as defined by the Interceptors specification.
		● The method defined on an interceptor class MUST HAVE one of the following signatures:
				□ void <METHOD>(InvocationContext)
				□ Object <METHOD>(InvocationContext) throws Exception
		  Note: A PostConstruct interceptor method must not throw application exceptions,
			    but it may be declared to throw checked exceptions including the java.lang.Exception
			    	if the same interceptor method interposes on business or timeout methods in addition to lifecycle events.
			    If a PostConstruct interceptor method returns a value, it is ignored by the container.
		● The method defined on a non-interceptor class MUST HAVE the following signature:
				□ void <METHOD>()
		● The method on which PostConstruct is applied MAY be public, protected, package private or private.
		● The method MUST NOT be static except for the application client.
		● The method MAY be final.
		● If the method throws an unchecked exception the class MUST NOT be put into service except in the case of EJBs where the EJB can handle exceptions and even recover from them.
	*/
	@PostConstruct
	public void initCredentialsMatcher() {	//【猜】这里的pwd的hash algorithm、hash iterations与SystemService的entryptPassword应保持(也确实)一致哦，能否考虑封成一个method？这样的话要改改一处即可...还有个salt，在new SimpleAuthenticationInfo(...)时也应与SystemService的entryptPassword中salt的设置保持一致...
		HashedCredentialsMatcher matcher = new HashedCredentialsMatcher(SystemService.HASH_ALGORITHM);	//HashedCredentialsMatcher 的 api doc 值得一读...
		matcher.setHashIterations(SystemService.HASH_INTERATIONS);
		setCredentialsMatcher(matcher);		//AuthenticatingRealm.setCredentialsMatcher
	}
	
//	/**
//	 * 清空用户关联权限认证，待下次使用时重新加载
//	 */
//	public void clearCachedAuthorizationInfo(Principal principal) {
//		SimplePrincipalCollection principals = new SimplePrincipalCollection(principal, getName());
//		clearCachedAuthorizationInfo(principals);
//	}

	/**
	 * 清空所有关联认证
	 * @Deprecated 不需要清空，授权缓存保存到session中
	 */
	@Deprecated
	public void clearAllCachedAuthorizationInfo() {
//		Cache<Object, AuthorizationInfo> cache = getAuthorizationCache();
//		if (cache != null) {
//			for (Object key : cache.keys()) {
//				cache.remove(key);
//			}
//		}
	}

	/**
	 * 获取系统业务对象
	 */
	public SystemService getSystemService() {
		if (systemService == null){
			systemService = SpringContextHolder.getBean(SystemService.class);
		}
		return systemService;
	}
	
	/**
	 * 授权用户信息
	 */
	public static class Principal implements Serializable {		//【鸣】(public) static inner class，外层enclosing class仅起namespace作用...

		private static final long serialVersionUID = 1L;
		
		private String id; // 编号
		private String loginName; // 登录名
		private String name; // 姓名
		private boolean mobileLogin; // 是否手机登录
		
//		private Map<String, Object> cacheMap;

		public Principal(User user, boolean mobileLogin) {
			this.id = user.getId();
			this.loginName = user.getLoginName();
			this.name = user.getName();
			this.mobileLogin = mobileLogin;
		}

		public String getId() {
			return id;
		}

		public String getLoginName() {
			return loginName;
		}

		public String getName() {
			return name;
		}

		public boolean isMobileLogin() {
			return mobileLogin;
		}

//		@JsonIgnore
//		public Map<String, Object> getCacheMap() {
//			if (cacheMap==null){
//				cacheMap = new HashMap<String, Object>();
//			}
//			return cacheMap;
//		}

		/**
		 * 获取SESSIONID
		  【鸣】在Principal instance上call thinkGem封装的 getSession() 即内部： subject.getSession(false) 肯定不会返回null，因为Principal只有一个有参constructor，传入的是User（保证已登入了？）
		 */
		public String getSessionid() {
			try{
				return (String) UserUtils.getSession().getId();
			}catch (Exception e) {
				return "";
			}
		}
		
		@Override
		public String toString() {
			return id;
		}

	}
}
